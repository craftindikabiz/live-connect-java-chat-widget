package com.techindika.liveconnect.callback

/**
 * Generic callback for async operations. Java-friendly.
 */
interface LiveConnectCallback<T> {
    fun onSuccess(result: T)
    fun onFailure(error: String)
}

/**
 * Callback for initialization. Java-friendly.
 */
interface InitCallback {
    fun onSuccess()
    fun onFailure(error: String)
}

/**
 * Listener for unread count changes. Java-friendly.
 */
interface UnreadCountListener {
    fun onUnreadCountChanged(count: Int)
}
