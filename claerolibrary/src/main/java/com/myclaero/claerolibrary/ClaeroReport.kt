package com.myclaero.claerolibrary

import android.util.Log
import com.myclaero.claerolibrary.extensions.toList
import org.json.JSONArray
import org.json.JSONObject

data class ClaeroReport(val invoice: ParseInvoice, val json: JSONObject) {

    companion object {
        const val TAG = "ClaeroReport"

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

        const val REMINDER_NUM = "status"
        const val STATE_NUM = "state"
        const val TYPE = "type"

        const val TITLE_OIL = "Engine Oil"
        const val TITLE_OIL_PLUG = "Oil Plug"
        const val TITLE_AIR_CABIN = "Cabin Air Filter"
        const val TITLE_AIR_ENGINE = "Engine Air Filter"
        const val TITLE_BATTERY = "Battery"
        const val TITLE_BRAKE = "Brake Fluid"
        const val TITLE_BELTS = "Drive Belt(s)"
        const val TITLE_COOLANT = "Coolant/Anti-Freeze"
        const val TITLE_DIFF_FRONT = "Front Differential"
        const val TITLE_DIFF_REAR = "Rear Differential"
        const val TITLE_GUARD = "Plates & Guards"
        const val TITLE_LIGHTS = "Exterior Lights"
        const val TITLE_REMINDER = "Maintenance Reminder"
        const val TITLE_STEERING = "Power Steering"
        const val TITLE_TCASE = "Transfer Case"
        const val TITLE_TIRES = "Tires"
        const val TITLE_TRANS = "Transmission"
        const val TITLE_WIPERS = "Wipers"

        enum class PartCondition(val string: String) {
            NA("N/A"),
            GOOD("Clean"),
            FAIR("Good"),
            WORN("Wearing"),
            DUE("Worn/Due"),
            LATE("Overdue"),
            RISK("Dangerous")
        }

        enum class FluidMeasure(val string: String) {
            NA("N/A"),
            FULL("At Full Mark"),
            Q1("1/4 qt Low"),
            Q2("1/2 qt Low"),
            Q3("3/4 qt Low"),
            Q4("At Low Mark"),
            LOW("Below Low Mark"),
            GONE("Not on Stick"),
            OVER("Above Full Mark")
        }

        enum class PartState { FUNCTIONING, NON_FUNCTIONING, NA }
        enum class ReminderState { RESET, NOT_RESET, NA }
    }

    init {
        listOf(
            OIL,
            PLUG,
            FLUID_BRAKE,
            FLUID_STEERING,
            FLUID_COOLANT,
            FLUID_TRANS,
            FLUID_DIFF_FRONT,
            FLUID_DIFF_REAR,
            FLUID_TCASE,
            LIGHTS,
            TIRES,
            BATTERY,
            FILTER_ENG,
            FILTER_CABIN,
            REMINDER,
            WIPERS,
            BELTS,
            GUARDS
        ).forEach {
            if (!json.has(it)) json.put(it, JSONObject())
        }

        listOf(
            TIRES,
            MISC
        ).forEach {
            if (!json.has(it)) json.put(it, JSONArray())
        }
    }

    val fluidOil = Fluid(OIL, TITLE_OIL)

    val plugOil = Part(PLUG, TITLE_OIL_PLUG)

    val fluidBrake = Fluid(FLUID_BRAKE, TITLE_BRAKE)

    val fluidSteering: Fluid = Fluid(FLUID_STEERING, TITLE_STEERING)

    val fluidCoolant: Fluid = Fluid(FLUID_COOLANT, TITLE_COOLANT)

