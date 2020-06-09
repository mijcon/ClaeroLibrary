package com.myclaero.claerolibrary.core

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import com.myclaero.claerolibrary.R
import com.myclaero.claerolibrary.extensions.*
import com.parse.*
import com.parse.ktx.putOrIgnore
import com.parse.ktx.putOrRemove
import kotlinx.coroutines.*
import org.json.JSONObject
import java.util.*

/**
 * A subclass of ParseObject representing customer vehicles within the Claero Parse database.
 *
 * @constructor The requisite empty constructor for Parse SDK to properly subclass this ParseObject.
 */
@ParseClassName(Vehicle.NAME)
class Vehicle constructor(): ParseObject() {

	companion object {
		const val NAME = "Vehicle"
		const val TAG = "Vehicle"

		// The Parse Server's key for each field.
		// Each is named "KEY_TYPE" so it's always clear what data-type to expect.
		const val ACTIVE_BOOL = "active"
		const val OWNER_POINT = "owner"
		const val NICKNAME_STR = "nickname"
		const val YEAR_STR = "year"
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
		const val ENGINE_STR = "engine"
		const val PARTS_JSON = "parts"
		const val FUEL_STR = "fuel"
		const val DRIVE_STR = "drive"
		const val TRANS_STR = "transmission"
		const val SUBSCRIPTION_POINT = "subscription"

		private val openStatus = listOf(
			Ticket.Status.NEW.value,
			Ticket.Status.DRAFT.value,
			Ticket.Status.OPEN.value,
			Ticket.Status.PENDING.value
		)

		/**
		 * Synchronously fetches all active Vehicles associated with the current ParseUser. See [getAllAsync] for asynchronous equivalent.
		 * @return A mutable list of Vehicles.
		 * @throws ParseException Throws an exception if no Vehicles can be found.
		 */
		@Deprecated("Deprecated as of version 1.0", ReplaceWith("fetch"))
		fun getAll() = ParseQuery(Vehicle::class.java)
			.whereEqualTo(OWNER_POINT, ParseUser.getCurrentUser())
			.whereEqualTo(ACTIVE_BOOL, true)
			.orderByAscending(ORDER_INT)
			.fromNetwork()
			.find()

		/**
		 * Synchronously fetches all active Vehicles associated with the current ParseUser.
		 *
		 * @param user The ParseUser we want to restrict the search to. This field is important
		 * for Technicians and Managers, to avoid simply showing all Vehicles for which
		 * they have visibility.
		 * @return A mutable list of Vehicles.
		 * @throws ParseException Throws an exception if no Vehicles can be found, or if some other
		 * error occurs.
		 */
		fun fetch(user: ParseUser = ParseUser.getCurrentUser()) =
			ParseQuery(Vehicle::class.java)
				.whereEqualTo(OWNER_POINT, ParseUser.getCurrentUser())
				.whereEqualTo(ACTIVE_BOOL, true)
				.orderByAscending(ORDER_INT)
				.fromNetwork()
				.find()

		/**
		 * Asynchronously fetches all active Vehicles associated with the current ParseUser.
		 *
		 * @param user The ParseUser we want to restrict the search to. This field is important
		 * for Technicians and Managers, to avoid simply showing all Vehicles for which
		 * they have visibility.
		 * @return The Kotlin Coroutine [Deferred] for the query.
		 * @throws ParseException Throws an exception if no Vehicle can be found, or some other
		 * error occurs.
		 */
		fun fetchAsync(user: ParseUser = ParseUser.getCurrentUser()): Deferred<List<Vehicle>> =
			GlobalScope.async(Dispatchers.IO) {
				fetch(
					user
				)
			}

		/**
		 * Asynchronously fetches all active Vehicles associated with the current ParseUser. See [getAll] for synchronous equivalent.
		 * @param callback A lambda function called upon completion of asynchronous query.
		 */
		@Deprecated("Deprecated as of version 1.0", ReplaceWith("fetchAsync"))
		fun getAllAsync(callback: (list: MutableList<Vehicle>?, e: ParseException?) -> Unit) =
			ParseQuery(Vehicle::class.java)
				.whereEqualTo(OWNER_POINT, ParseUser.getCurrentUser())
				.whereEqualTo(ACTIVE_BOOL, true)
				.orderByAscending(ORDER_INT)
				.fromNetwork()
				.findInBackground(callback)

		/**
		 * Asynchronously fetches all active Vehicles associated with the current ParseUser.
		 *
		 * @param user The ParseUser we want to restrict the search to. This field is important
		 * for Technicians and Managers, to avoid simply showing all Vehicles for which
		 * they have visibility.
		 * @param callback A lambda function called upon completion of asynchronous query.
		 */
		fun fetchAsync(
			user: ParseUser = ParseUser.getCurrentUser(),
			callback: FindCallback<Vehicle>
		) {
			GlobalScope.launch {
				try {
					callback.done(
						fetchAsync(
							user
						).await(), null)
				} catch (e: ParseException) {
					callback.done(null, e)
				}
			}
		}


		/**
		 * Synchronously fetches the Vehicle with a matching VIN.
		 *
		 * @param vin The VIN of the desired vehicle.
		 * @param user The ParseUser representing the owner of the vehicle in question.
		 * @return The matching Vehicle, if it exists and is associated with the current ParseUser.
		 */
		fun get(vin: String, user: ParseUser = ParseUser.getCurrentUser()): Vehicle? =
			ParseQuery<Vehicle>(
				Vehicle::class.java)
				.whereEqualTo(VIN_STR, vin)
				.whereEqualTo(OWNER_POINT, ParseUser.getCurrentUser())
				.firstOrNull

		/**
		 * Asynchronously fetches the Vehicle with a matching VIN.
		 *
		 * @param vin The VIN of the desired vehicle.
		 * @param user The ParseUser representing the owner of the vehicle in question.
		 * @return The Kotlin Coroutine [Deferred] for the query.
		 */
		fun getAsync(
			vin: String,
			user: ParseUser = ParseUser.getCurrentUser()
		): Deferred<Vehicle?> =
			GlobalScope.async(Dispatchers.IO) {
				get(
					vin,
					user
				)
			}

		/**
		 * Asynchronously fetches the Vehicle with a matching VIN.
		 * @param vin The VIN of the desired vehicle.
		 * @param user The ParseUser representing the owner of the vehicle in question.
		 * @param callback The lambda to invoke upon completion of the background task.
		 */
		fun getAsync(
			vin: String,
			user: ParseUser = ParseUser.getCurrentUser(),
			callback: GetCallback<Vehicle>
		) {
			GlobalScope.launch {
				try {
					callback.done(
						getAsync(
							vin,
							user
						).await(), null)
				} catch (e: ParseException) {
					callback.done(null, e)
				}
			}
		}

		/**
		 * Asynchronously fetches thumbnails for the corresponding Vehicles. See [getThumbnailAsync] to fetch a thumbnail for a specific Vehicle.
		 * @param vehicles A list of the Vehicles for which the desired thumbnails are associated.
		 * @param callback A lambda function called upon completion of the asynchronous query.
		 */
		@Deprecated("Deprecated since version 1.0, use the Picasso library instead.")
		fun getThumbnailsAsync(
			vehicles: MutableList<Vehicle>,
			callback: (vehicle: Vehicle, bitmap: Bitmap?) -> Unit
		) {
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
					if (e.code != ParseException.CONNECTION_FAILED) e.upload()
				}
			}
		}
	}

	enum class Transmission(val key: String) {
		NA("NA"),
		MANUAL("STD"),
		AUTO("AUTO"),
		CVT("CVT")
	}

	enum class Drive(val key: String) {
		FWD("FWD"),
		RWD("RWD"),
		AWD("AWD"),
		XWD("4WD")
	}

	enum class Fuel(val key: String) {
		GAS("GAS"),
		DIESEL("DSL"),
		FLEX("FF"),
		HYBRID("HYB"),
		ELECTRIC("ELEC")
	}

	/**
	 * Builds a [Vehicle] object using the minimal required information.
	 *
	 * @param year The model year of the vehicle.
	 * @param make The make/brand of the vehicle.
	 * @param model The model of the vehicle.
	 */
	constructor(year: String, make: String, model: String): this() {
		this.owner = ParseUser.getCurrentUser()
		this.year = year
		this.make = make
		this.model = model
	}

	/**
	 * Builds a [Vehicle] object using data stored in the [JSONObject] from VIN-Decoder.
	 *
	 * @param specifications The VIN-Decoder data stored in a JSONObject.
	 */
	constructor(specifications: JSONObject): this() {
		this.owner = ParseUser.getCurrentUser()
		this.specifications = specifications
	}

	/**
	 * A boolean representing the active status of a Vehicle. When vehicles are "deleted", [active]
	 * is usually just set to false. This way, records can continue to be associated with a Vehicle
	 * object.
	 */
	val active: Boolean
		get() = getBoolean(ACTIVE_BOOL)

	/**
	 * The User associated with the Vehicle.
	 */
	var owner: ParseUser
		get() = getParseUser(OWNER_POINT)!!
		private set(owner) = put(OWNER_POINT, owner)

	/**
	 * The friendly nickname of the vehicle.
	 */
	var nickname: String?
		get() = getString(NICKNAME_STR)
		set(nickname) = putOrRemove(NICKNAME_STR, nickname)

	/**
	 * The year of the vehicle.
	 */
	var year: String
		get() = getString(YEAR_STR)!!
		private set(year) = put(YEAR_STR, year)

	/**
	 * The make (i.e., brand) of the vehicle.
	 */
	var make: String
		get() = getString(MAKE_STR)!!
		private set(make) = put(MAKE_STR, make)

	/**
	 * The model of the vehicle.
	 */
	var model: String
		get() = getString(MODEL_STR)!!
		private set(model) = put(MODEL_STR, model)

	/**
	 * The name of the trim level of the vehicle, if it exists, or null.
	 */
	var trim: String?
		get() = getString(TRIM_STR)?.let {
			if (it.toLowerCase(Locale.ROOT) == "base") null
			else it
		}
		private set(trim) = putOrIgnore(TRIM_STR, trim)

	/**
	 * The [JSONObject] provided by the VIN-Decoder system.
	 */
	var specifications: JSONObject?
		get() = getJSONObject(SPECS_JSON)
		/**
		 * Sets the Vehicle's specifications field. Also updates the VIN, year, make, model and
		 * trim fields based on the data provided in the JSONObject.
		 *
		 * @param json the JSONObject containing vehicle information
		 */
		set(json) {
			put(SPECS_JSON, json!!)
			vin = json.getString("vin")
			year = json.getString("year")
			make = json.getString("make")
			model = json.getString("model")
			trim = json.getString("trim_level")
			engine =
				Engine(json.getString("engine"))
			drive = when (json.getString("drive_type")) {
				"FWD" -> Drive.FWD
				"AWD" -> Drive.AWD
				"4WD/4-Wheel Drive/4x4" -> Drive.XWD
				"RWD" -> Drive.RWD
				else -> null
			}
			fuel = when (json.getString("fuel_type")) {
				"Gasoline" -> Fuel.GAS
				"Diesel" -> Fuel.DIESEL
				"Flexible-Fuel" -> Fuel.FLEX
				"Electric" -> Fuel.ELECTRIC
				"Hybrid" -> Fuel.HYBRID
				else -> null
			}
			transmission = when (json.getString("transmission")) {
				"Automatic" -> Transmission.AUTO
				"Manual" -> Transmission.MANUAL
				"CVT" -> Transmission.CVT
				else -> null
			}
		}

	/**
	 * Returns a localized String representing the Year, Make and Model (and optional Trim) of the
	 * Vehicle.
	 *
	 * @param context The package context, granting access to XML String Resources.
	 * @param addTrim Boolean representing whether or not to include the Trim in the String.
	 * @return The String representing the Vehicle YMM(T).
	 */
	fun getTitle(context: Context, addTrim: Boolean): String {
		val r = if (addTrim) R.string.vehicle_title else R.string.vehicle_title_trim
		return context.getString(r, year, make, model, trim)
	}

	/**
	 * An Int representing the placement of this Vehicle in relation to other Vehicles listed
	 * under the same owner.
	 */
	var order: Int
		get() = getInt(ORDER_INT)
		set(order) = put(ORDER_INT, order)

	/**
	 * The [Vehicle]'s VIN, or null if unset.
	 */
	var vin: String?
		get() = getString(VIN_STR)
		private set(vin) = putOrIgnore(VIN_STR, vin)

	/**
	 * The license plate string for the [Vehicle].
	 */
	var license: String?
		get() = getString(PLATE_STR)
		set(license) = putOrRemove(PLATE_STR, license)

	/**
	 * An Int representing the state or commonwealth that the vehicle's registration lists, or
	 * null if not provided.
	 */
	var licenseStateInt: Int?
		get() = getInt(STATE_INT)
		set(state) = putOrRemove(STATE_INT, state)

	/**
	 * Returns the license plate and state as a localized String.
	 *
	 * @param context The package context, allowing access to XML resources.
	 * @return The localized, formatted String.
	 */
	fun getLicense(context: Context): String =
		// Build license plate String
		when {
			license == null -> ""
			licenseStateInt == null -> license ?: ""
			else -> context.getString(R.string.license_plate, license, getLicenseState(context))
		}

	/**
	 * Returns the state on the [Vehicle]'s license plate, in String format.
	 *
	 * @param context The package context.
	 * @return The license plate state.
	 */
	fun getLicenseState(context: Context): String? =
		licenseStateInt?.let {
			if (it > 0) context.resources.getStringArray(R.array.states)[licenseStateInt!!]
			else null
		}

	/**
	 * The String representation of the [Vehicle]'s engine, or null if unset.
	 */
	var engine: Engine?
		get() = getString(ENGINE_STR)?.let {
			Engine(
				it
			)
		}
		private set(engine) = putOrIgnore(ENGINE_STR, engine?.toString())

	/**
	 * The [Drive] type for the [Vehicle], or null if unset.
	 */
	var drive: Drive?
		get() = getString(DRIVE_STR)?.let { value -> Drive.values().first { it.key == value } }
		private set(drive) = putOrIgnore(DRIVE_STR, drive?.key)

	/**
	 * The [Fuel] type for the [Vehicle], or null if unset.
	 */
	var fuel: Fuel?
		get() = getString(FUEL_STR)?.let { value -> Fuel.values().first { it.key == value } }
		private set(fuel) = putOrIgnore(FUEL_STR, fuel?.key)

	var transmission: Transmission?
		get() = getString(TRANS_STR)?.let { key -> Transmission.values().first { it.key == key } }
		private set(trans) = putOrIgnore(TRANS_STR, trans?.key)

	/**
	 * A convenience function for [Ticket.fetch]; synchronously retrieves a list of all
	 * associated Tickets, from newest to oldest.
	 */
	val tickets: List<Ticket>
		get() = Ticket.fetch(this, true)

	/**
	 * A convenience function for [Ticket.fetchAsync]; retrieves a list of all
	 * associated Tickets, from newest to oldest.
	 */
	fun getTicketsAsync(callback: FindCallback<Ticket>) =
		Ticket.fetchAsync(
			this,
			true,
			callback
		)

	/**
	 * A synchronous convenience property for [Ticket.fetch] which retrieves all Tickets
	 * with an "open" Status (NEW, DRAFT, OPEN, PENDING).
	 */
	val openTickets: List<Ticket>
		get() = Ticket.fetch(this, false)

	/**
	 * An asynchronous convenience function for [Ticket.fetchAsync] which retrieves all Tickets
	 * with an "open" Status (NEW, DRAFT, OPEN, PENDING).
	 *
	 * @param callback The lambda to invoke upon completion of the search.
	 */
	fun getOpenTicketsAsync(callback: FindCallback<Ticket>) =
		Ticket.fetchAsync(
			this,
			false,
			callback
		)

	/**
	 * A convenience property for the synchronous function [Service.fetch]. Retrieves a list
	 * of all available Services that may apply to this Vehicle.
	 */
	val services: List<Service>
		get() = Service.fetch(this)

	/**
	 * An asynchronous convenience function for [Service.fetchAsync]. Retrieves a list of all
	 * available Services that may apply to this [Vehicle].
	 */
	fun getServicesAsync(): Deferred<List<Service>> = Service.fetchAsync(this)

	/**
	 * An asynchronous convenience function for [Service.fetchAsync]. Retrieves a list
	 * of all available Services that may apply to this Vehicle.
	 *
	 * @param callback The lambda to invoke upon completion of the query.
	 */
	fun getServicesAsync(callback: FindCallback<Service>) =
		Service.fetchAsync(this, callback)

	/**
	 * The full size [Bitmap] for the [Vehicle], or null.
	 */
	var image: Bitmap?
		get() = getParseFile(IMAGE_FILE)?.data?.let {
			BitmapFactory.decodeByteArray(it, 0, it.size)
		}
		set(image) {
			val file = image?.upload()
			putOrIgnore(IMAGE_FILE, file)
			save()
		}

	/**
	 * Asynchronously retrieves the [Vehicle]'s full size image.
	 *
	 * @return The Kotlin Coroutine [Deferred] managing the retrieval of the [Bitmap].
	 */
	fun getImageAsync(): Deferred<Bitmap?> = GlobalScope.async {
		val data = withContext(Dispatchers.IO) { getParseFile(IMAGE_FILE)?.data }
		data?.let {
			BitmapFactory.decodeByteArray(it, 0, it.size)
		}
	}

	/**
	 * Asynchronously retrieves the [Vehicle]'s full size image.
	 *
	 * @param callback The lambda to invoke upon completion of the task.
	 */
	fun getImageAsync(callback: GetBitmapCallback) {
		GlobalScope.launch {
			try {
				callback.done(getImageAsync().await(), null)
			} catch (e: Exception) {
				callback.done(null, e)
			}
		}
	}

	/**
	 * Asynchronously sets the [Vehicle]'s full size and thumbnail images from the given [Bitmap].
	 *
	 * @param bitmap The Bitmap to be saved to the vehicle.
	 * @param thumbWidth The desired width, in pixels, of the thumbnail.
	 * @param thumbHeight The desired height, in pixels, of the thumbnail.
	 * @return The Kotlin Coroutine [Deferred] for the task.
	 */
	fun setImageAsync(bitmap: Bitmap, thumbWidth: Int, thumbHeight: Int): Deferred<Unit> =
		GlobalScope.async {
			val thumb = async(Dispatchers.Default) { bitmap.resize(thumbWidth, thumbHeight) }
			val fullFile = async(Dispatchers.IO) { bitmap.upload() }
			val thumbFile = async(Dispatchers.IO) { thumb.await().upload() }
			put(THUMB_FILE, thumbFile.await())
			put(IMAGE_FILE, fullFile.await())
			return@async withContext(Dispatchers.IO) { save() }
		}

	/**
	 * Asynchronously sets the [Vehicle]'s full size and thumbnail images from the given [Bitmap].
	 *
	 * @param bitmap The Bitmap to be saved to the vehicle.
	 * @param thumbWidth The desired width, in pixels, of the thumbnail.
	 * @param thumbHeight The desired height, in pixels, of the thumbnail.
	 * @param callback The lambda to invoke upon completion.
	 * @return The thumbnail version of the provided image.
	 */
	fun setImageAsync(bitmap: Bitmap, thumbWidth: Int, thumbHeight: Int, callback: SaveCallback) =
		GlobalScope.launch {
			try {
				setImageAsync(bitmap, thumbWidth, thumbHeight).await()
				callback.done(null)
			} catch (e: ParseException) {
				callback.done(e)
			}
		}

	/**
	 * Similar to the other version of [setImageAsync], but retrieves from the given [Uri] and
	 * rotates the image based on EXIF data.
	 *
	 * @param context The [Context] used to retrieve the given Uri.
	 * @param uri The Uri for the desired [Bitmap].
	 * @param thumbWidth The desired width, in pixels, of the thumbnail.
	 * @param thumbHeight The desired height, in pixels, of the thumbnail.
	 * @param callback The lambda to invoke upon completion.
	 * @return The thumbnail version of the provided image.
	 */
	fun setImageAsync(
		context: Context,
		uri: Uri,
		thumbWidth: Int,
		thumbHeight: Int,
		callback: SaveCallback
	) =
		context.getBitmap(uri)?.let { setImageAsync(it, thumbWidth, thumbHeight, callback) }

	/**
	 * The thumbnail [Bitmap] for the [Vehicle], or null.
	 */
	var thumbnail: Bitmap?
		get() = getParseFile(THUMB_FILE)?.data?.let {
			BitmapFactory.decodeByteArray(it, 0, it.size)
		}
		set(image) {
			val file = image?.upload()
			putOrIgnore(THUMB_FILE, file)
			save()
		}

	/**
	 * Asynchronously retrieves the [Vehicle]'s thumbnail.
	 *
	 * @return The Kotlin Coroutine [Deferred] result of the task.
	 */
	fun getThumbnailAsync(): Deferred<Bitmap?> =
		GlobalScope.async(Dispatchers.Default) {
			val data = withContext(Dispatchers.IO) { getParseFile(THUMB_FILE)?.data }
			data?.let { BitmapFactory.decodeByteArray(it, 0, it.size) }
		}

	/**
	 * Asynchronously retrieves the [Vehicle]'s thumbnail.
	 *
	 * @param callback The lambda to invoke upon completion of the background task.
	 */
	fun getThumbnailAsync(callback: GetBitmapCallback) {
		GlobalScope.launch(Dispatchers.Main) {
			try {
				callback.done(getThumbnailAsync().await(), null)
			} catch (e: Exception) {
				callback.done(null, e)
			}
		}
	}

	data class Engine(private val string: String) {

		enum class EngineConfig(val key: String) {
			L("L"), // Inline
			V("V"), // Traditional V
			H("H"), // Boxer
			W("W"), // W (double-V)
			R("R")  // Wankel/Rotary
		}

		/**
		 * The approximate displacement of the engine, in liters.
		 */
		val displacement: Float?
			get() = string.split(' ').firstOrNull()?.dropLast(1)?.toFloatOrNull()

		/**
		 * The [EngineConfig] for the [Vehicle], if able to be determined from the available vehicle
		 * specifications JSON.
		 */
		val engineConfig: EngineConfig?
			get() = string.split(' ')[1]
				.substring(0, 1)
				.let { key ->
					EngineConfig.values().first { it.key == key }
				}

		/**
		 * A Boolean representing whether the Engine is turbocharged.
		 */
		val isTurbo: Boolean?
			get() = string.contains("T")

		/**
		 * A Boolean representing whether the Engine is supercharged.
		 */
		val isSuper: Boolean?
			get() = string.contains("S")

		override fun toString() = string

	}

}