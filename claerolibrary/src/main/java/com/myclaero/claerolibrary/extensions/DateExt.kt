package com.myclaero.claerolibrary.extensions

import android.icu.util.Calendar
import java.util.*

fun Date.toCalendar(): Calendar =
	Calendar.getInstance().also {
		it.time = this
	}