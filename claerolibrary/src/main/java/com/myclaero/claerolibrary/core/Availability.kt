package com.myclaero.claerolibrary.core

import org.json.JSONArray
import org.json.JSONObject

data class Availability(val open: Int, val close: Int) {

    companion object {
        const val TAG = "ClaeroAvailability"

        fun fromJSON(json: JSONObject): Availability {
            val availability = Availability(
                json.getInt("open"),
                json.getInt("close")
            )
            availability.addAllShifts(json.getJSONArray("shifts"))
            return availability
        }

    }

    val shifts: MutableSet<Shift> = mutableSetOf()

    private fun addShift(json: JSONObject) {
        shifts.add(Shift.fromJSON(json))
    }

    private fun addAllShifts(jsonArray: JSONArray) {
        for (i in 0.until(jsonArray.length())) {
            addShift(jsonArray.getJSONObject(i))
        }
    }

}