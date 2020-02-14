package com.myclaero.claerolibrary

import android.location.Location
import android.util.Log
import com.myclaero.claerolibrary.extensions.Verified
import com.myclaero.claerolibrary.extensions.readAll
import com.parse.ParseConfig
import com.parse.ParseUser
import khttp.get
import khttp.post
import khttp.put
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

        private const val CLAERO_USER_VERIFY = BuildConfig.CLAERO_API_URL + "v0/client/verify"
        // private const val CLAERO_USER_VERIFY = BuildConfig.CLAERO_API_URL + "v0/client/verify?user=%s"
        private const val CLAERO_PHONE_VERIFY = BuildConfig.CLAERO_API_URL + "v0/client/phone"
        private const val CLAERO_CHARGE = BuildConfig.CLAERO_API_URL + "v0/charge?req=%s"
        private const val CLAERO_CLIENT = BuildConfig.CLAERO_API_URL + "v0/client?user=%s"
        private const val CLAERO_SERVICE = BuildConfig.CLAERO_API_URL + "v0/service?ticket=%s"
        private const val CLAERO_CLIENT_PHONE = BuildConfig.CLAERO_API_URL + "v0/client/phone"
        private const val CLAERO_SOURCE_CARD = BuildConfig.CLAERO_API_URL + "v0/sources/card"
        private const val CLAERO_SOURCE_BANK = BuildConfig.CLAERO_API_URL + "v0/sources/bank"

        private const val CLAERO_SCHEDULE = BuildConfig.CLAERO_API_URL + "v1/schedule"
        private const val CLAERO_PUSH = BuildConfig.CLAERO_API_URL + "v1/push"
        private const val CLAERO_PUSH_TICKET = "$CLAERO_PUSH/ticket"
        private const val CLAERO_CLIENT_SEARCH = BuildConfig.CLAERO_API_URL + "v0/client?q=%s&p=%s&session=%s"
        private const val CLAERO_SERVICE_SEARCH = BuildConfig.CLAERO_API_URL + "v0/service?q=%s&p=%s&session=%s"
        private const val CLAERO_VEHICLE_SEARCH = BuildConfig.CLAERO_API_URL + "v0/vehicle?client=%s&session=%s"

        private val claeroApiHeader = mapOf(
            "X-Api-Key" to ParseConfig.getCurrentConfig().getString("claero_api_key")
        )

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

        /**
         * Synchronously either submits a request to send a one-time code or a request to verify a code.
         *
         * @param user   The Parse ObjectID of the ParseUser.
         * @param token  An App-Specific SMS Token (Oreo 8.0+) used to intercept verification texts auto-magically.
         * @param code   The temporary code used to verify the phone number.
         */
        fun verifyText(user: String, token: String? = null, code: String? = null): JSONObject {
            val params = mapOf("user" to user)
            val data = mapOf("token" to token, "code" to code)
            val request = post(
                CLAERO_PHONE_VERIFY,
                params = params,
                json = data,
                headers = claeroApiHeader
            )
            checkStatus(request.statusCode)
            return request.jsonObject
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
                val params = mapOf("user" to user.objectId)
                val data = mapOf("token" to token)
                val request = put(
                    CLAERO_SOURCE_CARD,
                    params = params,
                    json = data
                )
                var verified: Boolean? = null
                var error: Exception? = null

                try {
                    checkStatus(request.statusCode)
                    verified = request.jsonObject.getBoolean("emailVerified")
                } catch (e: Exception) {
                    error = e
                } finally {
                    uiThread { callback(verified, error) }
                }
            }
        }

        /**
         * Takes a Plaid-generated Plaid Token and exchanges it for the corresponding Stripe Source Token.
         */
        fun getBankToken(
            token: String, accountId: String, test: Boolean = true,
            callback: (stripeToken: String?, requestId: String?, error: Exception?) -> Unit
        ) {
            doAsync {
                val data = mapOf(
                    "token" to token,
                    "account_id" to accountId,
                    "test" to test
                )
                val request = post(
                    CLAERO_SOURCE_BANK,
                    json = data
                )

                var json: JSONObject? = null
                var error: Exception? = null

                try {
                    checkStatus(request.statusCode)
                    json = request.jsonObject
                } catch (e: Exception) {
                    error = e
                }
                uiThread {
                    callback(
                        json?.getString("stripe_bank_account_token"),
                        json?.getString("request_id"),
                        error
                    )
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

        fun queryServices(vehicle: Vehicle?, callback: (JSONArray?, Exception?) -> Unit) {
            queryServices(vehicle?.objectId, callback)
        }

        fun queryServices(vehicle: JSONObject?, callback: (JSONArray?, Exception?) -> Unit) {
            queryServices(vehicle?.optString("objectId"), callback)
        }

        /**
         * Synchronously queries the Claero API for an availability table.
         */
        fun queryAvailability(ticket: Ticket): ClaeroAvailability? {
            val params = mutableMapOf<String, String>()
            ticket.objectId?.let { params.put("ticket", it) }
            ticket.location?.geoPoint?.let { params.put("lat_lng", "${it.latitude},${it.longitude}") }
            val request = get(
                CLAERO_SCHEDULE,
                params = params,
                headers = claeroApiHeader
            )
            checkStatus(request.statusCode)
            return ClaeroAvailability.fromJSON(request.jsonObject)
        }

        fun scheduleTicket(ticket: Ticket, shiftId: String): Boolean {
            val params = mapOf(
                "ticket" to ticket.objectId,
                "shift" to shiftId
            )
            val request = put(
                CLAERO_SCHEDULE,
                params = params,
                headers = claeroApiHeader
            )
            checkStatus(request.statusCode)
            return request.statusCode == 200
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
                it.addRequestProperty("X-Api-Key", ParseConfig.getCurrentConfig().getString("claero_api_key"))
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

    class AcceptedException(message: String? = null) : Exception(message)  // 202
    class MalformedDataException(message: String? = null) : Exception(message)  // 400
    class UnauthorizedException(message: String? = null) : Exception(message)  // 401
    class InvalidApiKeyException(message: String? = null) : Exception(message)  // 403
    class InvalidApiPathException(message: String? = null) : Exception(message)  // 404
    class InvalidMethodException(message: String? = null) : Exception(message)  // 405
    class InternalErrorException(message: String? = null) : Exception(message)  // 500
    class ServerUnavailableException(message: String? = null) : Exception(message)  // 503
    class GatewayTimeoutException(message: String? = null) : Exception(message)  // 504
    class UnknownStatusException(message: String? = null) : Exception(message)  // Misc

}