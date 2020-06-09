package com.myclaero.claerolibrary.core.component

import com.myclaero.claerolibrary.ParseInventory
import com.parse.ParseDecoder
import com.parse.ParseObject

abstract class FluidSystem: Serviceable() {

	companion object {
		const val FLUID_PART = "fluidPart"
		const val FLUID_SPEC = "fluidSpec"
		const val FLUID_LEVEL = "fluidLevel"
		const val FLUID_COND = "fluidCond"
	}

	enum class FluidCondition {
		Good,
		Moderate,
		Fair,
		Poor
	}

	var fluidPart: ParseInventory?
		get() = optJSONObject(FLUID_PART)?.let {
			ParseObject.fromJSON(it, ParseInventory.NAME, ParseDecoder.get())
		}
		set(value) {
			put(FLUID_PART, value)
		}

	var fluidSpecification: String
		get() = optString(FLUID_SPEC)
		set(value) {
			put(FLUID_SPEC, value)
		}

	var fluidLevel: Double
		get() = optDouble(FLUID_LEVEL)
		set(value) {
			put(FLUID_LEVEL, value)
		}

	var fluidCondition: FluidCondition
		get() = FluidCondition.valueOf(optString(FLUID_COND))
		set(value) {
			put(FLUID_COND, value.name)
		}

}