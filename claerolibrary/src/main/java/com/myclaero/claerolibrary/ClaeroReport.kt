package com.myclaero.claerolibrary

/*
data class ClaeroReport(val json: JSONArray) {

    companion object {
	    const val TAG = "ClaeroReport"

	    // Type number 2
	    enum class Recommendation(val string: String) {

	    }

	    // Type number 3
	    enum class PartCondition(val stringRes: Int) {
		    NA(R.string.report_part_na),
		    GOOD(R.string.report_part_good),
		    FAIR(R.string.report_part_fair),
		    WORN(R.string.report_part_worn),
		    DUE(R.string.report_part_due),
		    LATE(R.string.report_part_late),
		    RISK(R.string.report_part_risk)
	    }

	    // Type number 4
	    enum class FluidMeasure(val stringRes: Int) {
		    NA(R.string.report_fluid_na),
		    FULL(R.string.report_fluid_full),
		    Q1(R.string.report_fluid_low1),
		    Q2(R.string.report_fluid_low2),
		    Q3(R.string.report_fluid_low3),
		    Q4(R.string.report_fluid_low4),
		    LOW(R.string.report_fluid_low5),
		    GONE(R.string.report_fluid_low6),
		    OVER(R.string.report_fluid_high)
	    }

	    enum class PartState(val stringRes: Int) {
		    NA(R.string.report_state_na),
		    FUNCTIONING(R.string.report_state_func),
		    NON_FUNCTIONING(R.string.report_state_nonf)
	    }
	    enum class ReminderState(val stringRes: Int) {
		    NA(R.string.report_reminder_na),
		    RESET(R.string.report_reminder_reset),
		    NOT_RESET(R.string.report_reminder_noreset)
	    }
    }

	val data: List<ReportItem>

	init {
		data = json.toList()
	}

	// This is not a memory-safe operation yet.
	private fun JSONArray.toList(): List<Any?> {
		val list = mutableListOf<Any?>()

		for (i in 0 until length()) {
			val value = get(i)
			val item = when (value) {
				is JSONObject -> value.toMap<JSONObject>()
				is JSONArray -> value.toList()
				else -> value
			}
			list.add(item)
		}
		return list.toList()
	}

	private fun <T> JSONObject.toMap(): MutableMap<String, T> {
		val map = mutableMapOf<String, Any?>()

		keys().forEach {
			val value = get(it)
			map[it] = when (value) {
				is JSONObject -> value.toMap<Any?>()
				is JSONArray -> value.toList()
				else -> value
			}
		}

		return map
	}

	data class ReportItem(val item: JSONObject) {

		val title = item.getString("title")
		val multiple = item.getBoolean("multiple")
		val units = item.getString("units")
		val data: List<>


	}

	fun build() {
		data.
	}



}*/