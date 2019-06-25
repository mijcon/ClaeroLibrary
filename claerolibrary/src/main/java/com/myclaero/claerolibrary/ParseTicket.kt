package com.myclaero.claerolibrary

import com.myclaero.claerolibrary.extensions.getTimeString
import com.myclaero.claerolibrary.extensions.upload
import com.parse.*
import com.parse.ktx.*
import kotlinx.coroutines.*
import org.jetbrains.anko.doAsync
import org.jetbrains.anko.uiThread
import java.util.*

@ParseClassName(ParseTicket.NAME)
class ParseTicket constructor() : ParseObject() {

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
    }

    enum class Status(val value: Int) {
        NEW (-1),
        DRAFT (0),
        OPEN (1),
        CANCELLED (2),
        CLOSED (3),
        PENDING (4)
    }

    enum class TechnicianStatus(value: Int) {
        DRIVING_PICKUP (1),
        ARRIVING_PICKUP (2),
        DRIVING_SHOP (3),
        ARRIVING_SHOP (4),
        DRIVING_DROPOFF (5),
        ARRIVING_DROPOFF (6)
    }

    enum class Field(val value: Int) {
        LOCATION (7),
        TIME (5),
        TIME_ESTIMATE (4),
        SERVICES (3),
        PAYMENT (9),
        STATUS (13)
    }

    val listeners: MutableSet<UpdateListener> = mutableSetOf()

    var vehicle: ParseVehicle?
        get() = getParseObject(VEHICLE_POINT) as ParseVehicle?
        set(value) = putOrRemove(VEHICLE_POINT, value)

    var owner: ParseUser?
        get() = getParseUser(USER_POINT)
        set(user) = putOrIgnore(USER_POINT, user)

    var location: ParseLocation?
        get() = getParseObject(LOCATION_POINT) as ParseLocation?
        set(location) = putOrIgnore(LOCATION_POINT, location)

    var serviceTime: Int?
        get() = getIntOrNull(DURATION_INT)
        set(value) = putOrIgnore(DURATION_INT, value)

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
        set(shiftId) = putOrRemove(SHIFT_POINT, createWithoutData(ParseShift::class.java, shiftId))

    fun setShift(shift: ClaeroShift) {
        this.shift = shift.objectId
    }

    var payment: ParseObject?
        get() = getParseObject(CHARGE_POINT)
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

    fun getServicesAsSet(callback: (servicesSet: Set<ParseService>?) -> Unit) {
        if (this.status == Status.NEW) {
            callback(null)
        } else {
            getRelation<ParseService>(SERVICES_REL).query.findInBackground { services, _ ->
                callback(services?.toSet())
                // e?.upload(TAG)
            }
        }
    }

    fun setServices(newServices: Set<ParseService>, callback: (e: ParseException?) -> Unit) {
        // Save new TimeEstimate to help our TimeListAdapter update quickly.
        val time = newServices.sumBy { it.duration }
        serviceTime = if (newServices.isEmpty()) null else time
        listeners.forEach { it.onUpdate(Field.TIME_ESTIMATE) }

        // Worry about saving the services themselves in the background.
        doAsync {
            var error: ParseException? = null
            try {
                val relation = getRelation<ParseService>(SERVICES_REL)
                val oldServices = relation.query.findAll()

                // Delete all of the services previously on the Ticket (if any)
                oldServices.forEach { if (!newServices.contains(it)) relation.remove(it) }
                save()

                // Add each of the new services to the Ticket
                newServices.forEach { if (!oldServices.contains(it)) relation.add(it) }
                save()
            } catch (e: ParseException) {
                // e.upload(TAG)
                error = e
            }
            uiThread {
                callback(error)
                listeners.forEach { it.onUpdate(Field.SERVICES) }
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
			    val response = ClaeroAPI.queryAvailability(this@ParseTicket)
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

    val timeString: String?
        get() = ticketTime?.let { time?.getTimeString((it + 1) / 2) }

    interface UpdateListener {

        fun onUpdate(field: Field)

    }
}