    inner class Transmission(
        override val key: String
    ) : Fluid(FLUID_TRANS, TITLE_TRANS) {
        override val report = this@ClaeroReport

        var type: ParseVehicle.Transmission?
            get() {
                val obj = report.json.getJSONObject(key)
                val num = if (obj.has(TYPE)) obj.getInt(TYPE) else null
                return when (num) {
                    0 -> ParseVehicle.Transmission.NA
                    1 -> ParseVehicle.Transmission.AUTO
                    2 -> ParseVehicle.Transmission.MANUAL
                    3 -> ParseVehicle.Transmission.CVT
                    else -> null
                }
            }
            set(value) {
                val i = when (value) {
                    ParseVehicle.Transmission.NA -> 0
                    ParseVehicle.Transmission.AUTO -> 1
                    ParseVehicle.Transmission.MANUAL -> 2
                    ParseVehicle.Transmission.CVT -> 3
                    else -> null
                }
                val obj = report.json.getJSONObject(key)
                obj.putOpt(TYPE, i)
                report.json.put(key, obj)

                Log.i(TAG, "Transmission.set($value): ${report.json}")
            }

    }

    val fluidTransmission: Transmission = Transmission(FLUID_TRANS)

    val fluidDifferentialFront: Fluid = Fluid(FLUID_DIFF_FRONT, TITLE_DIFF_FRONT)

    val fluidDifferentialRear: Fluid = Fluid(FLUID_DIFF_REAR, TITLE_DIFF_REAR)

    val fluidTransferCase: Fluid = Fluid(FLUID_TCASE, TITLE_TCASE)

    open inner class Tires : Titled, Notable {
        override val key = TIRES
        override val title = TITLE_TIRES
        override val invoice = this@ClaeroReport.invoice
        override val report = this@ClaeroReport

        fun getTread(index: Int): String? {
            if (!report.json.has(key)) return null
            val i = report.json.getJSONArray(key).optInt(index)
            if (i == -1) return null
            return "$i/32nd\""
        }

        fun setTread(index: Int, value: Int?) {
            val array =
                if (report.json.has(key)) report.json.getJSONArray(key)
                else JSONArray(listOf(-1, -1, -1, -1, -1))
            array.put(index, value ?: -1)
            report.json.put(key, array)

            Log.i(TAG, "Tires.setTread($index, $value): ${report.json}")
        }

    }

    val tires = Tires()

    val battery = Battery()

    val filterAirEngine: Part = Part(FILTER_ENG, TITLE_AIR_ENGINE)

    val filterAirCabin: Part = Part(FILTER_CABIN, TITLE_AIR_CABIN)

    open inner class Lights : Titled, Notable {
        override val key = LIGHTS
        override val title = TITLE_LIGHTS
        override val invoice = this@ClaeroReport.invoice
        override val report = this@ClaeroReport

        /**
         * @return Whether the light represented by the given key is non-functional
         * Example 1: getCondition("CHMSL") -> false == Center High Mount Stop Light is GOOD
         * Example 2: getCondition("LP1") -> true == License Plate light #1 is OUT
         */
        fun getCondition(light: String): PartState {
            if (!report.json.has(key)) return PartState.NA
            val i =
                if (report.json.getJSONObject(key).has(light)) report.json.getJSONObject(key).getBoolean(light)
                else null
            return when (i) {
                true -> PartState.NON_FUNCTIONING
                false -> PartState.FUNCTIONING
                else -> PartState.NA
            }
        }

        /**
         * @param light is the name of the Light in question
         * @param value is the state of the light (true == NON-FUNCTIONAL, false == FUNCTIONAL)
         */
        fun setCondition(light: String, value: PartState) {
            val map =
                if (report.json.has(key)) report.json.getJSONObject(key)
                else JSONObject()
            when (value) {
                PartState.NON_FUNCTIONING -> map.put(light, true)
                PartState.FUNCTIONING -> map.put(light, false)
                PartState.NA -> map.remove(light)
            }
            report.json.put(key, map)
            invoice.put(ParseInvoice.REPORT_JSON, report.json)

            Log.i(TAG, "Lights.setCondition(${value.name}): ${report.json}")
        }

    }

    val lights = Lights()

