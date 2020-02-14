package com.myclaero.claerolibrary

import android.annotation.SuppressLint
import android.content.Context
import com.google.android.gms.location.LocationServices
import com.myclaero.claerolibrary.extensions.getTimeString
import com.myclaero.claerolibrary.extensions.upload
import com.myclaero.claerolibrary.extensions.versus
import com.parse.*
import com.parse.ktx.*
import kotlinx.coroutines.*
import org.jetbrains.anko.doAsync
import org.jetbrains.anko.uiThread
import java.util.*

@ParseClassName(Ticket.NAME)
class Ticket constructor(): ParseObject() {

	companion object {
		const val NAME = "Ticket"
		const val TAG = "ParseTicket"

		// The Parse Server's key for each field.
		// Each is named "KEY_TYPE" so it's always clear what data-type to expect.
		const val STATUS_INT = "status"
		const val VEHICLE_POINT = "vehicle"
		const val SERVICES_REL = "services"
		const val LOCATION_POINT = "location"
		const val USER_POINT = "owner"
		const val DURATION_INT = "duration"
		const val TRAVEL_INT = "travel"
		const val CHARGE_POINT = "charge"
		const val START_LONG = "start"
		const val SHIFT_POINT = "shift"

		/**
		 * Synchronously fetches [Ticket]s for the given [Vehicle].
		 *
		 * @param vehicle The Vehicle for which we are searching Tickets.
		 * @param closed If true, includes Tickets with the Status of Closed. Defaults to "false".
		 * @return The List of Tickets.
		 */
		fun getTickets(vehicle: Vehicle? = null, closed: Boolean = false): List<Ticket> =
			ParseQuery(Ticket::class.java).run {
				include(LOCATION_POINT)
				include(SERVICES_REL)
				vehicle?.let { whereEqualTo(VEHICLE_POINT, it) }
				orderByDescending(START_LONG)
				find()
			}

		/**
		 * Asynchronously fetches [Ticket]s for the given [Vehicle], or all Tickets if no Vehicle
		 * provided.
		 *
		 * @param vehicle The Vehicle for which we are searching Tickets.
		 * @param closed If true, includes Tickets with the Status of Closed. Defaults to "false".
		 * @param callback The lambda to invoke upon completion of the query.
		 */
		fun getTicketsAsync(
			vehicle: Vehicle? = null,
			closed: Boolean = false,
			callback: FindCallback<Ticket>
		) {
			GlobalScope.launch(Dispatchers.Main) {
				try {
					val results = withContext(Dispatchers.IO) { getTickets(vehicle, closed) }
					callback.done(results, null)
				} catch (e: ParseException) {
					callback.done(listOf(), e)
				}
			}
		}

	}

	enum class Status(val value: Int) {
		NEW(-1),
		DRAFT(0),
		OPEN(1),
		CANCELLED(2),
		CLOSED(3),
		PENDING(4)
	}

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

	enum class Field(val value: Int) {
		LOCATION(7),
		TIME(5),
		TIME_ESTIMATE(4),
		SERVICES(3),
		PAYMENT(9),
		STATUS(13)
	}

	val listeners: MutableSet<UpdateListener> = mutableSetOf()

	var vehicle: Vehicle?
		get() = getParseObject(VEHICLE_POINT) as Vehicle?
		set(value) = putOrRemove(VEHICLE_POINT, value)

	var owner: ParseUser?
		get() = getParseUser(USER_POINT)
		set(user) = putOrIgnore(USER_POINT, user)

	var location: ParseLocation?
		get() = getParseObject(LOCATION_POINT) as ParseLocation?
		set(location) = putOrIgnore(LOCATION_POINT, location)

	var serviceTime: Int?
		get() = getIntOrNull(DURATION_INT)
		set(value) {
			putOrIgnore(DURATION_INT, value)
			GlobalScope.launch(Dispatchers.Main) {
				listeners.forEach { it.onUpdate(Field.TIME_ESTIMATE) }
			}
		}

	var travelTime: Int?
		get() = getIntOrNull(TRAVEL_INT)
		set(value) = putOrIgnore(TRAVEL_INT, value)

	val ticketTime: Int?
		get() = if (serviceTime != null && travelTime != null) serviceTime!! + travelTime!! else null

	var status: Status
		set(value) = put(STATUS_INT, value.value)
		get() = when (getIntOrNull(STATUS_INT)) {
			4 -> Status.PENDING
			3 -> Status.CLOSED
			2 -> Status.CANCELLED
			1 -> Status.OPEN
			0 -> Status.DRAFT
			else -> Status.NEW
		}

