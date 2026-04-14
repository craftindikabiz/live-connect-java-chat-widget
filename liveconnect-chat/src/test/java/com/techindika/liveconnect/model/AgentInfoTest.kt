package com.techindika.liveconnect.model

import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AgentInfoTest {

    @Test
    fun `fromJson reads id name photo status`() {
        val json = JSONObject(
            """{"id":"a1","name":"Sara","photo":"http://x/y.png","status":"online"}"""
        )
        val agent = AgentInfo.fromJson(json)
        assertEquals("a1", agent.id)
        assertEquals("Sara", agent.name)
        assertEquals("http://x/y.png", agent.photo)
        assertEquals(AgentStatus.ONLINE, agent.status)
    }

    @Test
    fun `fromJson falls back to _id when id is missing`() {
        val json = JSONObject("""{"_id":"a2","name":"Bob"}""")
        val agent = AgentInfo.fromJson(json)
        assertEquals("a2", agent.id)
        assertEquals("Bob", agent.name)
    }

    @Test
    fun `fromJson returns null photo when key missing — round 5 fix`() {
        // The prior bug was that json.optString("photo", null) caused a Kotlin
        // type-inference Type mismatch warning AND set photo to "null" string
        // in some paths. The optStringOrNull helper now returns true Kotlin null.
        val json = JSONObject("""{"id":"a3","name":"Eve"}""")
        val agent = AgentInfo.fromJson(json)
        assertNull(agent.photo)
    }

    @Test
    fun `fromJson returns null photo when key is empty string`() {
        val json = JSONObject("""{"id":"a4","name":"X","photo":""}""")
        val agent = AgentInfo.fromJson(json)
        assertNull(agent.photo)
    }

    @Test
    fun `fromJson defaults name to Agent when missing`() {
        val json = JSONObject("""{"id":"a5"}""")
        val agent = AgentInfo.fromJson(json)
        assertEquals("Agent", agent.name)
    }

    @Test
    fun `AgentStatus fromString maps known states`() {
        assertEquals(AgentStatus.ONLINE, AgentStatus.fromString("online"))
        assertEquals(AgentStatus.OFFLINE, AgentStatus.fromString("offline"))
        assertEquals(AgentStatus.BUSY, AgentStatus.fromString("busy"))
        assertEquals(AgentStatus.AWAY, AgentStatus.fromString("away"))
    }

    @Test
    fun `AgentStatus fromString is case-insensitive`() {
        assertEquals(AgentStatus.ONLINE, AgentStatus.fromString("ONLINE"))
        assertEquals(AgentStatus.BUSY, AgentStatus.fromString("Busy"))
    }

    @Test
    fun `AgentStatus fromString defaults unknown to OFFLINE`() {
        assertEquals(AgentStatus.OFFLINE, AgentStatus.fromString("garbage"))
        assertEquals(AgentStatus.OFFLINE, AgentStatus.fromString(null))
    }

    @Test
    fun `AgentStatus isAvailable is only true for ONLINE`() {
        assertTrue(AgentStatus.ONLINE.isAvailable)
        assertEquals(false, AgentStatus.OFFLINE.isAvailable)
        assertEquals(false, AgentStatus.BUSY.isAvailable)
        assertEquals(false, AgentStatus.AWAY.isAvailable)
    }
}
