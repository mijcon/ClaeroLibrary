package com.myclaero.claerolibrary.api

import com.myclaero.claerolibrary.core.Availability
import com.myclaero.claerolibrary.core.Ticket
import com.parse.ParseCloud
import org.json.JSONObject

private object CloudCode {

	const val AVAILABILITY_QUERY = "getAvailability"

	fun getAvailability(ticket: Ticket): Availability {
		val result = ParseCloud.callFunction<JSONObject>(
			AVAILABILITY_QUERY,
			mapOf("ticket" to ticket)
		)
		return Availability.fromJSON(result)
	}

	fun getAvailabilityAsync(
		ticket: Ticket,
		callback: (availability: Availability?, e: Exception?) -> Unit
	) {
		ParseCloud.callFunctionInBackground<JSONObject>(
			AVAILABILITY_QUERY,
			mapOf("ticket" to ticket)
		) { json, e ->
			callback(Availability.fromJSON(json), e)
		}
	}


}