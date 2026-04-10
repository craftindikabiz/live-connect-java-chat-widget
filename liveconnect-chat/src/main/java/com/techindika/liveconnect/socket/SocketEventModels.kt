package com.techindika.liveconnect.socket

import com.techindika.liveconnect.model.AgentInfo

/**
 * Data models for socket events.
 */

/** Emitted when a new ticket is created. */
data class TicketCreatedEvent(
    val ticketId: String,
    val agent: AgentInfo?
)

/** Emitted when an existing ticket is resumed. */
data class TicketResumedEvent(
    val ticketId: String,
    val agent: AgentInfo?
)

/** Emitted when a ticket is resolved. */
data class TicketResolvedEvent(
    val ticketId: String,
    val resolvedBy: String?
)

/** Emitted when message status is updated. */
data class MessageStatusEvent(
    val messageId: String,
    val ticketId: String,
    val status: String
)

/** Emitted when agent is typing. */
data class AgentTypingEvent(
    val ticketId: String,
    val isTyping: Boolean
)

/** Emitted when agent assignment changes. */
data class AgentChangedEvent(
    val agent: AgentInfo
)

/** Emitted when agent status changes. */
data class AgentStatusEvent(
    val agentId: String,
    val status: String
)

/** Emitted when unread count changes. */
data class UnreadCountEvent(
    val ticketId: String,
    val unreadCount: Int
)

/** Emitted when a rating is requested. */
data class RatePromptEvent(
    val ticketId: String
)
