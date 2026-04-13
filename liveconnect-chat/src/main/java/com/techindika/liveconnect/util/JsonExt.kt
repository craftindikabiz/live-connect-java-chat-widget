package com.techindika.liveconnect.util

import org.json.JSONObject

/**
 * Null-safe [JSONObject.optString] — returns null when the key is missing or the value is empty.
 * Replaces `optString(key, null)` which kotlinc flags as a `Nothing? vs String` type mismatch.
 */
internal fun JSONObject.optStringOrNull(key: String): String? =
    optString(key, "").ifEmpty { null }
