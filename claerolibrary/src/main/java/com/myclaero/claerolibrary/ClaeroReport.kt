package com.myclaero.claerolibrary

import com.myclaero.claerolibrary.extensions.toList
import org.json.JSONObject

data class ClaeroReport(val report: JSONObject) {

    companion object {
        // The Report Keys
        const val OIL = "oil"
        const val PLUG = "oilPlug"
        const val FLUID_BRAKE = "fluidBrake"
        const val FLUID_STEERING = "fluidSteering"
        const val FLUID_COOLANT = "fluidCoolant"
        const val FLUID_TRANS = "fluidTransmission"
        const val FLUID_DIFF_FRONT = "fluidDifferentialFront"
        const val FLUID_DIFF_REAR = "fluidDifferentialRear"
        const val FLUID_TCASE = "fluidTransferCase"
        const val TIRES = "tires"
        const val BATTERY = "battery"
        const val FILTER_ENG = "filterEngine"
        const val FILTER_CABIN = "filterCabin"
        const val LIGHTS = "light"
        const val WIPERS = "wiper"
        const val BELTS = "driveBelt"
        const val REMINDER = "reminder"
        const val GUARDS = "guard"
        const val MISC = "misc"

        enum class PartCondition { GOOD, FAIR, WORN, DUE, LATE, RISK, NA }
        enum class PartState { FUNCTIONING, NON_FUNCTIONING, NA }
        enum class ReminderState { RESET, NOT_RESET, NA }
    }

    val fluidOil: Fluid =
        if (report.has(OIL)) Fluid(report.getJSONObject(OIL))
        else Fluid()

    val plugOil: Part =
        if (report.has(PLUG)) Part(report.getJSONObject(PLUG))
        else Part()

    val fluidBrake: Fluid =
        if (report.has(FLUID_BRAKE)) Fluid(report.getJSONObject(FLUID_BRAKE))
        else Fluid()

    val fluidSteering: Fluid =
        if (report.has(FLUID_STEERING)) Fluid(report.getJSONObject(FLUID_STEERING))
        else Fluid()

    val fluidCoolant: Fluid =
        if (report.has(FLUID_COOLANT)) Fluid(report.getJSONObject(FLUID_COOLANT))
        else Fluid()

    val fluidTransmission: Fluid =
        if (report.has(FLUID_TRANS)) Fluid(report.getJSONObject(FLUID_TRANS))
        else Fluid()

    val fluidDifferentialFront: Fluid =
        if (report.has(FLUID_DIFF_FRONT)) Fluid(report.getJSONObject(FLUID_DIFF_FRONT))
        else Fluid()

    val fluidDifferentialRear: Fluid =
        if (report.has(FLUID_DIFF_REAR)) Fluid(report.getJSONObject(FLUID_DIFF_REAR))
        else Fluid()

    val fluidTransferCase: Fluid =
        if (report.has(FLUID_TCASE)) Fluid(report.getJSONObject(FLUID_TCASE))
        else Fluid()

    val tires: List<Tire> =
        if (report.has(TIRES)) report.getJSONArray(TIRES).toList()
        else List(5) { Tire() }

    val battery: Battery =
        if (report.has(BATTERY)) Battery(report.getJSONObject(BATTERY))
        else Battery()

    val filterAirEngine: Part =
        if (report.has(FILTER_ENG)) Part(report.getJSONObject(FILTER_ENG))
        else Part()

    val filterAirCabin: Part =
        if (report.has(FILTER_CABIN)) Part(report.getJSONObject(FILTER_CABIN))
        else Part()

    val lights: MutableMap<String, Light> =
        mutableMapOf<String, Light>().apply {
            if (report.has(LIGHTS)) {
                val jsonLights = report.getJSONObject(LIGHTS)
                jsonLights.keys().forEach {
                    this[it] = Light(jsonLights.getJSONObject(it))
                }
            }
        }

    val wipers: MutableMap<String, Part> =
        mutableMapOf<String, Part>().apply {
            if (report.has(WIPERS)) {
                val jsonWiper = report.getJSONObject(WIPERS)
                jsonWiper.keys().forEach {
                    this[it] = Part(jsonWiper.getJSONObject(it))
                }
            }
        }

    val belts: MutableMap<String, Part> =
        mutableMapOf<String, Part>().apply {
            if (report.has(BELTS)) {
                val jsonBelts = report.getJSONObject(BELTS)
                jsonBelts.keys().forEach {
                    this[it] = Part(jsonBelts.getJSONObject(it))
                }
            }
        }

