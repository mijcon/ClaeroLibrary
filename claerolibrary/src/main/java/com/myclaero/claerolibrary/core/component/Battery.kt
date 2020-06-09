package com.myclaero.claerolibrary.core.component

class Battery: Serviceable() {

	companion object {
		const val TYPE = "type"
		const val CCA_NOMINAL = "ccaNominal"
		const val CCA_TESTED = "ccaTested"
	}

	enum class Type {
		SLI,    // Starting, lights, ignition battery
		EVB     // Electric Vehicle battery
	}

	var type: Type
		get() = Type.valueOf(optString(TYPE))
		set(value) { put(TYPE, value.name) }

	var ampsRated: Int
		get() = optInt(CCA_NOMINAL)
		set(value) { put(CCA_NOMINAL, value) }

	var ampsTested: Int
		get() = optInt(CCA_TESTED)
		set(value) { put(CCA_TESTED, value) }

}