package com.techindika.liveconnect.socket

import android.os.Handler
import android.os.Looper
import android.util.Log
import com.techindika.liveconnect.model.*
import com.techindika.liveconnect.util.optStringOrNull
import org.json.JSONObject
import java.util.Date
import java.util.concurrent.ConcurrentHashMap

/**
 * Bridges raw Socket.IO events to typed callbacks.
 * Converts JSON payloads to data models and dispatches on the main thread.
 */
internal class SocketEventManager(private val socketService: SocketService) {

    private val mainHandler = Handler(Looper.getMainLooper())
    private var listenersRegistered = false

    // Pending message tracking for optimistic updates
    private val pendingMessages = ConcurrentHashMap<String, PendingMessageInfo>()
    private var pendingCounter = 0

    // Callbacks
    var onTicketCreated: ((TicketCreatedEvent) -> Unit)? = null
    var onTicketResumed: ((TicketResumedEvent) -> Unit)? = null
    var onTicketResolved: ((TicketResolvedEvent) -> Unit)? = null
    var onMessageReceived: ((Message) -> Unit)? = null
    var onMessageStatusUpdated: ((MessageStatusEvent) -> Unit)? = null
    var onAgentTyping: ((AgentTypingEvent) -> Unit)? = null
    var onAgentChanged: ((AgentChangedEvent) -> Unit)? = null
    var onAgentStatusChanged: ((AgentStatusEvent) -> Unit)? = null
    var onAgentReassigned: ((AgentChangedEvent) -> Unit)? = null
    var onUnreadCount: ((UnreadCountEvent) -> Unit)? = null
    var onRatePrompt: ((RatePromptEvent) -> Unit)? = null
    var onError: ((String) -> Unit)? = null

    /** Fires when the server assigns/reassigns an agent on an active ticket. */
    var onTicketAssigned: ((AgentInfo) -> Unit)? = null

    /** Fires when the underlying socket disconnects, with the disconnect reason. */
    var onSocketDisconnect: ((String) -> Unit)? = null

    /** Register all socket event listeners. Call once. */
    fun registerListeners() {
        if (listenersRegistered) return
        listenersRegistered = true

        socketService.on(SocketService.EVENT_TICKET_CREATED) { args ->
            logEvent(SocketService.EVENT_TICKET_CREATED, args)
            handleTicketCreated(args)
        }
        socketService.on(SocketService.EVENT_TICKET_RESUMED) { args ->
            logEvent(SocketService.EVENT_TICKET_RESUMED, args)
            handleTicketResumed(args)
        }
        socketService.on(SocketService.EVENT_TICKET_RESOLVED) { args ->
            logEvent(SocketService.EVENT_TICKET_RESOLVED, args)
            handleTicketResolved(args)
        }
        socketService.on(SocketService.EVENT_MESSAGE_RECEIVED) { args ->
            logEvent(SocketService.EVENT_MESSAGE_RECEIVED, args)
            handleMessageReceived(args)
        }
        socketService.on(SocketService.EVENT_MESSAGES_STATUS_UPDATED) { args ->
            logEvent(SocketService.EVENT_MESSAGES_STATUS_UPDATED, args)
            handleMessageStatusUpdated(args)
        }
        socketService.on(SocketService.EVENT_AGENT_TYPING) { args ->
            logEvent(SocketService.EVENT_AGENT_TYPING, args)
            handleAgentTyping(args)
        }
        socketService.on(SocketService.EVENT_AGENT_CHANGED) { args ->
            logEvent(SocketService.EVENT_AGENT_CHANGED, args)
            handleAgentChanged(args)
        }
        socketService.on(SocketService.EVENT_AGENT_REASSIGNED) { args ->
            logEvent(SocketService.EVENT_AGENT_REASSIGNED, args)
            handleAgentReassigned(args)
        }
        socketService.on(SocketService.EVENT_AGENT_STATUS) { args ->
            logEvent(SocketService.EVENT_AGENT_STATUS, args)
            handleAgentStatus(args)
        }
        socketService.on(SocketService.EVENT_TICKET_UNREAD_COUNT) { args ->
            logEvent(SocketService.EVENT_TICKET_UNREAD_COUNT, args)
            handleUnreadCount(args)
        }
        socketService.on(SocketService.EVENT_TICKET_RATE_PROMPT) { args ->
            logEvent(SocketService.EVENT_TICKET_RATE_PROMPT, args)
            handleRatePrompt(args)
        }
        socketService.on(SocketService.EVENT_BROADCAST_MESSAGE) { args ->
            logEvent(SocketService.EVENT_BROADCAST_MESSAGE, args)
            handleBroadcastMessage(args)
        }
        socketService.on(SocketService.EVENT_TICKET_ASSIGNED) { args ->
            logEvent(SocketService.EVENT_TICKET_ASSIGNED, args)
            handleTicketAssigned(args)
        }

        // Lifecycle hooks — bridge SocketService's connect/disconnect to typed callbacks
        socketService.onDisconnect = { reason ->
            Log.d(TAG, "Socket disconnected: $reason")
            dispatch { onSocketDisconnect?.invoke(reason) }
        }
    }

