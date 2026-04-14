package com.techindika.liveconnect.service

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class UnreadCountServiceTest {

    @get:Rule val rule = InstantTaskExecutorRule()

    @Before fun setUp() { UnreadCountService.reset() }

    @Test
    fun `getTotalUnreadCount sums per-ticket counts`() {
        UnreadCountService.handleUnreadCountEvent("t1", 3)
        UnreadCountService.handleUnreadCountEvent("t2", 5)
        assertEquals(8, UnreadCountService.getTotalUnreadCount())
    }

    @Test
    fun `getUnreadCount returns the per-ticket value`() {
        UnreadCountService.handleUnreadCountEvent("t1", 3)
        UnreadCountService.handleUnreadCountEvent("t2", 5)
        assertEquals(3, UnreadCountService.getUnreadCount("t1"))
        assertEquals(5, UnreadCountService.getUnreadCount("t2"))
    }

    @Test
    fun `getUnreadCount returns 0 for unknown ticket`() {
        assertEquals(0, UnreadCountService.getUnreadCount("nope"))
    }

    @Test
    fun `handleUnreadCountEvent overwrites existing count for the same ticket`() {
        UnreadCountService.handleUnreadCountEvent("t1", 3)
        UnreadCountService.handleUnreadCountEvent("t1", 10)
        assertEquals(10, UnreadCountService.getUnreadCount("t1"))
        assertEquals(10, UnreadCountService.getTotalUnreadCount())
    }

    @Test
    fun `clearForTicket removes only that ticket from the total`() {
        UnreadCountService.handleUnreadCountEvent("t1", 3)
        UnreadCountService.handleUnreadCountEvent("t2", 5)
        UnreadCountService.clearForTicket("t1")
        assertEquals(0, UnreadCountService.getUnreadCount("t1"))
        assertEquals(5, UnreadCountService.getUnreadCount("t2"))
        assertEquals(5, UnreadCountService.getTotalUnreadCount())
    }

    @Test
    fun `reset clears everything`() {
        UnreadCountService.handleUnreadCountEvent("t1", 3)
        UnreadCountService.handleUnreadCountEvent("t2", 5)
        UnreadCountService.reset()
        assertEquals(0, UnreadCountService.getTotalUnreadCount())
        assertEquals(0, UnreadCountService.getUnreadCount("t1"))
    }
}
