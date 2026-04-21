package com.techindika.liveconnect.socket

import android.util.Log
import com.techindika.liveconnect.network.ApiConstants
import io.socket.client.IO
import io.socket.client.Socket
import org.json.JSONObject
import java.net.URI

/**
 * Manages the Socket.IO connection to the LiveConnect server.
 * Singleton — one active connection at a time.
 */
internal class SocketService private constructor() {

    private var socket: Socket? = null
    private var isConnecting = false
    var isConnected = false
        private set

    var onConnect: (() -> Unit)? = null
    var onDisconnect: ((String) -> Unit)? = null
    var onConnectError: ((Exception) -> Unit)? = null

    // Event listeners map
    private val eventListeners = mutableMapOf<String, (Array<Any>) -> Unit>()

    /**
     * Connect to the Socket.IO server with authentication.
     *
     * @param widgetKey The widget identifier.
     * @param name Visitor name.
     * @param email Visitor email.
     * @param phone Visitor phone.
     * @param firstMessage First message text (required by server for auth).
     * @param ticketId Optional ticket ID for resumption.
     * @param domain Optional domain for widget association.
     */
    fun connect(
        widgetKey: String,
        name: String,
        email: String,
        phone: String,
        firstMessage: String,
        ticketId: String? = null,
        domain: String? = null
    ) {
        if (isConnecting || isConnected) {
            Log.d(TAG, "Already connected or connecting, skipping")
            return
        }
        isConnecting = true

        try {
            // Build auth payload
            val auth = mutableMapOf<String, String>(
                "widget" to widgetKey,
                "widgetKey" to widgetKey,
                "name" to name,
                "email" to email,
                "phone" to phone,
                "firstMessage" to firstMessage
            )
            ticketId?.let { auth["ticketId"] = it }
            domain?.let { auth["domain"] = it }

            val options = IO.Options.builder()
                .setAuth(auth)
                .setTransports(arrayOf("websocket"))
                .setReconnection(true)
                .setReconnectionAttempts(10)
                .setReconnectionDelay(1000)
                .build()

            val uri = URI.create("${ApiConstants.SOCKET_URL}${ApiConstants.SOCKET_NAMESPACE}")
            socket = IO.socket(uri, options)

            // Core connection events
            socket?.on(Socket.EVENT_CONNECT) {
                Log.d(TAG, "Socket connected")
                isConnected = true
                isConnecting = false
                onConnect?.invoke()
            }

            socket?.on(Socket.EVENT_DISCONNECT) { args ->
                val reason = args.firstOrNull()?.toString() ?: "unknown"
                Log.d(TAG, "Socket disconnected: $reason")
                isConnected = false
                onDisconnect?.invoke(reason)
            }

            socket?.on(Socket.EVENT_CONNECT_ERROR) { args ->
                val error = args.firstOrNull()
                Log.e(TAG, "Socket connect error: $error")
                isConnecting = false
                onConnectError?.invoke(
                    if (error is Exception) error else Exception(error?.toString() ?: "Connection error")
                )
            }

            // Register all stored event listeners (wrap in Emitter.Listener)
            for ((event, listener) in eventListeners) {
                socket?.on(event) { args -> listener(args) }
            }

            socket?.connect()
            Log.d(TAG, "Socket connecting to ${ApiConstants.SOCKET_URL}")

        } catch (e: Exception) {
            Log.e(TAG, "Failed to create socket: ${e.message}")
            isConnecting = false
            onConnectError?.invoke(e)
        }
    }

    /** Register a listener for a socket event. */
    fun on(event: String, listener: (Array<Any>) -> Unit) {
        eventListeners[event] = listener
        socket?.on(event) { args -> listener(args) }
    }

    /** Remove a listener for a socket event. */
    fun off(event: String) {
        eventListeners.remove(event)
        socket?.off(event)
    }

    /** Emit an event with data. */
    fun emit(event: String, data: JSONObject) {
        if (!isConnected) {
            Log.w(TAG, "Cannot emit '$event' — not connected")
            return
        }
        Log.d(TAG, "⬆ EMIT [$event] ${data.toString().take(500)}")
        socket?.emit(event, data)
    }

    /** Emit an event without data. */
    fun emit(event: String) {
        if (!isConnected) {
            Log.w(TAG, "Cannot emit '$event' — not connected")
            return
        }
        Log.d(TAG, "⬆ EMIT [$event] (no data)")
        socket?.emit(event)
    }

    /** Disconnect and clean up. */
    fun disconnect() {
        Log.d(TAG, "Disconnecting socket")
        socket?.disconnect()
        socket?.off()
        socket = null
        isConnected = false
        isConnecting = false
    }

    companion object {
        private const val TAG = "LiveConnect.Socket"

        @Volatile
        private var instance: SocketService? = null

        @JvmStatic
        fun getInstance(): SocketService {
            return instance ?: synchronized(this) {
                instance ?: SocketService().also { instance = it }
            }
        }

        /** Reset the singleton (for testing or re-initialization). */
        @JvmStatic
        fun reset() {
            instance?.disconnect()
            instance = null
        }

        // Socket event names (incoming)
        const val EVENT_TICKET_CREATED = "ticket:created"
        const val EVENT_TICKET_RESUMED = "ticket:resumed"
        const val EVENT_TICKET_RESOLVED = "ticket:resolved"
        const val EVENT_TICKET_UNREAD_COUNT = "ticket:unread_count"
        const val EVENT_TICKET_RATE_PROMPT = "ticket:rate:prompt"
        const val EVENT_MESSAGE_RECEIVED = "message:received"
        const val EVENT_MESSAGES_STATUS_UPDATED = "messages:status_updated"
        const val EVENT_AGENT_TYPING = "agent:typing"
        const val EVENT_AGENT_CHANGED = "agent:changed"
        const val EVENT_AGENT_REASSIGNED = "agent:reassigned"
        const val EVENT_AGENT_STATUS = "agent:status"
        const val EVENT_BROADCAST_MESSAGE = "broadcast:message"
        const val EVENT_TICKET_ASSIGNED = "ticket:assigned"

        // Socket event names (outgoing)
        const val EMIT_MESSAGE_SEND = "message:send"
        const val EMIT_MESSAGE_DELIVERED = "message:delivered"
        const val EMIT_MESSAGE_READ = "message:read"
        const val EMIT_TYPING = "typing"
        const val EMIT_TICKET_RESOLVE = "ticket:resolve"
        const val EMIT_TICKET_RATE = "ticket:rate"
    }
}
