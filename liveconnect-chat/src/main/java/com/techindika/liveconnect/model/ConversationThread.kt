package com.techindika.liveconnect.model

import java.util.Date
import java.util.UUID

/**
 * A conversation thread (one ticket = one thread).
 */
data class ConversationThread(
    val id: String = UUID.randomUUID().toString(),
    val title: String = "New Conversation",
    val status: ConversationStatus = ConversationStatus.ACTIVE,
    val messages: List<Message> = emptyList(),
    val agentName: String = "",
    val agentStatus: String = "",
    val agentAssignedAt: Date? = null,
    val updatedAt: Date = Date(),
    val createdAt: Date = Date(),
    val firstMessage: String? = null,
    val lastMessage: String? = null
) {
    val isActive: Boolean get() = status == ConversationStatus.ACTIVE
    val isClosed: Boolean get() = status == ConversationStatus.CLOSED

    fun copyWith(
        id: String = this.id,
        title: String = this.title,
        status: ConversationStatus = this.status,
        messages: List<Message> = this.messages,
        agentName: String = this.agentName,
        agentStatus: String = this.agentStatus,
        agentAssignedAt: Date? = this.agentAssignedAt,
        updatedAt: Date = this.updatedAt,
        createdAt: Date = this.createdAt,
        firstMessage: String? = this.firstMessage,
        lastMessage: String? = this.lastMessage
    ): ConversationThread = ConversationThread(
        id, title, status, messages, agentName, agentStatus,
        agentAssignedAt, updatedAt, createdAt, firstMessage, lastMessage
    )
}

/** Status of a conversation thread. */
enum class ConversationStatus {
    ACTIVE,
    CLOSED;

    companion object {
        @JvmStatic
        fun fromString(value: String?): ConversationStatus = when (value?.lowercase()) {
            "active", "open" -> ACTIVE
            "closed", "resolved" -> CLOSED
            else -> ACTIVE
        }
    }
}
