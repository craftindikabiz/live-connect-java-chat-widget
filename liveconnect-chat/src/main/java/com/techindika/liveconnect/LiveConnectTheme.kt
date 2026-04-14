package com.techindika.liveconnect

import android.graphics.Color
import androidx.annotation.ColorInt

/**
 * Theme configuration for the LiveConnect chat UI.
 * Use [Builder] for Java or [fromPrimary] for quick setup.
 *
 * Java usage:
 * ```java
 * LiveConnectTheme theme = LiveConnectTheme.builder()
 *     .primaryColor(Color.BLUE)
 *     .headerTitle("Support")
 *     .build();
 * ```
 *
 * Kotlin usage:
 * ```kotlin
 * val theme = LiveConnectTheme.fromPrimary(Color.BLUE)
 * ```
 */
class LiveConnectTheme private constructor(builder: Builder) {

    // ── Core Colors ──
    @ColorInt val primaryColor: Int = builder.primaryColor
    @ColorInt val secondaryColor: Int = builder.secondaryColor

    // ── Widget Config (from API) ──
    val iconUrl: String? = builder.iconUrl
    val welcomeText: String = builder.welcomeText
    val offlineText: String = builder.offlineText
    val widgetPosition: String = builder.widgetPosition
    val suggestedMessages: List<String> = builder.suggestedMessages

    // ── Screen ──
    @ColorInt val screenBackgroundColor: Int = builder.screenBackgroundColor

    // ── Header ──
    @ColorInt val headerBackgroundColor: Int = builder.headerBackgroundColor
    val headerTitle: String = builder.headerTitle
    val headerTitleFontSize: Float = builder.headerTitleFontSize
    @ColorInt val headerTitleColor: Int = builder.headerTitleColor
    @ColorInt val headerSubtitleColor: Int = builder.headerSubtitleColor
    val headerSubtitleFontSize: Float = builder.headerSubtitleFontSize
    @ColorInt val headerCloseIconColor: Int = builder.headerCloseIconColor
    val headerElevation: Float = builder.headerElevation
    @ColorInt val headerBorderColor: Int = builder.headerBorderColor
    @ColorInt val headerAvatarBackgroundColor: Int = builder.headerAvatarBackgroundColor

    // ── Message Bubbles ──
    @ColorInt val visitorBubbleColor: Int = builder.visitorBubbleColor
    @ColorInt val agentBubbleColor: Int = builder.agentBubbleColor
    @ColorInt val visitorTextColor: Int = builder.visitorTextColor
    @ColorInt val agentTextColor: Int = builder.agentTextColor
    @ColorInt val messageTimestampColor: Int = builder.messageTimestampColor
    val messageTimestampFontSize: Float = builder.messageTimestampFontSize
    val messageBorderRadius: Float = builder.messageBorderRadius
    val messagePaddingHorizontal: Float = builder.messagePaddingHorizontal
    val messagePaddingVertical: Float = builder.messagePaddingVertical

    // ── Input ──
    @ColorInt val inputBackgroundColor: Int = builder.inputBackgroundColor
    @ColorInt val inputFieldBackgroundColor: Int = builder.inputFieldBackgroundColor
    @ColorInt val inputFieldBorderColor: Int = builder.inputFieldBorderColor
    @ColorInt val inputFieldTextColor: Int = builder.inputFieldTextColor
    @ColorInt val inputFieldHintColor: Int = builder.inputFieldHintColor
    @ColorInt val inputFieldFocusBorderColor: Int = builder.inputFieldFocusBorderColor
    val inputFieldBorderRadius: Float = builder.inputFieldBorderRadius
    val inputFieldTextFontSize: Float = builder.inputFieldTextFontSize

    // ── Send Button ──
    val sendButtonSize: Float = builder.sendButtonSize
    val sendButtonBorderRadius: Float = builder.sendButtonBorderRadius
    @ColorInt val sendButtonIconColor: Int = builder.sendButtonIconColor
    @ColorInt val sendButtonStartColor: Int = builder.sendButtonStartColor
    @ColorInt val sendButtonEndColor: Int = builder.sendButtonEndColor

