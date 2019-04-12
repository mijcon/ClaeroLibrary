package com.myclaero.claerolibrary

import android.annotation.SuppressLint
import android.content.Context
import android.support.v4.view.ViewPager
import android.util.AttributeSet
import android.view.MotionEvent

class LockableViewPager : ViewPager {

    constructor(context: Context) : super(context)

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs)

    var isLocked: Boolean = false

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean = if (isLocked) false else super.onTouchEvent(event)

    override fun onInterceptTouchEvent(ev: MotionEvent?): Boolean = if (isLocked) false else super.onInterceptTouchEvent(ev)

}