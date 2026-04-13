package com.techindika.liveconnect.model

import com.techindika.liveconnect.util.optStringOrNull
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

/**
 * A message within a ticket, as returned by the REST API.
 */
data class TicketMessage(
    val id: String,
    val ticketId: String,
    val senderType: String,
    val senderId: String? = null,
    val type: String,
    val content: String? = null,
    val fileUrl: String? = null,
    val fileName: String? = null,
    val fileType: String? = null,
    val status: String? = null,
    val createdAt: Date,
    val updatedAt: Date
) {
    companion object {
        private val isoFormat = ThreadLocal.withInitial {
            SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US).apply {
                timeZone = TimeZone.getTimeZone("UTC")
            }
        }

        @JvmStatic
        fun fromJson(json: JSONObject): TicketMessage {
            val now = Date()
            return TicketMessage(
                id = json.optString("id", json.optString("_id", "")),
                ticketId = json.optString("ticketId", ""),
                senderType = json.optString("senderType", "system"),
                senderId = json.optStringOrNull("senderId"),
                type = json.optString("type", "text"),
                content = json.optStringOrNull("content"),
                fileUrl = json.optStringOrNull("fileUrl"),
                fileName = json.optStringOrNull("fileName"),
                fileType = json.optStringOrNull("fileType"),
                status = json.optStringOrNull("status"),
                createdAt = parseDate(json.optString("createdAt", "")) ?: now,
                updatedAt = parseDate(json.optString("updatedAt", "")) ?: now
            )
        }

        private fun parseDate(dateStr: String): Date? {
            if (dateStr.isBlank()) return null
            return try {
                // Handle ISO 8601 with milliseconds and Z suffix
                val cleaned = dateStr.replace("Z", "").split(".").first()
                isoFormat.get()!!.parse(cleaned)
            } catch (_: Exception) {
                null
            }
        }
    }
}
