package com.myclaero.claerolibrary

import org.json.JSONArray
import java.util.*

class ClaeroDate(_calendar: Calendar, openings: JSONArray) {

    companion object {
        const val TAG = "ClaeroDate"
    }

    val calendar = _calendar.clone() as Calendar

    val openings = List(openings.length()) { openings.getInt(it) }

    fun isActive(): Boolean = openings.filterNot { it == 0 }.isNotEmpty()

}