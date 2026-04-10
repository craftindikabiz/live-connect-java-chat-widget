package com.techindika.liveconnect.ui

import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.viewpager2.widget.ViewPager2
import com.bumptech.glide.Glide
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import com.techindika.liveconnect.LiveConnectChat
import com.techindika.liveconnect.R

/**
 * Main chat Activity hosting two tabs: Chat and Activity.
 * Launched via [LiveConnectChat.show].
 */
class ChatActivity : AppCompatActivity() {

    private lateinit var viewPager: ViewPager2
    private lateinit var tabLayout: TabLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Slide-up enter animation
        applyEnterTransition()

        setContentView(R.layout.activity_chat)

        val widgetKey = intent.getStringExtra(EXTRA_WIDGET_KEY) ?: run {
            finish()
            return
        }
        val showCloseButton = intent.getBooleanExtra(EXTRA_SHOW_CLOSE_BUTTON, true)

        // Handle back press with modern API
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                finishWithAnimation()
            }
        })

        // Check if profile is complete — if not, show form first
        if (!LiveConnectChat.hasCompleteProfile) {
            showMemberDetailsForm()
            return
        }

        setupUI(showCloseButton)
    }

    private fun setupUI(showCloseButton: Boolean) {
        val theme = LiveConnectChat.currentTheme

        // Header
        val headerTitle = findViewById<TextView>(R.id.headerTitle)
        val headerSubtitle = findViewById<TextView>(R.id.headerSubtitle)
        val headerAvatar = findViewById<ImageView>(R.id.headerAvatar)
        val closeButton = findViewById<ImageButton>(R.id.headerCloseButton)
        val menuButton = findViewById<ImageButton>(R.id.headerMenuButton)

        headerTitle.text = theme.headerTitle
        headerTitle.setTextColor(theme.headerTitleColor)
        headerSubtitle.setTextColor(theme.headerSubtitleColor)

        // Load icon
        theme.iconUrl?.let { url ->
            if (url.isNotEmpty()) {
                Glide.with(this).load(url).circleCrop().into(headerAvatar)
            }
        }

        closeButton.visibility = if (showCloseButton) View.VISIBLE else View.GONE
        closeButton.setOnClickListener { finishWithAnimation() }

        // Tab layout + ViewPager
        tabLayout = findViewById(R.id.tabLayout)
        viewPager = findViewById(R.id.viewPager)

        tabLayout.setSelectedTabIndicatorColor(theme.tabIndicatorColor)
        tabLayout.setTabTextColors(theme.tabUnselectedLabelColor, theme.tabLabelColor)

        val adapter = ChatPagerAdapter(this)
        viewPager.adapter = adapter

        TabLayoutMediator(tabLayout, viewPager) { tab, position ->
            tab.text = when (position) {
                0 -> getString(R.string.lc_chat_tab)
                1 -> getString(R.string.lc_activity_tab)
                else -> ""
            }
        }.attach()

        // Show menu button only on Chat tab
        viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                menuButton.visibility = if (position == 0) View.VISIBLE else View.GONE
            }
        })
    }

    private fun showMemberDetailsForm() {
        val dialog = MemberDetailsDialogFragment.newInstance()
        dialog.onProfileSubmitted = { profile ->
            setupUI(intent.getBooleanExtra(EXTRA_SHOW_CLOSE_BUTTON, true))
        }
        dialog.onCancelled = {
            finishWithAnimation()
        }
        dialog.show(supportFragmentManager, "member_details")
    }

    private fun applyEnterTransition() {
        if (Build.VERSION.SDK_INT >= 34) {
            overrideActivityTransition(OVERRIDE_TRANSITION_OPEN, R.anim.slide_up_enter, 0)
        } else {
            @Suppress("DEPRECATION")
            overridePendingTransition(R.anim.slide_up_enter, 0)
        }
    }

    private fun finishWithAnimation() {
        finish()
        if (Build.VERSION.SDK_INT >= 34) {
            overrideActivityTransition(OVERRIDE_TRANSITION_CLOSE, 0, R.anim.slide_down_exit)
        } else {
            @Suppress("DEPRECATION")
            overridePendingTransition(0, R.anim.slide_down_exit)
        }
    }

    companion object {
        const val EXTRA_WIDGET_KEY = "lc_widget_key"
        const val EXTRA_SHOW_CLOSE_BUTTON = "lc_show_close"
    }
}
