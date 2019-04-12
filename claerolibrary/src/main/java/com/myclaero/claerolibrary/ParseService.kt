package com.myclaero.claerolibrary

import com.parse.ParseClassName
import com.parse.ParseObject
import com.parse.ktx.getIntOrNull
import org.json.JSONObject

@ParseClassName(ParseService.NAME)
class ParseService constructor(): ParseObject() {

	companion object {
		const val NAME = "Service"
		const val TAG = "ParseService"

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

	/**
	 * Synchronously retrieves a Set of ParseServices that are in the same category as this ParseService.
	 * Note: This Set WILL include the object in question.
	 */
	val servicesSet: Set<ParseService>
		get() = getRelation<ParseService>(SERVICES_REL).query.find().toSet()

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
}