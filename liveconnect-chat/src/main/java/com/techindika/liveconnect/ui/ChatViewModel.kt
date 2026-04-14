package com.techindika.liveconnect.ui

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.techindika.liveconnect.service.ConversationManager
import com.techindika.liveconnect.socket.SocketEventManager
import com.techindika.liveconnect.socket.SocketService
import org.json.JSONObject

/**
 * Activity-scoped ViewModel that owns the chat state shared between
 * ChatTabFragment and ActivityTabFragment.
 *
 * Hoisting these managers out of individual fragments lets the Activity
 * tab tell the Chat tab which thread to load when the user taps an
 * old/resolved ticket — and survives configuration changes.
 */
internal class ChatViewModel : ViewModel() {

    val conversationManager: ConversationManager = ConversationManager()
    val socketService: SocketService = SocketService.getInstance()
    val socketEventManager: SocketEventManager = SocketEventManager(socketService)

    private val _navigateToThread = MutableLiveData<String?>()

    /** Ticket id the Chat tab should load + resume. Cleared after consume. */
    val navigateToThread: LiveData<String?> = _navigateToThread

    fun selectThread(ticketId: String) {
        _navigateToThread.value = ticketId
    }

    fun consumeNavigation() {
        _navigateToThread.value = null
    }

    /** Emit ticket:resolve for the currently-active ticket, if any. */
    fun markActiveTicketResolved() {
        val ticketId = conversationManager.activeTicketId ?: return
        socketService.emit(
            SocketService.EMIT_TICKET_RESOLVE,
            JSONObject().apply { put("ticketId", ticketId) }
        )
    }

    override fun onCleared() {
        super.onCleared()
        // The Activity is being destroyed for good — release the socket.
        socketService.disconnect()
    }
}
