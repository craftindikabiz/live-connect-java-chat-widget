package com.techindika.liveconnect.util

import android.graphics.Color
import androidx.annotation.ColorInt

/**
 * Color manipulation utilities.
 */
internal object ColorUtils {

    /** Parse a hex color string with fallback. */
    @ColorInt
    fun parseColor(hex: String?, @ColorInt fallback: Int): Int {
        if (hex.isNullOrBlank()) return fallback
        return try {
            Color.parseColor(if (hex.startsWith("#")) hex else "#$hex")
        } catch (_: Exception) {
            fallback
        }
    }

    /** Darken a color by a fraction (0.0 - 1.0). */
    @ColorInt
    fun darken(@ColorInt color: Int, fraction: Float): Int {
        val hsv = FloatArray(3)
        Color.colorToHSV(color, hsv)
        hsv[2] = (hsv[2] * (1f - fraction)).coerceIn(0f, 1f)
        return Color.HSVToColor(Color.alpha(color), hsv)
    }

    /** Apply alpha (0.0 - 1.0) to a color. */
    @ColorInt
    fun withAlpha(@ColorInt color: Int, alpha: Float): Int {
        val a = (alpha * 255).toInt().coerceIn(0, 255)
        return Color.argb(a, Color.red(color), Color.green(color), Color.blue(color))
    }
}