    // ── Attach Button ──
    @ColorInt val attachButtonBackgroundColor: Int = builder.attachButtonBackgroundColor
    @ColorInt val attachButtonIconColor: Int = builder.attachButtonIconColor

    // ── System Messages ──
    @ColorInt val systemMessageBackgroundColor: Int = builder.systemMessageBackgroundColor
    @ColorInt val systemMessageTextColor: Int = builder.systemMessageTextColor
    val systemMessageFontSize: Float = builder.systemMessageFontSize

    // ── Tabs ──
    @ColorInt val tabLabelColor: Int = builder.tabLabelColor
    @ColorInt val tabUnselectedLabelColor: Int = builder.tabUnselectedLabelColor
    @ColorInt val tabIndicatorColor: Int = builder.tabIndicatorColor

    // ── Form ──
    @ColorInt val formButtonColor: Int = builder.formButtonColor
    @ColorInt val formButtonTextColor: Int = builder.formButtonTextColor
    @ColorInt val formFieldFocusBorderColor: Int = builder.formFieldFocusBorderColor

    // ── Activity Tab ──
    @ColorInt val activityTitleColor: Int = builder.activityTitleColor
    @ColorInt val activityCardBorderColor: Int = builder.activityCardBorderColor

    // ── Empty Chat ──
    @ColorInt val emptyChatIconColor: Int = builder.emptyChatIconColor
    @ColorInt val emptyChatTitleColor: Int = builder.emptyChatTitleColor
    @ColorInt val emptyChatDescriptionColor: Int = builder.emptyChatDescriptionColor
    val emptyChatTitleFontSize: Float = builder.emptyChatTitleFontSize
    val emptyChatDescriptionFontSize: Float = builder.emptyChatDescriptionFontSize

    // ── Generic text shades (used by activity cards, hint text, etc.) ──
    @ColorInt val secondaryTextColor: Int = builder.secondaryTextColor
    @ColorInt val tertiaryTextColor: Int = builder.tertiaryTextColor

    // ── Read-Only Notice ──
    @ColorInt val readOnlyNoticeBackgroundColor: Int = builder.readOnlyNoticeBackgroundColor
    @ColorInt val readOnlyNoticeTextColor: Int = builder.readOnlyNoticeTextColor
    /** Optional override for the read-only banner text. Empty falls back to default. */
    val readOnlyMessagesText: String = builder.readOnlyMessagesText

    // ── Broadcast / system event chip ──
    @ColorInt val broadcastMessageBackgroundColor: Int = builder.broadcastMessageBackgroundColor

    // ── Rating Dialog ──
    @ColorInt val ratingDialogBackgroundColor: Int = builder.ratingDialogBackgroundColor
    @ColorInt val ratingDialogTitleColor: Int = builder.ratingDialogTitleColor

    // ── Error ──
    @ColorInt val errorMessageColor: Int = builder.errorMessageColor

    // ── Computed ──
    val isPositionLeft: Boolean get() = widgetPosition.contains("left", ignoreCase = true)
    val isPositionRight: Boolean get() = !isPositionLeft

