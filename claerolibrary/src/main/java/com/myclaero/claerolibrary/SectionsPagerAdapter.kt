package com.myclaero.claerolibrary

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentPagerAdapter

// Implement the FragmentPagerAdapter interface to accept a List of Fragments
class SectionsPagerAdapter(fm: FragmentManager) : FragmentPagerAdapter(fm) {

    var fragList = mutableListOf<Fragment>()

    override fun getItem(p0: Int): Fragment = fragList[p0]

    override fun getCount(): Int = fragList.size

}