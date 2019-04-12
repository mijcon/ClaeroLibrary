package com.myclaero.claerolibrary

import android.util.Log
import com.myclaero.claerolibrary.extensions.Verified
import com.myclaero.claerolibrary.extensions.readAll
import com.myclaero.claerolibrary.extensions.timeInSecs
import com.parse.ParseSession
import com.parse.ParseUser
import org.jetbrains.anko.doAsync
import org.jetbrains.anko.uiThread
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedOutputStream
import java.io.BufferedWriter
import java.io.OutputStreamWriter
import java.net.URL
import java.net.URLConnection
import java.util.*
import javax.net.ssl.HttpsURLConnection

/**
 * The purpose of this static class is to handle all input and output from the Claero API Gateway.
 * There should be no need for handling URLConnections or JSON outside of this class.
 */
class ClaeroAPI {

	companion object {
		const val TAG = "ClaeroAPI"

		const val STD_WINDOW_SECS: Long = 60 * 60 * 24 * 7 * 3

		private const val CLAERO_USER_VERIFY = BuildConfig.CLAERO_API_URL + "/client/verify?user=%s"
		private const val CLAERO_PHONE_VERIFY = BuildConfig.CLAERO_API_URL + "/client/phone?user=%s"
		private const val CLAERO_CHARGE = BuildConfig.CLAERO_API_URL + "/charge?req=%s"
		internal const val CLAERO_CLIENT = BuildConfig.CLAERO_API_URL + "/client?user=%s"
		private const val CLAERO_SERVICE = BuildConfig.CLAERO_API_URL + "/service?ticket=%s"
		private const val CLAERO_CLIENT_PHONE = BuildConfig.CLAERO_API_URL + "/client/phone"
		private const val CLAERO_SOURCE_CARD = BuildConfig.CLAERO_API_URL + "/sources/card?user=%s"
		private const val CLAERO_SOURCE_BANK = BuildConfig.CLAERO_API_URL + "/sources/bank"

		private const val CLAERO_SCHEDULE_SEARCH = BuildConfig.CLAERO_API_URL + "/schedule?ticket=%s&lat_lng=%s&z=%d"
		private const val CLAERO_SCHEDULE_SUBMIT = BuildConfig.CLAERO_API_URL + "/schedule?ticket=%s"
		private const val CLAERO_CLIENT_SEARCH = BuildConfig.CLAERO_API_URL + "/client?q=%s&p=%s&session=%s"
		private const val CLAERO_SERVICE_SEARCH = BuildConfig.CLAERO_API_URL + "/service?q=%s&p=%s&session=%s"
		private const val CLAERO_VEHICLE_SEARCH = BuildConfig.CLAERO_API_URL + "/vehicle?client=%s&session=%s"

		/**
		 * Checks whether the provided ParseUser's contact points have been verified.
		 */
		fun getVerificationStatus(user: ParseUser): Verified {
			val verifyUrl = URL(String.format(CLAERO_USER_VERIFY, user.objectId))
			val verifyCxn = verifyUrl.openConnection("GET")
			checkStatus(verifyCxn.responseCode)
			val response = JSONObject(verifyCxn.readAll())
			verifyCxn.disconnect()

			val email = response.getBoolean("emailVerified")
			val phone = response.getBoolean("phoneVerified")
			return when {
				email && phone -> Verified.BOTH
				email -> Verified.EMAIL
				phone -> Verified.PHONE
				else -> Verified.NEITHER
			}
		}

		/**
		 * Synchronously either submits a request to send a one-time code or a request to verify a code.
		 *
		 * @param user   The Parse ObjectID of the ParseUser.
		 * @param token  An App-Specific SMS Token (Oreo 8.0+) used to intercept verification texts auto-magically.
		 * @param code   The temporary code used to verify the phone number.
		 */
		fun verifyText(user: String, token: String? = null, code: String? = null): JSONObject {
			val verifyUrl = URL(String.format(CLAERO_PHONE_VERIFY, user))
			val connection = verifyUrl.openConnection("POST")
			val data = mapOf("token" to token, "code" to code)
			connection.setData(data)
			val statusCode = checkStatus(connection.responseCode)
			val response = connection.readAll()
			connection.disconnect()
			val json = JSONObject(response)
			return json
		}

		/**
		 * Takes a new ParseUser and creates a corresponding Stripe Customer
		 */
		fun createCustomer(user: ParseUser, callback: ((succeeded: Boolean) -> Unit)? = null) {
			doAsync {
				val registerUrl = URL(String.format(CLAERO_CLIENT, user.objectId))
				val claeroCxn = registerUrl.openConnection("POST")
				uiThread { callback?.invoke(claeroCxn.responseCode == 200) }
			}
		}

		/**
		 * Takes a Stripe-generated Token for a Card and saves it to the provided ParseUser's corresponding Stripe Customer.
		 */
		fun saveCard(user: ParseUser, token: String, callback: (verified: Boolean?, e: Exception?) -> (Unit)) {
			doAsync {
				val data = "{ \"token\": \"$token\"  }"

				val verifyUrl = URL(String.format(CLAERO_SOURCE_CARD, user.objectId))
				val verifyCxn = verifyUrl.openConnection("PUT")
				verifyCxn.setData(data)
				var verified: Boolean? = null
				var error: Exception? = null

				if (verifyCxn.responseCode == 200) {
					verified = JSONObject(verifyCxn.readAll()).getBoolean("emailVerified")
				} else {
					error = Exception(verifyCxn.responseMessage)
				}

				verifyCxn.disconnect()
				uiThread { callback(verified, error) }
			}
		}

		/**
		 * Takes a Plaid-generated Plaid Token and exchanges it for the corresponding Stripe Source Token.
		 */
		fun getBankToken(
			publicToken: String,
			accountId: String,
			test: Boolean = true,
			callback: (stripeToken: String?, requestId: String?, error: Exception?) -> Unit
		) {
			doAsync {
				try {
					val data = JSONObject().apply {
						put("token", publicToken)
						put("account_id", accountId)
						put("test", test)
					}.toString()

					val verifyUrl = URL(CLAERO_SOURCE_BANK)
					val verifyCxn = verifyUrl.openConnection("POST")
					verifyCxn.setData(data)

					if (verifyCxn.responseCode == 200) {
						val response = JSONObject(verifyCxn.readAll())
						uiThread {
							callback(
								response.getString("stripe_bank_account_token"),
								response.getString("request_id"),
								null
							)
						}
					} else {
						throw Exception(verifyCxn.responseMessage)
					}
					verifyCxn.disconnect()
				} catch (e: Exception) {
					uiThread { callback(null, null, e) }
				}
			}
		}

		/**
		 * Queries the Claero API for a list of JSONUsers that match the provided String.
		 */
		fun queryUsers(query: String, session: String, callback: (JSONArray, Long) -> Unit) {
			doAsync {
				try {
					val phone = query.toIntOrNull() ?: ""
					val verifyUrl = URL(String.format(CLAERO_CLIENT_SEARCH, query, phone, session))
					val verifyCxn = verifyUrl.openConnection("GET")
					if (verifyCxn.responseCode == 200) {
						val response = JSONObject(verifyCxn.readAll())
						val results = response.getJSONArray("results")
						uiThread { callback(results, Date().time) }
					} else {
						throw Exception(verifyCxn.responseMessage)
					}
					verifyCxn.disconnect()
				} catch (e: Exception) {
					Log.e(TAG, "Connection failed.")
				}
			}
		}

		/**
		 * Queries the Claero API for a list of JSONVehicles for the provided JSONUser
		 */
		fun queryVehicles(user: String, session: String, callback: (JSONArray?, Exception?) -> Unit) {
			doAsync {
				var verifyCxn: HttpsURLConnection? = null
				try {
					val verifyUrl = URL(String.format(CLAERO_VEHICLE_SEARCH, user, session))
					verifyCxn = verifyUrl.openConnection("GET")
					if (verifyCxn.responseCode == 200) {
						val response = JSONObject(verifyCxn.readAll())
						val results = response.getJSONArray("results")
						uiThread { callback(results, null) }
					} else {
						throw Exception(verifyCxn.responseMessage)
					}
				} catch (e: Exception) {
					uiThread { callback(null, e) }
				} finally {
					verifyCxn?.disconnect()
				}
			}
		}

		fun queryVehicles(user: JSONObject, session: String, callback: (JSONArray?, Exception?) -> Unit) {
			user.optString("objectId")?.let {
				queryVehicles(it, session, callback)
			} ?: callback(null, MalformedDataException("JSONUser missing objectId."))
		}

		fun queryVehicles(user: ParseUser, session: String, callback: (JSONArray?, Exception?) -> Unit) {
			queryVehicles(user.objectId, session, callback)
		}

		/**
		 * Queries the Claero API for a list of JSONServices for the provided ParseVehicle
		 */
		fun queryServices(vehicle: String?, callback: (JSONArray?, Exception?) -> Unit) {
			doAsync {
				var verifyCxn: HttpsURLConnection? = null
				try {
					val verifyUrl = URL(String.format(CLAERO_SERVICE_SEARCH, vehicle ?: ""))
					verifyCxn = verifyUrl.openConnection("GET")
					if (verifyCxn.responseCode == 200) {
						val response = JSONObject(verifyCxn.readAll())
						val results = response.getJSONArray("results")
						uiThread { callback(results, null) }
					} else {
						throw Exception(verifyCxn.responseMessage)
					}
				} catch (e: Exception) {
					uiThread { callback(null, e) }
				} finally {
					verifyCxn?.disconnect()
				}
			}
		}

		fun queryServices(vehicle: ParseVehicle?, callback: (JSONArray?, Exception?) -> Unit) {
			queryServices(vehicle?.objectId, callback)
		}

		fun queryServices(vehicle: JSONObject?, callback: (JSONArray?, Exception?) -> Unit) {
			queryServices(vehicle?.optString("objectId"), callback)
		}

		/**
		 * Synchronously queries the Claero API for an availability table.
		 */
		fun queryAvailability(ticketId: String?, latLng: String, midnight: Long? = null): ClaeroAvailability? {
			// Build a new Calendar object
			val cal = Calendar.getInstance().apply {
				set(Calendar.HOUR_OF_DAY, 0)
				set(Calendar.MINUTE, 0)
				set(Calendar.SECOND, 0)
				set(Calendar.MILLISECOND, 0)
				add(Calendar.DAY_OF_MONTH, 1)
			}
			val z = midnight ?: cal.timeInSecs
			val url = URL(String.format(ClaeroAPI.CLAERO_SCHEDULE_SEARCH, ticketId ?: "", latLng, z))
			val connection = url.openConnection("GET")
			ClaeroAPI.checkStatus(connection.responseCode)
			// Good Response
			val jsonData = JSONObject(connection.readAll())
			connection.disconnect()

			val dates = mutableListOf<ClaeroDate>()
			val jsonDates = jsonData.getJSONArray("dates")
			for (i in 0.until(jsonData.getInt("count"))) {
				val newDate = ClaeroDate(cal, jsonDates.getJSONArray(i))
				dates.add(newDate)
				cal.add(Calendar.DAY_OF_MONTH, 1)
			}

			val delay = jsonData.getInt("delay")
			val open = jsonData.getInt("open")
			val close = jsonData.getInt("close")

			return ClaeroAvailability(dates, delay, open, close)
		}

		fun scheduleTicket(ticketId: String): Boolean {
			val url = URL(String.format(ClaeroAPI.CLAERO_SCHEDULE_SUBMIT, ticketId))
			val connection = url.openConnection("PUT")
			val response = ClaeroAPI.checkStatus(connection.responseCode)
			connection.disconnect()
			return response == 200
		}

		/**
		 * Utilizes the ClaeroAPI to retrieve all available ParseSessions, overriding CLP and ACLs.
		 * Must have sufficient privileges or will return UnauthorizedException.
		 * @param session   ParseUser Session Token
		 * @param start     Time (in milliseconds from Epoch) for the earliest result.
		 * @param end       Time (in milliseconds from Epoch) for the latest result. (Optional, default is 3 weeks from start time.)
		 * @param callback  Returns results and/or Exceptions from Asynchronous query.
		 */
		fun queryShifts(
			session: String,
			start: Long,
			end: Long = start + STD_WINDOW_SECS,
			callback: ((shifts: MutableList<ParseShift>?, e: Exception?) -> Unit)
		) {
			TODO("implement")
		}

		/**
		 * Utilizes the ClaeroAPI to retrieve all available ParseSessions, overriding CLP and ACLs.
		 * Must have sufficient privileges or will return UnauthorizedException.
		 * @param session   ParseUser Session Token
		 * @param start     Date for the earliest result.
		 * @param end       Date for the latest result. (Optional, default is 3 weeks from start time.)
		 * @param callback  Returns results and/or Exceptions from Asynchronous query.
		 */
		fun queryShifts(
			session: ParseSession,
			start: Date,
			end: Date = Date(start.time + STD_WINDOW_SECS),
			callback: ((shifts: MutableList<ParseShift>?, e: Exception?) -> Unit)
		) {
			queryShifts(session.sessionToken, start.time, end.time, callback)
		}

		/*fun preauthCharge(callback: (verified: Boolean?, e: Exception?) -> (Unit)) {
			doAsync {
				val verifyUrl = URL(String.format(CLAERO_USER_VERIFY, ParseUser.getCurrentUser().objectId))
				val verifyCxn = verifyUrl.openConnection("GET")

				var verified: Boolean? = null
				var error: Exception? = null

				if (verifyCxn.responseCode == 200) {
					verified = JSONObject(verifyCxn.readAll()).getBoolean("emailVerified")
				} else {
					error = Exception(verifyCxn.responseMessage)
				}

				verifyCxn.disconnect()
				uiThread { callback(verified, error) }
			}
		}

		fun applyCharge(callback: (verified: Boolean?, e: Exception?) -> (Unit)) {
			doAsync {
				val verifyUrl = URL(String.format(CLAERO_USER_VERIFY, ParseUser.getCurrentUser().objectId))
				val verifyCxn = verifyUrl.openConnection("GET")

				var verified: Boolean? = null
				var error: Exception? = null

				if (verifyCxn.responseCode == 200) {
					verified = JSONObject(verifyCxn.readAll()).getBoolean("emailVerified")
				} else {
					error = Exception(verifyCxn.responseMessage)
				}

				verifyCxn.disconnect()
				uiThread { callback(verified, error) }
			}
		}*/


		private fun URL.openConnection(requestMethod: String): HttpsURLConnection {
			return (this.openConnection() as HttpsURLConnection).also {
				it.requestMethod = requestMethod
				it.addRequestProperty("X-Api-Key", BuildConfig.CLAERO_API_KEY)
				if (requestMethod == "GET") {
					it.addRequestProperty("Accept", "application/json")
				} else {
					it.addRequestProperty("Content-Type", "application/json")
				}
			}
		}

		fun checkStatus(statusCode: Int): Int {
			when (statusCode) {
				200, 201 -> return statusCode
				202 -> throw AcceptedException()
				400 -> throw MalformedDataException()
				401 -> throw UnauthorizedException()
				403 -> throw InvalidApiKeyException()
				404 -> throw InvalidApiPathException()
				405 -> throw InvalidMethodException()
				500 -> throw InternalErrorException()
				503 -> throw ServerUnavailableException()
				504 -> throw GatewayTimeoutException()
				else -> throw UnknownStatusException("Status Code: $statusCode")
			}
		}

		fun URLConnection.setData(data: Map<String, Any?>) {
			setData(JSONObject(data).toString())
		}

		fun URLConnection.setData(data: JSONObject) {
			setData(data.toString())
		}

		fun URLConnection.setData(data: String) {
			val outputStream = BufferedOutputStream(outputStream)
			BufferedWriter(OutputStreamWriter(outputStream, "utf-8")).run {
				write(data)
				flush()
				close()
			}
			outputStream.close()
		}

	}

	data class ClaeroAvailability(val dates: List<ClaeroDate>, val travelTime: Int, val open: Int, val close: Int)

	class AcceptedException(message: String? = null): Exception(message)  // 202
	class MalformedDataException(message: String? = null): Exception(message)  // 400
	class UnauthorizedException(message: String? = null): Exception(message)  // 401
	class InvalidApiKeyException(message: String? = null): Exception(message)  // 403
	class InvalidApiPathException(message: String? = null): Exception(message)  // 404
	class InvalidMethodException(message: String? = null): Exception(message)  // 405
	class InternalErrorException(message: String? = null): Exception(message)  // 500
	class ServerUnavailableException(message: String? = null): Exception(message)  // 503
	class GatewayTimeoutException(message: String? = null): Exception(message)  // 504
	class UnknownStatusException(message: String? = null): Exception(message)  // Misc

}