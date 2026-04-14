package com.techindika.liveconnect.service

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class TicketStorageTest {

    private lateinit var context: Context

    @Before fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        // Clear any leftover state from prior tests
        TicketStorage.clearActiveTicketId(context, "wk", "v1")
    }

    @Test
    fun `save and load active ticket id roundtrips`() {
        TicketStorage.saveActiveTicketId(context, "wk", "v1", "ticket-1")
        assertEquals("ticket-1", TicketStorage.loadActiveTicketId(context, "wk", "v1"))
    }

    @Test
    fun `loadActiveTicketId returns null when nothing is stored`() {
        assertNull(TicketStorage.loadActiveTicketId(context, "wk", "v1"))
    }

    @Test
    fun `clearActiveTicketId removes both ticket id and status — round 6 fix`() {
        TicketStorage.saveActiveTicketId(context, "wk", "v1", "ticket-1")
        TicketStorage.saveTicketStatus(context, "wk", "v1", TicketStorage.STATUS_OPEN)

        TicketStorage.clearActiveTicketId(context, "wk", "v1")

        assertNull(TicketStorage.loadActiveTicketId(context, "wk", "v1"))
        // Status must also be cleared per Flutter behaviour
        assertNull(TicketStorage.loadTicketStatus(context, "wk", "v1"))
    }

    @Test
    fun `save and load ticket status roundtrips`() {
        TicketStorage.saveTicketStatus(context, "wk", "v1", TicketStorage.STATUS_OPEN)
        assertEquals(TicketStorage.STATUS_OPEN, TicketStorage.loadTicketStatus(context, "wk", "v1"))

        TicketStorage.saveTicketStatus(context, "wk", "v1", TicketStorage.STATUS_RESOLVED)
        assertEquals(TicketStorage.STATUS_RESOLVED, TicketStorage.loadTicketStatus(context, "wk", "v1"))
    }

    @Test
    fun `loadTicketStatus returns null when nothing stored`() {
        assertNull(TicketStorage.loadTicketStatus(context, "wk", "v1"))
    }

    @Test
    fun `clearTicketStatus does not affect ticket id`() {
        TicketStorage.saveActiveTicketId(context, "wk", "v1", "ticket-1")
        TicketStorage.saveTicketStatus(context, "wk", "v1", TicketStorage.STATUS_OPEN)
        TicketStorage.clearTicketStatus(context, "wk", "v1")

        assertEquals("ticket-1", TicketStorage.loadActiveTicketId(context, "wk", "v1"))
        assertNull(TicketStorage.loadTicketStatus(context, "wk", "v1"))
    }

    @Test
    fun `keys are scoped per widget and visitor`() {
        TicketStorage.saveActiveTicketId(context, "wk-a", "v1", "ta")
        TicketStorage.saveActiveTicketId(context, "wk-b", "v1", "tb")
        TicketStorage.saveActiveTicketId(context, "wk-a", "v2", "ta-v2")

        assertEquals("ta", TicketStorage.loadActiveTicketId(context, "wk-a", "v1"))
        assertEquals("tb", TicketStorage.loadActiveTicketId(context, "wk-b", "v1"))
        assertEquals("ta-v2", TicketStorage.loadActiveTicketId(context, "wk-a", "v2"))
    }
}
