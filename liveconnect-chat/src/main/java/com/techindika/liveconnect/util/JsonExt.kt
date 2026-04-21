package com.techindika.liveconnect.util

import org.json.JSONObject

/**
 * Null-safe [JSONObject.optString] — returns null when the key is missing,
 * the value is JSON null, or the value is an empty string.
 *
 * Android's [JSONObject.optString] converts JSON null to the literal
 * string `"null"` on many API levels, so we must check [isNull] first.
 */
internal fun JSONObject.optStringOrNull(key: String): String? {
    if (!has(key) || isNull(key)) return null
    val value = optString(key, "")
    return value.ifEmpty { null }
}
