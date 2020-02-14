package com.myclaero.claerolibrary

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter

// Implement the FragmentPagerAdapter interface to accept a List of Fragments
class FragmentAdapter(activity: FragmentActivity) : FragmentStateAdapter(activity) {
    var fragments: MutableList<Fragment> = mutableListOf()
    override fun getItemCount(): Int = fragments.size
    override fun createFragment(position: Int): Fragment = fragments[position]
}