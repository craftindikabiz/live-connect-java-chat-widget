package com.techindika.liveconnect.model

import org.json.JSONObject

/**
 * Information about the assigned support agent.
 */
data class AgentInfo(
    val id: String,
    val name: String,
    val photo: String? = null,
    val status: AgentStatus = AgentStatus.ONLINE
) {
    fun copyWith(
        id: String = this.id,
        name: String = this.name,
        photo: String? = this.photo,
        status: AgentStatus = this.status
    ): AgentInfo = AgentInfo(id, name, photo, status)

    companion object {
        @JvmStatic
        fun fromJson(json: JSONObject): AgentInfo = AgentInfo(
            id = json.optString("id", json.optString("_id", "")),
            name = json.optString("name", "Agent"),
            photo = json.optString("photo", null),
            status = AgentStatus.fromString(json.optString("status", "online"))
        )
    }
}

/** Agent availability status. */
enum class AgentStatus {
    ONLINE,
    OFFLINE,
    BUSY,
    AWAY;

    val displayText: String
        get() = name.lowercase()

    val isAvailable: Boolean
        get() = this == ONLINE

    companion object {
        @JvmStatic
        fun fromString(value: String?): AgentStatus = when (value?.lowercase()) {
            "online" -> ONLINE
            "offline" -> OFFLINE
            "busy" -> BUSY
            "away" -> AWAY
            else -> OFFLINE
        }
    }
}