    val reminder: Reminder =
        if (report.has(REMINDER)) Reminder(report.getJSONObject(REMINDER))
        else Reminder()

    val guards: Guard =
        if (report.has(GUARDS)) Guard(report.getJSONObject(GUARDS))
        else Guard()

    val misc: MutableList<String> =
        if (report.has(MISC)) report.getJSONArray(MISC).toList<String>().toMutableList()
        else mutableListOf()

    data class Fluid(override val json: JSONObject = JSONObject()) : Measurable, Decayable, Notable

    data class Tire(override val json: JSONObject = JSONObject()) : Measurable, Notable

    data class Battery(override val json: JSONObject = JSONObject()) : Measurable, Notable

    data class Guard(override val json: JSONObject = JSONObject()) : Measurable, Notable

    data class Part(override val json: JSONObject = JSONObject()) : Decayable, Notable

    data class Light(override val json: JSONObject = JSONObject()) : Notable {
        companion object {
            const val STATE_NUM = "state"
        }

        var state: PartState?
            get() = when (if (json.has(STATE_NUM)) json.getInt(STATE_NUM) else null) {
                null -> null
                0 -> PartState.FUNCTIONING
                1 -> PartState.NON_FUNCTIONING
                else -> PartState.NA
            }
            set(value) {
                val num = when (value) {
                    null -> null
                    PartState.FUNCTIONING -> 0
                    PartState.NON_FUNCTIONING -> 1
                    else -> -1
                }
                json.put(STATE_NUM, num)
            }

    }

    data class Reminder(override val json: JSONObject = JSONObject()) : Notable {
        companion object {
            const val REMINDER_NUM = "status"
        }

        var reset: ReminderState?
            get() = when (if (json.has(REMINDER_NUM)) json.getInt(REMINDER_NUM) else null) {
                null -> null
                0 -> ReminderState.RESET
                1 -> ReminderState.NOT_RESET
                else -> ReminderState.NA
            }
            set(value) {
                val num = when (value) {
                    null -> null
                    ReminderState.RESET -> 0
                    ReminderState.NOT_RESET -> 1
                    else -> -1
                }
                json.put(REMINDER_NUM, num)
            }

    }

    private interface Measurable {
        companion object {
            const val VALUE_NUM = "deviation"
        }

        val json: JSONObject

        /**
         * An integer representation of this consumable.
         *
         * For fluids, measured in 1/4 qt deviation from FULL mark in inverted sign.
         * Example 1: a value of 5 represents 1.25 qts BELOW the full mark
         * Example 2: a value of -2 represents 0.50 qts ABOVE the full mark
         *
         * For solids, measured in their standard units in standard (non-inverted) sign.
         * Example 1: a value of 7 for a Tire represents 7/32nds of an inch of tread remaining.
         * Example 2: a value of -2 for a skid plate/splash guard represents 2 bolts missing from plate.
         */
        var value: Int?
            get() = if (json.has(VALUE_NUM)) json.getInt(VALUE_NUM) else null
            set(value) {
                json.put(VALUE_NUM, value)
            }
    }

    private interface Notable {
        companion object {
            const val NOTE_STR = "note"
        }

        val json: JSONObject
        var note: String?
            get() = if (json.has(NOTE_STR)) json.getString(NOTE_STR) else null
            set(value) {
                json.put(NOTE_STR, value)
            }
    }

    private interface Decayable {
        companion object {
            const val CONDITION_NUM = "condition"
        }

        val json: JSONObject

        /**
         * The technician's subjective opinion as to the condition of the respective part or fluid.
         */
        var condition: PartCondition?
            get() = when (if (json.has(CONDITION_NUM)) json.getInt(CONDITION_NUM) else null) {
                null -> null
                0 -> PartCondition.GOOD
                1 -> PartCondition.FAIR
                2 -> PartCondition.WORN
                3 -> PartCondition.DUE
                4 -> PartCondition.LATE
                5 -> PartCondition.RISK
                else -> PartCondition.NA
            }
            set(value) {
                val num = when (value) {
                    null -> null
                    PartCondition.GOOD -> 0
                    PartCondition.FAIR -> 1
                    PartCondition.WORN -> 2
                    PartCondition.DUE -> 3
                    PartCondition.LATE -> 4
                    PartCondition.RISK -> 5
                    else -> -1
                }
                json.put(CONDITION_NUM, num)
            }
    }

}