package com.techindika.liveconnect.service

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.techindika.liveconnect.model.*
import java.util.Date
import java.util.UUID

/**
 * Manages conversation thread state. Observable via LiveData for UI updates.
 */
internal class ConversationManager {

    private val _threads = MutableLiveData<List<ConversationThread>>(emptyList())
    val threads: LiveData<List<ConversationThread>> = _threads

    private val _activeThreadId = MutableLiveData<String?>(null)
    val activeThreadId: LiveData<String?> = _activeThreadId

    // Maps threadId -> ticketId (API)
    private val threadToTicket = mutableMapOf<String, String>()

    /** The currently active thread. */
    val activeThread: ConversationThread?
        get() {
            val id = _activeThreadId.value ?: return null
            return _threads.value?.find { it.id == id }
        }

    /** Ticket ID for the active thread. */
    val activeTicketId: String?
        get() {
            val threadId = _activeThreadId.value ?: return null
            return threadToTicket[threadId]
        }

    /** Initialize from API ticket list. Always called on the main thread. */
    fun initializeFromTickets(tickets: List<WidgetTicket>) {
        val converted = tickets.map { convertTicketToThread(it) }
        // Use setValue (not postValue) so _threads.value is immediately correct.
        // postValue is async — a subsequent initializeWithNewThread() call would
        // read the stale empty list and overwrite the ticket threads.
        _threads.value = converted

        // Find first open ticket for active thread
        val activeTicket = tickets.firstOrNull { it.status == "open" }
        if (activeTicket != null) {
            val thread = converted.find { threadToTicket[it.id] == activeTicket.id }
            _activeThreadId.value = thread?.id
        } else {
            initializeWithNewThread()
        }
    }

    /** Create a fresh empty active thread. Always called on the main thread. */
    fun initializeWithNewThread() {
        val newThread = ConversationThread(
            id = UUID.randomUUID().toString(),
            title = "New Conversation",
            status = ConversationStatus.ACTIVE,
            messages = emptyList(),
            updatedAt = Date(),
            createdAt = Date()
        )
        val current = _threads.value.orEmpty().toMutableList()
        current.add(0, newThread)
        _threads.value = current
        _activeThreadId.value = newThread.id
    }

    /** Set the ticket ID for the active thread (after ticket:created). */
    fun setTicketIdForActiveThread(ticketId: String) {
        val threadId = _activeThreadId.value ?: return
        threadToTicket[threadId] = ticketId
    }

    /** Record when an agent was assigned to the active thread (from broadcast socket event). */
    fun setAgentAssignedAt(timestamp: Date) {
        val threadId = _activeThreadId.value ?: return
        updateThread(threadId) { it.copyWith(agentAssignedAt = timestamp) }
    }

    /** Add a message to the active thread. */
    fun addMessageToActiveThread(message: Message) {
        val threadId = _activeThreadId.value ?: return
        updateThread(threadId) { thread ->
            thread.copyWith(
                messages = thread.messages + message,
                updatedAt = Date(),
                lastMessage = message.text
            )
        }
    }

    /** Add a message to a thread identified by ticket ID. */
    fun addMessageToThreadByTicketId(ticketId: String, message: Message) {
        val threadId = threadToTicket.entries.find { it.value == ticketId }?.key
            ?: _activeThreadId.value ?: return
        updateThread(threadId) { thread ->
            thread.copyWith(
                messages = thread.messages + message,
                updatedAt = Date()
            )
        }
    }

    /** Replace an optimistic message with the server-confirmed version. */
    fun replaceOptimisticMessage(optimisticId: String, serverMessage: Message) {
        val threadId = _activeThreadId.value ?: return
        updateThread(threadId) { thread ->
            val updated = thread.messages.map {
                if (it.id == optimisticId) serverMessage else it
            }
            thread.copyWith(messages = updated)
        }
    }

