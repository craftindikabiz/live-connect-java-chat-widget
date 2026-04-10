package com.techindika.liveconnect.model

import org.json.JSONObject

/**
 * Paginated ticket list result from API.
 */
data class WidgetTicketsResult(
    val tickets: List<WidgetTicket>,
    val pagination: WidgetTicketsPagination
) {
    companion object {
        @JvmStatic
        fun fromJson(json: JSONObject): WidgetTicketsResult {
            val ticketsArray = json.optJSONArray("tickets")
            val tickets = mutableListOf<WidgetTicket>()
            ticketsArray?.let { arr ->
                for (i in 0 until arr.length()) {
                    tickets.add(WidgetTicket.fromJson(arr.getJSONObject(i)))
                }
            }
            val paginationJson = json.optJSONObject("pagination")
            val pagination = if (paginationJson != null) {
                WidgetTicketsPagination.fromJson(paginationJson)
            } else {
                WidgetTicketsPagination(total = tickets.size, page = 1, limit = 20, pages = 1)
            }
            return WidgetTicketsResult(tickets, pagination)
        }
    }
}

/**
 * Pagination metadata for ticket lists.
 */
data class WidgetTicketsPagination(
    val total: Int,
    val page: Int,
    val limit: Int,
    val pages: Int
) {
    companion object {
        @JvmStatic
        fun fromJson(json: JSONObject): WidgetTicketsPagination = WidgetTicketsPagination(
            total = json.optString("total", "0").toIntOrNull() ?: 0,
            page = json.optString("page", "1").toIntOrNull() ?: 1,
            limit = json.optString("limit", "20").toIntOrNull() ?: 20,
            pages = json.optString("pages", "1").toIntOrNull() ?: 1
        )
    }
}

/**
 * Paginated messages result from API.
 */
data class TicketMessagesResult(
    val ticket: WidgetTicket,
    val messages: List<TicketMessage>,
    val pagination: WidgetTicketsPagination
) {
    companion object {
        @JvmStatic
        fun fromJson(json: JSONObject): TicketMessagesResult {
            val ticket = WidgetTicket.fromJson(json.getJSONObject("ticket"))
            val messagesArray = json.optJSONArray("messages")
            val messages = mutableListOf<TicketMessage>()
            messagesArray?.let { arr ->
                for (i in 0 until arr.length()) {
                    messages.add(TicketMessage.fromJson(arr.getJSONObject(i)))
                }
            }
            val paginationJson = json.optJSONObject("pagination")
            val pagination = if (paginationJson != null) {
                WidgetTicketsPagination.fromJson(paginationJson)
            } else {
                WidgetTicketsPagination(total = messages.size, page = 1, limit = 50, pages = 1)
            }
            return TicketMessagesResult(ticket, messages, pagination)
        }
    }
}
