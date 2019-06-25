package com.myclaero.claerolibrary

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import android.os.Build
import androidx.exifinterface.media.ExifInterface
import android.widget.ImageView
import com.myclaero.claerolibrary.extensions.*
import com.parse.*
import com.parse.ktx.findAll
import com.parse.ktx.getBooleanOrNull
import com.parse.ktx.getIntOrNull
import com.parse.ktx.putOrIgnore
import kotlinx.coroutines.*
import org.jetbrains.anko.doAsync
import org.jetbrains.anko.uiThread
import org.json.JSONObject
import java.io.ByteArrayOutputStream

@ParseClassName(ParseVehicle.NAME)
class ParseVehicle constructor() : ParseObject() {

    companion object {
        const val NAME = "Vehicle"
        const val TAG = "ParseVehicle"

        const val THUMB_HEIGHT = 84
        const val THUMB_WIDTH = 112

        // The Parse Server's key for each field.
        // Each is named "KEY_TYPE" so it's always clear what data-type to expect.
        const val ACTIVE_BOOL = "active"
        const val OWNER_POINT = "owner"
        const val NICKNAME_STR = "nickname"
        const val YEAR_INT = "year"
        const val MAKE_STR = "make"
        const val MODEL_STR = "model"
        const val TRIM_STR = "trim"
        const val VIN_STR = "vin"
        const val TICKETS_REL = "tickets"
        const val IMAGE_FILE = "image"
        const val THUMB_FILE = "thumbnail"
        const val STATE_INT = "licenseState"
        const val PLATE_STR = "license"
        const val MOBILE_BOOL = "mobileService"
        const val ORDER_INT = "order"
        const val SPECS_JSON = "specifications"
        const val PARTS_JSON = "parts"
        const val SUBSCRIPTION_POINT = "subscription"

        const val VEHICLE_SERVICES_FUN = "vehicleServices"

        fun getAll() = ParseQuery(ParseVehicle::class.java)
            .whereEqualTo(OWNER_POINT, ParseUser.getCurrentUser())
            .whereEqualTo(ACTIVE_BOOL, true)
            .orderByAscending(ORDER_INT)
            .fromNetwork()
            .find()

        fun getAllAsync(callback: ((list: MutableList<ParseVehicle>?, e: ParseException?) -> Unit)) =
            ParseQuery(ParseVehicle::class.java)
                .whereEqualTo(OWNER_POINT, ParseUser.getCurrentUser())
                .whereEqualTo(ACTIVE_BOOL, true)
                .orderByAscending(ORDER_INT)
                .fromNetwork()
                .findInBackground(callback)

        fun get(vin: String) = ParseQuery(ParseVehicle::class.java)
            .whereEqualTo(VIN_STR, vin)
            .whereEqualTo(OWNER_POINT, ParseUser.getCurrentUser())
            .getFirstOrNull()

        fun getAllThumbnailsInParallel(vehicles: MutableList<ParseVehicle>, callback: (vehicle: ParseVehicle, bitmap: Bitmap?) -> Unit) {
            GlobalScope.launch(Dispatchers.Main) {
                try {
                    vehicles.forEach {
                        val job = async(Dispatchers.IO) { it.getParseFile(THUMB_FILE)?.data }
                        val thumbnail = async(Dispatchers.Default) {
                            job.await()?.let {
                                BitmapFactory.decodeByteArray(it, 0, it.size)
                            }
                        }
                        withContext(Dispatchers.Main) { callback(it, thumbnail.await()) }
                    }
                } catch (e: ParseException) {
                    if (e.code != ParseException.CONNECTION_FAILED) e.uploadAsync(TAG)
                }
            }
        }

        // Coroutine method of getting ALL thumbnails...
        // fun getThumbnails(vehicles: Set<ParseVehicle>, callback: ())
    }

    enum class Transmission(val string: String) {
        NA("N/A"),
        MANUAL("Manual"),
        AUTO("Automatic"),
        CVT("CVT")
    }

    enum class Drive(val string: String) {
        FWD("FWD"),
        RWD("RWD"),
        AWD("AWD"),
        XWD("4WD")
    }

    enum class Fuel {
        GAS,
        DIESEL,
        FLEX,
        HYBRID,
        ELECTRIC
    }

    var active: Boolean
        get() = getBooleanOrNull(ACTIVE_BOOL) ?: true
        set(active) = put(ACTIVE_BOOL, active)

