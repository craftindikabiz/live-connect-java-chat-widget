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
import com.techindika.liveconnect.R
import com.techindika.liveconnect.model.*
import com.techindika.liveconnect.network.ApiResult
import com.techindika.liveconnect.network.RetrofitClient
import com.techindika.liveconnect.service.*
import com.techindika.liveconnect.socket.SocketEventManager
import com.techindika.liveconnect.socket.SocketService
import com.techindika.liveconnect.ui.adapter.MessageAdapter
import kotlinx.coroutines.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
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

    private val vm: ChatViewModel by activityViewModels()
    private val conversationManager: ConversationManager get() = vm.conversationManager
    private val socketService: SocketService get() = vm.socketService
    private val socketEventManager: SocketEventManager get() = vm.socketEventManager

    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private var isSending = false
    private var isSocketConnected = false
    private var isSocketConnecting = false
    private var currentAgent: AgentInfo? = null
    private var pendingFirstMessage: String? = null

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

        // Observe thread changes (managers come from the activity-scoped ViewModel)
        conversationManager.threads.observe(viewLifecycleOwner) { threads ->
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
        if (thread != null) {
            conversationManager.switchToThread(thread.id)
        }

        // Always reload messages from the API (resolved tickets show as read-only)
        loadMessagesForTicket(ticketId)

        // Reconnect the socket if needed (open tickets only — closed ones stay read-only)
        if (thread != null && !thread.isClosed && !isSocketConnected) {
            connectSocketToResume(ticketId)
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
                            connectSocketToResume(ticketToResumeId)
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

    private fun loadMessagesForTicket(ticketId: String) {
        val widgetKey = LiveConnectChat.widgetKey ?: return
        val profile = LiveConnectChat.visitorProfile ?: return

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
                    val threadId = conversationManager.activeThreadId.value
                    if (threadId != null) {
                        conversationManager.updateThreadMessages(threadId, result.messages)
                    }
                }
            } catch (e: Exception) {
                Log.w(TAG, "Failed to load messages: ${e.message}")
            }
        }
    }

    private fun connectSocketToResume(ticketId: String) {
        if (isSocketConnecting || isSocketConnected) return
        isSocketConnecting = true

        val widgetKey = LiveConnectChat.widgetKey ?: return
        val profile = LiveConnectChat.visitorProfile ?: return
        val thread = conversationManager.activeThread

        registerSocketEventHandlers()

        socketService.connect(
            widgetKey = widgetKey,
            name = profile.name,
            email = profile.email,
            phone = profile.phone,
            firstMessage = thread?.firstMessage ?: thread?.lastMessage ?: "__resume__$ticketId",
            ticketId = ticketId
        )

        socketService.onConnect = {
            isSocketConnected = true
            isSocketConnecting = false
        }
    }

    private fun connectSocketWithFirstMessage(text: String) {
        if (isSocketConnecting || isSocketConnected) return
        isSocketConnecting = true

        val widgetKey = LiveConnectChat.widgetKey ?: return
        val profile = LiveConnectChat.visitorProfile ?: return

        registerSocketEventHandlers()

        socketService.connect(
            widgetKey = widgetKey,
            name = profile.name,
            email = profile.email,
            phone = profile.phone,
            firstMessage = text
        )

        socketService.onConnect = {
            isSocketConnected = true
            isSocketConnecting = false
        }
    }

    private fun registerSocketEventHandlers() {
        socketEventManager.registerListeners()

        socketEventManager.onTicketCreated = handler@{ event ->
            val widgetKey = LiveConnectChat.widgetKey ?: return@handler
            val visitorId = LiveConnectChat.visitorId ?: ""
            val context = LiveConnectChat.appContext ?: return@handler

            conversationManager.setTicketIdForActiveThread(event.ticketId)
            TicketStorage.saveActiveTicketId(context, widgetKey, visitorId, event.ticketId)
            // Save status as open so the next session knows the ticket is still active.
            TicketStorage.saveTicketStatus(
                context, widgetKey, visitorId, TicketStorage.STATUS_OPEN
            )
            event.agent?.let { currentAgent = it }

            // Emit delivered status
            socketService.emit(SocketService.EMIT_MESSAGE_DELIVERED, JSONObject().apply {
                put("ticketId", event.ticketId)
            })
        }

        // Agent reassigned during an active session — update the AgentInfoChip.
        // Mirrors Flutter's _handleTicketAssigned in SocketEventManager.
        socketEventManager.onTicketAssigned = { agent ->
            currentAgent = agent
        }

        // Show a toast when the socket drops; the underlying socket.io client
        // handles reconnection attempts itself, so we only need to surface the state.
        socketEventManager.onSocketDisconnect = { _ ->
            isSocketConnected = false
            if (isAdded) {
                Toast.makeText(
                    requireContext(),
                    "Connection lost. Attempting to reconnect…",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }

        socketEventManager.onTicketResumed = handler@{ event ->
            event.agent?.let { currentAgent = it }

            socketService.emit(SocketService.EMIT_MESSAGE_DELIVERED, JSONObject().apply {
                put("ticketId", event.ticketId)
            })
        }

        socketEventManager.onTicketResolved = handler@{ event ->
            val widgetKey = LiveConnectChat.widgetKey ?: return@handler
            val visitorId = LiveConnectChat.visitorId ?: ""
            val context = LiveConnectChat.appContext ?: return@handler

            conversationManager.markActiveThreadAsResolved()
            // clearActiveTicketId() also clears the status key — see TicketStorage.
            TicketStorage.clearActiveTicketId(context, widgetKey, visitorId)
            // Persist the resolved status so the next launch knows not to resume.
            TicketStorage.saveTicketStatus(
                context, widgetKey, visitorId, TicketStorage.STATUS_RESOLVED
            )
            socketService.disconnect()
            isSocketConnected = false
            currentAgent = null
            if (isAdded) {
                Toast.makeText(requireContext(), R.string.lc_conversation_resolved, Toast.LENGTH_SHORT).show()
            }
        }

        socketEventManager.onMessageReceived = { message ->
            // Check if this is our own message echoed back
            val pending = socketEventManager.matchPendingMessage(message.text)
            if (pending != null) {
                conversationManager.replaceOptimisticMessage(pending.optimisticId, message)
            } else {
                conversationManager.addMessageToActiveThread(message)
            }

            // Emit delivered
            conversationManager.activeTicketId?.let { ticketId ->
                socketService.emit(SocketService.EMIT_MESSAGE_DELIVERED, JSONObject().apply {
                    put("ticketId", ticketId)
                })
            }
        }

        socketEventManager.onMessageStatusUpdated = { event ->
            conversationManager.updateMessageStatus(event.messageId, MessageStatus.fromString(event.status))
        }

        socketEventManager.onAgentTyping = { event ->
            // Only show the typing bubble if the event is for the currently active
            // ticket — otherwise typing on an old/closed conversation would leak
            // into the visible chat. Mirrors Flutter's chat_screen_tabbed.dart.
            if (event.ticketId == conversationManager.activeTicketId) {
                // Surface to UI once a typing-bubble view is wired up.
                // For now the scoping itself is the bug fix.
            }
        }

        socketEventManager.onAgentChanged = { event ->
            currentAgent = event.agent
        }

        socketEventManager.onUnreadCount = { event ->
            UnreadCountService.handleUnreadCountEvent(event.ticketId, event.unreadCount)
        }

        socketEventManager.onRatePrompt = { event ->
            showRatingDialog()
        }
    }

    private fun sendMessage(text: String, attachment: Attachment? = null) {
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
        if (!isSocketConnected) {
            pendingFirstMessage = text
            connectSocketWithFirstMessage(text)
            // First message is delivered via the auth payload's firstMessage field.
            // The server creates the ticket and echoes back via ticket:created + message:received.
            // No need to emit separately — it would fail anyway since ticketId doesn't exist yet.
            socketService.onConnect = {
                isSocketConnected = true
                isSocketConnecting = false
            }
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
