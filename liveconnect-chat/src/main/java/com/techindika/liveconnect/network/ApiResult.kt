package com.techindika.liveconnect.network

/**
 * Sealed result type for API calls. Java-friendly via static factories.
 */
sealed class ApiResult<out T> {
    data class Success<T>(val data: T) : ApiResult<T>()
    data class Failure(val message: String) : ApiResult<Nothing>()

    val isSuccess: Boolean get() = this is Success
    val isFailure: Boolean get() = this is Failure

    /** Returns the data if success, or null. */
    fun getOrNull(): T? = (this as? Success)?.data

    /** Returns the error message if failure, or null. */
    fun errorOrNull(): String? = (this as? Failure)?.message

    companion object {
        @JvmStatic
        fun <T> success(data: T): ApiResult<T> = Success(data)

        @JvmStatic
        fun failure(message: String): ApiResult<Nothing> = Failure(message)
    }
}
