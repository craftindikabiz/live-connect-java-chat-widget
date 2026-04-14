package com.techindika.liveconnect.ui

import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.PopupMenu
import android.widget.TextView
import androidx.activity.OnBackPressedCallback
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
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

    private val viewModel: ChatViewModel by viewModels()

    private lateinit var viewPager: ViewPager2
    private lateinit var tabLayout: TabLayout
    private lateinit var menuButton: ImageButton

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Edge-to-edge so the activity draws under status + nav bars; we apply
        // the system-bar + IME insets as padding on the root view ourselves so
        // the header is never under the clock and the input is never under the
        // gesture indicator or keyboard.
        WindowCompat.setDecorFitsSystemWindows(window, false)

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

        // ── Apply system-bar + IME insets as padding on the root layout ──
        // This handles status bar (top), nav/gesture bar (bottom), AND keyboard.
        val root = findViewById<View>(R.id.activityChatRoot)
        ViewCompat.setOnApplyWindowInsetsListener(root) { v, insets ->
            val bars = insets.getInsets(
                WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.ime()
            )
            v.setPadding(bars.left, bars.top, bars.right, bars.bottom)
            insets
        }

        // Header
        val headerTitle = findViewById<TextView>(R.id.headerTitle)
        val headerAvatar = findViewById<ImageView>(R.id.headerAvatar)
        val closeButton = findViewById<ImageButton>(R.id.headerCloseButton)
        menuButton = findViewById(R.id.headerMenuButton)

        headerTitle.text = theme.headerTitle
        headerTitle.setTextColor(theme.headerTitleColor)

        // Load icon
        theme.iconUrl?.let { url ->
            if (url.isNotEmpty()) {
                Glide.with(this).load(url).circleCrop().into(headerAvatar)
            }
        }

        closeButton.visibility = if (showCloseButton) View.VISIBLE else View.GONE
        closeButton.setOnClickListener { finishWithAnimation() }

        // 3-dot menu → "Mark as resolved" popup
        menuButton.setOnClickListener { showHeaderMenu(menuButton) }

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

        // Show menu button only on Chat tab AND only when there's an active ticket
        viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                updateMenuButtonVisibility(position)
            }
        })

        // React to ticket lifecycle so the menu button hides when a ticket is
        // resolved (and re-appears when a new one is created).
        viewModel.conversationManager.activeThreadId.observe(this) {
            updateMenuButtonVisibility(viewPager.currentItem)
        }
    }

    private fun updateMenuButtonVisibility(position: Int) {
        val showMenu = position == 0 && viewModel.conversationManager.activeTicketId != null
        menuButton.visibility = if (showMenu) View.VISIBLE else View.GONE
    }

    private fun showHeaderMenu(anchor: View) {
        val popup = PopupMenu(this, anchor)
        popup.menu.add(0, MENU_MARK_RESOLVED, 0, R.string.lc_mark_resolved)
        popup.setOnMenuItemClickListener { item ->
            if (item.itemId == MENU_MARK_RESOLVED) {
                viewModel.markActiveTicketResolved()
                true
            } else false
        }
        popup.show()
    }

    /** Public — let ActivityTabFragment ask us to switch to the Chat tab. */
    internal fun switchToTab(index: Int) {
        if (::viewPager.isInitialized) {
            viewPager.currentItem = index
        }
    }

    private fun showMemberDetailsForm() {
        val dialog = MemberDetailsDialogFragment.newInstance()
        dialog.onProfileSubmitted = { _ ->
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
        private const val MENU_MARK_RESOLVED = 1
    }
}
