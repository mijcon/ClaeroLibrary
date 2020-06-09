package com.myclaero.claerolibrary.core

import com.myclaero.claerolibrary.ParseInventory
import com.myclaero.claerolibrary.extensions.toList
import com.parse.*
import com.parse.ktx.getIntOrNull
import kotlinx.coroutines.*
import org.json.JSONObject

/**
 * A subclass of ParseObject representing an available Service for a User's [Vehicle].
 *
 * @constructor The default constructor as required by Parse to correctly subclass the object. Do
 * not use this constructor directly through the app.
 */
@ParseClassName(Service.NAME)
class Service constructor(): ParseObject() {

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

		/**
		 * *Synchronously* retrieves the available [Service]s for the provided [Vehicle], or a list
		 * of general Services if no Vehicle specified.
		 *
		 * @param vehicle The Vehicle for which to customize the Services.
		 * @return The collection of Services, customized for the specified Vehicle (or not
		 * customized if no Vehicle provided).
		 */
		fun fetch(vehicle: Vehicle? = null): List<Service> =
			ParseCloud.callFunction<List<Service>>(
				VEHICLE_SERVICES_FUN,
				mapOf("vehicle" to vehicle?.objectId)
			)

		/**
		 * Asynchronously retrieves the available [Service]s for the provided [Vehicle], or a list
		 * of general Services if no Vehicle specified.
		 *
		 * @param vehicle The Vehicle for which to customize the Services.
		 * @return The Kotlin Coroutine [Job] for the query.
		 */
		fun fetchAsync(vehicle: Vehicle? = null) =
			GlobalScope.async(Dispatchers.IO) {
				fetch(
					vehicle
				)
			}

		/**
		 * Asynchronously retrieves the available [Service]s for the provided [Vehicle], or a list
		 * of general Services if no Vehicle specified.
		 *
		 * @param vehicle The Vehicle for which to customize the Services.
		 * @param callback The [FindCallback] invoked upon completion of the query.
		 */
		fun fetchAsync(vehicle: Vehicle? = null, callback: FindCallback<Service>) {
			GlobalScope.launch(Dispatchers.Main) {
				try {
					val services =
						fetchAsync(
							vehicle
						)
					callback.done(services.await(), null)
				} catch (e: ParseException) {
					callback.done(null, e)
				}
			}
		}
	}

	/**
	 * The standard name of this Service.
	 */
	val title: String
		get() = getString(TITLE_STR)!!

	/**
	 * The long form description of the given Service.
	 */
	val description: String
		get() = getString(DESCRIPTION_STR)!!

	/**
	 * A boolean representing whether this Service is an active Service.
	 */
	val isActive: Boolean
		get() = getBoolean(ACTIVE_BOOL)

	/**
	 * A unique identifier for this Service.
	 */
	val sku: String
		get() = getString(SKU_STR)!!

	/**
	 * The price as an integer representing the smallest discrete unit of measure (e.g. pennies in
	 * USD, or single Yen in JPY).
	 */
	val priceInt: Int
		get() = getInt(PRICE_INT)

	/**
	 * The price as a float representing the fractional amount in the standard currency units
	 * (e.g. 150.00 for USD, but still 15000 for JPY).
	 */
	val priceFloat: Float
		get() = priceInt.toFloat() / 100

	/**
	 * An integer used to sort the individual Services.
	 */
	val order: Int
		get() = getInt(ORDER_INT)

	/**
	 * A String representing the unit of measure for the given Service (e.g. "qts" for quarts).
	 */
	val unit: String?
		get() = getString(UNIT_STR)

	/**
	 * The number of units included with the given Service. For example, an oil change may
	 * include 5 quarts of oil with the price, so this value would return 5.
	 */
	val unitsIncluded: Int?
		get() = getIntOrNull(UNITS_INCL_INT)?.let { if (it > 0) it else null }

	val restrictions: JSONObject
		get() = getJSONObject(RESTRICTIONS_JSON) ?: JSONObject()

	val standalone: Boolean
		get() = getBoolean(STANDALONE_BOOL)

	/**
	 * The amount of time, in minutes, this service is expected to take to complete.
	 */
	val duration: Int
		get() = getInt(DURATION_INT)

	/**
	 * Synchronously retrieves a Set of ParseInventories that qualify as "extra" items on this ParseService.
	 */
	val additional: Set<ParseInventory>
		get() = TODO()

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

	fun toSparceService(): SparseService =
		SparseService(objectId)

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

		constructor(json: JSONObject): this(json.getString(OBJECTID_STR)) {
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