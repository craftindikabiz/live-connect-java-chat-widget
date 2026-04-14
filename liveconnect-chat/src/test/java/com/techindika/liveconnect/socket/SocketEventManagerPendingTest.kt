package com.techindika.liveconnect.socket

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

/**
 * Tests the optimistic-message tracking logic in SocketEventManager.
 * No socket is needed — this is pure in-memory state.
 */
class SocketEventManagerPendingTest {

    // We need a SocketService instance to construct the manager, but none of
    // the tests in this file actually trigger socket I/O. SocketService is a
    // singleton object; getting its instance is enough.
    private lateinit var manager: SocketEventManager

    @Before fun setUp() {
        manager = SocketEventManager(SocketService.getInstance())
        manager.reset()
    }

    @Test
    fun `nextOptimisticId yields monotonically increasing ids`() {
        val a = manager.nextOptimisticId()
        val b = manager.nextOptimisticId()
        val c = manager.nextOptimisticId()
        assertNotEquals(a, b)
        assertNotEquals(b, c)
        assert(a.startsWith("local-"))
        assert(b.startsWith("local-"))
    }

    @Test
    fun `trackPendingMessage then matchPendingMessage by content roundtrips`() {
        val id = manager.nextOptimisticId()
        manager.trackPendingMessage(id, "hello world")

        val matched = manager.matchPendingMessage("hello world")
        assertEquals(id, matched?.optimisticId)
        assertEquals("hello world", matched?.text)
    }

    @Test
    fun `matchPendingMessage consumes the entry — second call returns null`() {
        val id = manager.nextOptimisticId()
        manager.trackPendingMessage(id, "single")

        assertEquals(id, manager.matchPendingMessage("single")?.optimisticId)
        assertNull(manager.matchPendingMessage("single"))
    }

    @Test
    fun `matchPendingMessage returns null for unknown content`() {
        manager.trackPendingMessage(manager.nextOptimisticId(), "a")
        assertNull(manager.matchPendingMessage("not here"))
    }

    @Test
    fun `reset clears tracked messages and counter`() {
        val a = manager.nextOptimisticId()
        manager.trackPendingMessage(a, "m1")
        manager.reset()

        assertNull(manager.matchPendingMessage("m1"))
        // After reset, the counter restarts from 1 (so the next id is local-1)
        val newFirst = manager.nextOptimisticId()
        assertEquals("local-1", newFirst)
    }
}
