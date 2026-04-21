package com.techindika.liveconnect.ui

import android.net.Uri
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.techindika.liveconnect.LiveConnectChat
import com.techindika.liveconnect.LiveConnectTheme
import com.techindika.liveconnect.R
import com.techindika.liveconnect.model.*
import com.techindika.liveconnect.network.ApiResult
import com.techindika.liveconnect.network.RetrofitClient
import com.techindika.liveconnect.service.*
import com.techindika.liveconnect.socket.SocketEventManager
import com.techindika.liveconnect.socket.SocketService
import com.techindika.liveconnect.ui.adapter.MessageAdapter
import kotlinx.coroutines.*
import org.json.JSONObject
import java.util.Date

/**
 * Chat tab fragment — message list, input bar, socket events.
 */
class ChatTabFragment : Fragment() {

    private lateinit var messageRecyclerView: RecyclerView
    private lateinit var messageAdapter: MessageAdapter
    private lateinit var inputEditText: EditText
    private lateinit var sendButton: ImageButton
    private lateinit var attachButton: ImageButton
    private lateinit var emptyChatState: View
    private lateinit var readOnlyNotice: View
    private lateinit var suggestedMessagesContainer: android.widget.HorizontalScrollView
    private lateinit var suggestedMessagesRow: android.widget.LinearLayout
    private lateinit var agentChipContainer: android.widget.FrameLayout
    private lateinit var chatSwipeRefresh: androidx.swiperefreshlayout.widget.SwipeRefreshLayout

    private val vm: ChatViewModel by activityViewModels()
    private val conversationManager: ConversationManager get() = vm.conversationManager
    private val socketService: SocketService get() = vm.socketService
    private val socketEventManager: SocketEventManager get() = vm.socketEventManager

    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private var isSending = false
    private lateinit var socketHandler: ChatTabSocketHandler

