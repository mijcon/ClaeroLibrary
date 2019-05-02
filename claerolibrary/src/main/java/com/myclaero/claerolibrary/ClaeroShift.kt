package com.myclaero.claerolibrary

import com.myclaero.claerolibrary.extensions.timeInSecs
import com.myclaero.claerolibrary.extensions.toList
import org.json.JSONObject
import java.util.*

data class ClaeroShift internal constructor(val objectId: String, val travel: Int, val calendar: Calendar, val availability: List<Int>) {

    companion object {
        const val TAG = "ClaeroShift"

        internal fun fromJSON(json: JSONObject): ClaeroShift {
            val availability = json.getJSONArray("availability").toList<Int>()
            return ClaeroShift(
                json.getString("objectId"),
                json.getInt("travelTime"),
                json.getLong("start"),
                availability
            )
        }
    }

    constructor(objectId: String, travel: Int, date: Long, availability: List<Int>):
            this(objectId, travel, Calendar.getInstance().apply { timeInSecs = date }, availability)

}