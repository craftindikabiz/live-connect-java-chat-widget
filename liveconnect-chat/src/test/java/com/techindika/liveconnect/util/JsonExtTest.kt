package com.techindika.liveconnect.util

import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class JsonExtTest {

    @Test
    fun `optStringOrNull returns null when key is missing`() {
        val json = JSONObject("""{"foo": "bar"}""")
        assertNull(json.optStringOrNull("missing"))
    }

    @Test
    fun `optStringOrNull returns null when value is empty string`() {
        val json = JSONObject("""{"foo": ""}""")
        assertNull(json.optStringOrNull("foo"))
    }

    @Test
    fun `optStringOrNull returns the value when present and non-empty`() {
        val json = JSONObject("""{"foo": "bar"}""")
        assertEquals("bar", json.optStringOrNull("foo"))
    }

    @Test
    fun `optStringOrNull returns null when value is JSON null`() {
        val json = JSONObject("""{"foo": null}""")
        assertNull(json.optStringOrNull("foo"))
    }

    @Test
    fun `optStringOrNull preserves whitespace inside the value`() {
        val json = JSONObject("""{"foo": "  hi  "}""")
        assertEquals("  hi  ", json.optStringOrNull("foo"))
    }
}
