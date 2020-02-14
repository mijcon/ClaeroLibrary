package com.myclaero.claerolibrary

import com.myclaero.claerolibrary.extensions.toList
import com.parse.FindCallback
import com.parse.ParseClassName
import com.parse.ParseObject
import com.parse.SaveCallback
import com.parse.ktx.getIntOrNull
import com.parse.ktx.putOrIgnore
import org.json.JSONArray
import org.json.JSONObject

@ParseClassName(ParseInvoice.NAME)
class ParseInvoice constructor() : ParseObject() {

    companion object {
        const val NAME = "Invoice"
        const val TAG = "ParseInvoice"

        // The Parse Server's key for each field.
        // Each is named "KEY_TYPE" so it's always clear what data-type to expect.
        const val NOTES_ARRAY = "notes"
        const val TICKET_POINT = "ticket"
        const val MILEAGE_NUM = "mileage"
        const val SUBTOTAL_NUM = "subtotal"
        const val SERVICES_ARRAY = "services"
        const val CHARGE_REL = "charge"
        const val REPORT_JSON = "json"
    }

    constructor(ticket: Ticket) : this() {
        this.ticket = ticket
    }

    val notes: MutableList<String>
        get() = getList<String>(NOTES_ARRAY) ?: mutableListOf()

    var mileage: Int?
        get() = getIntOrNull(MILEAGE_NUM)
        set(value) {
            putOrIgnore(MILEAGE_NUM, value)
        }

    var ticket: Ticket?
        get() = getParseObject(TICKET_POINT) as Ticket
        set(value) {
            putOrIgnore(TICKET_POINT, value)
        }

    val subtotal: Int?
        get() = getIntOrNull(SUBTOTAL_NUM)

    val services: MutableList<Service.SparseService> =
        (getJSONArray(SERVICES_ARRAY) ?: JSONArray()).toList<JSONObject>()
            .map { Service.SparseService(it) }
            .toMutableList()

    val charges: MutableList<ParseCharge>
        get() = getRelation<ParseCharge>(CHARGE_REL).query.find()

    fun getChargesAsync(callback: FindCallback<ParseCharge>) {
        getRelation<ParseCharge>(CHARGE_REL).query.findInBackground(callback)
    }

    fun addChargeAsync(charge: ParseCharge, callback: SaveCallback?) {
        getRelation<ParseCharge>(CHARGE_REL).run {
            add(charge)
            saveInBackground(callback)
        }
    }

    fun removeChargeAsync(charge: ParseCharge, callback: SaveCallback?) {
        getRelation<ParseCharge>(CHARGE_REL).run {
            remove(charge)
            saveInBackground(callback)
        }
    }

    /*
    var report: ClaeroReport
        get() = ClaeroReport(this, getJSONObject(REPORT_JSON) ?: JSONObject()).also { report = it }
        private set(value) { put(REPORT_JSON, value.json) }
     */

}