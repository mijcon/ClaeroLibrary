package com.myclaero.claerolibrary.core.component

import com.myclaero.claerolibrary.R

class Steering: FluidSystem() {

	companion object {
		const val TYPE = "type"
	}

	enum class Type(stringId: Int) {
		Hydraulic(R.string.steering_hydraulic),
		Electronic(R.string.steering_electronic),
		Manual(R.string.steering_manual)
	}

	var type: Type
		get() = Type.valueOf(optString(TYPE))
		set(value) { put(TYPE, value.name) }

}