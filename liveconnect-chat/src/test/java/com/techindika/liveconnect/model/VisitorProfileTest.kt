package com.techindika.liveconnect.model

import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class VisitorProfileTest {

    @Test
    fun `isComplete is true when name and email are non-blank`() {
        val p = VisitorProfile("John", "john@example.com", "+14155552671")
        assertTrue(p.isComplete)
    }

    @Test
    fun `isComplete is true when phone is empty`() {
        val p = VisitorProfile("John", "john@example.com", "")
        assertTrue(p.isComplete)
    }

    @Test
    fun `isComplete is false when name is blank`() {
        val p = VisitorProfile("  ", "john@example.com")
        assertFalse(p.isComplete)
    }

    @Test
    fun `isComplete is false when email is blank`() {
        val p = VisitorProfile("John", "")
        assertFalse(p.isComplete)
    }

    @Test
    fun `isComplete accepts username (no @ required) — matches Flutter`() {
        // Flutter's VisitorProfileValidator no longer requires email format —
        // a bare username is valid.
        val p = VisitorProfile("John", "johndoe")
        assertTrue(p.isComplete)
    }

    @Test
    fun `fromJson reads name email phone`() {
        val json = JSONObject("""{"name":"John","email":"j@e.com","phone":"+1"}""")
        val p = VisitorProfile.fromJson(json)
        assertEquals("John", p.name)
        assertEquals("j@e.com", p.email)
        assertEquals("+1", p.phone)
    }

    @Test
    fun `fromJson defaults missing fields to empty strings`() {
        val json = JSONObject("""{"name":"John"}""")
        val p = VisitorProfile.fromJson(json)
        assertEquals("John", p.name)
        assertEquals("", p.email)
        assertEquals("", p.phone)
    }

    @Test
    fun `toJson roundtrips`() {
        val p = VisitorProfile("John", "j@e.com", "+1")
        val parsed = VisitorProfile.fromJson(p.toJson())
        assertEquals(p, parsed)
    }

    @Test
    fun `empty factory returns blank profile`() {
        val p = VisitorProfile.empty()
        assertEquals("", p.name)
        assertEquals("", p.email)
        assertEquals("", p.phone)
        assertFalse(p.isComplete)
    }
}
