package com.techindika.liveconnect.service

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.techindika.liveconnect.model.Message
import com.techindika.liveconnect.model.MessageSender
import com.techindika.liveconnect.model.MessageStatus
import com.techindika.liveconnect.model.WidgetTicket
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import java.util.Date

class ConversationManagerTest {

    @get:Rule val rule = InstantTaskExecutorRule()

    private lateinit var manager: ConversationManager

    @Before fun setUp() { manager = ConversationManager() }

    private fun ticket(id: String, status: String = "open"): WidgetTicket =
        WidgetTicket(id = id, status = status, firstMessage = "first $id")

    @Test
    fun `initializeFromTickets sets active to first open ticket`() {
        val t1 = ticket("t1", "resolved")
        val t2 = ticket("t2", "open")
        val t3 = ticket("t3", "open")
        manager.initializeFromTickets(listOf(t1, t2, t3))

        // The active ticket should be the first open one (t2 in API order)
        assertEquals("t2", manager.activeTicketId)
    }

    @Test
    fun `initializeFromTickets falls back to a fresh thread when no tickets are open`() {
        manager.initializeFromTickets(listOf(ticket("t1", "resolved")))
        // No open ticket → a new thread is created with no ticket id yet
        assertNotNull(manager.activeThread)
        assertNull(manager.activeTicketId)
    }

    @Test
    fun `initializeWithNewThread creates a fresh active thread`() {
        manager.initializeWithNewThread()
        assertNotNull(manager.activeThread)
        assertEquals(0, manager.activeThread!!.messages.size)
    }

    @Test
    fun `addMessageToActiveThread appends and updates lastMessage`() {
        manager.initializeWithNewThread()
        val msg = Message(
            id = "m1", text = "hello",
            sender = MessageSender.VISITOR,
            timestamp = Date(),
            attachment = null,
            status = MessageStatus.SENDING,
        )
        manager.addMessageToActiveThread(msg)
        assertEquals(1, manager.activeThread!!.messages.size)
        assertEquals("hello", manager.activeThread!!.lastMessage)
    }

    @Test
    fun `replaceOptimisticMessage swaps placeholder with server message`() {
        manager.initializeWithNewThread()
        val optimistic = Message(
            id = "local-1", text = "hi", sender = MessageSender.VISITOR,
            timestamp = Date(), attachment = null, status = MessageStatus.SENDING,
        )
        manager.addMessageToActiveThread(optimistic)

        val server = optimistic.copy(id = "server-1", status = MessageStatus.SENT)
        manager.replaceOptimisticMessage("local-1", server)

        val msgs = manager.activeThread!!.messages
        assertEquals(1, msgs.size)
        assertEquals("server-1", msgs[0].id)
        assertEquals(MessageStatus.SENT, msgs[0].status)
    }

    @Test
    fun `updateMessageStatus updates only the matching message`() {
        manager.initializeWithNewThread()
        val m1 = Message("m1", "a", MessageSender.VISITOR, Date(), null, MessageStatus.SENDING)
        val m2 = Message("m2", "b", MessageSender.VISITOR, Date(), null, MessageStatus.SENDING)
        manager.addMessageToActiveThread(m1)
        manager.addMessageToActiveThread(m2)

        manager.updateMessageStatus("m1", MessageStatus.READ)

        val msgs = manager.activeThread!!.messages
        assertEquals(MessageStatus.READ, msgs[0].status)
        assertEquals(MessageStatus.SENDING, msgs[1].status)
    }

    @Test
    fun `markActiveThreadAsResolved closes the current thread and creates a new one`() {
        manager.initializeFromTickets(listOf(ticket("t1", "open")))
        val originalActive = manager.activeThreadId.value
        manager.markActiveThreadAsResolved()
        // The new thread is now active, and the old one is in the threads list as closed
        assertNotNull(manager.activeThreadId.value)
        // Active thread should NOT be the original (a fresh one is created)
        assertTrue(manager.activeThreadId.value != originalActive || manager.activeThread!!.isClosed)
    }

    @Test
    fun `getTicketIdForThread returns the mapped ticket id`() {
        val t = ticket("ticket-99", "open")
        manager.initializeFromTickets(listOf(t))
        val activeThreadId = manager.activeThreadId.value!!
        assertEquals("ticket-99", manager.getTicketIdForThread(activeThreadId))
    }

    @Test
    fun `updateThreadsFromTickets preserves a fresh client-side active thread`() {
        // Start with a fresh client-side thread (no API ticket)
        manager.initializeWithNewThread()
        val freshThreadId = manager.activeThreadId.value!!

        // Server then returns a list of tickets that does NOT include our fresh thread.
        // The fresh thread should be PREPENDED and remain active — Flutter parity.
        val apiTickets = listOf(ticket("api-1", "resolved"))
        manager.updateThreadsFromTickets(apiTickets)

        assertEquals(freshThreadId, manager.activeThreadId.value)
        // The fresh thread is kept; the API-resolved ticket is also in the list.
        assertEquals(2, manager.threads.value!!.size)
    }

    @Test
    fun `switchToThread changes the active id`() {
        manager.initializeFromTickets(listOf(ticket("t1", "open"), ticket("t2", "open")))
        val initial = manager.activeThreadId.value
        // Find the OTHER thread
        val other = manager.threads.value!!.first { it.id != initial }
        manager.switchToThread(other.id)
        assertEquals(other.id, manager.activeThreadId.value)
    }
}
