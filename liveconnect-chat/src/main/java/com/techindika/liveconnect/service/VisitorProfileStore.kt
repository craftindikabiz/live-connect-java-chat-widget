package com.techindika.liveconnect.service

import android.content.Context
import android.content.SharedPreferences
import com.techindika.liveconnect.model.VisitorProfile
import org.json.JSONObject

/**
 * Persists visitor profile using SharedPreferences.
 */
internal object VisitorProfileStore {

    private const val PREFS_NAME = "liveconnect_profiles"

    private fun prefs(context: Context): SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private fun key(widgetKey: String) = "liveconnect_profile_$widgetKey"

    fun save(context: Context, widgetKey: String, profile: VisitorProfile) {
        val json = JSONObject().apply {
            put("name", profile.name)
            put("email", profile.email)
            put("phone", profile.phone)
        }
        prefs(context).edit().putString(key(widgetKey), json.toString()).apply()
    }

    fun load(context: Context, widgetKey: String): VisitorProfile? {
        val jsonStr = prefs(context).getString(key(widgetKey), null) ?: return null
        return try {
            val json = JSONObject(jsonStr)
            VisitorProfile(
                name = json.optString("name", ""),
                email = json.optString("email", ""),
                phone = json.optString("phone", "")
            )
        } catch (_: Exception) {
            null
        }
    }

    fun clear(context: Context, widgetKey: String) {
        prefs(context).edit().remove(key(widgetKey)).apply()
    }

    /** Save visitor ID. */
    fun saveVisitorId(context: Context, widgetKey: String, visitorId: String) {
        prefs(context).edit().putString("liveconnect_visitor_id_$widgetKey", visitorId).apply()
    }

    /** Load visitor ID. */
    fun loadVisitorId(context: Context, widgetKey: String): String? {
        return prefs(context).getString("liveconnect_visitor_id_$widgetKey", null)
    }
}
