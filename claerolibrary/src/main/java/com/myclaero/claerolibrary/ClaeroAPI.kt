package com.myclaero.claerolibrary

import android.location.Location
import com.myclaero.claerolibrary.core.Ticket
import com.myclaero.claerolibrary.extensions.Verified
import com.parse.ParseUser
import khttp.get
import khttp.post

/**
 * The purpose of this static class is to handle all input and output from the Claero API Gateway.
 * There should be no need for handling URLConnections or JSON outside of this class.
 */
object ClaeroAPI {

	const val TAG = "ClaeroAPI"

	const val STD_WINDOW_SECS: Long = 60 * 60 * 24 * 7 * 3

	private const val CLAERO_PUSH = BuildConfig.CLAERO_API_URL + "v1/push"
	private const val CLAERO_PUSH_TICKET = "$CLAERO_PUSH/ticket"
	private const val CLAERO_SCHEDULE = BuildConfig.CLAERO_API_URL + "v1/schedule"


	/**
	 * Checks whether the provided ParseUser's contact points have been verified.
	 */
	fun getVerificationStatus(user: ParseUser): Verified {
		val params = mapOf("user" to user.objectId)
		val request = get(
			CLAERO_USER_VERIFY,
			headers = claeroApiHeader,
			params = params
		)
		checkStatus(request.statusCode)

		val json = request.jsonObject
		val email = json.getBoolean("emailVerified")
		val phone = json.getBoolean("phoneVerified")
		return when {
			email && phone -> Verified.BOTH
			email -> Verified.EMAIL
			phone -> Verified.PHONE
			else -> Verified.NEITHER
		}
	}

	fun pushTicketStatus(
		ticket: Ticket,
		location: Location,
		status: Ticket.TechnicianStatus,
		callback: ((success: Boolean, e: Exception?) -> Unit)
	) {
		doAsync {
			try {
				val data = mapOf(
					"ticket" to ticket.objectId,
					"lat" to location.latitude,
					"lng" to location.longitude,
					"status" to status.value
				)

				val request = post(
					CLAERO_PUSH_TICKET,
					headers = claeroApiHeader,
					json = data
				)
				checkStatus(request.statusCode)
				uiThread { callback(request.statusCode == 200, null) }
			} catch (e: Exception) {
				uiThread { callback(false, e) }
			}
		}
	}

	fun queryAvailability(ticket: Ticket) {
		try {
			val data = mapOf("ticket" to ticket.objectId)

			val request = get(
				CLAERO_SCHEDULE,
				headers = claeroApiHeader,
				params = mapOf()
			)
		}
	}

}