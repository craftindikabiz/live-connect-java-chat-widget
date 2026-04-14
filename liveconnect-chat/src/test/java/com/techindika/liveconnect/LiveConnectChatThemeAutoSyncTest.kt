package com.techindika.liveconnect

import android.graphics.Color
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * Verifies the round-6 setTheme auto-sync rule:
 * when the developer changes only `primaryColor` but leaves
 * `visitorBubbleColor` at the OLD primary, the new primary propagates
 * into all primary-dependent fields. If the developer overrode
 * `visitorBubbleColor` explicitly, no propagation happens.
 *
 * Mirrors Flutter's `LiveConnectChat.setTheme` in `liveconnect_chat.dart`.
 */
@RunWith(RobolectricTestRunner::class)
class LiveConnectChatThemeAutoSyncTest {

    @After fun tearDown() {
        // Reset to the default theme so tests don't pollute each other
        LiveConnectChat.setTheme(LiveConnectTheme.defaults())
    }

    @Test
    fun `setTheme propagates new primary to dependent fields when bubble was untouched`() {
        // Start: default theme, visitorBubbleColor == DEFAULT_PRIMARY (indigo)
        LiveConnectChat.setTheme(LiveConnectTheme.defaults())
        val oldPrimary = LiveConnectChat.currentTheme.primaryColor

        // Build a theme that ONLY changes primary; visitorBubbleColor stays at oldPrimary
        val onlyPrimary = LiveConnectChat.currentTheme.toBuilder().apply {
            primaryColor = Color.RED
            // do NOT set visitorBubbleColor — it remains == oldPrimary
        }.build()
        // Sanity: visitorBubbleColor on the new theme still equals OLD primary
        assertEquals(oldPrimary, onlyPrimary.visitorBubbleColor)

        LiveConnectChat.setTheme(onlyPrimary)
        val applied = LiveConnectChat.currentTheme

        assertEquals(Color.RED, applied.primaryColor)
        // ── 15 propagated fields ──
        assertEquals(Color.RED, applied.visitorBubbleColor)
        assertEquals(Color.RED, applied.sendButtonStartColor)
        assertEquals(Color.RED, applied.attachButtonIconColor)
        assertEquals(Color.RED, applied.emptyChatIconColor)
        assertEquals(Color.RED, applied.tabLabelColor)
        assertEquals(Color.RED, applied.tabIndicatorColor)
        assertEquals(Color.RED, applied.formButtonColor)
        assertEquals(Color.RED, applied.formFieldFocusBorderColor)
        assertEquals(Color.RED, applied.activityTitleColor)
        assertEquals(Color.RED, applied.inputFieldFocusBorderColor)
        assertEquals(Color.RED, applied.readOnlyNoticeTextColor)
        assertEquals(Color.RED, applied.headerAvatarBackgroundColor)
    }

    @Test
    fun `setTheme does NOT propagate when visitorBubbleColor was explicitly overridden`() {
        LiveConnectChat.setTheme(LiveConnectTheme.defaults())

        // Developer overrides BOTH primary and visitorBubbleColor — no propagation.
        val explicit = LiveConnectChat.currentTheme.toBuilder().apply {
            primaryColor = Color.RED
            visitorBubbleColor = Color.GREEN
        }.build()

        LiveConnectChat.setTheme(explicit)
        val applied = LiveConnectChat.currentTheme

        assertEquals(Color.RED, applied.primaryColor)
        // The explicit override is preserved
        assertEquals(Color.GREEN, applied.visitorBubbleColor)
        // Other primary-dependent fields stay at whatever the explicit theme had
        // (i.e. they do NOT get force-set to RED)
        assertNotEquals(Color.RED, applied.tabLabelColor)
    }

    @Test
    fun `setTheme is a no-op for primary when the new primary equals the old`() {
        LiveConnectChat.setTheme(LiveConnectTheme.defaults())
        val before = LiveConnectChat.currentTheme.primaryColor
        LiveConnectChat.setTheme(LiveConnectTheme.defaults())
        val after = LiveConnectChat.currentTheme.primaryColor
        assertEquals(before, after)
    }
}
