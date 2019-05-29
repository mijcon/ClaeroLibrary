package com.myclaero.claerolibrary

import com.parse.*
import com.parse.ktx.findAll
import com.parse.ktx.getBooleanOrNull
import com.parse.ktx.getLongOrNull
import com.parse.ktx.putOrRemove
import kotlinx.coroutines.*
import java.util.*
import kotlin.system.measureTimeMillis

@ParseClassName(ParseShift.NAME)
class ParseShift constructor() : ParseObject() {

    companion object {
        const val NAME = "Shift"
        const val TAG = "ParseShift"

        const val ACTIVE_BOOL = "active"
        const val START_LONG = "start"
        const val END_LONG = "end"
        const val HUB_POINT = "hub"
        const val TECH_POINT = "technician"
        const val TICKETS_REL = "tickets"

        // This function could take a hot second...
        fun getShifts(
            t0: Long,
            t1: Long,
            callback: (shifts: Map<ParseShift, Set<ParseTicket>>?, e: Exception?) -> Unit
        ) {
            GlobalScope.launch(Dispatchers.Main) {
                val start = if (t0 > Int.MAX_VALUE) t0 / 1000L else t0
                val end = if (t1 > Int.MAX_VALUE) t1 / 1000L else t1
                try {
                    val shifts = withContext(Dispatchers.IO) {
                        ParseQuery(ParseShift::class.java)
                            .whereEqualTo(TECH_POINT, ParseUser.getCurrentUser())
                            .whereGreaterThanOrEqualTo(START_LONG, start)
                            .whereLessThanOrEqualTo(START_LONG, end)
                            .whereEqualTo(ACTIVE_BOOL, true)
                            .include(HUB_POINT)
                            .findAll()
                    }
                    val shiftIds = List(shifts.size) { shifts[it].objectId }
                    val tickets = withContext(Dispatchers.IO) {
                        ParseQuery(ParseTicket::class.java)
                            .whereContainedIn(ParseTicket.SHIFT_POINT, shiftIds)
                            .whereGreaterThanOrEqualTo(ParseTicket.STATUS_INT, ParseTicket.Status.OPEN.value)
                            .whereGreaterThanOrEqualTo(ParseTicket.START_LONG, start)
                            .whereLessThanOrEqualTo(ParseTicket.START_LONG, end)
                            .include(ParseTicket.LOCATION_POINT)
                            .include(ParseTicket.VEHICLE_POINT)
                            .findAll()
                    }
                    val shiftMap = mutableMapOf<ParseShift, Set<ParseTicket>>()
                    shifts.forEach { shift ->
                        shiftMap[shift] = tickets.filter { it.shift == shift.objectId }.toSet()
                    }
                    callback(shiftMap.toMap(), null)
                } catch (e: Exception) {
                    callback(null, e)
                }
            }
        }
    }

    var start: Date?
        get() = getLongOrNull(START_LONG)?.let { Date(it * 1000) }
        set(value) = put(START_LONG, value!!.time / 1000)

    var end: Date?
        get() = getLongOrNull(END_LONG)?.let { Date(it * 1000) }
        set(value) = put(END_LONG, value!!.time / 1000)

    var hub: ParseHub?
        get() = getParseObject(HUB_POINT) as ParseHub?
        set(value) = putOrRemove(HUB_POINT, value)

    var technician: ParseUser?
        get() = getParseUser(TECH_POINT)
        set(value) = putOrRemove(TECH_POINT, value)

    var isActive: Boolean?
        get() = getBooleanOrNull(ACTIVE_BOOL) ?: false
        set(value) = put(ACTIVE_BOOL, value!!)

    @Deprecated(
        "We'll avoid using this, because we had problems with too many simultaneous calls to the ParseServer.",
        ReplaceWith("ParseShift.companion.getShifts()")
    )
    var tickets: MutableList<ParseTicket>
        get() {
            val params = mapOf("shiftId" to objectId)
            return ParseCloud.callFunction("getTickets", params)
        }
        set(value) = Unit
}