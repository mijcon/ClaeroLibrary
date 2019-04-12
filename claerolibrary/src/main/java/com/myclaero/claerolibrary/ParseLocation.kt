package com.myclaero.claerolibrary

import android.widget.ImageView
import com.google.android.gms.location.places.Place
import com.myclaero.claerolibrary.extensions.dpToPx
import com.myclaero.claerolibrary.extensions.uploadAsync
import com.parse.ParseClassName
import com.parse.ParseGeoPoint
import com.parse.ParseObject
import com.parse.ParseUser
import com.parse.ktx.putOrIgnore
import com.squareup.picasso.Picasso
import org.json.JSONObject

@ParseClassName(ParseLocation.NAME)
class ParseLocation constructor(): ParseObject() {

	companion object {
		const val NAME = "Location"
		const val TAG = "ParseLocation"

		// The Parse Server's key for each field.
		// Each is named "KEY_TYPE" so it's always clear what data-type to expect.
		const val UNIT_STR = "unit"
		const val OWNER_POINT = "owner"
		const val MAPS_NAME_STR = "mapName"
		const val NICKNAME_STR = "nickname"
		const val MAPS_PHONE_LONG = "mapPhone"
		const val ADDRESS_LINE_ONE_STR = "addressOne"
		const val ADDRESS_LINE_TWO_STR = "addressTwo"
		const val NOTE_STR = "note"
		const val MAPS_ID_JSON = "mapId"
		const val GEOPOINT_GEOPOINT = "geoPoint"
		const val ACTIVE_BOOL = "active"

		const val MAPS_GOOGLE_KEY = "google"
		const val MAPS_HERE_KEY = "here"
	}

	constructor(place: Place): this() {
		val (lineOne, lineTwo) = place.address!!.removeSuffix(", USA").split(Regex(", "), 2)

		owner = ParseUser.getCurrentUser()
		nickname = place.name.toString()
		mapsName = place.name.toString()
		geoPoint = ParseGeoPoint(place.latLng.latitude, place.latLng.longitude)
		googleMapsId = place.id
		isActive = true
		addressOne = lineOne
		addressTwo = lineTwo
		place.phoneNumber?.let { setPhone(it.toString()) }

		saveInBackground { it?.uploadAsync(TAG) }
	}

	var unit: String?
		get() = if (getString(UNIT_STR).isNullOrBlank()) null else getString(UNIT_STR)
		set(value) = putOrIgnore(UNIT_STR, value)

	var owner: ParseUser?
		get() = getParseUser(OWNER_POINT)
		set(owner) = put(OWNER_POINT, owner!!)

	var isActive: Boolean
		get() = getBoolean(ACTIVE_BOOL)
		set(value) = put(ACTIVE_BOOL, value)

	var nickname: String
		get() = getString(NICKNAME_STR) ?: getString(ADDRESS_LINE_ONE_STR)!!
		set(value) = putOrIgnore(NICKNAME_STR, value)

	var addressOne: String
		get() = getString(ADDRESS_LINE_ONE_STR)!!
		private set(value) = put(ADDRESS_LINE_ONE_STR, value)

	var addressTwo: String
		get() = getString(ADDRESS_LINE_TWO_STR)!!
		private set(value) = put(ADDRESS_LINE_TWO_STR, value)

	var note: String?
		get() = getString(NOTE_STR)
		set(value) = putOrIgnore(NOTE_STR, value)

	var geoPoint: ParseGeoPoint
		get() = getParseGeoPoint(GEOPOINT_GEOPOINT)!!
		private set(value) = put(GEOPOINT_GEOPOINT, value)

	var mapsName: String?
		get() = getString(MAPS_NAME_STR)
		private set(value) = putOrIgnore(MAPS_NAME_STR, value)

	var mapsPhone: Long?
		get() = getLong(MAPS_PHONE_LONG).let { if (it >= 1_000_000_0000) it else null }
		private set(value) = putOrIgnore(MAPS_PHONE_LONG, value)

	fun getPhone(): String? =
		mapsPhone?.let {
			val no = it.toString()
			"+${no[0]} (${no.substring(1, 4)}) ${no.substring(4, 7)}-${no.substring(7)}"
		}

	fun setPhone(phone: String) {
		mapsPhone = phone.filter { it.isDigit() }.toLongOrNull()
	}

	private var mapsId: JSONObject
		get() = getJSONObject(MAPS_ID_JSON) ?: JSONObject()
		set(value) = put(MAPS_ID_JSON, value)

	/**
	 * Returned a formatted address, including the unit number.
	 * If the unit "number" has a space, it's assumed they included a prefix (e.g., "Apt" or "Unit") and omits the '#'
	 */
	fun getAddress(delim: String = "\n"): String {
		val unitString = if (unit.isNullOrBlank()) "" else if (unit!!.contains(' ')) ", $unit" else ", #$unit"
		val addressOne = addressOne + unitString
		return listOf(addressOne, addressTwo).joinToString(delim)
	}

	var googleMapsId: String?
		get() = mapsId.optString(MAPS_GOOGLE_KEY)
		private set(value) {
			mapsId.put(MAPS_GOOGLE_KEY, value)
		}

	var hereMapsId: String?
		get() = mapsId.optString(MAPS_HERE_KEY)
		private set(value) {
			mapsId.put(MAPS_HERE_KEY, value)
		}

	fun mapInto(imageView: ImageView, width: Int = 96, height: Int = 72) {
		val mapString = String.format(
			BuildConfig.HERE_THUMB_URL,
			BuildConfig.HERE_APP_ID,
			BuildConfig.HERE_APP_CODE,
			250,
			width.dpToPx(),
			height.dpToPx(),
			15.0,
			geoPoint.latitude,
			geoPoint.longitude
		)
		Picasso.get().load(mapString).into(imageView)
	}

	fun mapLargeInto(imageView: ImageView, width: Int = 250, height: Int = 350) {
		val mapString = String.format(
			BuildConfig.HERE_THUMB_URL,
			BuildConfig.HERE_APP_ID,
			BuildConfig.HERE_APP_CODE,
			250,
			width.dpToPx(),
			height.dpToPx(),
			16.5,
			geoPoint.latitude,
			geoPoint.longitude
		)
		Picasso.get().load(mapString).into(imageView)
	}

}