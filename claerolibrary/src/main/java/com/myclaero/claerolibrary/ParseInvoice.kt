package com.myclaero.claerolibrary

import com.myclaero.claerolibrary.extensions.toList
import com.parse.*
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
        const val REPORT_JSON = "report"
    }

    val notes: MutableList<String> = getList<String>(NOTES_ARRAY) ?: mutableListOf()

    var mileage: Int?
        get() = getIntOrNull(MILEAGE_NUM)
        set(value) {
            putOrIgnore(MILEAGE_NUM, value)
        }

    var ticket: ParseTicket?
        get() = getParseObject(TICKET_POINT) as ParseTicket
        set(value) {
            putOrIgnore(TICKET_POINT, value)
        }

    val subtotal: Int? = getIntOrNull(SUBTOTAL_NUM)

    val services: MutableList<ParseService.SparseService> =
        (getJSONArray(SERVICES_ARRAY) ?: JSONArray()).toList<JSONObject>()
            .map { ParseService.SparseService(it) }
            .toMutableList()

    val charges: MutableList<ParseCharge> = getRelation<ParseCharge>(CHARGE_REL).query.find()

    fun getChargesAsync(callback: FindCallback<ParseCharge>) =
        getRelation<ParseCharge>(CHARGE_REL).query.findInBackground(callback)

    fun addChargeAsync(charge: ParseCharge, callback: SaveCallback?) =
        getRelation<ParseCharge>(CHARGE_REL).run {
            add(charge)
            saveInBackground(callback)
        }

    fun removeChargeAsync(charge: ParseCharge, callback: SaveCallback?) =
        getRelation<ParseCharge>(CHARGE_REL).run {
            remove(charge)
            saveInBackground(callback)
        }

    val report: ClaeroReport = ClaeroReport(getJSONObject(REPORT_JSON) ?: JSONObject())

}