    open inner class PartSet(
        override val key: String,
        override val title: String
    ) : Titled, Notable {
        override val invoice = this@ClaeroReport.invoice
        override val report = this@ClaeroReport

        fun getCondition(name: String): PartCondition {
            if (!report.json.has(key)) return PartCondition.NA
            val i = report.json.getJSONObject(key).optInt(name, -1)
            return when (i) {
                0 -> PartCondition.GOOD
                1 -> PartCondition.FAIR
                2 -> PartCondition.WORN
                3 -> PartCondition.DUE
                4 -> PartCondition.LATE
                5 -> PartCondition.RISK
                else -> PartCondition.NA
            }
        }

        fun setCondition(name: String, value: PartCondition?) {
            val map =
                if (report.json.has(key)) report.json.getJSONObject(key)
                else JSONObject()
            val i = when (value) {
                null -> null
                PartCondition.GOOD -> 0
                PartCondition.FAIR -> 1
                PartCondition.WORN -> 2
                PartCondition.DUE -> 3
                PartCondition.LATE -> 4
                PartCondition.RISK -> 5
                else -> -1
            }
            map.put(name, i)
            report.json.put(key, map)
            invoice.put(ParseInvoice.REPORT_JSON, report.json)

            Log.i(TAG, "PartSet.setCondition($name, ${value?.name}): ${report.json}")
        }

    }

    val wipers: PartSet = PartSet(WIPERS, TITLE_WIPERS)

    val belts: PartSet = PartSet(BELTS, TITLE_BELTS)

    val reminder: Reminder = Reminder(REMINDER)

    val guards: Guard = Guard(GUARDS)

    val misc: MutableList<String> =
        if (json.has(MISC)) json.getJSONArray(MISC).toList<String>().toMutableList()
        else mutableListOf()

    open inner class Fluid(
        override val key: String,
        override val title: String
    ) : Titled, Measurable, Decayable, Notable {
        override val invoice = this@ClaeroReport.invoice
        override val report = this@ClaeroReport
        override fun toString() = value?.string ?: "None"
    }

    open inner class Battery: Titled, Countable, Notable {
        override val key = BATTERY
        override val title = TITLE_BATTERY
        override val invoice = this@ClaeroReport.invoice
        override val report = this@ClaeroReport

        override fun toString(): String {
            return value.toString() + " CCAs"
        }
    }

    open inner class Guard(
        override val key: String
    ) : Titled, Countable, Notable {
        override val report = this@ClaeroReport
        override val invoice = this@ClaeroReport.invoice
        override val title = TITLE_GUARD

        override fun toString(): String {
            return value.toString() + " clip(s)"
        }
    }

    open inner class Part(
        override val key: String,
        override val title: String
    ) : Titled, Decayable, Notable {
        override val invoice = this@ClaeroReport.invoice
        override val report = this@ClaeroReport
    }

    open inner class Reminder(
        override val key: String
    ) : Titled, Notable {
        override val invoice = this@ClaeroReport.invoice
        override val report = this@ClaeroReport
        override val title = TITLE_REMINDER

        var reset: ReminderState?
            get() {
                val obj = report.json.getJSONObject(key)
                val num = if (obj.has(REMINDER_NUM)) obj.getInt(REMINDER_NUM) else null
                return when (num) {
                    null -> null
                    0 -> ReminderState.RESET
                    1 -> ReminderState.NOT_RESET
                    else -> ReminderState.NA
                }
            }
            set(value) {
                val num = when (value) {
                    null -> null
                    ReminderState.RESET -> 0
                    ReminderState.NOT_RESET -> 1
                    else -> -1
                }
                val obj = report.json.getJSONObject(key)
                obj.putOpt(REMINDER_NUM, num)
                report.json.put(key, obj)
                invoice.put(ParseInvoice.REPORT_JSON, report.json)

                Log.i(TAG, "Reminder.set($value): ${report.json}")
            }

    }

    interface Measurable {
        companion object {
            const val VALUE_NUM = "deviation"
        }
        val invoice: ParseInvoice
        val report: ClaeroReport
        val key: String

