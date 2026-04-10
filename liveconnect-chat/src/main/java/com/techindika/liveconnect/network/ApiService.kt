package com.techindika.liveconnect.network

import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.ResponseBody
import retrofit2.http.*

/**
 * Retrofit interface for all LiveConnect REST API endpoints.
 */
internal interface ApiService {

    /** Fetch widget configuration. */
    @GET("api/widgets/{widgetKey}")
    suspend fun fetchWidgetConfig(
        @Path("widgetKey") widgetKey: String
    ): ResponseBody

    /** Create or update a visitor profile. */
    @POST("api/widgets/{widgetKey}/visitor")
    suspend fun upsertVisitor(
        @Path("widgetKey") widgetKey: String,
        @Body body: RequestBody
    ): ResponseBody

    /** Fetch tickets for a visitor (paginated). */
    @GET("api/widgets/{widgetKey}/tickets")
    suspend fun fetchTickets(
        @Path("widgetKey") widgetKey: String,
        @Query("email") email: String,
        @Query("phone") phone: String? = null,
        @Query("page") page: Int = 1,
        @Query("limit") limit: Int = 20
    ): ResponseBody

    /** Fetch messages for a specific ticket (paginated). */
    @GET("api/widgets/{widgetKey}/tickets/{ticketId}/messages")
    suspend fun fetchMessages(
        @Path("widgetKey") widgetKey: String,
        @Path("ticketId") ticketId: String,
        @Query("email") email: String,
        @Query("page") page: Int = 1,
        @Query("limit") limit: Int = 50
    ): ResponseBody

    /** Upload a file attachment. */
    @Multipart
    @POST("api/widgets/{widgetKey}/chat/upload")
    suspend fun uploadFile(
        @Path("widgetKey") widgetKey: String,
        @Part file: MultipartBody.Part,
        @Header("X-Widget-Key") widgetKeyHeader: String,
        @Header("X-Widget-Domain") domain: String? = null
    ): ResponseBody

    /** Register FCM push token. */
    @POST("api/widgets/fcm-token")
    suspend fun registerFcmToken(
        @Body body: RequestBody
    ): ResponseBody

    /** Upload Firebase service account JSON. */
    @PUT("api/admin/widgets/{widgetId}/firebase")
    suspend fun uploadFirebaseServiceAccount(
        @Path("widgetId") widgetId: String,
        @Body body: RequestBody
    ): ResponseBody
}
