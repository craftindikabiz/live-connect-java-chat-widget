package com.techindika.liveconnect.ui.view

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.util.AttributeSet
import android.view.Gravity
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import androidx.annotation.ColorInt
import androidx.lifecycle.Observer
import com.bumptech.glide.Glide
import com.techindika.liveconnect.LiveConnectChat
import com.techindika.liveconnect.R
import com.techindika.liveconnect.service.UnreadCountService

/**
 * Floating chat button with unread badge. Drop into any layout.
 *
 * XML usage:
 * ```xml
 * <com.techindika.liveconnect.ui.view.FloatingChatButton
 *     android:layout_width="56dp"
 *     android:layout_height="56dp"
 *     android:layout_gravity="bottom|end"
 *     android:layout_margin="16dp" />
 * ```
 *
 * Programmatic usage (Kotlin):
 * ```kotlin
 * val fab = FloatingChatButton(context)
 * fab.setOnClickListener { LiveConnectChat.show(context) }
 * ```
 *
 * Java:
 * ```java
 * FloatingChatButton fab = new FloatingChatButton(context);
 * fab.setOnClickListener(v -> LiveConnectChat.show(context));
 * ```
 */
class FloatingChatButton @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {

    private val iconView: ImageView
    private val badgeView: TextView
    private var unreadObserver: Observer<Int>? = null

    @ColorInt
    private var bgColor: Int = LiveConnectChat.currentTheme.primaryColor

    init {
        // Read custom attributes
        attrs?.let {
            val ta = context.obtainStyledAttributes(it, R.styleable.FloatingChatButton)
            bgColor = ta.getColor(R.styleable.FloatingChatButton_lc_backgroundColor, bgColor)
            ta.recycle()
        }

        // Background circle
        val bgDrawable = GradientDrawable().apply {
            shape = GradientDrawable.OVAL
            setColor(bgColor)
        }
        background = bgDrawable
        elevation = 6f
        isClickable = true
        isFocusable = true

        // Icon
        iconView = ImageView(context).apply {
            layoutParams = LayoutParams(
                dpToPx(30),
                dpToPx(30),
                Gravity.CENTER
            )
            setColorFilter(Color.WHITE)
            // Default chat icon
            setImageResource(android.R.drawable.sym_action_chat)
        }
        addView(iconView)

        // Load custom icon from theme if available
        LiveConnectChat.currentTheme.iconUrl?.let { url ->
            if (url.isNotEmpty()) {
                Glide.with(context).load(url).circleCrop().into(iconView)
                iconView.clearColorFilter()
            }
        }

        // Badge
        badgeView = TextView(context).apply {
            layoutParams = LayoutParams(dpToPx(20), dpToPx(20), Gravity.TOP or Gravity.END).apply {
                marginEnd = 0
                topMargin = 0
            }
            textSize = 10f
            setTextColor(Color.WHITE)
            gravity = Gravity.CENTER
            visibility = View.GONE

            val badgeBg = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(Color.parseColor("#EF4444"))
            }
            background = badgeBg
        }
        addView(badgeView)

        // Default click opens chat
        setOnClickListener {
            if (LiveConnectChat.isInitialized) {
                LiveConnectChat.show(context)
            }
        }

        // Observe unread count
        observeUnreadCount()
    }

    private fun observeUnreadCount() {
        unreadObserver = Observer { count ->
            post {
                if (count > 0) {
                    badgeView.visibility = View.VISIBLE
                    badgeView.text = if (count > 99) "99+" else count.toString()
                } else {
                    badgeView.visibility = View.GONE
                }
            }
        }
        UnreadCountService.totalUnreadCount.observeForever(unreadObserver!!)
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        unreadObserver?.let { UnreadCountService.totalUnreadCount.removeObserver(it) }
    }

    /** Set the background color programmatically. */
    fun setButtonColor(@ColorInt color: Int) {
        bgColor = color
        (background as? GradientDrawable)?.setColor(color)
    }

    /** Set a custom icon drawable resource. */
    fun setIconResource(resId: Int) {
        iconView.setImageResource(resId)
    }

    /** Set a custom icon from URL. */
    fun setIconUrl(url: String) {
        Glide.with(context).load(url).circleCrop().into(iconView)
        iconView.clearColorFilter()
    }

    private fun dpToPx(dp: Int): Int = (dp * context.resources.displayMetrics.density).toInt()
}
