package com.techindika.liveconnect.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import com.techindika.liveconnect.LiveConnectChat
import com.techindika.liveconnect.ui.ChatActivity

/**
 * Helper for handling LiveConnect push notifications.
 *
 * Consumer apps should call [handleRemoteMessage] from their
 * `FirebaseMessagingService.onMessageReceived()` to display a
 * notification and, when tapped, open the chat screen.
 *
 * **Usage (Java):**
 * ```java
 * @Override
 * public void onMessageReceived(RemoteMessage message) {
 *     if (LiveConnectNotificationHelper.isLiveConnectMessage(message.getData())) {
 *         LiveConnectNotificationHelper.handleRemoteMessage(this, message.getData());
 *     }
 * }
 * ```
 *
 * **Usage (Kotlin):**
 * ```kotlin
 * override fun onMessageReceived(message: RemoteMessage) {
 *     if (LiveConnectNotificationHelper.isLiveConnectMessage(message.data)) {
 *         LiveConnectNotificationHelper.handleRemoteMessage(this, message.data)
 *     }
 * }
 * ```
 */
object LiveConnectNotificationHelper {

    private const val TAG = "LiveConnect.Notif"
    private const val CHANNEL_ID = "liveconnect_chat"
    private const val CHANNEL_NAME = "Chat Messages"
    private const val NOTIFICATION_ID_BASE = 9000

    /**
     * Check if a push notification data payload originates from LiveConnect.
     * Looks for the `liveconnect` or `widgetKey` key in the data map.
     */
    @JvmStatic
    fun isLiveConnectMessage(data: Map<String, String>): Boolean {
        return data.containsKey("liveconnect") ||
               data.containsKey("widgetKey") ||
               data.containsKey("ticketId")
    }

    /**
     * Display an Android notification for a LiveConnect push message.
     * Tapping the notification opens [ChatActivity].
     *
     * @param context Application or Service context.
     * @param data The FCM data payload.
     */
    @JvmStatic
    fun handleRemoteMessage(context: Context, data: Map<String, String>) {
        val title = data["title"] ?: data["agentName"] ?: "New Message"
        val body = data["body"] ?: data["content"] ?: data["message"] ?: ""
        val ticketId = data["ticketId"] ?: ""

        if (body.isBlank()) {
            Log.d(TAG, "Ignoring push with empty body")
            return
        }

        Log.d(TAG, "Handling push notification: title=$title ticketId=$ticketId")

        createNotificationChannel(context)

        // Build the intent to open the chat screen
        val intent = Intent(context, ChatActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(ChatActivity.EXTRA_WIDGET_KEY, LiveConnectChat.widgetKey ?: "")
            putExtra(ChatActivity.EXTRA_SHOW_CLOSE_BUTTON, true)
            if (ticketId.isNotEmpty()) {
                putExtra("ticketId", ticketId)
            }
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            ticketId.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_email)
            .setContentTitle(title)
            .setContentText(body)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .build()

        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(
            NOTIFICATION_ID_BASE + ticketId.hashCode().and(0xFFF),
            notification
        )
    }

    private fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "LiveConnect chat message notifications"
            }
            val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            nm.createNotificationChannel(channel)
        }
    }
}
