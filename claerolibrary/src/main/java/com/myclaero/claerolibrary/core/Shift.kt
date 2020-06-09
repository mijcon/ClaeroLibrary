package com.myclaero.claerolibrary.core

import com.myclaero.claerolibrary.ParseHub
import com.myclaero.claerolibrary.extensions.timeInSecs
import com.myclaero.claerolibrary.extensions.toList
import com.parse.*
import com.parse.ktx.findAll
import com.parse.ktx.getLongOrNull
import com.parse.ktx.putOrRemove
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.util.*

@ParseClassName(Shift.NAME)
class Shift constructor() : ParseObject() {

    companion object {
        const val NAME = "Shift"
        const val TAG = "Shift"

        const val ACTIVE_BOOL = "active"
        const val START_LONG = "start"
        const val END_LONG = "end"
        const val HUB_POINT = "hub"
        const val TECH_POINT = "technician"
        const val TICKETS_REL = "tickets"
        const val SERVICES_JSON = "services"

        // This function could take a hot second...
        fun getShifts(
            t0: Long,
            t1: Long,
            callback: (shifts: Map<Shift, Set<Ticket>>?, e: Exception?) -> Unit
        ) {
            GlobalScope.launch(Dispatchers.Main) {
                val start = if (t0 > Int.MAX_VALUE) t0 / 1000L else t0
                val end = if (t1 > Int.MAX_VALUE) t1 / 1000L else t1
                try {
                    val shifts = withContext(Dispatchers.IO) {
                        ParseQuery(Shift::class.java)
                            .whereEqualTo(TECH_POINT, ParseUser.getCurrentUser())
                            .whereGreaterThanOrEqualTo(START_LONG, start)
                            .whereLessThanOrEqualTo(START_LONG, end)
                            .whereEqualTo(ACTIVE_BOOL, true)
                            .include(HUB_POINT)
                            .findAll()
                    }
                    val shiftIds = List(shifts.size) { shifts[it].objectId }
                    val tickets = withContext(Dispatchers.IO) {
                        ParseQuery(Ticket::class.java)
                            .whereContainedIn(Ticket.SHIFT_POINT, shiftIds)
                            .whereGreaterThanOrEqualTo(Ticket.STATUS_INT, Ticket.Status.OPEN.value)
                            .whereGreaterThanOrEqualTo(Ticket.START_DATE, start)
                            .whereLessThanOrEqualTo(Ticket.START_DATE, end)
                            .include(Ticket.PICKUP_POINT)
                            .include(Ticket.VEHICLE_POINT)
                            .findAll()
                    }
                    val shiftMap = mutableMapOf<Shift, Set<Ticket>>()
                    shifts.forEach { shift ->
                        shiftMap[shift] = tickets.filter { it.shift?.objectId == shift.objectId }.toSet()
                    }
                    callback(shiftMap.toMap(), null)
                } catch (e: Exception) {
                    callback(null, e)
                }
            }
        }

        internal fun fromJSON(json: JSONObject): Shift {
            val availability = json.getJSONArray("availability").toList<Int>()
            return Shift(
	            json.getString("objectId"),
	            json.getInt("travelTime"),
	            json.getLong("start"),
	            availability
            )
        }

    }

    constructor(val objectId: String, val travel: Int, val calendar: Calendar, val availability: List<Int>)

    constructor(objectId: String, travel: Int, date: Long, availability: List<Int>):
            this(objectId, travel, Calendar.getInstance().apply { timeInSecs = date }, availability)

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

    var isActive: Boolean
        get() = getBoolean(ACTIVE_BOOL)
        set(value) = put(ACTIVE_BOOL, value)

    var services: Map<String, Boolean>
        get() {
            val map = mutableMapOf<String, Boolean>()
            val json = getJSONObject(SERVICES_JSON)

            if (json == null) {
                val types = ParseConfig.getCurrentConfig().getJSONArray("service_types")
                for (i in 0.until(types.length())) {
                    map[types.getString(i)] = false
                }
            } else json.keys().forEach { key -> map[key] = json.optBoolean(key) }

            return map.toMap()
        }
        set(value) {
            val json = JSONObject()
            value.keys.forEach { k -> json.putOpt(k, value[k]) }
            put(SERVICES_JSON, json)
        }



}