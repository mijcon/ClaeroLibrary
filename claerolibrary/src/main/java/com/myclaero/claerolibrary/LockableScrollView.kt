package com.myclaero.claerocustom

import android.annotation.SuppressLint
import android.content.Context
import android.util.AttributeSet
import android.view.MotionEvent
import android.widget.ScrollView

class LockableScrollView : ScrollView {

    constructor(context: Context) : super(context)

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs)

    var locked: Boolean = false

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean = if (locked) false else super.onTouchEvent(event)

    override fun onInterceptTouchEvent(ev: MotionEvent?): Boolean = if (locked) false else super.onInterceptTouchEvent(ev)

}