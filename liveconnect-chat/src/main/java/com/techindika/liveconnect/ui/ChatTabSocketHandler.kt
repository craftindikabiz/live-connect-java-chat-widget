package com.techindika.liveconnect.ui

import com.techindika.liveconnect.LiveConnectChat
import com.techindika.liveconnect.model.AgentInfo
import com.techindika.liveconnect.model.MessageStatus
import com.techindika.liveconnect.service.ConversationManager
import com.techindika.liveconnect.service.TicketStorage
import com.techindika.liveconnect.service.UnreadCountService
import com.techindika.liveconnect.socket.SocketEventManager
import com.techindika.liveconnect.socket.SocketService
import org.json.JSONObject

/**
 * Manages socket connection lifecycle and event-callback wiring for the chat tab.
 * Extracted from [ChatTabFragment] to keep the fragment focused on UI concerns.
 */
internal class ChatTabSocketHandler(
    private val conversationManager: ConversationManager,
    private val socketService: SocketService,
    private val socketEventManager: SocketEventManager,
) {
    /** The currently assigned agent, if any. */
    var currentAgent: AgentInfo? = null
        private set

    var isSocketConnected: Boolean = false
        private set

    var isSocketConnecting: Boolean = false
        private set

    /** Reset connection and agent state before starting a new conversation. */
    fun reset() {
        currentAgent = null
        isSocketConnected = false
        isSocketConnecting = false
        socketEventManager.reset()
    }

    // ── UI callbacks — set by the hosting fragment ──

    /** Called when the assigned agent changes (or is first set). */
    var onAgentUpdated: (() -> Unit)? = null

    /** Called when the socket disconnects unexpectedly. */
    var onDisconnected: ((reason: String) -> Unit)? = null

    /** Called when the server resolves the active ticket. */
    var onTicketResolved: (() -> Unit)? = null

    /** Called when the server requests a rating from the visitor. */
    var onRatePrompted: (() -> Unit)? = null

    // ── Connection ──

    fun connectToResume(ticketId: String) {
        if (isSocketConnecting || isSocketConnected) return
        isSocketConnecting = true

        val widgetKey = LiveConnectChat.widgetKey ?: return
        val profile = LiveConnectChat.visitorProfile ?: return
        val thread = conversationManager.activeThread

        registerListeners()

        socketService.connect(
            widgetKey = widgetKey,
            name = profile.name,
            email = profile.email,
            phone = profile.phone,
            firstMessage = thread?.firstMessage ?: thread?.lastMessage ?: "",
            ticketId = ticketId
        )

        socketService.onConnect = {
            isSocketConnected = true
            isSocketConnecting = false
        }
    }

    fun connectWithFirstMessage(text: String) {
        if (isSocketConnecting || isSocketConnected) return
        isSocketConnecting = true

        val widgetKey = LiveConnectChat.widgetKey ?: return
        val profile = LiveConnectChat.visitorProfile ?: return

        registerListeners()

        socketService.connect(
            widgetKey = widgetKey,
            name = profile.name,
            email = profile.email,
            phone = profile.phone,
            firstMessage = text
        )

        socketService.onConnect = {
            isSocketConnected = true
            isSocketConnecting = false
        }
    }

    // ── Event callbacks ──

    private fun registerListeners() {
        socketEventManager.registerListeners()

        socketEventManager.onTicketCreated = handler@{ event ->
            val widgetKey = LiveConnectChat.widgetKey ?: return@handler
            val visitorId = LiveConnectChat.visitorId ?: ""
            val context = LiveConnectChat.appContext ?: return@handler

            conversationManager.setTicketIdForActiveThread(event.ticketId)
            // Server confirmed receipt of the first message (sent via auth payload).
            // It never echoes message:received for it, so promote SENDING → SENT here.
            conversationManager.markSendingMessagesAsSent()
            TicketStorage.saveActiveTicketId(context, widgetKey, visitorId, event.ticketId)
            TicketStorage.saveTicketStatus(
                context, widgetKey, visitorId, TicketStorage.STATUS_OPEN
            )
            event.agent?.let {
                currentAgent = it
                onAgentUpdated?.invoke()
            }

            socketService.emit(SocketService.EMIT_MESSAGE_DELIVERED, JSONObject().apply {
                put("ticketId", event.ticketId)
            })
        }

        socketEventManager.onTicketAssigned = { agent ->
            currentAgent = agent
            onAgentUpdated?.invoke()
        }

        socketEventManager.onSocketDisconnect = { reason ->
            isSocketConnected = false
            onDisconnected?.invoke(reason)
        }

        socketEventManager.onTicketResumed = handler@{ event ->
            event.agent?.let {
                currentAgent = it
                onAgentUpdated?.invoke()
            }

            socketService.emit(SocketService.EMIT_MESSAGE_DELIVERED, JSONObject().apply {
                put("ticketId", event.ticketId)
            })
        }

        socketEventManager.onTicketResolved = handler@{ event ->
            val widgetKey = LiveConnectChat.widgetKey ?: return@handler
            val visitorId = LiveConnectChat.visitorId ?: ""
            val context = LiveConnectChat.appContext ?: return@handler

            conversationManager.markActiveThreadAsResolved()
            TicketStorage.clearActiveTicketId(context, widgetKey, visitorId)
            TicketStorage.saveTicketStatus(
                context, widgetKey, visitorId, TicketStorage.STATUS_RESOLVED
            )
            socketService.disconnect()
            isSocketConnected = false
            currentAgent = null

            onTicketResolved?.invoke()
        }

        socketEventManager.onMessageReceived = { message ->
            val pending = socketEventManager.matchPendingMessage(message.text)
            if (pending != null) {
                conversationManager.replaceOptimisticMessage(pending.optimisticId, message)
            } else {
                conversationManager.addMessageToActiveThread(message)
            }
            // Track when the agent first joined (broadcast join message) so that
            // updateThreadMessages can skip injecting a duplicate synthetic join message.
            if (message.sender == com.techindika.liveconnect.model.MessageSender.BROADCAST
                && message.text.contains("joined the chat", ignoreCase = true)) {
                conversationManager.setAgentAssignedAt(message.timestamp)
            }

            conversationManager.activeTicketId?.let { ticketId ->
                socketService.emit(SocketService.EMIT_MESSAGE_DELIVERED, JSONObject().apply {
                    put("ticketId", ticketId)
                })
            }
        }

        socketEventManager.onMessageStatusUpdated = { event ->
            val status = MessageStatus.fromString(event.status)
            if (event.messageId.isBlank()) {
                // Ticket-level bulk update — server advances all messages in the ticket.
                conversationManager.updateAllVisitorMessagesStatus(status)
            } else {
                conversationManager.updateMessageStatus(event.messageId, status)
            }
        }

        socketEventManager.onAgentTyping = { event ->
            if (event.ticketId == conversationManager.activeTicketId) {
                // Typing indicator scoped to active ticket — UI wiring TBD
            }
        }

        socketEventManager.onAgentChanged = { event ->
            currentAgent = event.agent
            onAgentUpdated?.invoke()
        }

        socketEventManager.onAgentStatusChanged = { event ->
            // Update the status on the currently displayed agent if the ID matches.
            currentAgent?.let { agent ->
                if (agent.id == event.agentId) {
                    currentAgent = agent.copyWith(status = com.techindika.liveconnect.model.AgentStatus.fromString(event.status))
                    onAgentUpdated?.invoke()
                }
            }
        }

        socketEventManager.onUnreadCount = { event ->
            UnreadCountService.handleUnreadCountEvent(event.ticketId, event.unreadCount)
        }

        socketEventManager.onRatePrompt = { _ ->
            onRatePrompted?.invoke()
        }
    }
}
