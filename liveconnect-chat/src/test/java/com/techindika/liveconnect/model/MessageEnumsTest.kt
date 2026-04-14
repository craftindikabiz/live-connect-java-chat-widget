package com.techindika.liveconnect.model

import org.junit.Assert.assertEquals
import org.junit.Test

class MessageEnumsTest {

    @Test
    fun `MessageStatus fromString maps all values`() {
        assertEquals(MessageStatus.SENDING, MessageStatus.fromString("sending"))
        assertEquals(MessageStatus.SENT, MessageStatus.fromString("sent"))
        assertEquals(MessageStatus.DELIVERED, MessageStatus.fromString("delivered"))
        assertEquals(MessageStatus.READ, MessageStatus.fromString("read"))
    }

    @Test
    fun `MessageStatus fromString is case-insensitive`() {
        assertEquals(MessageStatus.SENT, MessageStatus.fromString("SENT"))
        assertEquals(MessageStatus.READ, MessageStatus.fromString("Read"))
    }

    @Test
    fun `MessageStatus fromString defaults unknown to SENT`() {
        // Conservative default — message has been emitted, so SENT is the
        // safest assumption when the status string is missing or garbage.
        assertEquals(MessageStatus.SENT, MessageStatus.fromString(null))
        assertEquals(MessageStatus.SENT, MessageStatus.fromString(""))
        assertEquals(MessageStatus.SENT, MessageStatus.fromString("garbage"))
    }

    @Test
    fun `MessageSender fromString maps all values`() {
        assertEquals(MessageSender.VISITOR, MessageSender.fromString("visitor"))
        assertEquals(MessageSender.AGENT, MessageSender.fromString("agent"))
        assertEquals(MessageSender.SYSTEM, MessageSender.fromString("system"))
        assertEquals(MessageSender.BROADCAST, MessageSender.fromString("broadcast"))
    }

    @Test
    fun `MessageSender fromString defaults unknown to SYSTEM`() {
        assertEquals(MessageSender.SYSTEM, MessageSender.fromString(null))
        assertEquals(MessageSender.SYSTEM, MessageSender.fromString("garbage"))
    }
}
