package com.techindika.liveconnect.network

/**
 * API endpoints and constants for the LiveConnect backend.
 */
internal object ApiConstants {

    const val BASE_URL = "https://api.dpchat.net/"
    const val SOCKET_URL = "https://api.dpchat.net"
    const val SOCKET_NAMESPACE = "/visitor"

    // Widget config
    fun widgetConfig(widgetKey: String) = "api/widgets/$widgetKey"

    // Visitor upsert
    fun visitorUpsert(widgetKey: String) = "api/widgets/$widgetKey/visitor"

    // Tickets (paginated)
    fun tickets(widgetKey: String) = "api/widgets/$widgetKey/tickets"

    // Ticket messages (paginated)
    fun ticketMessages(widgetKey: String, ticketId: String) =
        "api/widgets/$widgetKey/tickets/$ticketId/messages"

    // File upload
    fun fileUpload(widgetKey: String) = "api/widgets/$widgetKey/chat/upload"

    // FCM token
    const val FCM_TOKEN = "api/widgets/fcm-token"

    // Firebase service account
    fun firebaseServiceAccount(widgetId: String) = "api/admin/widgets/$widgetId/firebase"
}