    var owner: ParseUser?
        get() = getParseUser(OWNER_POINT)
        set(owner) = put(OWNER_POINT, owner!!)

    var nickname: String?
        get() = getString(NICKNAME_STR)
        set(nickname) = put(NICKNAME_STR, nickname!!)

    var year: Int
        get() = getInt(YEAR_INT)
        set(year) = putOrIgnore(YEAR_INT, year)

    var make: String
        get() = getString(MAKE_STR)!!
        set(make) = put(MAKE_STR, make)

    var model: String
        get() = getString(MODEL_STR)!!
        set(model) = put(MODEL_STR, model)

    var trim: String?
        get() = getString(TRIM_STR)
        set(trim) = put(TRIM_STR, trim!!)

    var specs: JSONObject?
        get() = getJSONObject(SPECS_JSON)
        set(json) {
            put(SPECS_JSON, json!!)
            vin = json.getString("vin")
            year = json.getString("year").toInt()
            make = json.getString("make")
            model = json.getString("model")
            trim = json.getString("trim_level")
        }

    var order: Int?
        get() = getIntOrNull(ORDER_INT)
        set(value) = put(ORDER_INT, value!!)

    var vin: String?
        get() = if (getString(VIN_STR)?.length == 17) getString(VIN_STR) else null
        set(vin) = if (vin?.length == 17) put(VIN_STR, vin) else Unit

    var license: String?
        get() = getString(PLATE_STR)
        set(license) = put(PLATE_STR, license!!)

    var licenseStateInt: Int?
        get() = getInt(STATE_INT)
        set(value) = put(STATE_INT, value!!)

    /**
     * A synchronous call to retrieve the ParseFile. Does not download the File, rather it provides a Pointer.
     */
    var thumbnail: ParseFile?
        get() = getParseFile(THUMB_FILE)
        set(value) = put(THUMB_FILE, value!!)

    val titleYmmt: String
        get() = "$year $make $model" + if (trim != null && trim!!.toLowerCase() != "base") " $trim" else ""

    val titleYmm: String
        get() = "$year $make $model"

    fun getPlate(context: Context): String {
        // Build license plate String
        val (s, p) = listOf(getLicenseState(context), license)
        return if (s != null && !p.isNullOrBlank()) "$s \u2013 $p" else ""
    }

    /*var engine: String?
        get() = getString("engine")
        set(engine) = put("engine", engine!!)

    val engineSize: Float?
        get() = engine!!.split(" ".single())[0].dropLast(1).toFloatOrNull()

    val engineConfig: String
        get() = engine!!.split(" ".single())[1]

    val isTurbo: Boolean?
        get() = engine!!.contains("T")

    val driveType: Int?
        get() = when (getString("drive_type")) {
            "FWD" -> DRIVE_FWD
            "RWD" -> DRIVE_RWD
            "AWD" -> DRIVE_AWD
            "4WD" -> DRIVE_4WD
            else -> null
        }

    val fuelType: Int?
        get() = when (getString("fuel_type")) {
            "Gasoline" -> FUEL_GASOLINE
            "Flexible-Fuel" -> FUEL_FLEX
            "Diesel" -> FUEL_DIESEL
            "Hybrid" -> FUEL_HYBRID
            "Electric" -> FUEL_ELECTRIC
            else -> null
        }


    fun setDriveType(driveType: Int) {
        when (driveType) {
            DRIVE_FWD -> {
                put("drive_type", "FWD")
            }
            DRIVE_RWD -> {
                put("drive_type", "RWD")
            }
            DRIVE_AWD -> {
                put("drive_type", "AWD")
            }
            DRIVE_4WD -> put("drive_type", "4WD")
        }
    }

    fun setFuelType(fuelType: Int) {
        when (fuelType) {
            FUEL_GASOLINE -> put("fuel_type", "Gasoline")
            FUEL_FLEX -> put("fuel_type", "Flexible-Fuel")
            FUEL_DIESEL -> put("fuel_type", "Diesel")
            FUEL_HYBRID -> put("fuel_type", "Hybrid")
            FUEL_ELECTRIC -> put("fuel_type", "Electric")
        }
    }*/

    val openTicket: ParseTicket
        get() = ParseQuery(ParseTicket::class.java)
            .whereEqualTo(ParseTicket.VEHICLE_POINT, this)
            .whereLessThanOrEqualTo(ParseTicket.STATUS_INT, ParseTicket.Status.OPEN.value)
            .include(ParseTicket.LOCATION_POINT)
            .first

