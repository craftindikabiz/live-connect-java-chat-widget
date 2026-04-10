package com.techindika.liveconnect.model

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
                else -> json.optString("agentId", null)
            }
            return WidgetTicket(
                id = json.optString("id", json.optString("_id", "")),
                widgetId = json.optString("widgetId", null),
                visitorId = json.optString("visitorId", null),
                agentId = agentId,
                agentName = json.optString("agentName", null),
                agentPhoto = json.optString("agentPhoto", null),
                agentStatus = json.optString("agentStatus", null),
                status = json.optString("status", null),
                firstMessage = json.optString("firstMessage", null),
                lastMessage = json.optString("lastMessage", null),
                lastMessageAt = json.optString("lastMessageAt", null),
                agentAssignedAt = json.optString("agentAssignedAt", null),
                agentLastReplyAt = json.optString("agentLastReplyAt", null),
                resolvedAt = json.optString("resolvedAt", null),
                resolvedBy = json.optString("resolvedBy", null),
                createdAt = json.optString("createdAt", null),
                updatedAt = json.optString("updatedAt", null)
            )
        }
    }
}
