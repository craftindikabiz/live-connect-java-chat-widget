package com.techindika.liveconnect.ui

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.techindika.liveconnect.LiveConnectChat
import com.techindika.liveconnect.R
import com.techindika.liveconnect.model.WidgetTicket
import com.techindika.liveconnect.model.WidgetTicketsResult
import com.techindika.liveconnect.network.RetrofitClient
import com.techindika.liveconnect.ui.adapter.TicketAdapter
import kotlinx.coroutines.*
import org.json.JSONObject

/**
 * Activity tab — shows conversation history (ticket list).
 */
class ActivityTabFragment : Fragment() {

    private val vm: ChatViewModel by activityViewModels()

    private lateinit var recyclerView: RecyclerView
    private lateinit var emptyState: View
    private lateinit var ticketAdapter: TicketAdapter

    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private val tickets = mutableListOf<WidgetTicket>()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return inflater.inflate(R.layout.fragment_activity_tab, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        recyclerView = view.findViewById(R.id.ticketRecyclerView)
        emptyState = view.findViewById(R.id.emptyState)

        ticketAdapter = TicketAdapter(LiveConnectChat.currentTheme) { ticket ->
            // Tell the Chat tab to open this ticket, then switch tabs.
            // Mirrors Flutter's _handleThreadSelect.
            vm.selectThread(ticket.id)
            (activity as? ChatActivity)?.switchToTab(0)
        }
        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        recyclerView.adapter = ticketAdapter

        loadTickets()
    }

    private fun loadTickets() {
        val widgetKey = LiveConnectChat.widgetKey ?: return
        val profile = LiveConnectChat.visitorProfile ?: return

        scope.launch(Dispatchers.IO) {
            try {
                val response = RetrofitClient.apiService.fetchTickets(
                    widgetKey = widgetKey,
                    email = profile.email,
                    phone = profile.phone
                )
                val json = JSONObject(response.string())
                val data = json.optJSONObject("data")
                if (data != null) {
                    val result = WidgetTicketsResult.fromJson(data)
                    withContext(Dispatchers.Main) {
                        tickets.clear()
                        tickets.addAll(result.tickets)
                        ticketAdapter.submitList(tickets.toList())
                        emptyState.visibility = if (tickets.isEmpty()) View.VISIBLE else View.GONE
                        recyclerView.visibility = if (tickets.isEmpty()) View.GONE else View.VISIBLE
                    }
                }
            } catch (e: Exception) {
                Log.w(TAG, "Failed to load tickets: ${e.message}")
                withContext(Dispatchers.Main) {
                    emptyState.visibility = View.VISIBLE
                    recyclerView.visibility = View.GONE
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        scope.cancel()
    }

    companion object {
        private const val TAG = "LiveConnect.ActivityTab"
    }
}