    /** Create a new Builder pre-filled with this theme's values. */
    fun toBuilder(): Builder = Builder().also { b ->
        b.primaryColor = primaryColor
        b.secondaryColor = secondaryColor
        b.iconUrl = iconUrl
        b.welcomeText = welcomeText
        b.offlineText = offlineText
        b.widgetPosition = widgetPosition
        b.suggestedMessages = suggestedMessages.toMutableList()
        b.screenBackgroundColor = screenBackgroundColor
        b.headerBackgroundColor = headerBackgroundColor
        b.headerTitle = headerTitle
        b.headerTitleFontSize = headerTitleFontSize
        b.headerTitleColor = headerTitleColor
        b.headerSubtitleColor = headerSubtitleColor
        b.headerSubtitleFontSize = headerSubtitleFontSize
        b.headerCloseIconColor = headerCloseIconColor
        b.headerElevation = headerElevation
        b.headerBorderColor = headerBorderColor
        b.headerAvatarBackgroundColor = headerAvatarBackgroundColor
        b.visitorBubbleColor = visitorBubbleColor
        b.agentBubbleColor = agentBubbleColor
        b.visitorTextColor = visitorTextColor
        b.agentTextColor = agentTextColor
        b.messageTimestampColor = messageTimestampColor
        b.messageTimestampFontSize = messageTimestampFontSize
        b.messageBorderRadius = messageBorderRadius
        b.messagePaddingHorizontal = messagePaddingHorizontal
        b.messagePaddingVertical = messagePaddingVertical
        b.inputBackgroundColor = inputBackgroundColor
        b.inputFieldBackgroundColor = inputFieldBackgroundColor
        b.inputFieldBorderColor = inputFieldBorderColor
        b.inputFieldTextColor = inputFieldTextColor
        b.inputFieldHintColor = inputFieldHintColor
        b.inputFieldFocusBorderColor = inputFieldFocusBorderColor
        b.inputFieldBorderRadius = inputFieldBorderRadius
        b.inputFieldTextFontSize = inputFieldTextFontSize
        b.sendButtonSize = sendButtonSize
        b.sendButtonBorderRadius = sendButtonBorderRadius
        b.sendButtonIconColor = sendButtonIconColor
        b.sendButtonStartColor = sendButtonStartColor
        b.sendButtonEndColor = sendButtonEndColor
        b.attachButtonBackgroundColor = attachButtonBackgroundColor
        b.attachButtonIconColor = attachButtonIconColor
        b.systemMessageBackgroundColor = systemMessageBackgroundColor
        b.systemMessageTextColor = systemMessageTextColor
        b.systemMessageFontSize = systemMessageFontSize
        b.tabLabelColor = tabLabelColor
        b.tabUnselectedLabelColor = tabUnselectedLabelColor
        b.tabIndicatorColor = tabIndicatorColor
        b.formButtonColor = formButtonColor
        b.formButtonTextColor = formButtonTextColor
        b.formFieldFocusBorderColor = formFieldFocusBorderColor
        b.activityTitleColor = activityTitleColor
        b.activityCardBorderColor = activityCardBorderColor
        b.emptyChatIconColor = emptyChatIconColor
        b.emptyChatTitleColor = emptyChatTitleColor
        b.emptyChatDescriptionColor = emptyChatDescriptionColor
        b.emptyChatTitleFontSize = emptyChatTitleFontSize
        b.emptyChatDescriptionFontSize = emptyChatDescriptionFontSize
        b.secondaryTextColor = secondaryTextColor
        b.tertiaryTextColor = tertiaryTextColor
        b.readOnlyNoticeBackgroundColor = readOnlyNoticeBackgroundColor
        b.readOnlyNoticeTextColor = readOnlyNoticeTextColor
        b.readOnlyMessagesText = readOnlyMessagesText
        b.broadcastMessageBackgroundColor = broadcastMessageBackgroundColor
        b.ratingDialogBackgroundColor = ratingDialogBackgroundColor
        b.ratingDialogTitleColor = ratingDialogTitleColor
        b.errorMessageColor = errorMessageColor
    }

    /**
     * Builder for constructing a [LiveConnectTheme].
     * All colors default to the standard LiveConnect indigo palette.
     */
    class Builder {
        @ColorInt var primaryColor: Int = DEFAULT_PRIMARY
        @ColorInt var secondaryColor: Int = Color.WHITE

        var iconUrl: String? = null
        var welcomeText: String = "Send us a message and we'll get back to you shortly."
        var offlineText: String = "We are currently offline."
        var widgetPosition: String = "bottom-right"
        var suggestedMessages: MutableList<String> = mutableListOf()

        @ColorInt var screenBackgroundColor: Int = Color.WHITE

