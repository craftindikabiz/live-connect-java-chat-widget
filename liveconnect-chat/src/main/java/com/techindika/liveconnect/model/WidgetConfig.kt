package com.techindika.liveconnect.model

import android.graphics.Color
import androidx.annotation.ColorInt
import org.json.JSONObject

/**
 * Widget configuration fetched from the server API.
 */
data class WidgetConfig(
    val id: String,
    val name: String,
    val color: String,
    val position: String,
    val welcomeText: String,
    val offlineText: String,
    val iconUrl: String,
    val widgetKey: String,
    val suggestedMessages: List<String>,
    val createdAt: String? = null,
    val updatedAt: String? = null
) {
    /** Parses the hex color string, with a fallback. */
    @ColorInt
    fun parsedColor(@ColorInt fallback: Int = Color.parseColor("#4F46E5")): Int {
        return try {
            Color.parseColor(color)
        } catch (_: Exception) {
            fallback
        }
    }

    val isPositionRight: Boolean get() = position.contains("right", ignoreCase = true)
    val isPositionLeft: Boolean get() = position.contains("left", ignoreCase = true)

    companion object {
        @JvmStatic
        fun fromJson(json: JSONObject): WidgetConfig {
            val suggestions = mutableListOf<String>()
            json.optJSONArray("suggestedMessages")?.let { arr ->
                for (i in 0 until arr.length()) {
                    arr.optString(i)?.takeIf { it.isNotEmpty() }?.let { suggestions.add(it) }
                }
            }
            return WidgetConfig(
                id = json.optString("id", json.optString("_id", "")),
                name = json.optString("name", ""),
                color = json.optString("color", "#4F46E5"),
                position = json.optString("position", "bottom-right"),
                welcomeText = json.optString("welcomeText", ""),
                offlineText = json.optString("offlineText", ""),
                iconUrl = json.optString("iconUrl", ""),
                widgetKey = json.optString("widgetKey", json.optString("key", "")),
                suggestedMessages = suggestions,
                createdAt = json.optString("createdAt", null),
                updatedAt = json.optString("updatedAt", null)
            )
        }
    }
}
