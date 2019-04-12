package com.myclaero.claerolibrary

import com.parse.ParseClassName
import com.parse.ParseObject

@ParseClassName(ParseInventory.NAME)
class ParseInventory constructor() : ParseObject() {

	companion object {
		const val NAME = "Inventory"
		const val TAG = "ParseInventory"

		// The Parse Server's key for each field.
		// Each is named "KEY_TYPE" so it's always clear what data-type to expect.
		const val SKU_STR = "sku"
		const val ACTIVE_BOOL = "active"
		const val ONHAND_INT = "quantity"
		const val COST_FLOAT = "unitCost"
		const val PRICE_INT = "unitPrice"
		const val UNIT_STR = "unit"
		const val PRECEDES_POINT = "precedes"
		const val SUPERSEDES_POINT = "supersedes"
		const val TITLE_STR = "titleYmmt"
		const val VENDOR_STR = "vendor"
		const val BRAND_STR = "brand"
		const val VENDOR_SKU_STR = "vendorSku"
	}

	val title: String
		get() = getString(TITLE_STR)!!

	val isActive: Boolean
		get() = getBoolean(ACTIVE_BOOL)

	val sku: String
		get() = getString(SKU_STR)!!

	val priceInt: Int
		get() = getInt(PRICE_INT)

	val priceFloat: Float
		get() = priceInt.toFloat() / 100

	val costFloat: Float
		get() = getDouble(COST_FLOAT).toFloat()

	val onHand: Int
		get() = getInt(ONHAND_INT)

	val precedingItem: ParseInventory?
		get() = getParseObject(PRECEDES_POINT) as ParseInventory?

	val supersedingItem: ParseInventory?
		get() = getParseObject(SUPERSEDES_POINT) as ParseInventory?

	val vendor: String?
		get() = getString(VENDOR_STR)

	val vendorSku: String?
		get() = getString(VENDOR_SKU_STR)

	val brand: String
		get() = getString(BRAND_STR)!!

	val unit: String?
		get() {
			val string = getString(UNIT_STR)
			return if (string.isNullOrBlank()) null else string
		}

}