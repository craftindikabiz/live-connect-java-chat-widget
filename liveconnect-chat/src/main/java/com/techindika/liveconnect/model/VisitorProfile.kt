package com.techindika.liveconnect.model

import org.json.JSONObject

/**
 * Visitor profile for chat identification.
 *
 * @property name Full name of the visitor (required).
 * @property email Email address (required).
 * @property phone Phone number in E.164 format (optional).
 */
data class VisitorProfile @JvmOverloads constructor(
    val name: String,
    val email: String,
    val phone: String = ""
) {
    /** Whether both name and email are non-empty. */
    val isComplete: Boolean get() = name.isNotBlank() && email.isNotBlank()

    fun toJson(): JSONObject = JSONObject().apply {
        put("name", name)
        put("email", email)
        put("phone", phone)
    }

    companion object {
        @JvmStatic
        fun empty(): VisitorProfile = VisitorProfile("", "", "")

        @JvmStatic
        fun fromJson(json: JSONObject): VisitorProfile = VisitorProfile(
            name = json.optString("name", ""),
            email = json.optString("email", ""),
            phone = json.optString("phone", "")
        )
    }
}
