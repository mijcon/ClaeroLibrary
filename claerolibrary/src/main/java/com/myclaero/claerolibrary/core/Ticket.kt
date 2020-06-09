package com.myclaero.claerolibrary.core

import android.annotation.SuppressLint
import android.content.Context
import android.icu.util.Calendar
import com.google.android.gms.location.LocationServices
import com.myclaero.claerolibrary.ClaeroAPI
import com.myclaero.claerolibrary.extensions.getSet
import com.parse.*
import com.parse.ktx.getAs
import com.parse.ktx.getAsOrNull
import com.parse.ktx.putOrIgnore
import com.parse.ktx.putOrRemove
import kotlinx.coroutines.*
import org.json.JSONArray

/**
 * A subclass of ParseObject representing a service ticket for a User's [Vehicle].
 *
 * @constructor The default constructor as required by Parse to correctly subclass the object. Do
 * not use this constructor directly through the app.
 */
@ParseClassName(Ticket.NAME)
class Ticket constructor(): ParseObject() {

	companion object {
		const val NAME = "Ticket"
		const val TAG = "ParseTicket"

		// The Parse Server's key for each field.
		// Each is named "KEY_TYPE" so it's always clear what data-type to expect.
		const val STATUS_INT = "status"
		const val VEHICLE_POINT = "vehicle"
		const val SERVICES_ARRAY = "services"
		const val PICKUP_POINT = "locationPickup"
		const val DROPOFF_POINT = "locationDropoff"
		const val DURATION_INT = "duration"
		const val CHARGE_POINT = "charge"
		const val INVOICE_POINT = "invoice"
		const val START_DATE = "start"
		const val SHIFT_POINT = "shift"
		const val REPORT_POINT = "report"

		/**
		 * Synchronously fetches [Ticket]s for the given [Vehicle].
		 *
		 * @param vehicle The Vehicle for which we are searching Tickets.
		 * @param closed If true, includes Tickets with the Status of Closed. Defaults to "false".
		 * @return The List of Tickets.
		 */
		fun fetch(vehicle: Vehicle? = null, closed: Boolean = false): List<Ticket> {
			val q = ParseQuery(Ticket::class.java)
				.include(DROPOFF_POINT)
				.include(PICKUP_POINT)
				.include(SERVICES_ARRAY)
				.include(INVOICE_POINT)
				.orderByDescending(START_DATE)

			vehicle?.let { q.whereEqualTo(VEHICLE_POINT, it) }

			return q.find()
		}

		/**
		 * Asynchronously fetches [Ticket]s for the given [Vehicle], or all Tickets for the user
		 * if no Vehicle provided.
		 *
		 * @param vehicle The Vehicle for which we are searching Tickets.
		 * @param closed If true, includes Tickets with the Status of Closed. Defaults to "false".
		 * @return The Kotlin Coroutine [Deferred] task, supplying a [List] of [Ticket]s.
		 */
		fun fetchAsync(vehicle: Vehicle? = null, closed: Boolean = false): Deferred<List<Ticket>> =
			GlobalScope.async(Dispatchers.IO) {
				fetch(
					vehicle,
					closed
				)
			}

		/**
		 * Asynchronously fetches [Ticket]s for the given [Vehicle], or all Tickets if no Vehicle
		 * provided.
		 *
		 * @param vehicle The Vehicle for which we are searching Tickets.
		 * @param closed If true, includes Tickets with the Status of Closed. Defaults to "false".
		 * @param callback The lambda to invoke upon completion of the query.
		 */
		fun fetchAsync(
			vehicle: Vehicle? = null,
			closed: Boolean = false,
			callback: FindCallback<Ticket>
		) {
			GlobalScope.launch(Dispatchers.Main) {
				try {
					val results = withContext(Dispatchers.IO) {
						fetch(
							vehicle,
							closed
						)
					}
					callback.done(results, null)
				} catch (e: ParseException) {
					callback.done(listOf(), e)
				}
			}
		}

	}

