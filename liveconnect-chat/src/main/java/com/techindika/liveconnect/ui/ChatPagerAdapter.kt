package com.techindika.liveconnect.ui

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter

/**
 * ViewPager2 adapter for Chat and Activity tabs.
 */
internal class ChatPagerAdapter(activity: FragmentActivity) : FragmentStateAdapter(activity) {

    override fun getItemCount(): Int = 2

    override fun createFragment(position: Int): Fragment = when (position) {
        0 -> ChatTabFragment()
        1 -> ActivityTabFragment()
        else -> throw IllegalArgumentException("Invalid tab position: $position")
    }
}
