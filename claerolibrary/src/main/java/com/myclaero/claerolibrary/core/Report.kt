package com.myclaero.claerolibrary.core

import android.icu.util.Calendar
import com.myclaero.claerolibrary.core.component.*
import com.myclaero.claerolibrary.extensions.toCalendar
import com.parse.ParseClassName
import com.parse.ParseObject
import com.parse.ktx.putOrRemove

@ParseClassName(Report.NAME)
class Report: ParseObject() {

	companion object {
		const val NAME = "Report"

		const val DATE = "serviceDate"

		const val NOTES_ARRAY = "notes"
		const val TRANSMISSION_OBJ = "transmission"
		const val BATTERIES_ARRAY = "batteries"
		const val STEERING_OBJ = "steering"
		const val WIPERS_ARRAY = "wipers"
		const val BRAKE_FLUID_OBJ = "brakeFluid"
		const val ENGINE_OIL_OBJ = "engineOil"
		const val GEARBOXES_ARRAY = "gearboxes"
		const val ENGINE_AIR_OBJ = "engineAir"
		const val CABIN_AIR_OBJ = "cabinAir"
		const val COOLANT_OBJ = "coolant"

	}

	var serviceTime: Calendar?
		get() = getDate(DATE)?.toCalendar()
		set(value) { putOrRemove(DATE, value?.time) }

	var notes: List<String>
		get() = getList(NOTES_ARRAY) ?: listOf()
		set(value) = put(NOTES_ARRAY, value)

	var transmission: Transmission
		get() = getJSONObject(TRANSMISSION_OBJ) as Transmission
		set(value) = put(TRANSMISSION_OBJ, value)

	var engineAir: EngineAir
		get() = getJSONObject(ENGINE_AIR_OBJ) as EngineAir
		set(value) = put(ENGINE_AIR_OBJ, value)

	var batteries: List<Battery>
		get()= getList(BATTERIES_ARRAY) ?: listOf()
		set(value) = put(BATTERIES_ARRAY, value)

	var steering: Steering
		get() = getJSONObject(STEERING_OBJ) as Steering
		set(value) = put(STEERING_OBJ, value)

	var wipers: List<Wiper>
		get() = getList(WIPERS_ARRAY) ?: listOf()
		set(value) = put(WIPERS_ARRAY, value)

	var brakeFluid: BrakeFluid
		get() = getJSONObject(BRAKE_FLUID_OBJ) as BrakeFluid
		set(value) = put(BRAKE_FLUID_OBJ, value)

	var engineOil: EngineOil
		get() = getJSONObject(ENGINE_OIL_OBJ) as EngineOil
		set(value) = put(ENGINE_OIL_OBJ, value)

	var gearboxes: List<Gearbox>
		get() = getList(GEARBOXES_ARRAY) ?: listOf()
		set(value) = put(GEARBOXES_ARRAY, value)

	var cabinAir: CabinAir
		get() = getJSONObject(CABIN_AIR_OBJ) as CabinAir
		set(value) = put(CABIN_AIR_OBJ, value)

	var coolant: Coolant
		get() = getJSONObject(COOLANT_OBJ) as Coolant
		set(value) = put(COOLANT_OBJ, value)

}