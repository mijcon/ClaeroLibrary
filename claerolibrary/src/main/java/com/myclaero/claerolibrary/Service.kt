package com.myclaero.claerolibrary

import com.myclaero.claerolibrary.extensions.toList
import com.parse.*
import com.parse.ktx.getIntOrNull
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject

@ParseClassName(Service.NAME)
class Service constructor() : ParseObject() {

    companion object {
        const val NAME = "Service"
        const val TAG = "Service"

        // The Parse Server's key for each field.
        // Each is named "KEY_TYPE" so it's always clear what data-type to expect.
        const val TITLE_STR = "title"
        const val ACTIVE_BOOL = "active"
        const val DESCRIPTION_STR = "description"
        const val SKU_STR = "sku"
        const val PRICE_INT = "price"
        const val SERVICES_REL = "serviceSet"
        const val UNIT_STR = "unit"
        const val UNITS_INCL_INT = "unitsIncluded"
        const val RESTRICTIONS_JSON = "restrictions"
        const val STANDALONE_BOOL = "standalone"
        const val DURATION_INT = "duration"
        const val ADDL_ITEM_REL = "additional"
        const val ADDL_PRICE_INT = "additionalPrice"
        const val REQUIRES_JSON = "requires"
        const val ORDER_INT = "order"
        const val REPORT_ARRAY = "report"

        const val VEHICLE_SERVICES_FUN = "vehicleServices"

        fun getServices(vehicle: Vehicle? = null): List<Service> =
            ParseCloud.callFunction<List<Service>>(
                VEHICLE_SERVICES_FUN,
                mapOf("vehicle" to vehicle?.objectId)
            )

        fun getServicesAsync(vehicle: Vehicle? = null, callback: FindCallback<Service>) {
            GlobalScope.launch(Dispatchers.Main) {
                try {
                    val services = withContext(Dispatchers.IO) { getServices(vehicle) }
                    callback.done(services, null)
                } catch (e: ParseException) {
                    callback.done(null, e)
                }
            }
        }
    }

    val title: String
        get() = getString(TITLE_STR)!!

    val description: String
        get() = getString(DESCRIPTION_STR)!!

    val isActive: Boolean
        get() = getBoolean(ACTIVE_BOOL)

    val sku: String
        get() = getString(SKU_STR)!!

    val priceInt: Int
        get() = getInt(PRICE_INT)

    val priceFloat: Float
        get() = priceInt.toFloat() / 100

    val frequency: Int
        get() = getInt(ORDER_INT)

    /**
     * Synchronously retrieves a Set of ParseServices that are in the same category as this ParseService.
     * Note: This Set WILL include the object in question.
     */
    val servicesSet: Set<Service>
        get() = getRelation<Service>(SERVICES_REL).query.find().toSet()

    val unit: String?
        get() {
            val string = getString(UNIT_STR)
            return if (string.isNullOrBlank()) null else string
        }

    val unitsIncluded: Int?
        get() = getIntOrNull(UNITS_INCL_INT)?.let { if (it > 0) it else null }

    val restrictions: JSONObject
        get() = getJSONObject(RESTRICTIONS_JSON) ?: JSONObject()

    val standalone: Boolean
        get() = getBoolean(STANDALONE_BOOL)

    val duration: Int
        get() = getInt(DURATION_INT)

    /**
     * Synchronously retrieves a Set of ParseInventories that qualify as "extra" items on this ParseService.
     */
    val additional: Set<ParseInventory>
        get() = getRelation<ParseInventory>(ADDL_ITEM_REL).query.find().toSet()

    /**
     * Returns the price of additional units in whole cents (e.g., 199 == $1.99)
     */
    val additionalPrice: Int?
        get() = getIntOrNull(ADDL_PRICE_INT)?.let { if (it > 0) it else null }

    /**
     * Returns the price of additional units in fractional dollars (e.g., 1.0F == $1.00)
     */
    val additionalPriceFloat: Float?
        get() = additionalPrice?.let { it.toFloat() / 100 }

    val requisites: JSONObject
        get() = getJSONObject(REQUIRES_JSON) ?: JSONObject()

    fun toSparceService(): SparseService = SparseService(objectId)

    /*
    var report: ClaeroReport?
        get() = getJSONArray(REPORT_ARRAY)?.let { ClaeroReport(it) }
        set(value) = putOrIgnore(REPORT_ARRAY, value)
     */

    data class SparseService(val objectId: String) {
        companion object {
            const val OBJECTID_STR = "objectId"
            const val INVENTORY_ARRAY = "inventory"
        }

        constructor(json: JSONObject) : this(json.getString(OBJECTID_STR)) {
            this.json = json
        }

        private var json: JSONObject = JSONObject()
        val inventory: MutableList<SparseInventory> =
            json.getJSONArray(INVENTORY_ARRAY)
                .toList<JSONObject>()
                .map { SparseInventory(it) }
                .toMutableList()

    }

    data class SparseInventory(val json: JSONObject) {
        companion object {
            const val QUANTITY_NUM = "quantity"
            const val OBJECTID_STR = "objectId"
        }

        val objectId = json.getString(OBJECTID_STR)
        val quantityInt = json.getInt(QUANTITY_NUM)
        val quantityFloat = json.getDouble(QUANTITY_NUM).toFloat()

    }

}