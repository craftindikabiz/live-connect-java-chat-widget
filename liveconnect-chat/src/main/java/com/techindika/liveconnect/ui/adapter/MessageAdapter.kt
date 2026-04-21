package com.techindika.liveconnect.ui.adapter

import android.content.Intent
import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.techindika.liveconnect.LiveConnectTheme
import com.techindika.liveconnect.R
import com.techindika.liveconnect.model.Message
import com.techindika.liveconnect.model.MessageSender
import com.techindika.liveconnect.model.MessageStatus
import java.text.SimpleDateFormat
import java.util.Locale

/**
 * RecyclerView adapter for chat messages with multiple view types.
 */
internal class MessageAdapter(
    private val theme: LiveConnectTheme
) : ListAdapter<Message, RecyclerView.ViewHolder>(MessageDiffCallback()) {

    override fun getItemViewType(position: Int): Int {
        val message = getItem(position)
        return when (message.sender) {
            MessageSender.VISITOR -> VIEW_TYPE_VISITOR
            MessageSender.AGENT -> VIEW_TYPE_AGENT
            MessageSender.SYSTEM, MessageSender.BROADCAST -> VIEW_TYPE_SYSTEM
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return when (viewType) {
            VIEW_TYPE_VISITOR -> {
                val view = inflater.inflate(R.layout.item_message_visitor, parent, false)
                VisitorViewHolder(view)
            }
            VIEW_TYPE_AGENT -> {
                val view = inflater.inflate(R.layout.item_message_agent, parent, false)
                AgentViewHolder(view)
            }
            else -> {
                val view = inflater.inflate(R.layout.item_message_system, parent, false)
                SystemViewHolder(view)
            }
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val message = getItem(position)
        when (holder) {
            is VisitorViewHolder -> holder.bind(message, theme)
            is AgentViewHolder -> holder.bind(message, theme)
            is SystemViewHolder -> holder.bind(message, theme)
        }
    }

    // ── View Holders ──

    class VisitorViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val bubbleContainer: LinearLayout = itemView.findViewById(R.id.bubbleContainer)
        private val messageText: TextView = itemView.findViewById(R.id.messageText)
        private val timestamp: TextView = itemView.findViewById(R.id.timestamp)
        private val statusIcon: TextView = itemView.findViewById(R.id.statusIcon)
        private val attachmentImage: ImageView = itemView.findViewById(R.id.attachmentImage)
        private val attachmentDoc: LinearLayout = itemView.findViewById(R.id.attachmentDoc)
        private val docEmoji: TextView = itemView.findViewById(R.id.docEmoji)
        private val docName: TextView = itemView.findViewById(R.id.docName)

        fun bind(message: Message, theme: LiveConnectTheme) {
            messageText.text = message.text
            messageText.setTextColor(theme.visitorTextColor)
            messageText.visibility = if (message.text.isNotEmpty()) View.VISIBLE else View.GONE

            timestamp.text = TIME_FORMAT.get()!!.format(message.timestamp)

            // Status icon — glyph + colour mirror Flutter's _buildStatusIndicator.
            // Read state uses golden so it stands out against the primary-colour bubble.
            when (message.status) {
                MessageStatus.SENDING -> {
                    statusIcon.text = "\u2713" // ✓ single check (sending)
                    statusIcon.setTextColor(STATUS_COLOR_TRANSLUCENT_WHITE)
                }
                MessageStatus.SENT -> {
                    statusIcon.text = "\u2713" // ✓ single check
                    statusIcon.setTextColor(STATUS_COLOR_TRANSLUCENT_WHITE)
                }
                MessageStatus.DELIVERED -> {
                    statusIcon.text = "\u2713\u2713" // ✓✓ double check
                    statusIcon.setTextColor(STATUS_COLOR_TRANSLUCENT_WHITE)
                }
                MessageStatus.READ -> {
                    statusIcon.text = "\u2713\u2713" // ✓✓ double check, golden
                    statusIcon.setTextColor(STATUS_COLOR_GOLD)
                }
            }

            // Attachment
            val attachment = message.attachment
            if (attachment != null && attachment.isImage) {
                attachmentImage.visibility = View.VISIBLE
                attachmentDoc.visibility = View.GONE
                Glide.with(itemView.context)
                    .load(attachment.filePath)
                    .centerCrop()
                    .into(attachmentImage)
            } else if (attachment != null) {
                attachmentImage.visibility = View.GONE
                attachmentDoc.visibility = View.VISIBLE
                docEmoji.text = attachment.typeEmoji
                docName.text = attachment.filename
                // Open the document in the user's default handler on tap.
                // Mirrors Flutter's url_launcher path in message_bubble.dart.
                attachmentDoc.setOnClickListener { v ->
                    openAttachment(v, attachment.filePath)
                }
            } else {
                attachmentImage.visibility = View.GONE
                attachmentDoc.visibility = View.GONE
                attachmentDoc.setOnClickListener(null)
            }

            // Theme bubble color
            bubbleContainer.background?.setTint(theme.visitorBubbleColor)
        }
    }

    class AgentViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val bubbleContainer: LinearLayout = itemView.findViewById(R.id.bubbleContainer)
        private val messageText: TextView = itemView.findViewById(R.id.messageText)
        private val timestamp: TextView = itemView.findViewById(R.id.timestamp)
        private val attachmentImage: ImageView = itemView.findViewById(R.id.attachmentImage)
        private val attachmentDoc: LinearLayout = itemView.findViewById(R.id.attachmentDoc)
        private val docEmoji: TextView = itemView.findViewById(R.id.docEmoji)
        private val docName: TextView = itemView.findViewById(R.id.docName)

        fun bind(message: Message, theme: LiveConnectTheme) {
            messageText.text = message.text
            messageText.setTextColor(theme.agentTextColor)
            messageText.visibility = if (message.text.isNotEmpty()) View.VISIBLE else View.GONE

            timestamp.text = TIME_FORMAT.get()!!.format(message.timestamp)

            val attachment = message.attachment
            if (attachment != null && attachment.isImage) {
                attachmentImage.visibility = View.VISIBLE
                attachmentDoc.visibility = View.GONE
                Glide.with(itemView.context)
                    .load(attachment.filePath)
                    .centerCrop()
                    .into(attachmentImage)
            } else if (attachment != null) {
                attachmentImage.visibility = View.GONE
                attachmentDoc.visibility = View.VISIBLE
                docEmoji.text = attachment.typeEmoji
                docName.text = attachment.filename
                attachmentDoc.setOnClickListener { v ->
                    openAttachment(v, attachment.filePath)
                }
            } else {
                attachmentImage.visibility = View.GONE
                attachmentDoc.visibility = View.GONE
                attachmentDoc.setOnClickListener(null)
            }

            bubbleContainer.background?.setTint(theme.agentBubbleColor)
        }
    }

    class SystemViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val systemText: TextView = itemView.findViewById(R.id.systemText)

        fun bind(message: Message, theme: LiveConnectTheme) {
            systemText.text = message.text
            // Broadcast events (e.g. "Chat reassigned to John") use a distinct colour
            // so they don't visually compete with regular system notices.
            // Mirrors Flutter's _buildBroadcastChip vs _buildSystemMessage.
            if (message.sender == MessageSender.BROADCAST) {
                systemText.setTextColor(theme.agentTextColor)
                systemText.background?.setTint(theme.broadcastMessageBackgroundColor)
            } else {
                systemText.setTextColor(theme.systemMessageTextColor)
                systemText.background?.setTint(theme.systemMessageBackgroundColor)
            }
        }
    }

    companion object {
        /**
         * Open a remote attachment URL in the OS default handler (browser, PDF
         * viewer, etc.). Mirrors Flutter's `url_launcher` behaviour. No-op on
         * empty url or activity-not-found — failure is silent rather than crashy.
         */
        private fun openAttachment(anchor: View, url: String) {
            if (url.isEmpty()) return
            try {
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                anchor.context.startActivity(intent)
            } catch (_: Exception) {
                // No matching activity / malformed url — ignore silently.
            }
        }

        private const val VIEW_TYPE_VISITOR = 0
        private const val VIEW_TYPE_AGENT = 1
        private const val VIEW_TYPE_SYSTEM = 2

        // Status icon colours on visitor (primary-coloured) bubbles.
        // Translucent white = sent/delivered, gold = read — matches Flutter intent.
        private const val STATUS_COLOR_TRANSLUCENT_WHITE = 0xB3FFFFFF.toInt()
        private const val STATUS_COLOR_GOLD = 0xFFFFD700.toInt()

        private val TIME_FORMAT = ThreadLocal.withInitial { SimpleDateFormat("h:mm a", Locale.US) }
    }
}

internal class MessageDiffCallback : DiffUtil.ItemCallback<Message>() {
    override fun areItemsTheSame(oldItem: Message, newItem: Message) = oldItem.id == newItem.id
    override fun areContentsTheSame(oldItem: Message, newItem: Message) = oldItem == newItem
}
