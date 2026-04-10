package com.techindika.liveconnect.service

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData

/**
 * Tracks unread message counts per ticket.
 * Observable via LiveData for badge updates.
 */
internal object UnreadCountService {

    private val counts = mutableMapOf<String, Int>()

    private val _totalUnreadCount = MutableLiveData(0)

    /** Total unread count across all tickets. Observable. */
    val totalUnreadCount: LiveData<Int> = _totalUnreadCount

    /** Handle an unread count event from the socket. */
    @JvmStatic
    fun handleUnreadCountEvent(ticketId: String, unreadCount: Int) {
        counts[ticketId] = unreadCount
        _totalUnreadCount.postValue(counts.values.sum())
    }

    /** Get the current total. */
    @JvmStatic
    fun getTotalUnreadCount(): Int = counts.values.sum()

    /** Clear counts for a specific ticket. */
    @JvmStatic
    fun clearForTicket(ticketId: String) {
        counts.remove(ticketId)
        _totalUnreadCount.postValue(counts.values.sum())
    }

    /** Reset all counts. */
    @JvmStatic
    fun reset() {
        counts.clear()
        _totalUnreadCount.postValue(0)
    }
}