    /**
     * Advance every [MessageStatus.SENDING] message in the active thread to [MessageStatus.SENT].
     * Called after [ticket:created] because the server does not echo the first message back via
     * [message:received] (it arrives through the auth payload), so the optimistic local message
     * would otherwise stay stuck at SENDING indefinitely.
     */
    fun markSendingMessagesAsSent() {
        val threadId = _activeThreadId.value ?: return
        updateThread(threadId) { thread ->
            val updated = thread.messages.map {
                if (it.status == MessageStatus.SENDING) it.copyWith(status = MessageStatus.SENT) else it
            }
            thread.copyWith(messages = updated)
        }
    }

    /** Update a single message's delivery/read status by message ID. */
    fun updateMessageStatus(messageId: String, status: MessageStatus) {
        val threadId = _activeThreadId.value ?: return
        updateThread(threadId) { thread ->
            val updated = thread.messages.map {
                if (it.id == messageId) it.copyWith(status = status) else it
            }
            thread.copyWith(messages = updated)
        }
    }

    /**
     * Bulk-update all VISITOR messages in the active thread to [status].
     * Used when the server sends a ticket-level [messages:status_updated] event
     * (no messageId — the entire ticket's messages advance to the new status).
     * Only moves status forward (SENDING < SENT < DELIVERED < READ).
     */
    fun updateAllVisitorMessagesStatus(status: MessageStatus) {
        val threadId = _activeThreadId.value ?: return
        updateThread(threadId) { thread ->
            val updated = thread.messages.map {
                if (it.sender == MessageSender.VISITOR && it.status < status)
                    it.copyWith(status = status)
                else it
            }
            thread.copyWith(messages = updated)
        }
    }

    /** Load API messages into a thread. */
    fun updateThreadMessages(threadId: String, messages: List<TicketMessage>) {
        updateThread(threadId) { thread ->
            val converted = messages
                .filter { it.type != "pin" }                      // drop pin-type messages
                .map { convertTicketMessageToMessage(it) }
                .filter { it.text.isNotBlank() || it.hasAttachment } // drop empty content

            // The "X has joined the chat" broadcast is a socket-only event — the REST
            // API messages endpoint doesn't return it. Synthesize it from ticket data
            // so it always shows up, even after an app restart.
            val agentName = thread.agentName
            val assignedAt = thread.agentAssignedAt
            val alreadyHasJoinMessage = converted.any {
                (it.sender == MessageSender.SYSTEM || it.sender == MessageSender.BROADCAST)
                && it.text.contains("joined the chat", ignoreCase = true)
            }
            val finalMessages = if (agentName.isNotBlank() && assignedAt != null && !alreadyHasJoinMessage) {
                val joinMsg = Message(
                    id = "synthetic-join-$threadId",
                    text = "$agentName has joined the chat",
                    sender = MessageSender.SYSTEM,
                    timestamp = assignedAt,
                    status = MessageStatus.READ
                )
                (converted + joinMsg).sortedBy { it.timestamp }
            } else {
                converted
            }

            thread.copyWith(messages = finalMessages)
        }
    }

    /** Mark the active thread as resolved (closed). The UI will switch to read-only mode. */
    fun markActiveThreadAsResolved() {
        val threadId = _activeThreadId.value ?: return
        val current = _threads.value.orEmpty().toMutableList()
        val index = current.indexOfFirst { it.id == threadId }
        if (index >= 0) {
            current[index] = current[index].copyWith(status = ConversationStatus.CLOSED)
            _threads.postValue(current)
        }
    }

    /** Switch to a different thread by ID. */
    fun switchToThread(threadId: String) {
        _activeThreadId.postValue(threadId)
    }