	var shift: String?
		get() = getParseObject(SHIFT_POINT)?.objectId
		set(shiftId) = putOrRemove(SHIFT_POINT, createWithoutData(Shift::class.java, shiftId))

	fun setShift(shift: ClaeroShift) {
		this.shift = shift.objectId
	}

	var charge: ParseCharge?
		get() = getParseObject(CHARGE_POINT) as ParseCharge?
		set(value) = put(CHARGE_POINT, value!!)

	var time: Date?
		get() = getLongOrNull(START_LONG)?.let { Date(it * 1000) }
		set(value) = value?.let { put(START_LONG, value.time / 1000) } ?: Unit

	fun getInvoiceAsync(callback: GetCallback<ParseInvoice>) {
		ParseQuery.getQuery(ParseInvoice::class.java)
			.whereEqualTo(ParseInvoice.TICKET_POINT, this)
			.getFirstInBackground { invoice, e ->
				if (e.code == ParseException.OBJECT_NOT_FOUND) callback.done(null, null)
				else callback.done(invoice, e)
			}
	}

	var services: Set<Service>
		get() = getRelation<Service>(SERVICES_REL).query.find().toSet()
		set(value) {
			// Save new TimeEstimate to help our TimeListAdapter update quickly.
			val time = services.sumBy { it.duration }
			serviceTime = if (services.isEmpty()) null else time

			// Worry about saving the services themselves in the background.
			val relation = getRelation<Service>(SERVICES_REL)
			val oldServices = relation.query.findAll()

			oldServices.versus(value) { a, both, b ->
				// Delete all of the services previously on the Ticket (if any)
				a.forEach { relation.remove(it) }
				save()
				b.forEach { relation.add(it) }
				save()

				GlobalScope.launch(Dispatchers.Main) {
					listeners.forEach { it.onUpdate(Field.SERVICES) }
				}
			}
		}

	fun getServicesAsync(callback: (services: Set<Service>?, e: ParseException?) -> Unit) {
		if (this.status == Status.NEW) {
			callback(setOf(), null)
		} else {
			GlobalScope.launch {
				try {
					val set = withContext(Dispatchers.IO) { services }
					callback(set, null)
				} catch (e: ParseException) {
					callback(null, e)
				}
			}
		}
	}

	fun setServicesAsync(services: Set<Service>, callback: SaveCallback = SaveCallback {}) {
		GlobalScope.launch {
			try {
				withContext(Dispatchers.IO) { this@Ticket.services = services }
				callback.done(null)
			} catch (e: ParseException) {
				callback.done(e)
			}
		}
	}

	fun cancel(callback: ((succeeded: Boolean, error: ParseException?) -> Unit)? = null) {
		status = Status.CANCELLED
		saveInBackground {
			callback?.invoke(it == null, it)
		}
	}

	/**
	 * A special save() function used to trigger the UpdateListener.
	 */
	fun saveDraft(field: Field): Boolean {
		val reopen = status == Status.OPEN
		status = Status.DRAFT
		saveInBackground {
			// Switch back to an update-after-save model if there are errors...
			// listeners.forEach { it.onUpdate(field) }
		}
		listeners.forEach { it.onUpdate(field) }
		return reopen
	}

	fun getOpenings(callback: ((availability: ClaeroAvailability?, error: Exception?) -> Unit)) {
		doAsync {
			try {
				val response = ClaeroAPI.queryAvailability(this@Ticket)
				uiThread { callback(response, null) }
			} catch (e: Exception) {
				uiThread { callback(null, e) }
				e.upload(TAG)
			}
		}
	}

	fun finalizeDraft() {
		status = Status.OPEN
		save()
		GlobalScope.launch {
			withContext(Dispatchers.Main) { listeners.forEach { it.onUpdate(Field.STATUS) } }
		}
	}

	fun finalizeDraftAsync(callback: (e: ParseException?) -> Unit) {
		GlobalScope.launch {
			status = Status.OPEN
			try {
				val save = async(Dispatchers.IO) { save() }
				withContext(Dispatchers.Main) { listeners.forEach { it.onUpdate(Field.STATUS) } }
				save.await()
				withContext(Dispatchers.Main) { callback(null) }
			} catch (e: ParseException) {
				withContext(Dispatchers.Main) { callback(e) }
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
							ClaeroAPI.pushTicketStatus(this@Ticket, it, stage, callback)
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

	interface UpdateListener {

		fun onUpdate(field: Field)

	}
}