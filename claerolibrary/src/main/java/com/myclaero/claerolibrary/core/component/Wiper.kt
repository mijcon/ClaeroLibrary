package com.myclaero.claerolibrary.core.component

import com.myclaero.claerolibrary.R

class Wiper: Serviceable() {

	companion object {
		const val POSITION = "position"
	}

	enum class Position(stringId: Int) {
		Driver(R.string.wiper_driver),
		Passenger(R.string.wiper_passenger),
		Rear(R.string.wiper_rear)
	}

	var position: Position
		get() = Position.valueOf(optString(POSITION))
		set(value) { put(POSITION, value.name) }

}