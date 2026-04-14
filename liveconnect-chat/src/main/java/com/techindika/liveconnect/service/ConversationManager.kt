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

    /** Initialize from API ticket list. */
    fun initializeFromTickets(tickets: List<WidgetTicket>) {
        val converted = tickets.map { convertTicketToThread(it) }
        _threads.postValue(converted)

        // Find first open ticket for active thread
        val activeTicket = tickets.firstOrNull { it.status == "open" }
        if (activeTicket != null) {
            val thread = converted.find { threadToTicket[it.id] == activeTicket.id }
            _activeThreadId.postValue(thread?.id)
        } else {
            initializeWithNewThread()
        }
    }

    /** Create a fresh empty active thread. */
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
        _threads.postValue(current)
        _activeThreadId.postValue(newThread.id)
    }

    /** Set the ticket ID for the active thread (after ticket:created). */
    fun setTicketIdForActiveThread(ticketId: String) {
        val threadId = _activeThreadId.value ?: return
        threadToTicket[threadId] = ticketId
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

    /** Update message delivery/read status. */
    fun updateMessageStatus(messageId: String, status: MessageStatus) {
        val threadId = _activeThreadId.value ?: return
        updateThread(threadId) { thread ->
            val updated = thread.messages.map {
                if (it.id == messageId) it.copyWith(status = status) else it
            }
            thread.copyWith(messages = updated)
        }
    }

    /** Load API messages into a thread. */
    fun updateThreadMessages(threadId: String, messages: List<TicketMessage>) {
        updateThread(threadId) { thread ->
            val converted = messages.map { convertTicketMessageToMessage(it) }
            thread.copyWith(messages = converted)
        }
    }

    /** Mark the active thread as resolved and create a new one. */
    fun markActiveThreadAsResolved() {
        val threadId = _activeThreadId.value ?: return
        updateThread(threadId) { it.copyWith(status = ConversationStatus.CLOSED) }
        initializeWithNewThread()
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
            _threads.postValue(current)
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