    /**
     * Update threads from a fresh API ticket list while PRESERVING the current
     * active thread. Used after marking a conversation as resolved to refresh
     * the Activity tab without losing the new welcome-message thread the user
     * is now sitting on. Mirrors Flutter's `updateThreadsFromTickets`.
     */
    fun updateThreadsFromTickets(tickets: List<WidgetTicket>) {
        val currentActiveId = _activeThreadId.value
        val currentActiveThread = _threads.value?.firstOrNull { it.id == currentActiveId }

        // Rebuild threads from the API list (this regenerates threadToTicket too)
        val newThreads = tickets.map { convertTicketToThread(it) }

        if (currentActiveId != null && currentActiveThread != null) {
            // Only prepend the existing active thread if it isn't already from the API
            // (i.e. it's a fresh client-side thread the API doesn't know about yet).
            val isClientSide = newThreads.none { it.id == currentActiveId }
            if (isClientSide) {
                _threads.postValue(listOf(currentActiveThread) + newThreads)
                _activeThreadId.postValue(currentActiveId)
                return
            }
        }
        _threads.postValue(newThreads)
    }

    /** Get ticket ID for a thread. */
    fun getTicketIdForThread(threadId: String): String? = threadToTicket[threadId]

    // ── Internal helpers ──

    private fun updateThread(threadId: String, transform: (ConversationThread) -> ConversationThread) {
        val current = _threads.value.orEmpty().toMutableList()
        val index = current.indexOfFirst { it.id == threadId }
        if (index >= 0) {
            current[index] = transform(current[index])
            // Use setValue (synchronous) so _threads.value reflects the update immediately.
            // postValue is async — rapid sequential calls (e.g. broadcast:message then
            // ticket:created) would each read the stale old value, and the last postValue
            // would clobber earlier updates (e.g. the join message would be lost).
            // All callers are already on the main thread (dispatched via mainHandler.post
            // or withContext(Dispatchers.Main)), so setValue is safe here.
            _threads.value = current
        }
    }

    private fun convertTicketToThread(ticket: WidgetTicket): ConversationThread {
        val threadId = UUID.randomUUID().toString()
        threadToTicket[threadId] = ticket.id

        val status = if (ticket.status == "open") ConversationStatus.ACTIVE else ConversationStatus.CLOSED
        val createdAt = parseIsoDate(ticket.createdAt) ?: Date()
        val updatedAt = parseIsoDate(ticket.updatedAt) ?: createdAt

        return ConversationThread(
            id = threadId,
            title = ticket.firstMessage?.take(50) ?: "Conversation",
            status = status,
            messages = emptyList(),
            agentName = ticket.agentName ?: "",
            agentStatus = ticket.agentStatus ?: "",
            agentAssignedAt = parseIsoDate(ticket.agentAssignedAt),
            updatedAt = updatedAt,
            createdAt = createdAt,
            firstMessage = ticket.firstMessage,
            lastMessage = ticket.lastMessage
        )
    }

    private fun convertTicketMessageToMessage(tm: TicketMessage): Message {
        val sender = MessageSender.fromString(tm.senderType)
        val status = MessageStatus.fromString(tm.status)

        val attachment = if (!tm.fileUrl.isNullOrEmpty()) {
            val mimeType = normalizeMimeType(tm.fileType ?: "")
            val type = if (mimeType.startsWith("image/")) AttachmentType.MEDIA else AttachmentType.DOCUMENT
            Attachment(
                filename = tm.fileName ?: "attachment",
                filePath = tm.fileUrl,
                size = 0,
                type = type,
                mimeType = mimeType
            )
        } else null

        return Message(
            id = tm.id,
            text = tm.content ?: "",
            sender = sender,
            timestamp = tm.createdAt,
            attachment = attachment,
            status = status
        )
    }

    private fun normalizeMimeType(fileType: String): String = when (fileType.lowercase()) {
        "image" -> "image/jpeg"
        "document" -> "application/pdf"
        "video" -> "video/mp4"
        else -> if (fileType.contains("/")) fileType else "application/octet-stream"
    }

    private fun parseIsoDate(dateStr: String?): Date? {
        if (dateStr.isNullOrBlank()) return null
        return try {
            val cleaned = dateStr.replace("Z", "").split(".").first()
            val sdf = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", java.util.Locale.US)
            sdf.timeZone = java.util.TimeZone.getTimeZone("UTC")
            sdf.parse(cleaned)
        } catch (_: Exception) {
            null
        }
    }
}