        /**
         * An integer representation of this consumable.
         *
         * For fluids, measured in 1/4 qt deviation from FULL mark in inverted sign, up to 1 qt either direction.
         * Example 1: a value of 3 represents 0.75 qts BELOW the full mark
         * Example 2: a value of 5 represents MORE THAN 1.0 qts BELOW the full mark
         * Example 3: a value of 6 represents an oil level that is not measurable (not on stick)
         * Example 4: a value of -2 represents 0.50 qts ABOVE the full mark
         */
        var value: FluidMeasure?
            get() {
                val obj = report.json.getJSONObject(key)
                val num = if (obj.has(VALUE_NUM)) obj.getInt(VALUE_NUM) else null
                return when (num) {
                    0 -> FluidMeasure.NA
                    1 -> FluidMeasure.OVER
                    2 -> FluidMeasure.FULL
                    3 -> FluidMeasure.Q1
                    4 -> FluidMeasure.Q2
                    5 -> FluidMeasure.Q3
                    6 -> FluidMeasure.Q4
                    7 -> FluidMeasure.LOW
                    8 -> FluidMeasure.GONE
                    else -> null
                }
            }
            set(value) {
                val obj = report.json.getJSONObject(key)
                obj.putOpt(VALUE_NUM, value?.ordinal)
                report.json.put(key, obj)
                invoice.put(ParseInvoice.REPORT_JSON, report.json)

                Log.i(TAG, "Measurable.set($value): ${report.json}")
            }
    }

    interface Countable {
        companion object {
            const val VALUE_NUM = "deviation"
        }
        val invoice: ParseInvoice
        val report: ClaeroReport
        val key: String

        /**
         * An integer representation of this consumable.
         *
         * For solids, measured in their standard units in standard (non-inverted) sign.
         * Example 1: a value of 7 for a Tire represents 7/32nds of an inch of tread remaining.
         * Example 2: a value of -2 for a skid plate/splash guard represents 2 bolts missing from plate.
         */
        var value: Int?
            get() {
                val obj = report.json.getJSONObject(key)
                return if (obj.has(VALUE_NUM)) obj.getInt(VALUE_NUM) else null
            }
            set(value) {
                val obj = report.json.getJSONObject(key)
                obj.putOpt(VALUE_NUM, value)
                report.json.put(key, obj)
                invoice.put(ParseInvoice.REPORT_JSON, report.json)

                Log.i(TAG, "Countable.set($value): ${report.json}")
            }

    }

    interface Notable {
        companion object {
            const val NOTE_STR = "note"
        }
        val invoice: ParseInvoice
        val report: ClaeroReport
        val key: String

        var note: String?
            get() {
                val obj = report.json.getJSONObject(key)
                return if (obj.has(NOTE_STR)) obj.getString(NOTE_STR) else null
            }
            set(value) {
                val obj = report.json.getJSONObject(key)
                obj.putOpt(NOTE_STR, value)
                report.json.put(key, obj)
                invoice.put(ParseInvoice.REPORT_JSON, report.json)

                Log.i(TAG, "Notable.set($value): ${report.json}")
            }
    }

    interface Decayable {
        companion object {
            const val CONDITION_NUM = "condition"
        }

        val invoice: ParseInvoice
        val report: ClaeroReport
        val key: String

        /**
         * The technician's subjective opinion as to the condition of the respective part or fluid.
         */
        var condition: PartCondition?
            get() {
                val obj = report.json.getJSONObject(key)
                val num = if (obj.has(CONDITION_NUM)) obj.getInt(CONDITION_NUM) else null
                return when (num) {
                    null -> null
                    0 -> PartCondition.GOOD
                    1 -> PartCondition.FAIR
                    2 -> PartCondition.WORN
                    3 -> PartCondition.DUE
                    4 -> PartCondition.LATE
                    5 -> PartCondition.RISK
                    else -> PartCondition.NA
                }
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
                val obj = report.json.getJSONObject(key)
                obj.putOpt(CONDITION_NUM, num)
                report.json.put(key, obj)
                invoice.put(ParseInvoice.REPORT_JSON, report.json)

                Log.i(TAG, "Decayable.set($value): ${report.json}")
            }
    }

    interface Titled {
        val title: String
    }

}