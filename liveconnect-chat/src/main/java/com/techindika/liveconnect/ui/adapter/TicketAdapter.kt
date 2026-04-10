package com.techindika.liveconnect.ui.adapter

import android.graphics.drawable.GradientDrawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.techindika.liveconnect.LiveConnectTheme
import com.techindika.liveconnect.R
import com.techindika.liveconnect.model.WidgetTicket
import com.techindika.liveconnect.util.TimeUtils

/**
 * RecyclerView adapter for the Activity tab ticket list.
 */
internal class TicketAdapter(
    private val theme: LiveConnectTheme,
    private val onTicketClick: (WidgetTicket) -> Unit
) : ListAdapter<WidgetTicket, TicketAdapter.TicketViewHolder>(TicketDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TicketViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_ticket, parent, false)
        return TicketViewHolder(view)
    }

    override fun onBindViewHolder(holder: TicketViewHolder, position: Int) {
        holder.bind(getItem(position), theme, onTicketClick)
    }

    class TicketViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val title: TextView = itemView.findViewById(R.id.ticketTitle)
        private val timestamp: TextView = itemView.findViewById(R.id.ticketTimestamp)
        private val preview: TextView = itemView.findViewById(R.id.ticketPreview)
        private val status: TextView = itemView.findViewById(R.id.ticketStatus)

        fun bind(ticket: WidgetTicket, theme: LiveConnectTheme, onClick: (WidgetTicket) -> Unit) {
            title.text = ticket.firstMessage?.take(50) ?: "Conversation"
            title.setTextColor(theme.activityTitleColor)

            preview.text = ticket.lastMessage ?: ""

            timestamp.text = TimeUtils.relativeTime(ticket.updatedAt ?: ticket.createdAt)

            val isOpen = ticket.status == "open"
            status.text = if (isOpen) "Active" else "Resolved"
            status.setTextColor(if (isOpen) theme.primaryColor else theme.systemMessageTextColor)
            val bg = status.background as? GradientDrawable ?: GradientDrawable()
            bg.cornerRadius = 12f
            if (isOpen) {
                bg.setColor(android.graphics.Color.argb(26, android.graphics.Color.red(theme.primaryColor), android.graphics.Color.green(theme.primaryColor), android.graphics.Color.blue(theme.primaryColor)))
            } else {
                bg.setColor(theme.systemMessageBackgroundColor)
            }
            status.background = bg

            itemView.setOnClickListener { onClick(ticket) }
        }
    }
}

internal class TicketDiffCallback : DiffUtil.ItemCallback<WidgetTicket>() {
    override fun areItemsTheSame(oldItem: WidgetTicket, newItem: WidgetTicket) = oldItem.id == newItem.id
    override fun areContentsTheSame(oldItem: WidgetTicket, newItem: WidgetTicket) = oldItem == newItem
}