    fun getOpenTicketInBackground(callback: (ticket: ParseTicket?, e: ParseException?) -> Unit) {
        val openStatus = listOf<Int>(
            ParseTicket.Status.NEW.value,
            ParseTicket.Status.DRAFT.value,
            ParseTicket.Status.OPEN.value
        )

        ParseQuery(ParseTicket::class.java)
            .whereEqualTo(ParseTicket.VEHICLE_POINT, this)
            .whereContainedIn(ParseTicket.STATUS_INT, openStatus)
            .include(ParseTicket.LOCATION_POINT)
            .fromNetwork()
            .getFirstInBackground { ticket, e: ParseException? ->
                if (e?.code == ParseException.OBJECT_NOT_FOUND)
                    callback(ticket, null)
                else
                    callback(ticket, e)
            }
    }

    fun safeDelete(callback: (e: ParseException?) -> Unit) {
        doAsync {
            var e: ParseException? = null
            try {
                active = false
                save()
                unpin()
            } catch (ex: ParseException) {
                e = ex
            }
            uiThread {
                callback(e)
            }
        }
    }

    /**
     * Synchronously retrieves a sorted list of all associated ParseTickets, sorted newest to oldest.
     */
    val tickets: List<ParseTicket>?
        get() = ParseQuery(ParseTicket::class.java)
            .findAll()
            .toList()
            .sortedByDescending { it.time }

    /**
     * Asynchronously retrieves a sorted list of all associated ParseTickets, from newest to oldest.
     */
    fun getTicketsInBackground(callback: (tickets: List<ParseTicket>?, e: ParseException?) -> Unit) {
        ParseQuery(ParseTicket::class.java)
            .whereEqualTo(ParseTicket.VEHICLE_POINT, this)
            .include(ParseTicket.LOCATION_POINT)
            .include(ParseTicket.SERVICES_REL)
            .findInBackground { objects, e ->
                val list = objects
                    .toList()
                    .sortedByDescending { it.time }
                callback(list, e)
            }
    }

    fun getServicesAsync(callback: (services: List<ParseService>?, error: ParseException?) -> Unit) {
        ParseCloud.callFunctionInBackground<List<ParseService>>(
            VEHICLE_SERVICES_FUN,
            mapOf("vehicle" to objectId)
        ) { result, e ->
            callback(result, e)
        }
    }

    fun getLicenseState(context: Context): String? {
        val stateInt = licenseStateInt
        return if (stateInt == null || stateInt == 0) {
            null
        } else {
            context.resources.getStringArray(R.array.states)[licenseStateInt!!]
        }
    }

    fun setImageInBackground(context: Context, uri: Uri): Bitmap {
        val imgFullBitmap = context.getBitmap(uri)
        val imgThumbBitmap = imgFullBitmap.resize(THUMB_WIDTH, THUMB_HEIGHT)
        GlobalScope.launch(Dispatchers.Main) {
            val fullFile = async(Dispatchers.IO) { imgFullBitmap.upload() }
            val thumbFile = async(Dispatchers.IO) { imgThumbBitmap.upload() }
            put(THUMB_FILE, thumbFile.await())
            put(IMAGE_FILE, fullFile.await())
            saveInBackground()
        }
        return imgThumbBitmap
    }

    fun getThumbnailInBackground(callback: (vehicle: ParseVehicle, bitmap: Bitmap?, e: Exception?) -> Unit) {
        GlobalScope.launch(Dispatchers.Main) {
            try {
                val data = withContext(Dispatchers.IO) { getParseFile(THUMB_FILE)?.data }
                val thumbnail = data?.let { BitmapFactory.decodeByteArray(it, 0, it.size) }
                callback(this@ParseVehicle, thumbnail, null)
            } catch (e: ParseException) {
                callback(this@ParseVehicle, null, e)
            }
        }
    }

    fun getImageInBackground(callback: (vehicle: ParseVehicle, bitmap: Bitmap?, e: Exception?) -> Unit) {
        getParseFile(IMAGE_FILE)?.getDataInBackground { data, e ->
            val image = data?.let { BitmapFactory.decodeByteArray(data, 0, it.size) }
            callback(this, image, e)
        } ?: callback(this, null, null)
    }
}