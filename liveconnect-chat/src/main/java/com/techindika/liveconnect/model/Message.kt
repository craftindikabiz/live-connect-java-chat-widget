package com.techindika.liveconnect.model

import java.util.Date

/**
 * A single chat message in a conversation.
 */
data class Message(
    val id: String,
    val text: String,
    val sender: MessageSender,
    val timestamp: Date,
    val attachment: Attachment? = null,
    val status: MessageStatus = MessageStatus.SENT
) {
    val isVisitor: Boolean get() = sender == MessageSender.VISITOR
    val isAgent: Boolean get() = sender == MessageSender.AGENT
    val isSystem: Boolean get() = sender == MessageSender.SYSTEM
    val isBroadcast: Boolean get() = sender == MessageSender.BROADCAST
    val hasAttachment: Boolean get() = attachment != null

    fun copyWith(
        id: String = this.id,
        text: String = this.text,
        sender: MessageSender = this.sender,
        timestamp: Date = this.timestamp,
        attachment: Attachment? = this.attachment,
        status: MessageStatus = this.status
    ): Message = Message(id, text, sender, timestamp, attachment, status)
}

/** Who sent the message. */
enum class MessageSender {
    VISITOR,
    AGENT,
    SYSTEM,
    BROADCAST;

    companion object {
        @JvmStatic
        fun fromString(value: String?): MessageSender = when (value?.lowercase()) {
            "visitor" -> VISITOR
            "agent" -> AGENT
            "system" -> SYSTEM
            "broadcast" -> BROADCAST
            else -> SYSTEM
        }
    }
}

/** Delivery status of a message. */
enum class MessageStatus {
    SENDING,
    SENT,
    DELIVERED,
    READ;

    companion object {
        @JvmStatic
        fun fromString(value: String?): MessageStatus = when (value?.lowercase()) {
            "sending" -> SENDING
            "sent" -> SENT
            "delivered" -> DELIVERED
            "read" -> READ
            else -> SENT
        }
    }
}
