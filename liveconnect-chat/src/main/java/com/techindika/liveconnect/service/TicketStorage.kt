package com.techindika.liveconnect.service

import android.content.Context
import android.content.SharedPreferences

/**
 * Persists the active ticket ID and its open/resolved status using SharedPreferences.
 *
 * Two independent keys per (widget, visitor) pair:
 *  - `liveconnect:active-ticket:<widgetKey>:<visitorId>` → ticket id string
 *  - `liveconnect:ticket-status:<widgetKey>:<visitorId>` → "open" or "resolved"
 *
 * Status is used to detect when an agent resolved the ticket while the app
 * was offline — so on next launch we don't try to resume a closed ticket.
 */
internal object TicketStorage {

    private const val PREFS_NAME = "liveconnect_tickets"

    const val STATUS_OPEN = "open"
    const val STATUS_RESOLVED = "resolved"

    private fun prefs(context: Context): SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private fun ticketKey(widgetKey: String, visitorId: String) =
        "liveconnect:active-ticket:${widgetKey.trim()}:${visitorId.trim()}"

    private fun statusKey(widgetKey: String, visitorId: String) =
        "liveconnect:ticket-status:${widgetKey.trim()}:${visitorId.trim()}"

    // ── Active ticket id ──

    fun saveActiveTicketId(context: Context, widgetKey: String, visitorId: String, ticketId: String) {
        prefs(context).edit().putString(ticketKey(widgetKey, visitorId), ticketId).apply()
    }

    fun loadActiveTicketId(context: Context, widgetKey: String, visitorId: String): String? {
        return prefs(context).getString(ticketKey(widgetKey, visitorId), null)
    }

    fun clearActiveTicketId(context: Context, widgetKey: String, visitorId: String) {
        prefs(context).edit().remove(ticketKey(widgetKey, visitorId)).apply()
        // Clearing the ticket also clears its status (matches Flutter behaviour).
        clearTicketStatus(context, widgetKey, visitorId)
    }

    // ── Ticket status (open / resolved) ──

    /** Save the ticket status — pass [STATUS_OPEN] or [STATUS_RESOLVED]. */
    fun saveTicketStatus(context: Context, widgetKey: String, visitorId: String, status: String) {
        prefs(context).edit().putString(statusKey(widgetKey, visitorId), status).apply()
    }

    /** Load the stored ticket status, or `null` if none. */
    fun loadTicketStatus(context: Context, widgetKey: String, visitorId: String): String? {
        return prefs(context).getString(statusKey(widgetKey, visitorId), null)
    }

    /** Clear the stored ticket status. Called by [clearActiveTicketId]. */
    fun clearTicketStatus(context: Context, widgetKey: String, visitorId: String) {
        prefs(context).edit().remove(statusKey(widgetKey, visitorId)).apply()
    }
}