        @ColorInt var headerBackgroundColor: Int = Color.WHITE
        var headerTitle: String = "Chat with us"
        var headerTitleFontSize: Float = 15f
        @ColorInt var headerTitleColor: Int = 0xFF1A1A2E.toInt()
        @ColorInt var headerSubtitleColor: Int = 0xFF6B7280.toInt()
        var headerSubtitleFontSize: Float = 11f
        @ColorInt var headerCloseIconColor: Int = 0xFF374151.toInt()
        var headerElevation: Float = 0f
        @ColorInt var headerBorderColor: Int = 0xFFE5E7EB.toInt()
        @ColorInt var headerAvatarBackgroundColor: Int = DEFAULT_PRIMARY

        @ColorInt var visitorBubbleColor: Int = DEFAULT_PRIMARY
        @ColorInt var agentBubbleColor: Int = 0xFFF3F4F6.toInt()
        @ColorInt var visitorTextColor: Int = Color.WHITE
        @ColorInt var agentTextColor: Int = 0xFF1A1A2E.toInt()
        @ColorInt var messageTimestampColor: Int = 0xFF9CA3AF.toInt()
        var messageTimestampFontSize: Float = 11f
        var messageBorderRadius: Float = 16f
        var messagePaddingHorizontal: Float = 14f
        var messagePaddingVertical: Float = 10f

        @ColorInt var inputBackgroundColor: Int = Color.WHITE
        @ColorInt var inputFieldBackgroundColor: Int = 0xFFF8F9FA.toInt()
        @ColorInt var inputFieldBorderColor: Int = 0xFFE5E7EB.toInt()
        @ColorInt var inputFieldTextColor: Int = 0xFF1A1A2E.toInt()
        @ColorInt var inputFieldHintColor: Int = 0xFF9CA3AF.toInt()
        @ColorInt var inputFieldFocusBorderColor: Int = DEFAULT_PRIMARY
        var inputFieldBorderRadius: Float = 14f
        var inputFieldTextFontSize: Float = 14f

        var sendButtonSize: Float = 44f
        var sendButtonBorderRadius: Float = 14f
        @ColorInt var sendButtonIconColor: Int = Color.WHITE
        @ColorInt var sendButtonStartColor: Int = DEFAULT_PRIMARY
        @ColorInt var sendButtonEndColor: Int = darken(DEFAULT_PRIMARY, 0.15f)

        @ColorInt var attachButtonBackgroundColor: Int = 0xFFF3F4F6.toInt()
        @ColorInt var attachButtonIconColor: Int = DEFAULT_PRIMARY

        @ColorInt var systemMessageBackgroundColor: Int = 0xFFF0F0F0.toInt()
        @ColorInt var systemMessageTextColor: Int = 0xFF888888.toInt()
        var systemMessageFontSize: Float = 12f

        @ColorInt var tabLabelColor: Int = DEFAULT_PRIMARY
        @ColorInt var tabUnselectedLabelColor: Int = 0xFF9CA3AF.toInt()
        @ColorInt var tabIndicatorColor: Int = DEFAULT_PRIMARY

        @ColorInt var formButtonColor: Int = DEFAULT_PRIMARY
        @ColorInt var formButtonTextColor: Int = Color.WHITE
        @ColorInt var formFieldFocusBorderColor: Int = DEFAULT_PRIMARY

        @ColorInt var activityTitleColor: Int = DEFAULT_PRIMARY
        @ColorInt var activityCardBorderColor: Int = withAlpha(DEFAULT_PRIMARY, 0.20f)

        @ColorInt var emptyChatIconColor: Int = DEFAULT_PRIMARY
        @ColorInt var emptyChatTitleColor: Int = 0xFF374151.toInt()
        @ColorInt var emptyChatDescriptionColor: Int = 0xFF6B7280.toInt()
        var emptyChatTitleFontSize: Float = 20f
        var emptyChatDescriptionFontSize: Float = 14f

        @ColorInt var secondaryTextColor: Int = 0xFF64748B.toInt()
        @ColorInt var tertiaryTextColor: Int = 0xFF0F172A.toInt()

