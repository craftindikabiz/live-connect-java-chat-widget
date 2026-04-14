package com.techindika.liveconnect

import android.graphics.Color
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

// LiveConnectTheme uses android.graphics.Color so we run under Robolectric.
@RunWith(RobolectricTestRunner::class)
class LiveConnectThemeTest {

    @Test
    fun `defaults uses indigo primary`() {
        val theme = LiveConnectTheme.defaults()
        assertEquals(LiveConnectTheme.DEFAULT_PRIMARY, theme.primaryColor)
    }

    @Test
    fun `fromPrimary tints primary-dependent fields`() {
        val theme = LiveConnectTheme.fromPrimary(Color.RED)
        assertEquals(Color.RED, theme.primaryColor)
        assertEquals(Color.RED, theme.visitorBubbleColor)
        assertEquals(Color.RED, theme.tabIndicatorColor)
        assertEquals(Color.RED, theme.tabLabelColor)
        assertEquals(Color.RED, theme.formButtonColor)
        assertEquals(Color.RED, theme.activityTitleColor)
        assertEquals(Color.RED, theme.attachButtonIconColor)
    }

    @Test
    fun `builder applies fluent setters`() {
        val theme = LiveConnectTheme.builder()
            .primaryColor(Color.BLUE)
            .headerTitle("Custom Title")
            .visitorBubbleColor(Color.MAGENTA)
            .build()
        assertEquals(Color.BLUE, theme.primaryColor)
        assertEquals("Custom Title", theme.headerTitle)
        assertEquals(Color.MAGENTA, theme.visitorBubbleColor)
    }

    @Test
    fun `darkenColor produces a darker variant`() {
        val original = LiveConnectTheme.DEFAULT_PRIMARY
        val darkened = LiveConnectTheme.darkenColor(original, 0.5f)
        assertNotEquals(original, darkened)
        // Darkened version has lower luminance — easy proxy: sum of RGB components
        val origSum = Color.red(original) + Color.green(original) + Color.blue(original)
        val darkSum = Color.red(darkened) + Color.green(darkened) + Color.blue(darkened)
        assert(darkSum < origSum) { "darkenColor should reduce luminance" }
    }

    @Test
    fun `withAlphaColor applies the requested alpha channel`() {
        val withAlpha = LiveConnectTheme.withAlphaColor(Color.RED, 0.5f)
        // Alpha component should be roughly 50% (allow small rounding tolerance)
        val alpha = Color.alpha(withAlpha)
        assert(alpha in 120..135) { "expected alpha around 127, got $alpha" }
        // RGB unchanged
        assertEquals(Color.red(Color.RED), Color.red(withAlpha))
        assertEquals(Color.green(Color.RED), Color.green(withAlpha))
        assertEquals(Color.blue(Color.RED), Color.blue(withAlpha))
    }

    @Test
    fun `new round 6 fields have sane defaults`() {
        val theme = LiveConnectTheme.defaults()
        // From the upstream Flutter port:
        assertEquals("", theme.readOnlyMessagesText)
        assertNotNull(theme.broadcastMessageBackgroundColor)
        assertEquals(Color.WHITE, theme.ratingDialogBackgroundColor)
        // secondary/tertiary text shades exist
        assertNotEquals(0, theme.secondaryTextColor)
        assertNotEquals(0, theme.tertiaryTextColor)
    }

    @Test
    fun `toBuilder roundtrips`() {
        val theme = LiveConnectTheme.builder()
            .primaryColor(0xFF112233.toInt())
            .headerTitle("X")
            .build()
        val rebuilt = theme.toBuilder().build()
        assertEquals(theme.primaryColor, rebuilt.primaryColor)
        assertEquals(theme.headerTitle, rebuilt.headerTitle)
        assertEquals(theme.visitorBubbleColor, rebuilt.visitorBubbleColor)
    }

    @Test
    fun `isPositionLeft and isPositionRight respect widgetPosition`() {
        val left = LiveConnectTheme.builder().build().toBuilder().apply {
            widgetPosition = "bottom-left"
        }.build()
        val right = LiveConnectTheme.builder().build().toBuilder().apply {
            widgetPosition = "bottom-right"
        }.build()
        assertEquals(true, left.isPositionLeft)
        assertEquals(false, left.isPositionRight)
        assertEquals(false, right.isPositionLeft)
        assertEquals(true, right.isPositionRight)
    }
}