    private fun handleTicketAssigned(args: Array<Any>) {
        val json = parseJson(args) ?: return
        val agentJson = json.optJSONObject("agent") ?: return
        val agent = AgentInfo.fromJson(agentJson)
        dispatch { onTicketAssigned?.invoke(agent) }
    }

    /** Track a message sent optimistically (before server confirms). */
    fun trackPendingMessage(optimisticId: String, text: String): String {
        pendingMessages[optimisticId] = PendingMessageInfo(optimisticId, text, Date())
        return optimisticId
    }

    /** Generate a new optimistic message ID. */
    fun nextOptimisticId(): String = "local-${++pendingCounter}"

    /** Find and remove a pending message by matching content. */
    fun matchPendingMessage(content: String): PendingMessageInfo? {
        val entry = pendingMessages.entries.firstOrNull { it.value.text == content }
        return entry?.let {
            pendingMessages.remove(it.key)
            it.value
        }
    }

    /** Clear registered state for re-registration. */
    fun reset() {
        listenersRegistered = false
        pendingMessages.clear()
        pendingCounter = 0
    }

    // ── Event handlers ──

    private fun handleTicketCreated(args: Array<Any>) {
        val json = parseJson(args) ?: return
        val ticketJson = json.optJSONObject("ticket")
        val agentJson = json.optJSONObject("agent")
        val ticketId = ticketJson?.optString("_id", "") ?: ""
        val agent = agentJson?.let { AgentInfo.fromJson(it) }
        dispatch { onTicketCreated?.invoke(TicketCreatedEvent(ticketId, agent)) }
    }

    private fun handleTicketResumed(args: Array<Any>) {
        val json = parseJson(args) ?: return
        val ticketJson = json.optJSONObject("ticket")
        val agentJson = json.optJSONObject("agent")
        val ticketId = ticketJson?.optString("_id", "") ?: ""
        val agent = agentJson?.let { AgentInfo.fromJson(it) }
        dispatch { onTicketResumed?.invoke(TicketResumedEvent(ticketId, agent)) }
    }

    private fun handleTicketResolved(args: Array<Any>) {
        val json = parseJson(args) ?: return
        val ticketId = json.optString("ticketId", "")
        val resolvedBy = json.optStringOrNull("resolvedBy")
        dispatch { onTicketResolved?.invoke(TicketResolvedEvent(ticketId, resolvedBy)) }
    }

    private fun handleMessageReceived(args: Array<Any>) {
        val json = parseJson(args) ?: return
        val msgJson = json.optJSONObject("message") ?: json
        // Filter out pin-type and empty-content messages
        val type = msgJson.optString("type", "text")
        if (type == "pin") return
        val message = convertSocketMessage(msgJson)
        if (message.text.isBlank() && !message.hasAttachment) return
        dispatch { onMessageReceived?.invoke(message) }
    }

    private fun handleBroadcastMessage(args: Array<Any>) {
        val json = parseJson(args) ?: return
        val msgJson = json.optJSONObject("message") ?: json
        val type = msgJson.optString("type", "text")
        if (type == "pin") return
        val message = convertSocketMessage(msgJson).copy(
            sender = MessageSender.BROADCAST
        )
        if (message.text.isBlank() && !message.hasAttachment) return
        dispatch { onMessageReceived?.invoke(message) }
    }

    private fun handleMessageStatusUpdated(args: Array<Any>) {
        val json = parseJson(args) ?: return
        val event = MessageStatusEvent(
            messageId = json.optString("messageId", ""),
            ticketId = json.optString("ticketId", ""),
            status = json.optString("status", "sent")
        )
        dispatch { onMessageStatusUpdated?.invoke(event) }
    }

    private fun handleAgentTyping(args: Array<Any>) {
        val json = parseJson(args) ?: return
        val event = AgentTypingEvent(
            ticketId = json.optString("ticketId", ""),
            isTyping = json.optBoolean("isTyping", json.optBoolean("typing", false))
        )
        dispatch { onAgentTyping?.invoke(event) }
    }

    private fun handleAgentChanged(args: Array<Any>) {
        val json = parseJson(args) ?: return
        val agentJson = json.optJSONObject("agent") ?: json
        val agent = AgentInfo.fromJson(agentJson)
        dispatch { onAgentChanged?.invoke(AgentChangedEvent(agent)) }
    }

