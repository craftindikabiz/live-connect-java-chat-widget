package com.techindika.liveconnect.service

import android.content.Context
import android.content.SharedPreferences

/**
 * Persists the active ticket ID using SharedPreferences.
 */
internal object TicketStorage {

    private const val PREFS_NAME = "liveconnect_tickets"

    private fun prefs(context: Context): SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private fun key(widgetKey: String, visitorId: String) =
        "liveconnect:active-ticket:$widgetKey:$visitorId"

    fun saveActiveTicketId(context: Context, widgetKey: String, visitorId: String, ticketId: String) {
        prefs(context).edit().putString(key(widgetKey, visitorId), ticketId).apply()
    }

    fun loadActiveTicketId(context: Context, widgetKey: String, visitorId: String): String? {
        return prefs(context).getString(key(widgetKey, visitorId), null)
    }

    fun clearActiveTicketId(context: Context, widgetKey: String, visitorId: String) {
        prefs(context).edit().remove(key(widgetKey, visitorId)).apply()
    }
}
