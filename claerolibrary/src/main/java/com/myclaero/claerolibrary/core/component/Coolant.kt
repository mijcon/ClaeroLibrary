package com.myclaero.claerolibrary.core.component

class Coolant: FluidSystem() {

	companion object {
		const val FREEZING = "freezePoint"
	}

	var freezingPoint: Int
		get() = optInt(FREEZING)
		set(value) { put(FREEZING, value) }

}