package com.myclaero.claerolibrary

import org.json.JSONArray
import org.json.JSONObject

data class ClaeroAvailability(val open: Int, val close: Int) {

    companion object {
        const val TAG = "ClaeroAvailability"

        fun fromJSON(json: JSONObject): ClaeroAvailability {
            val availability = ClaeroAvailability(
                json.getInt("open"),
                json.getInt("close")
            )
            availability.addAllShifts(json.getJSONArray("shifts"))
            return availability
        }
    }

    val shifts: MutableSet<ClaeroShift> = mutableSetOf()

    private fun addShift(json: JSONObject) {
        shifts.add(ClaeroShift.fromJSON(json))
    }

    private fun addAllShifts(jsonArray: JSONArray) {
        for (i in 0.until(jsonArray.length())) {
            addShift(jsonArray.getJSONObject(i))
        }
    }

}