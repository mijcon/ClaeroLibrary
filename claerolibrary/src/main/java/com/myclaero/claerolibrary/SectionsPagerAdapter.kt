package com.myclaero.claerolibrary

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentPagerAdapter

// Implement the FragmentPagerAdapter interface to accept a List of Fragments
class SectionsPagerAdapter(fm: androidx.fragment.app.FragmentManager) : androidx.fragment.app.FragmentPagerAdapter(fm) {

    var fragList = mutableListOf<androidx.fragment.app.Fragment>()

    override fun getItem(p0: Int): androidx.fragment.app.Fragment = fragList[p0]

    override fun getCount(): Int = fragList.size

}