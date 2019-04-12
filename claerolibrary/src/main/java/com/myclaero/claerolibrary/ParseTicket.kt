package com.myclaero.claerolibrary

import android.content.Context
import android.support.v7.app.AlertDialog
import com.myclaero.claerolibrary.extensions.getTimeString
import com.myclaero.claerolibrary.extensions.upload
import com.parse.ParseClassName
import com.parse.ParseException
import com.parse.ParseObject
import com.parse.ParseUser
import com.parse.ktx.findAll
import com.parse.ktx.getIntOrNull
import com.parse.ktx.getLongOrNull
import com.parse.ktx.putOrIgnore
import org.jetbrains.anko.doAsync
import org.jetbrains.anko.uiThread
import java.util.*

@ParseClassName(ParseTicket.NAME)
class ParseTicket constructor() : ParseObject() {

    companion object {
        const val NAME = "Ticket"
        const val TAG = "ParseTicket"

        const val MILLIS_IN_TIME_BLOCK = 1000 * 60 * 30

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
        CLOSED (3)
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

    var vehicle: ParseVehicle? = null
        get() = getParseObject(VEHICLE_POINT) as ParseVehicle?
        set(value) = if (field == null) put(VEHICLE_POINT, value!!) else Unit

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
            3 -> Status.CLOSED
            2 -> Status.CANCELLED
            1 -> Status.OPEN
            0 -> Status.DRAFT
            else -> Status.NEW
        }

    var payment: ParseObject?
        get() = getParseObject(CHARGE_POINT)
        set(value) = put(CHARGE_POINT, value!!)

    var time: Date?
        get() = getLongOrNull(START_LONG)?.let { Date(it * 1000) }
        set(value) = value?.let { put(START_LONG, value.time / 1000) } ?: Unit

	val shift: ParseShift?
		get() = getParseObject(SHIFT_POINT) as ParseShift?

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
        val time = newServices.sumBy { it.getIntOrNull(ParseService.DURATION_INT) ?: 0 }
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
    fun saveDraft(context: Context, field: Field) {
        if (status == Status.OPEN) {
            AlertDialog.Builder(context)
                .setTitle("Draft Ticket")
                .setMessage("Your ticket is now in draft mode. Please make sure to Confirm your ticket.")
                .setPositiveButton("Okay") { d, w ->
                    d.dismiss()
                }
                .create()
                .show()
        }

        status = Status.DRAFT
        saveInBackground {
            // Switch back to an update-after-save model if there are errors...
            // listeners.forEach { it.onUpdate(field) }
        }
        listeners.forEach { it.onUpdate(field) }
    }

    fun getOpenings(callback: ((availability: ClaeroAPI.ClaeroAvailability?, error: Exception?) -> Unit)) {
	    doAsync {
		    try {
			    val latLng = location!!.geoPoint.let { "${it.latitude},${it.longitude}" }
			    val response = ClaeroAPI.queryAvailability(objectId, latLng)
			    uiThread { callback(response, null) }
		    } catch (e: Exception) {
			    uiThread { callback(null, e) }
			    e.upload(TAG)
		    }
	    }
    }

    fun finalizeDraft(field: Field): Boolean {
        status = Status.OPEN
        save()
        // val success = ClaeroAPI.scheduleTicket(objectId)
        listeners.forEach { it.onUpdate(field) }
        return true
    }

    fun finalizeDraftAsync(callback: (e: Exception?) -> Unit) {
        this.status = Status.OPEN
        doAsync {
            try {
                save()
	            val success = ClaeroAPI.scheduleTicket(objectId)
                uiThread {
	                callback(null)
                    listeners.forEach { it.onUpdate(Field.STATUS) }
                }
            } catch (e: Exception) {
                uiThread {
	                callback(e)
	                e.upload(TAG)
                }
            }
        }
    }

    val timeString: String?
        get() = ticketTime?.let { time?.getTimeString((it + 1) / 2) }

    interface UpdateListener {

        fun onUpdate(field: Field)

    }
}