	/**
	 * The constructor to be used in most cases by apps dependent on this package.
	 *
	 * @param vehicle The [Vehicle] being assigned to this [Ticket].
	 */
	constructor(vehicle: Vehicle): this() {
		this.vehicle = vehicle
		this.status = Status.NEW
	}

	/**
	 * Enumerable Class representing the submission Status of the [Ticket].
	 */
	enum class Status(val value: Int) {
		NEW(-1),
		DRAFT(0),
		OPEN(1),
		CANCELLED(2),
		CLOSED(3),
		PENDING(4)
	}

	/**
	 * Enumerable Class representing the Status of the Technician for the [Ticket].
	 */
	enum class TechnicianStatus(val value: Int) {
		DRIVING_PICKUP(1),
		ARRIVING_PICKUP(2),
		DRIVING_SHOP(3),
		ARRIVING_SHOP(4),
		DRIVING_DROPOFF(5),
		ARRIVING_DROPOFF(6),
		WORKING_START_ONSITE(7),
		WORKING_END_ONSITE(8)
	}

	val listeners: MutableSet<ChangeListener> = mutableSetOf()

	/**
	 * The [Vehicle] that this [Ticket] is for.
	 */
	var vehicle: Vehicle
		get() = getAs(VEHICLE_POINT)
		private set(value) = put(VEHICLE_POINT, value)

	/**
	 * A shortcut for retrieving the [ParseUser] who owns the [Vehicle] for this [Ticket].
	 */
	val owner: ParseUser
		get() = vehicle.owner

	/**
	 * The pick-up [Locus] set for this [Ticket], or null if not yet set.
	 */
	var pickup: Locus?
		get() = getAs(PICKUP_POINT)
		set(location) {
			putOrRemove(PICKUP_POINT, location)
			location.let {
				listeners.forEach { listener -> listener.onUpdateLocation(it, dropoff) }
			}
		}

	/**
	 * The drop-off [Locus] set for this [Ticket], or null if not yet set.
	 */
	var dropoff: Locus?
		get() = getAs(DROPOFF_POINT)
		set(location) {
			putOrRemove(DROPOFF_POINT, location)
			location.let {
				listeners.forEach { listener -> listener.onUpdateLocation(pickup, it) }
			}
		}

	/**
	 * The start time of the Ticket, as a [android.icu.util.Calendar], or null if not yet set.
	 *
	 * Interacts with the Server, which stores time in the [java.util.Date] format, and converts it
	 * to the Android Calendar for convenience. Also provides updates to any listeners.
	 */
	var time: Calendar?
		get() = Calendar.getInstance().apply { time = getDate(START_DATE) }
		set(calendar) {
			putOrRemove(START_DATE, calendar?.time)
			listeners.forEach { it.onUpdateTime(calendar) }
		}

	/**
	 * The duration of all services associated with this Ticket, as an [Int] measuring seconds.
	 */
	val duration: Int
		get() = services.sumBy { it.duration }

	/**
	 * The status of the [Ticket].
	 */
	var status: Status
		get() = getInt(STATUS_INT).let { i -> Status.values().first { v -> v.value == i } }
		private set(value) {
			val old = status
			put(STATUS_INT, value.value)
			listeners.forEach { it.onUpdateStatus(old, value) }
		}

	/**
	 * The [Shift] associated with this [Ticket].
	 */
	var shift: Shift?
		get() = getAs(SHIFT_POINT)
		private set(shift) = putOrRemove(SHIFT_POINT, shift)

	/**
	 * The [Invoice] for the given [Ticket].
	 */
	var invoice: Invoice?
		get() = getAs(INVOICE_POINT)
		private set(invoice) {
			putOrIgnore(INVOICE_POINT, invoice)
		}

	/**
	 * A [Set] of [Service]s associated with this [Ticket].
	 */
	var services: Set<Service>
		get() = getSet(SERVICES_ARRAY)
		set(value) {
			put(SERVICES_ARRAY, JSONArray(value))

			// Update any listeners
			listeners.forEach { it.onUpdateServices(duration, value) }
		}

