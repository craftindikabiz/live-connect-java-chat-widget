package com.techindika.liveconnect.model

import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class WidgetTicketTest {

    @Test
    fun `fromJson reads core fields`() {
        val json = JSONObject(
            """{
              "_id":"t1",
              "widgetId":"w1",
              "visitorId":"v1",
              "agentId":"a1",
              "agentName":"Sara",
              "agentStatus":"online",
              "status":"open",
              "firstMessage":"hello",
              "lastMessage":"bye",
              "createdAt":"2026-01-01T00:00:00",
              "updatedAt":"2026-01-02T00:00:00"
            }"""
        )
        val ticket = WidgetTicket.fromJson(json)
        assertEquals("t1", ticket.id)
        assertEquals("w1", ticket.widgetId)
        assertEquals("v1", ticket.visitorId)
        assertEquals("a1", ticket.agentId)
        assertEquals("Sara", ticket.agentName)
        assertEquals("online", ticket.agentStatus)
        assertEquals("open", ticket.status)
        assertEquals("hello", ticket.firstMessage)
        assertEquals("bye", ticket.lastMessage)
    }

    @Test
    fun `fromJson handles agentId as nested object — round 5 fix`() {
        // The server may send agentId as either a raw string or { _id: "..." }
        val json = JSONObject(
            """{"_id":"t2","agentId":{"_id":"a2","name":"X"},"status":"open"}"""
        )
        val ticket = WidgetTicket.fromJson(json)
        assertEquals("a2", ticket.agentId)
    }

    @Test
    fun `fromJson returns null for missing optional fields — round 5 JsonExt fix`() {
        val json = JSONObject("""{"_id":"t3","status":"resolved"}""")
        val ticket = WidgetTicket.fromJson(json)
        assertEquals("t3", ticket.id)
        assertEquals("resolved", ticket.status)
        assertNull(ticket.widgetId)
        assertNull(ticket.visitorId)
        assertNull(ticket.agentId)
        assertNull(ticket.agentName)
        assertNull(ticket.firstMessage)
        assertNull(ticket.lastMessage)
    }

    @Test
    fun `fromJson uses id field when _id missing`() {
        val json = JSONObject("""{"id":"t4","status":"open"}""")
        val ticket = WidgetTicket.fromJson(json)
        assertEquals("t4", ticket.id)
    }
}
