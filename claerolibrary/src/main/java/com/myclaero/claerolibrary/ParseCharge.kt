package com.myclaero.claerolibrary

import com.parse.ParseClassName
import com.parse.ParseObject
import org.jetbrains.anko.doAsync
import org.json.JSONObject

@ParseClassName(ParseCharge.NAME)
class ParseCharge private constructor() : ParseObject() {

    companion object {
        const val NAME = "Charge"
        const val TAG = "ParseCharge"
    }

    enum class Status(val value: Int) {
        DRAFT(-1),      //  Is not set to be Authorized
        PENDING(0),     //  Is waiting for be Authorized automatically 5 days prior to service
        AUTHORIZED(1),  //  Has been Authorized prior to a scheduled Service
        CAPTURED(2),    //  Has been successfully Captured after a scheduled Service
        CANCELED(3),    //  The ticket has been cancelled without any Charges occurring
        REFUNDED(4),    //  The ticket has been cancelled and funds have been returned to Source
        FAILED(5)       //  Stripe returned that the Charge failed
    }

    private lateinit var user: JSONObject
    private var amount: Int? = null
    private var source: JSONObject? = null
    private var ticket: JSONObject? = null
    private var status: Status =
        Status.DRAFT

    private val sourceId: String?
        get() = source?.getString("objectId")
    private val ticketId: String?
        get() = ticket?.getString("objectId")
    private val userId: String?
        get() = user.getString("objectId")

    constructor(charge: JSONObject) : this() {
        user = charge.getJSONObject("user")
        amount = charge.getInt("amount")
        source = charge.getJSONObject("source")
        ticket = charge.getJSONObject("ticket")
        status = when (charge.getInt("status")) {
            0 -> Status.PENDING
            1 -> Status.AUTHORIZED
            2 -> Status.CAPTURED
            3 -> Status.CANCELED
            4 -> Status.REFUNDED
            5 -> Status.FAILED
            else -> Status.DRAFT
        }
    }

    constructor(
        user: JSONObject,
        amount: Int? = null,
        source: JSONObject? = null,
        ticket: JSONObject? = null
    ) : this() {
        this.user = user
        this.amount = amount
        this.source = source
        this.ticket = ticket
    }




    /*
    /**
     * Creates a pre-authorized Charge through Stripe. Only available to Clients and Managers.
     */
    fun authorize(callback: (success: Boolean?, error: Exception?) -> Unit) {
        // Capture ONLY works when associated with a ParseTicket and there is an existing Charge
        // Process ONLY works for Managers.
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


	        var cxn: HttpsURLConnection? = null
            try {
                val roles = ParseRole.getQuery().find()
                if (roles.filter { it["objectId"] == CHARGE_ID }.count() < 1)
                    throw ClaeroAPI.UnauthorizedException("User is not authorized to authorize Charges.")

                val cxn = getConnection(capture = false)
	            ClaeroAPI.checkStatus(cxn.responseCode)
	            val response = cxn.readAll()
	            uiThread { callback(true, null) }
            } catch (e: Exception) {
	            uiThread { callback(null, e) }
            } finally {
                cxn?.disconnect()
            }
        }
    }

    /**
     * Captures a pre-authorized Charge or initiates an immediate Charge if Source was never pre-authorized.
     */
    fun capture(callback: (response: String?, error: Exception?) -> Unit) {
        // Capture ONLY works when associated with a ParseTicket and there is an existing Charge
        // Process ONLY works for Managers.
        doAsync {
            var response: String? = null
            var error: Exception? = null

            try {
                val roles = ParseRole.getQuery().find()
                if (roles.filter { it["objectId"] == CHARGE_ID }.count() < 1)
                    throw ClaeroAPI.UnauthorizedException("User is not authorized to process Charges.")

                if (ticket == null && roles.filter { it["objectId"] != MGR_ID }.count() < 1)
                    throw ClaeroAPI.UnauthorizedException("Only Managers may process Charges without associated Tickets.")

                val cxn = getConnection(capture = true)
                when (cxn.responseCode) {
                    200 -> { response = cxn.readAll() }
                    400 -> throw ClaeroAPI.MalformedDataException()
                    401 -> throw ClaeroAPI.UnauthorizedException("Session key could not be verified.")
                    500 -> throw ClaeroAPI.InternalErrorException()
                    else -> throw Exception("Unknown error.")
                }
            } catch (e: Exception) {
                error = e
            } finally {
                uiThread { callback(response, error) }
            }
        }
    }

	/*
    private fun getConnection(capture: Boolean = false): HttpsURLConnection {
        //
        //  Verify a few things: If cancelled, is Source different? Retry? Etc.
        //

        //  Submit the Charge
        val req = if (capture) "capture" else "authorize"
        val chargeUrl = URL(String.format(ClaeroAPI.API_CLAERO_CHARGE, req))
        val chargeCxn = chargeUrl.openConnection("POST")
        chargeCxn.setData(this@ParseCharge.toDataMap())
        return chargeCxn
    }*/

    private fun toDataMap(): String {
        val map = mutableMapOf(
            "user" to userId,
            "amount" to amount,
            "sessionToken" to ParseUser.getCurrentSessionToken(),
            "uuid" to objectId
        )
        ticket?.let { map.put("ticket", ticketId) }
        source?.let { map.put("token", sourceId) }

        return JSONObject(map).toString()
    }

    var customer: JSONObject? = null
    var vehicle: JSONObject? = null
    var service: JSONObject? = null
    var payment: JSONObject? = null
*/
}