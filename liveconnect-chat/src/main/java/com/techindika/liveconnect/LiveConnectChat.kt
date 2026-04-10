package com.techindika.liveconnect

import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.techindika.liveconnect.callback.InitCallback
import com.techindika.liveconnect.model.VisitorProfile
import com.techindika.liveconnect.model.WidgetConfig
import com.techindika.liveconnect.network.ApiResult
import com.techindika.liveconnect.network.RetrofitClient
import com.techindika.liveconnect.service.VisitorProfileStore
import com.techindika.liveconnect.ui.ChatActivity
import kotlinx.coroutines.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject

/**
 * Main entry point for the LiveConnect Chat SDK.
 *
 * **Kotlin usage:**
 * ```kotlin
 * LiveConnectChat.init(context, "widget-key",
 *     visitorDetails = VisitorProfile("John", "john@example.com"))
 * LiveConnectChat.show(context)
 * ```
 *
 * **Java usage:**
 * ```java
 * LiveConnectChat.init(context, "widget-key",
 *     new VisitorProfile("John", "john@example.com", ""),
 *     null, callback);
 * LiveConnectChat.show(context);
 * ```
 */
object LiveConnectChat {

    private const val TAG = "LiveConnect"

    private var _initialized = false
    private var _widgetKey: String? = null
    private var _visitorProfile: VisitorProfile? = null
    private var _theme: LiveConnectTheme = LiveConnectTheme.defaults()
    private var _widgetConfig: WidgetConfig? = null
    private var _fcmToken: String? = null
    private var _firebaseServiceAccount: Map<String, Any>? = null
    private var _appContext: Context? = null
    private var _visitorId: String? = null

    private var scope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    private val _themeVersion = MutableLiveData(0)
    private val themeCounter = java.util.concurrent.atomic.AtomicInteger(0)

    // ── Public getters ──

    @JvmStatic val isInitialized: Boolean get() = _initialized
    @JvmStatic val widgetKey: String? get() = _widgetKey
    @JvmStatic val visitorProfile: VisitorProfile? get() = _visitorProfile
    @JvmStatic val hasCompleteProfile: Boolean get() = _visitorProfile?.isComplete == true
    @JvmStatic val currentTheme: LiveConnectTheme get() = _theme
    @JvmStatic val widgetConfig: WidgetConfig? get() = _widgetConfig
    @JvmStatic val themeVersion: LiveData<Int> get() = _themeVersion

    internal val appContext: Context? get() = _appContext
    internal val visitorId: String? get() = _visitorId

    // ── Initialization ──