	var report: Report?
		get() = getAsOrNull(REPORT_POINT)
		set(value) = putOrRemove(REPORT_POINT, value)

	/**
	 * Synchronously retrieves [Availability] for this Ticket.
	 */
	fun getOpenings() {

	}


	/**
	 * Asynchronously retrieves [Availability] for this Ticket.
	 *
	 * @param callback The lambda invoked upon completion of the query.
	 */
	fun getOpeningsAsync(callback: ((availability: Availability?, e: Exception?) -> Unit)) {
		GlobalScope.launch {
			try {
				val response = withContext(Dispatchers.IO) {
					ClaeroAPI.queryAvailability(this@Ticket)
				}
				callback(response, null)
			} catch (e: Exception) {
				callback(null, e)
			}
		}
	}

	/**
	 * Marks and *synchronously* saves the [Ticket] as [Status.OPEN], notifying any listeners in
	 * the process.
	 *
	 * @throws ParseException
	 */
	fun finalizeDraft() {
		val oldStatus = status
		status = Status.OPEN
		GlobalScope.launch(Dispatchers.Main) {
			listeners.forEach { it.onUpdateStatus(oldStatus,
				Status.OPEN
			) }
		}
		save()
	}

	/**
	 * Marks and asynchronously saves the [Ticket] as [Status.OPEN], notifying any listeners in
	 * the process.
	 *
	 * @param callback The lambda invoked upon completion or failure of the task.
	 */
	fun finalizeDraftAsync(callback: SaveCallback = SaveCallback {}) {
		GlobalScope.launch {
			try {
				withContext(Dispatchers.IO) { finalizeDraft() }
				callback.done(null)
			} catch (e: ParseException) {
				callback.done(e)
			}
		}
	}

	@SuppressLint("MissingPermission")
	fun notify(
		context: Context,
		stage: TechnicianStatus,
		callback: (success: Boolean, e: Exception?) -> Unit
	) {
		val locationClient = LocationServices.getFusedLocationProviderClient(context)
		locationClient.flushLocations().apply {
			addOnSuccessListener {
				locationClient.lastLocation.apply {
					addOnSuccessListener { technicianLocation ->
						technicianLocation?.let {
							ClaeroAPI.pushTicketStatus(
								this@Ticket,
								it,
								stage,
								callback
							)
						} ?: run {
							callback(false, java.lang.Exception("Unable to retrieve location."))
						}
					}
					addOnFailureListener {
						callback(false, it)
					}
				}
			}
			addOnFailureListener {
				callback(false, it)
			}
		}
	}

	val timeString: String?
		get() = ticketTime?.let { time?.getTimeString((it + 1) / 2) }

	interface ChangeListener {

		/**
		 * Provides the listener with updates to the start time of the [Ticket].
		 *
		 * @param newTime The [android.icu.util.Calendar] object representing the start time for
		 * the Ticket, or null if the time was cleared.
		 */
		fun onUpdateTime(newTime: Calendar?) = Unit

		/**
		 * Provides the listener with updates to the selection of [Service]s and the
		 * total time needed to perform those Services.
		 *
		 * @param duration The duration, in minutes, for all of the services combined.
		 * @param services The new Services being saved to the [Ticket].
		 */
		fun onUpdateServices(duration: Int, services: Set<Service>) = Unit

		/**
		 * Provides the listener with updates to the pick-up and drop-off locations for the
		 * [Vehicle].
		 *
		 * @param pickup The starting location for the Vehicle to be picked up at.
		 * @param dropoff The ending location for the Vehicle to be dropped off at.
		 */
		fun onUpdateLocation(pickup: Locus?, dropoff: Locus?) = Unit

		// fun onUpdatePayment() = Unit

		/**
		 * Provides the listener with updates to the [Status] of the [Ticket].
		 *
		 * @param prev The Status for the Ticket before the most recent update.
		 * @param new The newest Status for the Ticket, after update.
		 */
		fun onUpdateStatus(prev: Status, new: Status) = Unit

	}
}