        @ColorInt var readOnlyNoticeBackgroundColor: Int = withAlpha(DEFAULT_PRIMARY, 0.08f)
        @ColorInt var readOnlyNoticeTextColor: Int = DEFAULT_PRIMARY
        var readOnlyMessagesText: String = ""

        @ColorInt var broadcastMessageBackgroundColor: Int = 0xFFF3E2E2.toInt()

        @ColorInt var ratingDialogBackgroundColor: Int = Color.WHITE
        @ColorInt var ratingDialogTitleColor: Int = 0xFF111827.toInt()

        @ColorInt var errorMessageColor: Int = 0xFFDC2626.toInt()

        // Fluent setters for Java
        fun primaryColor(@ColorInt c: Int) = apply { primaryColor = c }
        fun secondaryColor(@ColorInt c: Int) = apply { secondaryColor = c }
        fun headerTitle(t: String) = apply { headerTitle = t }
        fun headerBackgroundColor(@ColorInt c: Int) = apply { headerBackgroundColor = c }
        fun headerTitleColor(@ColorInt c: Int) = apply { headerTitleColor = c }
        fun visitorBubbleColor(@ColorInt c: Int) = apply { visitorBubbleColor = c }
        fun agentBubbleColor(@ColorInt c: Int) = apply { agentBubbleColor = c }
        fun welcomeText(t: String) = apply { welcomeText = t }
        fun widgetPosition(p: String) = apply { widgetPosition = p }
        fun suggestedMessages(m: List<String>) = apply { suggestedMessages = m.toMutableList() }

        fun build(): LiveConnectTheme = LiveConnectTheme(this)
    }

    companion object {
        @JvmField
        val DEFAULT_PRIMARY = 0xFF4F46E5.toInt()

        @JvmStatic
        fun builder(): Builder = Builder()

        @JvmStatic
        fun defaults(): LiveConnectTheme = Builder().build()

        /**
         * Generate a complete theme from a single primary color.
         * Auto-derives all sub-colors using alpha and darken/lighten variations.
         */
        @JvmStatic
        fun fromPrimary(@ColorInt primary: Int): LiveConnectTheme = Builder().apply {
            primaryColor = primary
            headerAvatarBackgroundColor = primary
            visitorBubbleColor = primary
            inputFieldFocusBorderColor = primary
            sendButtonStartColor = primary
            sendButtonEndColor = darken(primary, 0.15f)
            attachButtonIconColor = primary
            tabLabelColor = primary
            tabIndicatorColor = primary
            formButtonColor = primary
            formFieldFocusBorderColor = primary
            activityTitleColor = primary
            activityCardBorderColor = withAlpha(primary, 0.20f)
            emptyChatIconColor = primary
            readOnlyNoticeBackgroundColor = withAlpha(primary, 0.08f)
            readOnlyNoticeTextColor = primary
        }.build()

        /** Darken a color by a fraction (0.0-1.0). Public so [LiveConnectChat.setTheme] can reuse. */
        @JvmStatic
        fun darkenColor(@ColorInt color: Int, fraction: Float): Int = darken(color, fraction)

        /** Apply alpha to a color (0.0-1.0). Public so [LiveConnectChat.setTheme] can reuse. */
        @JvmStatic
        fun withAlphaColor(@ColorInt color: Int, alpha: Float): Int = withAlpha(color, alpha)

        /** Darken a color by a fraction (0.0-1.0). */
        private fun darken(@ColorInt color: Int, fraction: Float): Int {
            val hsv = FloatArray(3)
            Color.colorToHSV(color, hsv)
            hsv[2] = (hsv[2] * (1f - fraction)).coerceIn(0f, 1f)
            return Color.HSVToColor(Color.alpha(color), hsv)
        }

        /** Apply alpha to a color (0.0-1.0). */
        private fun withAlpha(@ColorInt color: Int, alpha: Float): Int {
            val a = (alpha * 255).toInt().coerceIn(0, 255)
            return Color.argb(a, Color.red(color), Color.green(color), Color.blue(color))
        }
    }
}