    private fun handleAgentReassigned(args: Array<Any>) {
        val json = parseJson(args) ?: return
        val agentJson = json.optJSONObject("agent") ?: json
        val agent = AgentInfo.fromJson(agentJson)
        dispatch { onAgentReassigned?.invoke(AgentChangedEvent(agent)) }
    }

    private fun handleAgentStatus(args: Array<Any>) {
        val json = parseJson(args) ?: return
        val event = AgentStatusEvent(
            agentId = json.optString("agentId", ""),
            status = json.optString("status", "offline")
        )
        dispatch { onAgentStatusChanged?.invoke(event) }
    }

    private fun handleUnreadCount(args: Array<Any>) {
        val json = parseJson(args) ?: return
        val event = UnreadCountEvent(
            ticketId = json.optString("ticketId", ""),
            unreadCount = json.optInt("unreadCount", 0)
        )
        dispatch { onUnreadCount?.invoke(event) }
    }

    private fun handleRatePrompt(args: Array<Any>) {
        val json = parseJson(args) ?: return
        val ticketId = json.optString("ticketId", "")
        dispatch { onRatePrompt?.invoke(RatePromptEvent(ticketId)) }
    }

    // ── Helpers ──

    /** Convert a socket message JSON to a Message model. */
    private fun convertSocketMessage(json: JSONObject): Message {
        val id = json.optString("_id", json.optString("id", ""))
        val content = json.optStringOrNull("content") ?: ""
        val senderType = MessageSender.fromString(json.optString("senderType", "system"))
        val statusStr = json.optString("status", "sent")
        val status = MessageStatus.fromString(statusStr)

        // Parse timestamp
        val createdAtStr = json.optString("createdAt", "")
        val timestamp = parseIsoDate(createdAtStr) ?: Date()

        // Parse attachment if present
        val fileUrl = json.optStringOrNull("fileUrl") ?: ""
        val fileName = json.optStringOrNull("fileName") ?: ""
        val fileType = json.optStringOrNull("fileType") ?: ""
        val attachment = if (fileUrl.isNotEmpty()) {
            val mimeType = normalizeMimeType(fileType)
            val type = if (mimeType.startsWith("image/")) AttachmentType.MEDIA else AttachmentType.DOCUMENT
            Attachment(
                filename = fileName.ifEmpty { "attachment" },
                filePath = fileUrl,
                size = 0,
                type = type,
                mimeType = mimeType
            )
        } else null

        return Message(
            id = id,
            text = content,
            sender = senderType,
            timestamp = timestamp,
            attachment = attachment,
            status = status
        )
    }

    /** Normalize short file types like "image" to proper MIME types. */
    private fun normalizeMimeType(fileType: String): String = when (fileType.lowercase()) {
        "image" -> "image/jpeg"
        "document" -> "application/pdf"
        "video" -> "video/mp4"
        else -> if (fileType.contains("/")) fileType else "application/octet-stream"
    }

    private fun parseIsoDate(dateStr: String): Date? {
        if (dateStr.isBlank()) return null
        return try {
            val cleaned = dateStr.replace("Z", "").split(".").first()
            val sdf = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", java.util.Locale.US)
            sdf.timeZone = java.util.TimeZone.getTimeZone("UTC")
            sdf.parse(cleaned)
        } catch (_: Exception) {
            null
        }
    }

    private fun parseJson(args: Array<Any>): JSONObject? {
        return try {
            when (val first = args.firstOrNull()) {
                is JSONObject -> first
                is String -> JSONObject(first)
                else -> {
                    Log.w(TAG, "Unexpected socket arg type: ${first?.javaClass}")
                    null
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to parse socket event JSON: ${e.message}")
            null
        }
    }

    private fun dispatch(action: () -> Unit) {
        mainHandler.post(action)
    }

    /** Log incoming socket events for debugging. */
    private fun logEvent(event: String, args: Array<Any>) {
        try {
            val payload = when (val first = args.firstOrNull()) {
                is JSONObject -> first.toString().take(500)
                is String -> first.take(500)
                else -> first?.toString()?.take(200) ?: "(empty)"
            }
            Log.d(TAG, "⚡ [$event] $payload")
        } catch (e: Exception) {
            Log.d(TAG, "⚡ [$event] (failed to log payload: ${e.message})")
        }
    }

    /** Pending message info for optimistic message matching. */
    data class PendingMessageInfo(
        val optimisticId: String,
        val text: String,
        val sentAt: Date
    )

    companion object {
        private const val TAG = "LiveConnect.EventMgr"
    }
}
