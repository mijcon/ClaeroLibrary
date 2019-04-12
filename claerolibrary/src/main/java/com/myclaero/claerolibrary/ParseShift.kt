package com.myclaero.claerolibrary

import com.parse.*
import com.parse.ktx.getBooleanOrNull
import com.parse.ktx.getLongOrNull
import org.jetbrains.anko.doAsync
import org.jetbrains.anko.uiThread
import java.util.*

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

        fun getShifts(callback: (shifts: List<ParseShift>?, e: ParseException?) -> Unit) {
            ParseQuery(ParseShift::class.java)
                .whereEqualTo(TECH_POINT, ParseUser.getCurrentUser())
                .include(HUB_POINT)
                .findInBackground { shifts, e ->
                    callback(shifts.sortedBy { it.start }, e)
                }
        }

        /*fun getAllShifts(callback: (shifts: MutableList<ParseHub>?, e: ParseException?) -> Unit) {
            ClaeroAPI.queryShifts(ParseUser.getCurrentSessionToken(), Date()) {

            }
            ParseDecoder.get().decode(JSONObject())
        }*/
    }

    var start: Date?
        get() = getLongOrNull(START_LONG)?.let { Date(it * 1000) }
        set(value) = put(START_LONG, value!!.time / 1000)

    var end: Date?
        get() = getLongOrNull(END_LONG)?.let { Date(it * 1000) }
        set(value) = put(END_LONG, value!!.time / 1000)

    var hub: ParseHub?
        get() = getParseObject(HUB_POINT) as ParseHub?
        set(value) = put(HUB_POINT, value!!)

    var technician: ParseUser?
        get() = getParseUser(TECH_POINT)
        set(value) = put(TECH_POINT, value!!)

    var isActive: Boolean?
        get() = getBooleanOrNull(ACTIVE_BOOL) ?: false
        set(value) = put(ACTIVE_BOOL, value!!)

    var tickets: MutableList<ParseTicket>
        get() = getRelation<ParseTicket>(TICKETS_REL)
            .query
            .include(ParseTicket.VEHICLE_POINT)
            .include(ParseTicket.LOCATION_POINT)
            .find()
            .sortedBy { it.time }
            .toMutableList()
        set(value) {

        }

    fun getTickets(callback: (tickets: List<ParseTicket>?, e: ParseException?) -> Unit) {
        doAsync {
            var tickets: List<ParseTicket>? = null
            var error: ParseException? = null
            try {
                tickets = this@ParseShift.tickets
            } catch (e: ParseException) {
                error = e
            }
            uiThread { callback(tickets, error) }
        }
    }

}