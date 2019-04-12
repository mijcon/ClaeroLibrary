package com.myclaero.claerolibrary.extensions

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import com.myclaero.claerolibrary.BuildConfig
import com.myclaero.claerolibrary.ClaeroAPI
import com.parse.ParseException
import com.parse.ParseUser
import com.parse.ktx.getBooleanOrNull
import com.parse.ktx.getIntOrNull
import com.parse.ktx.getLongOrNull
import com.parse.ktx.putOrIgnore
import org.jetbrains.anko.doAsync
import org.jetbrains.anko.uiThread
import java.net.URL
import javax.net.ssl.HttpsURLConnection

private const val PARSE_RESEND_EMAIL = BuildConfig.PARSE_API_URL + "/verificationEmailRequest"
private const val PHONE_LONG = "phone"
private const val FAMILY_NAME_STR = "familyName"
private const val GIVEN_NAME_STR = "givenName"
private const val EMAIL_VERIFIED_BOOL = "emailVerified"
private const val PHONE_VERIFIED_BOOL = "phoneVerified"
private const val THUMB_FILE = "thumbnail"

enum class Verified {
	NEITHER,
	EMAIL,
	PHONE,
	BOTH
}

fun ParseUser.updateEmail(email: String) {
	if (this.email.trim() != email.trim()) {
		this.email = email.trim()
	}
}

fun ParseUser.isProfileComplete(): Boolean {
	val phone = getIntOrNull("phone") ?: 0
	val givenName = getString("givenName")
	val familyName = getString("familyName")

	return !(phone == 0 || givenName == "" || familyName == "" || email.isNullOrBlank())
}

fun ParseUser.getThumbnailAsync(callback: (image: Bitmap?, e: Exception?) -> Unit) {
	getParseFile(THUMB_FILE)?.getDataInBackground { data, e ->
		val thumbnail = data?.let { BitmapFactory.decodeByteArray(data, 0, it.size) }
		callback(thumbnail, e)
	} ?: callback(null, null)
}

var ParseUser.thumbnail: Bitmap?
	get() = getParseFile(THUMB_FILE)?.data?.let { BitmapFactory.decodeByteArray(it, 0, it.size) }
	set(value) = put(THUMB_FILE, value!!)

/**
 * Contacts the Parse Server to verify whether email and phone number are verified.
 * @param callback  A lambda that defaults to true for both fields if an exception is thrown.
 */
fun ParseUser.checkVerificationAsync(callback: (email: Boolean, phone: Boolean, e: Exception?) -> Unit) {
	doAsync {
		try {
			val status = ClaeroAPI.getVerificationStatus(this@checkVerificationAsync)
			val email = status == Verified.EMAIL || status == Verified.BOTH
			val phone = status == Verified.PHONE || status == Verified.BOTH
			uiThread { callback(email, phone, null) }
		} catch (e: Exception) {
			uiThread { callback(true, true, e) }
		}
	}
}

fun ParseUser.checkVerification(): Verified = ClaeroAPI.getVerificationStatus(this)

fun ParseUser.sendTextTokenAsync(token: String? = null, callback: ((e: Exception?) -> Unit)? = null) {
	doAsync {
		try {
			val response = ClaeroAPI.verifyText(objectId, token)
			uiThread { callback?.invoke(null) }
		} catch (e: Exception) {
			uiThread { callback?.invoke(e) }
		}
	}
}

fun ParseUser.sendTextToken(token: String? = null): Boolean {
	val response = ClaeroAPI.verifyText(objectId, token)
	return if (response.has("success")) response.getBoolean("success") else false
}

fun ParseUser.checkTextTokenAsync(code: String, callback: ((matches: Boolean, e: Exception?) -> Unit)? = null) {
	doAsync {
		try {
			val response = ClaeroAPI.verifyText(objectId, null, code)
			val matches = response.getBoolean("match")
			uiThread { callback?.invoke(matches, null) }
		} catch (e: Exception) {
			uiThread { callback?.invoke(false, e) }
		}
	}
}

val ParseUser.emailVerified: Boolean
	get() = getBooleanOrNull(EMAIL_VERIFIED_BOOL) ?: false

var ParseUser.phone: Long?
	get() = getLongOrNull(PHONE_LONG)
	set(value) {
		if (phone != value) {
			putOrIgnore(PHONE_LONG, value)
			put(PHONE_VERIFIED_BOOL, false)
		}
	}

var ParseUser.familyName: String?
	get() = getString(FAMILY_NAME_STR)
	set(value) = putOrIgnore(FAMILY_NAME_STR, value)

var ParseUser.givenName: String?
	get() = getString(GIVEN_NAME_STR)
	set(value) = putOrIgnore(GIVEN_NAME_STR, value)

fun ParseUser.setPhone(phone: String) {
	val num = phone.filter { it.isDigit() }
	if (this.getPhone() != num) {
		putOrIgnore(PHONE_LONG, num.toLongOrNull())
		put(PHONE_VERIFIED_BOOL, false)
	}
}

fun ParseUser.getPhone(): String = getLongOrNull(PHONE_LONG)?.toString() ?: ""

/**
 * Makes a REST API request to Parse Server to re-send a new verification email to the email address on file.
 * Returns server's responseCode if API was called, or returns '-1' if email has already been verified.
 */
fun ParseUser.resendEmailVerificationInBackground(callback: ((responseCode: Int) -> Unit)? = null) {
	var response = -1

	doAsync {
		val resendUrl = URL(PARSE_RESEND_EMAIL)
		val resendCxn = (resendUrl.openConnection() as HttpsURLConnection).apply {
			requestMethod = "POST"
			addRequestProperty("X-Parse-Application-Id", BuildConfig.PARSE_APP_ID)
			addRequestProperty("Content-Type", "application/json")
			setData("{\"email\":\"$email\"}")
		}
		response = resendCxn.responseCode
		uiThread {
			callback?.invoke(response)
		}
	}
}

fun ParseUser.registerInBackground(callback: ((success: Boolean, e: ParseException?) -> Unit)? = null) {
	doAsync {
		try {
			signUp()
			val registerUrl = URL(String.format(ClaeroAPI.CLAERO_CLIENT, ParseUser.getCurrentUser().objectId))
			val claeroCxn = (registerUrl.openConnection() as HttpsURLConnection).apply {
				requestMethod = "POST"
				addRequestProperty("x-api-key", BuildConfig.CLAERO_API_KEY)
				addRequestProperty("content-type", "application/json")
			}
			val responseCode = claeroCxn.responseCode
			if (responseCode != 200) Exception().upload(
				"ParseUserExt",
				"Unable to register Stripe Customer: $claeroCxn.responseMessage"
			)
			uiThread { callback?.invoke(responseCode == 200, null) }
		} catch (e: ParseException) {
			uiThread { callback?.invoke(false, e) }
		}
	}
}