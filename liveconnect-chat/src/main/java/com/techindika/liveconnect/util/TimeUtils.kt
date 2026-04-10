package com.techindika.liveconnect.util

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import java.util.concurrent.TimeUnit

/**
 * Time formatting utilities.
 */
internal object TimeUtils {

    private val isoFormat = ThreadLocal.withInitial {
        SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US).apply {
            timeZone = TimeZone.getTimeZone("UTC")
        }
    }

    private val timeFormat = ThreadLocal.withInitial { SimpleDateFormat("h:mm a", Locale.US) }
    private val dateFormat = ThreadLocal.withInitial { SimpleDateFormat("MMM d", Locale.US) }
    private val fullDateFormat = ThreadLocal.withInitial { SimpleDateFormat("MMM d, yyyy", Locale.US) }

    /** Format a Date to a readable time string. */
    fun formatTime(date: Date): String = timeFormat.get()!!.format(date)

    /** Format a Date to a readable date string. */
    fun formatDate(date: Date): String = dateFormat.get()!!.format(date)

    /** Convert an ISO 8601 date string to a relative time string. */
    fun relativeTime(isoDate: String?): String {
        if (isoDate.isNullOrBlank()) return ""
        val date = parseIso(isoDate) ?: return ""
        return relativeTime(date)
    }

    /** Convert a Date to a relative time string (e.g., "2m ago", "Yesterday"). */
    fun relativeTime(date: Date): String {
        val now = System.currentTimeMillis()
        val diff = now - date.time

        return when {
            diff < TimeUnit.MINUTES.toMillis(1) -> "Just now"
            diff < TimeUnit.HOURS.toMillis(1) -> "${TimeUnit.MILLISECONDS.toMinutes(diff)}m ago"
            diff < TimeUnit.DAYS.toMillis(1) -> "${TimeUnit.MILLISECONDS.toHours(diff)}h ago"
            diff < TimeUnit.DAYS.toMillis(2) -> "Yesterday"
            diff < TimeUnit.DAYS.toMillis(365) -> dateFormat.get()!!.format(date)
            else -> fullDateFormat.get()!!.format(date)
        }
    }

    /** Parse an ISO 8601 date string. */
    fun parseIso(dateStr: String): Date? {
        if (dateStr.isBlank()) return null
        return try {
            val cleaned = dateStr.replace("Z", "").split(".").first()
            isoFormat.get()!!.parse(cleaned)
        } catch (_: Exception) {
            null
        }
    }
}