    /**
     * Initialize the SDK. Must be called before [show].
     *
     * @param context Application or Activity context.
     * @param widgetKey Unique key from the LiveConnect dashboard.
     * @param visitorDetails Optional visitor profile. If null or incomplete, a form is shown.
     * @param theme Optional theme override.
     * @param callback Optional callback for Java callers (null for Kotlin — use suspend version).
     */
    @JvmStatic
    @JvmOverloads
    fun init(
        context: Context,
        widgetKey: String,
        visitorDetails: VisitorProfile? = null,
        theme: LiveConnectTheme? = null,
        callback: InitCallback? = null
    ) {
        scope.launch {
            try {
                val result = initInternal(context, widgetKey, visitorDetails, theme)
                if (result.isSuccess) {
                    callback?.onSuccess()
                } else {
                    callback?.onFailure(result.errorOrNull() ?: "Initialization failed")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Init failed: ${e.message}")
                callback?.onFailure(e.message ?: "Initialization failed")
            }
        }
    }

    /**
     * Kotlin-friendly suspend initialization.
     */
    suspend fun initSuspend(
        context: Context,
        widgetKey: String,
        visitorDetails: VisitorProfile? = null,
        theme: LiveConnectTheme? = null
    ): ApiResult<Unit> {
        return initInternal(context, widgetKey, visitorDetails, theme)
    }

    private suspend fun initInternal(
        context: Context,
        widgetKey: String,
        visitorDetails: VisitorProfile?,
        theme: LiveConnectTheme?
    ): ApiResult<Unit> = withContext(Dispatchers.IO) {
        // Cancel any previously running init coroutines
        if (_initialized) {
            scope.cancel()
            scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
        }

        _appContext = context.applicationContext
        _widgetKey = widgetKey

        // Apply user theme if provided
        if (theme != null) {
            _theme = theme
        }

        // Fetch widget config from API
        try {
            val response = RetrofitClient.apiService.fetchWidgetConfig(widgetKey)
            val json = JSONObject(response.string())
            val status = json.optString("status", "")
            if (status == "success") {
                val widgetJson = json.optJSONObject("data")?.optJSONObject("widget")
                if (widgetJson != null) {
                    _widgetConfig = WidgetConfig.fromJson(widgetJson)

                    // Merge API config into theme (API colors are lower priority than user theme)
                    if (theme == null) {
                        _widgetConfig?.let { config ->
                            _theme = LiveConnectTheme.fromPrimary(config.parsedColor())
                                .toBuilder().apply {
                                    welcomeText = config.welcomeText.ifEmpty { welcomeText }
                                    offlineText = config.offlineText.ifEmpty { offlineText }
                                    widgetPosition = config.position
                                    suggestedMessages = config.suggestedMessages.toMutableList()
                                    iconUrl = config.iconUrl.ifEmpty { null }
                                    headerTitle = config.name.ifEmpty { headerTitle }
                                }.build()
                        }
                    } else {
                        // User provided theme — merge only non-theme config
                        _widgetConfig?.let { config ->
                            _theme = _theme.toBuilder().apply {
                                if (welcomeText == LiveConnectTheme.builder().welcomeText) {
                                    welcomeText = config.welcomeText.ifEmpty { welcomeText }
                                }
                                widgetPosition = config.position
                                suggestedMessages = config.suggestedMessages.toMutableList()
                                if (iconUrl == null) iconUrl = config.iconUrl.ifEmpty { null }
                            }.build()
                        }
                    }
                    _themeVersion.postValue(themeCounter.incrementAndGet())
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to fetch widget config: ${e.message}")
            // Non-fatal — continue with defaults
        }

        // Handle visitor profile
        if (visitorDetails != null && visitorDetails.isComplete) {
            _visitorProfile = visitorDetails
            // Register visitor with API
            registerVisitor(visitorDetails)
            // Persist locally
            VisitorProfileStore.save(context.applicationContext, widgetKey, visitorDetails)
        } else {
            // Try loading from storage
            val stored = VisitorProfileStore.load(context.applicationContext, widgetKey)
            if (stored?.isComplete == true) {
                _visitorProfile = stored
            }
        }

        // Load visitor ID
        _visitorId = VisitorProfileStore.loadVisitorId(context.applicationContext, widgetKey)

        _initialized = true
        Log.d(TAG, "SDK initialized for widget: $widgetKey")

        ApiResult.success(Unit)
    }

    // ── Show / Hide ──

    /**
     * Open the chat screen. If visitor profile is incomplete, a form is shown first.
     *
     * @param context Activity context (required for starting Activity).
     */
    @JvmStatic
    fun show(context: Context) {
        check(_initialized && _widgetKey != null) {
            "Call LiveConnectChat.init() before LiveConnectChat.show()"
        }
        val intent = Intent(context, ChatActivity::class.java).apply {
            putExtra(ChatActivity.EXTRA_WIDGET_KEY, _widgetKey)
            putExtra(ChatActivity.EXTRA_SHOW_CLOSE_BUTTON, true)
        }
        context.startActivity(intent)
    }

    // ── FCM ──

    /**
     * Set the FCM token for push notifications.
     */
    @JvmStatic
    fun setFcmToken(fcmToken: String) {
        _fcmToken = fcmToken
        // Register with server if profile is complete
        if (_initialized && hasCompleteProfile) {
            scope.launch(Dispatchers.IO) {
                registerFcmToken(fcmToken)
            }
        }
    }

    /**
     * Set the Firebase service account JSON for admin push notifications.
     */
    @JvmStatic
    fun setFirebaseServiceAccount(serviceAccount: Map<String, Any>) {
        _firebaseServiceAccount = serviceAccount
        // Upload to server if widget config available
        if (_initialized && _widgetConfig != null) {
            scope.launch(Dispatchers.IO) {
                uploadFirebaseServiceAccount(serviceAccount)
            }
        }
    }

    // ── Theme ──

    /**
     * Override the theme at runtime.
     */
    @JvmStatic
    fun setTheme(theme: LiveConnectTheme) {
        _theme = theme
        _themeVersion.postValue(themeCounter.incrementAndGet())
    }

    // ── Internal API calls ──

    private suspend fun registerVisitor(profile: VisitorProfile) {
        try {
            val body = JSONObject().apply {
                put("name", profile.name)
                put("email", profile.email)
                put("phone", profile.phone)
            }
            val requestBody = body.toString()
                .toRequestBody("application/json".toMediaType())
            val response = RetrofitClient.apiService.upsertVisitor(_widgetKey!!, requestBody)
            val json = JSONObject(response.string())
            // Extract visitor ID if returned
            val data = json.optJSONObject("data")
            val visitorId = data?.optString("visitorId", data.optString("_id", ""))
            if (!visitorId.isNullOrEmpty()) {
                _visitorId = visitorId
                _appContext?.let { ctx ->
                    VisitorProfileStore.saveVisitorId(ctx, _widgetKey!!, visitorId)
                }
            }
            Log.d(TAG, "Visitor registered: ${profile.email}")
        } catch (e: Exception) {
            Log.w(TAG, "Failed to register visitor: ${e.message}")
        }
    }

    internal suspend fun registerVisitorProfile(profile: VisitorProfile): Boolean {
        _visitorProfile = profile
        _appContext?.let { ctx ->
            VisitorProfileStore.save(ctx, _widgetKey!!, profile)
        }
        return try {
            registerVisitor(profile)
            true
        } catch (_: Exception) {
            false
        }
    }

    private suspend fun registerFcmToken(token: String) {
        try {
            val profile = _visitorProfile ?: return
            val body = JSONObject().apply {
                put("widgetKey", _widgetKey)
                put("email", profile.email)
                put("fcmToken", token)
            }
            val requestBody = body.toString()
                .toRequestBody("application/json".toMediaType())
            RetrofitClient.apiService.registerFcmToken(requestBody)
            Log.d(TAG, "FCM token registered")
        } catch (e: Exception) {
            Log.w(TAG, "Failed to register FCM token: ${e.message}")
        }
    }

    private suspend fun uploadFirebaseServiceAccount(serviceAccount: Map<String, Any>) {
        try {
            val widgetId = _widgetConfig?.id ?: return
            val body = JSONObject(serviceAccount)
            val requestBody = body.toString()
                .toRequestBody("application/json".toMediaType())
            RetrofitClient.apiService.uploadFirebaseServiceAccount(widgetId, requestBody)
            Log.d(TAG, "Firebase service account uploaded")
        } catch (e: Exception) {
            Log.w(TAG, "Failed to upload Firebase service account: ${e.message}")
        }
    }
}
