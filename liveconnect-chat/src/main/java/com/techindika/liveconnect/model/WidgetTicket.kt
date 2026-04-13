package com.techindika.liveconnect.model

import com.techindika.liveconnect.util.optStringOrNull
import org.json.JSONObject

/**
 * A support ticket from the API.
 */
data class WidgetTicket(
    val id: String,
    val widgetId: String? = null,
    val visitorId: String? = null,
    val agentId: String? = null,
    val agentName: String? = null,
    val agentPhoto: String? = null,
    val agentStatus: String? = null,
    val status: String? = null,
    val firstMessage: String? = null,
    val lastMessage: String? = null,
    val lastMessageAt: String? = null,
    val agentAssignedAt: String? = null,
    val agentLastReplyAt: String? = null,
    val resolvedAt: String? = null,
    val resolvedBy: String? = null,
    val createdAt: String? = null,
    val updatedAt: String? = null
) {
    companion object {
        @JvmStatic
        fun fromJson(json: JSONObject): WidgetTicket {
            // agentId can be a string or nested object
            val agentId = when {
                json.optJSONObject("agentId") != null ->
                    json.optJSONObject("agentId")?.optString("_id", "")
                else -> json.optStringOrNull("agentId")
            }
            return WidgetTicket(
                id = json.optString("id", json.optString("_id", "")),
                widgetId = json.optStringOrNull("widgetId"),
                visitorId = json.optStringOrNull("visitorId"),
                agentId = agentId,
                agentName = json.optStringOrNull("agentName"),
                agentPhoto = json.optStringOrNull("agentPhoto"),
                agentStatus = json.optStringOrNull("agentStatus"),
                status = json.optStringOrNull("status"),
                firstMessage = json.optStringOrNull("firstMessage"),
                lastMessage = json.optStringOrNull("lastMessage"),
                lastMessageAt = json.optStringOrNull("lastMessageAt"),
                agentAssignedAt = json.optStringOrNull("agentAssignedAt"),
                agentLastReplyAt = json.optStringOrNull("agentLastReplyAt"),
                resolvedAt = json.optStringOrNull("resolvedAt"),
                resolvedBy = json.optStringOrNull("resolvedBy"),
                createdAt = json.optStringOrNull("createdAt"),
                updatedAt = json.optStringOrNull("updatedAt")
            )
        }
    }
}