    // File picker
    private val filePickerLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { handleFilePicked(it) }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return inflater.inflate(R.layout.fragment_chat_tab, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val theme = LiveConnectChat.currentTheme

        // Bind views
        messageRecyclerView = view.findViewById(R.id.messageRecyclerView)
        inputEditText = view.findViewById(R.id.inputEditText)
        sendButton = view.findViewById(R.id.sendButton)
        attachButton = view.findViewById(R.id.attachButton)
        emptyChatState = view.findViewById(R.id.emptyChatState)
        readOnlyNotice = view.findViewById(R.id.readOnlyNotice)
        suggestedMessagesContainer = view.findViewById(R.id.suggestedMessagesContainer)
        suggestedMessagesRow = view.findViewById(R.id.suggestedMessagesRow)
        agentChipContainer = view.findViewById(R.id.agentChipContainer)
        chatSwipeRefresh = view.findViewById(R.id.chatSwipeRefresh)

        chatSwipeRefresh.setColorSchemeColors(theme.primaryColor)
        chatSwipeRefresh.setOnRefreshListener { refreshChat() }

        applyEmptyStateTheme(theme)

        // "Start new conversation" button inside the read-only notice
        val newConversationButton = view.findViewById<android.widget.Button>(R.id.newConversationButton)
        newConversationButton.setTextColor(theme.primaryColor)
        newConversationButton.setOnClickListener { startNewConversation() }

        // Setup RecyclerView
        val layoutManager = LinearLayoutManager(requireContext())
        layoutManager.stackFromEnd = true
        messageRecyclerView.layoutManager = layoutManager
        messageAdapter = MessageAdapter(theme)
        messageRecyclerView.adapter = messageAdapter

        // Apply theme colors
        sendButton.setColorFilter(theme.sendButtonIconColor)
        inputEditText.setTextColor(theme.inputFieldTextColor)
        inputEditText.setHintTextColor(theme.inputFieldHintColor)

        // Input text watcher
        inputEditText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                sendButton.isEnabled = !s.isNullOrBlank() && !isSending
            }
        })

        // Send button
        sendButton.setOnClickListener {
            val text = inputEditText.text.toString().trim()
            if (text.isNotEmpty()) {
                sendMessage(text)
            }
        }

        // Attach button
        attachButton.setOnClickListener { showAttachmentMenu() }

        // Initialize socket handler
        socketHandler = ChatTabSocketHandler(conversationManager, socketService, socketEventManager)
        socketHandler.onAgentUpdated = { updateAgentChip() }
        socketHandler.onDisconnected = { _ ->
            if (isAdded) {
                Toast.makeText(
                    requireContext(),
                    "Connection lost. Attempting to reconnect…",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
        socketHandler.onTicketResolved = {
            // Notify ActivityTabFragment to refresh its ticket list from the API.
            vm.notifyTicketResolved()
            if (isAdded) {
                // Immediate UI feedback — hide input, show read-only notice.
                val inputContainer = view?.findViewById<View>(R.id.messageInput)
                inputContainer?.visibility = View.GONE
                readOnlyNotice.visibility = View.VISIBLE
                Toast.makeText(requireContext(), R.string.lc_conversation_resolved, Toast.LENGTH_SHORT).show()
                // Re-fetch tickets from API so conversationManager reflects the true
                // server state (resolved ticket closed, any other open ticket becomes active).
                loadTicketsFromAPI()
                // Switch to Activity tab so the user sees the refreshed ticket list.
                (activity as? ChatActivity)?.switchToTab(1)
            }
        }
        socketHandler.onRatePrompted = { showRatingDialog() }

        // Observe thread changes (managers come from the activity-scoped ViewModel)
        conversationManager.threads.observe(viewLifecycleOwner) { threads ->
            val active = conversationManager.activeThread
            if (active != null) {
                updateUI(active)
            }
        }

        // Also observe activeThreadId for UI refreshes.
        conversationManager.activeThreadId.observe(viewLifecycleOwner) { _ ->
            val active = conversationManager.activeThread
            if (active != null) {
                updateUI(active)
            }
        }

        // Observe navigation requests from ActivityTabFragment ("tap an old ticket")
        vm.navigateToThread.observe(viewLifecycleOwner) { ticketId ->
            if (ticketId != null) {
                openTicket(ticketId)
                vm.consumeNavigation()
            }
        }

        // Load tickets and connect (only once — VM survives fragment recreation)
        if (conversationManager.threads.value.isNullOrEmpty()) {
            loadTicketsFromAPI()
        }
    }

    /**
     * Switch the active thread to the one matching the given ticket id and
     * pull its messages from the API. Mirrors Flutter's _handleThreadSelect.
     */
    private fun openTicket(ticketId: String) {
        // Find the local thread that maps to this ticket id
        val thread = conversationManager.threads.value
            ?.firstOrNull { conversationManager.getTicketIdForThread(it.id) == ticketId }

        // If the thread is not in local state, don't load messages into whatever
        // thread happens to be active (could be an unrelated open thread).
        if (thread == null) return

        conversationManager.switchToThread(thread.id)

        // Reload messages from the API (resolved tickets show as read-only)
        loadMessagesForTicket(ticketId)

        // Reconnect the socket if needed (open tickets only — closed ones stay read-only)
        if (!thread.isClosed && !socketHandler.isSocketConnected) {
            socketHandler.connectToResume(ticketId)
        }
    }

    private fun loadTicketsFromAPI() {
        val widgetKey = LiveConnectChat.widgetKey ?: return
        val profile = LiveConnectChat.visitorProfile ?: return
        val context = LiveConnectChat.appContext ?: return
        val visitorId = LiveConnectChat.visitorId ?: ""

        scope.launch(Dispatchers.IO) {
            try {
                // Load stored ticket ID
                val storedTicketId = TicketStorage.loadActiveTicketId(context, widgetKey, visitorId)

                // Fetch tickets from API
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
                        conversationManager.initializeFromTickets(result.tickets)

                        // ── Two-tier resumption strategy (matches Flutter) ──
                        var ticketToResumeId: String? = null

                        // Tier 1: try the stored ticket id, but only if it's still OPEN.
                        // If the agent resolved it while the app was offline, we drop it
                        // and fall through to Tier 2.
                        if (!storedTicketId.isNullOrEmpty()) {
                            val stored = result.tickets.firstOrNull { it.id == storedTicketId }
                            if (stored != null && stored.status == "open") {
                                ticketToResumeId = stored.id
                                TicketStorage.saveTicketStatus(
                                    context, widgetKey, visitorId, TicketStorage.STATUS_OPEN
                                )
                            } else if (stored != null) {
                                // Stored ticket was resolved in the background — clear it.
                                Log.d(TAG, "Stored ticket $storedTicketId was resolved in background")
                                TicketStorage.clearActiveTicketId(context, widgetKey, visitorId)
                                TicketStorage.saveTicketStatus(
                                    context, widgetKey, visitorId, TicketStorage.STATUS_RESOLVED
                                )
                            }
                        }

                        // Tier 2: fall back to the first open ticket from the API list
                        // (handles app uninstall/reinstall — fresh slate, no stored id).
                        if (ticketToResumeId == null) {
                            val firstOpen = result.tickets.firstOrNull { it.status == "open" }
                            if (firstOpen != null) {
                                ticketToResumeId = firstOpen.id
                                TicketStorage.saveActiveTicketId(
                                    context, widgetKey, visitorId, firstOpen.id
                                )
                                TicketStorage.saveTicketStatus(
                                    context, widgetKey, visitorId, TicketStorage.STATUS_OPEN
                                )
                            }
                        }

                        if (ticketToResumeId != null) {
                            loadMessagesForTicket(ticketToResumeId)
                            socketHandler.connectToResume(ticketToResumeId)
                        }
                    }
                } else {
                    withContext(Dispatchers.Main) {
                        conversationManager.initializeWithNewThread()
                    }
                }
            } catch (e: Exception) {
                Log.w(TAG, "Failed to load tickets: ${e.message}")
                withContext(Dispatchers.Main) {
                    conversationManager.initializeWithNewThread()
                }
            }
        }
    }

    /** Pull-to-refresh: reload messages for the active ticket. */
    private fun refreshChat() {
        val ticketId = conversationManager.activeTicketId
        if (ticketId != null) {
            loadMessagesForTicket(ticketId, dismissRefresh = true)
        } else {
            chatSwipeRefresh.isRefreshing = false
        }
    }

    private fun loadMessagesForTicket(ticketId: String, dismissRefresh: Boolean = false) {
        val widgetKey = LiveConnectChat.widgetKey ?: run {
            if (dismissRefresh) chatSwipeRefresh.isRefreshing = false
            return
        }
        val profile = LiveConnectChat.visitorProfile ?: run {
            if (dismissRefresh) chatSwipeRefresh.isRefreshing = false
            return
        }

        scope.launch(Dispatchers.IO) {
            try {
                val response = RetrofitClient.apiService.fetchMessages(
                    widgetKey = widgetKey,
                    ticketId = ticketId,
                    email = profile.email
                )
                val json = JSONObject(response.string())
                val data = json.optJSONObject("data")
                if (data != null) {
                    val result = TicketMessagesResult.fromJson(data)
                    withContext(Dispatchers.Main) {
                        val threadId = conversationManager.activeThreadId.value
                        if (threadId != null) {
                            conversationManager.updateThreadMessages(threadId, result.messages)
                        }
                    }
                }
            } catch (e: Exception) {
                Log.w(TAG, "Failed to load messages: ${e.message}")
            } finally {
                if (dismissRefresh) {
                    withContext(Dispatchers.Main) { chatSwipeRefresh.isRefreshing = false }
                }
            }
        }
    }

    /**
     * Start a fresh conversation. Resets the socket handler and creates a new
     * client-side thread. The user's first message will open a new ticket on the server.
     */
    private fun startNewConversation() {
        socketService.disconnect()
        socketHandler.reset()
        conversationManager.initializeWithNewThread()
    }

    private fun sendMessage(text: String, attachment: Attachment? = null) {
        // Hard guard — never send on a closed/resolved thread regardless of UI state.
        if (conversationManager.activeThread?.isClosed == true) return

        isSending = true
        sendButton.isEnabled = false

        // Create optimistic message
        val optimisticId = socketEventManager.nextOptimisticId()
        socketEventManager.trackPendingMessage(optimisticId, text)

        val message = Message(
            id = optimisticId,
            text = text,
            sender = MessageSender.VISITOR,
            timestamp = Date(),
            attachment = attachment,
            status = MessageStatus.SENDING
        )
        conversationManager.addMessageToActiveThread(message)
        inputEditText.text.clear()

        // If no socket connection, connect with first message (sent via auth payload)
        if (!socketHandler.isSocketConnected) {
            socketHandler.connectWithFirstMessage(text)
            // First message is delivered via the auth payload's firstMessage field.
            // The server creates the ticket and echoes back via ticket:created + message:received.
            // No need to emit separately — it would fail anyway since ticketId doesn't exist yet.
        } else {
            emitMessage(text, attachment)
        }

        isSending = false
    }

    private fun emitMessage(text: String, attachment: Attachment? = null) {
        val ticketId = conversationManager.activeTicketId ?: return
        val data = JSONObject().apply {
            put("ticketId", ticketId)
            put("content", text)
            put("type", if (attachment != null) {
                if (attachment.isImage) "image" else "document"
            } else "text")
            attachment?.let {
                put("fileUrl", it.filePath)
                put("fileName", it.filename)
                put("fileType", if (it.isImage) "image" else "document")
            }
        }
        socketService.emit(SocketService.EMIT_MESSAGE_SEND, data)
    }

    private fun updateUI(thread: ConversationThread) {
        val messages = thread.messages
        if (messages.isEmpty()) {
            messageRecyclerView.visibility = View.GONE
            emptyChatState.visibility = View.VISIBLE
        } else {
            messageRecyclerView.visibility = View.VISIBLE
            emptyChatState.visibility = View.GONE
            messageAdapter.submitList(messages)
            messageRecyclerView.scrollToPosition(messages.size - 1)
        }

        // Read-only mode
        val inputContainer = view?.findViewById<View>(R.id.messageInput)
        if (thread.isClosed) {
            readOnlyNotice.visibility = View.VISIBLE
            inputContainer?.visibility = View.GONE
        } else {
            readOnlyNotice.visibility = View.GONE
            inputContainer?.visibility = View.VISIBLE
        }

        // ── Starter suggestions ────────────────────────────────────────────
        // Mirrors Flutter's chat_screen_tabbed.dart:
        //   showSuggestions = !hasVisitorMessages && activeThread.isActive
        //                     && theme.suggestedMessages.isNotEmpty
        renderSuggestions(thread)

        // Show/hide agent chip based on current agent info
        updateAgentChip()
    }

    /**
     * Inflate and populate the agent info chip when an agent is assigned.
     * Hides the chip when no agent is set (e.g. new/resolved thread).
     */
    private fun updateAgentChip() {
        val agent = socketHandler.currentAgent
        if (agent == null || agent.name.isBlank()) {
            agentChipContainer.visibility = View.GONE
            return
        }

        agentChipContainer.removeAllViews()
        val chipView = layoutInflater.inflate(R.layout.view_agent_info_chip, agentChipContainer, false)

        val agentNameView = chipView.findViewById<android.widget.TextView>(R.id.agentName)
        val agentStatusView = chipView.findViewById<android.widget.TextView>(R.id.agentStatus)
        val agentAvatar = chipView.findViewById<android.widget.ImageView>(R.id.agentAvatar)
        val statusDot = chipView.findViewById<View>(R.id.statusDot)

        agentNameView.text = agent.name
        agentStatusView.text = agent.status.displayText

        // Status dot color
        val dotColor = when (agent.status) {
            com.techindika.liveconnect.model.AgentStatus.ONLINE ->
                resources.getColor(R.color.lc_status_online, null)
            com.techindika.liveconnect.model.AgentStatus.BUSY ->
                resources.getColor(R.color.lc_status_busy, null)
            com.techindika.liveconnect.model.AgentStatus.AWAY ->
                resources.getColor(R.color.lc_status_away, null)
            else ->
                resources.getColor(R.color.lc_status_offline, null)
        }
        (statusDot.background as? android.graphics.drawable.GradientDrawable)?.setColor(dotColor)

        // Avatar
        if (!agent.photo.isNullOrEmpty()) {
            com.bumptech.glide.Glide.with(this).load(agent.photo).circleCrop().into(agentAvatar)
        } else {
            agentAvatar.setImageResource(android.R.drawable.sym_def_app_icon)
        }

        agentChipContainer.addView(chipView)
        agentChipContainer.visibility = View.VISIBLE
    }

    /**
     * Tint the empty-state icon + circle from the active theme. Called once
     * at onViewCreated; the drawables honour theme.emptyChatIconColor and a
     * derived 8% primary tint for the circle (matches Flutter's default).
     */
    private fun applyEmptyStateTheme(theme: LiveConnectTheme) {
        val v = view ?: return
        val circle = v.findViewById<View>(R.id.emptyIconCircle)
        val icon = v.findViewById<android.widget.ImageView>(R.id.emptyIcon)
        val title = v.findViewById<android.widget.TextView>(R.id.emptyTitle)
        val desc = v.findViewById<android.widget.TextView>(R.id.emptyDescription)

        // Circle background at 8% primary (matches Flutter's default derivation)
        val circleBg = android.graphics.drawable.GradientDrawable().apply {
            shape = android.graphics.drawable.GradientDrawable.OVAL
            setColor(LiveConnectTheme.withAlphaColor(theme.emptyChatIconColor, 0.08f))
        }
        circle.background = circleBg
        icon.setColorFilter(theme.emptyChatIconColor)
        title.setTextColor(theme.emptyChatTitleColor)
        desc.setTextColor(theme.emptyChatDescriptionColor)
        title.textSize = theme.emptyChatTitleFontSize
        desc.textSize = theme.emptyChatDescriptionFontSize
    }

    /**
     * Rebuild the suggestion-chip row for the given thread. Shown when:
     *  - the thread is active (not closed),
     *  - the visitor hasn't sent any message yet in this thread, AND
     *  - theme.suggestedMessages is non-empty.
     *
     * Tapping a chip sends it as a visitor message immediately — matches
     * Flutter's _handleSuggestedMessageTap.
     */
    private fun renderSuggestions(thread: ConversationThread) {
        val theme = LiveConnectChat.currentTheme
        val suggestions = theme.suggestedMessages
        val hasVisitorMessage = thread.messages.any {
            it.sender == com.techindika.liveconnect.model.MessageSender.VISITOR
        }
        val shouldShow = thread.isActive && !hasVisitorMessage && suggestions.isNotEmpty()

        suggestedMessagesRow.removeAllViews()
        if (!shouldShow) {
            suggestedMessagesContainer.visibility = View.GONE
            return
        }

        val ctx = requireContext()
        val density = ctx.resources.displayMetrics.density
        fun dp(n: Int) = (n * density).toInt()

        suggestions.forEach { text ->
            val chip = android.widget.TextView(ctx).apply {
                this.text = text
                textSize = 13f
                setTypeface(typeface, android.graphics.Typeface.BOLD)
                setTextColor(theme.primaryColor)
                setPadding(dp(14), dp(8), dp(14), dp(8))
                isSingleLine = true
                ellipsize = android.text.TextUtils.TruncateAt.END

                // Pill background with primary border — tinted drawable so it
                // picks up the active theme's primary colour at render time.
                val bg = android.graphics.drawable.GradientDrawable().apply {
                    shape = android.graphics.drawable.GradientDrawable.RECTANGLE
                    cornerRadius = dp(20).toFloat()
                    setColor(android.graphics.Color.WHITE)
                    setStroke(
                        dp(1),
                        LiveConnectTheme.withAlphaColor(theme.primaryColor, 0.35f)
                    )
                }
                background = bg

                val lp = android.widget.LinearLayout.LayoutParams(
                    android.widget.LinearLayout.LayoutParams.WRAP_CONTENT,
                    android.widget.LinearLayout.LayoutParams.WRAP_CONTENT
                )
                lp.marginEnd = dp(8)
                layoutParams = lp

                setOnClickListener { sendMessage(text) }
            }
            suggestedMessagesRow.addView(chip)
        }
        suggestedMessagesContainer.visibility = View.VISIBLE
    }

    private fun showAttachmentMenu() {
        val popup = PopupMenu(requireContext(), attachButton)
        popup.menu.add(0, 1, 0, R.string.lc_attach_media)
        popup.menu.add(0, 2, 1, R.string.lc_attach_document)
        popup.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                1 -> filePickerLauncher.launch("image/*")
                2 -> filePickerLauncher.launch("application/*")
            }
            true
        }
        popup.show()
    }

    private fun handleFilePicked(uri: Uri) {
        val context = requireContext()
        val attachment = FileUploadService.createAttachmentFromUri(context, uri) ?: return

        // Validate size
        val sizeError = attachment.validateSize()
        if (sizeError != null) {
            Toast.makeText(context, sizeError, Toast.LENGTH_SHORT).show()
            return
        }

        val widgetKey = LiveConnectChat.widgetKey ?: return

        // Upload then send
        scope.launch {
            val result = FileUploadService.upload(context, widgetKey, uri)
            when (result) {
                is ApiResult.Success -> {
                    val uploadResult = result.data
                    val mimeType = attachment.mimeType
                    val serverAttachment = attachment.copy(filePath = uploadResult.fileUrl)
                    val text = inputEditText.text.toString().trim().ifEmpty { attachment.filename }
                    sendMessage(text, serverAttachment)
                }
                is ApiResult.Failure -> {
                    Toast.makeText(context, result.message, Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun showRatingDialog() {
        val dialog = RatingDialogFragment.newInstance()
        dialog.onRatingSubmitted = { rating ->
            conversationManager.activeTicketId?.let { ticketId ->
                socketService.emit(SocketService.EMIT_TICKET_RATE, JSONObject().apply {
                    put("ticketId", ticketId)
                    put("rating", rating)
                })
            }
        }
        dialog.show(parentFragmentManager, "rating_dialog")
    }

    override fun onDestroyView() {
        super.onDestroyView()
        scope.cancel()
        // Don't disconnect socket here — ViewPager2 may recreate fragments on tab switch.
        // Socket is managed by the singleton SocketService and disconnects when:
        // 1. Ticket is resolved (onTicketResolved handler)
        // 2. Activity is destroyed (handled via Activity lifecycle)
    }

    companion object {
        private const val TAG = "LiveConnect.ChatTab"
    }
}
