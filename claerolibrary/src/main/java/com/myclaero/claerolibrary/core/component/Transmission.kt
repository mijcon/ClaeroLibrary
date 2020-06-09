package com.myclaero.claerolibrary.core.component

import com.myclaero.claerolibrary.R

class Transmission: FluidSystem() {

	companion object {
		const val TYPE = "type"
	}

	enum class Type(stringId: Int) {
		Automatic(R.string.transmission_automatic),
		Manual(R.string.transmission_manual),
		CVT(R.string.transmission_cvt),
		Dual(R.string.transmission_dct),
		Direct(R.string.transmission_direct)
	}

	var type: Type
		get() = Type.valueOf(optString(TYPE))
		set(value) { put(TYPE, value.name) }

}