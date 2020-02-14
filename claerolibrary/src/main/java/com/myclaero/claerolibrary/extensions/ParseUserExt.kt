package com.myclaero.claerolibrary.extensions

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.telephony.PhoneNumberUtils
import com.myclaero.claerolibrary.ClaeroAPI
import com.parse.ParseUser
import com.parse.ktx.getIntOrNull
import com.parse.ktx.putOrIgnore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async
import kotlinx.coroutines.launch

private const val PHONE_STR = "phone"
private const val FAMILY_NAME_STR = "familyName"
private const val GIVEN_NAME_STR = "givenName"
private const val EMAIL_VERIFIED_BOOL = "emailVerified"
private const val PHONE_VERIFIED_BOOL = "phoneVerified"
private const val PROFILE_FILE = "profileImage"

enum class Verified {
    NEITHER,
    EMAIL,
    PHONE,
    BOTH
}

fun ParseUser.updateEmail(email: String): Boolean {
    val change = this.email.trim() != email.trim()
    if (change) this.email = email.trim()
    return change
}

fun ParseUser.isProfileComplete(): Boolean {
    val phone = getIntOrNull("phone") ?: 0
    val givenName = getString("givenName")
    val familyName = getString("familyName")

    return !(phone == 0 || givenName == "" || familyName == "" || email.isNullOrBlank())
}

fun ParseUser.getProfileAsync(callback: (image: Bitmap?, e: Exception?) -> Unit) {
    getParseFile(PROFILE_FILE)?.getDataInBackground { data, e ->
        val thumbnail = data?.let { BitmapFactory.decodeByteArray(data, 0, it.size) }
        callback(thumbnail, e)
    } ?: callback(null, null)
}

fun ParseUser.setProfileAsync(context: Context, uri: Uri): Bitmap {
    val imgFullBitmap = context.getBitmap(uri)
    val imgThumbBitmap = imgFullBitmap.resize(128, 128)
    GlobalScope.launch(Dispatchers.Main) {
        val thumbFile = async(Dispatchers.IO) { imgThumbBitmap.upload() }
        put(PROFILE_FILE, thumbFile.await())
        saveInBackground()
    }
    return imgThumbBitmap
}

var ParseUser.thumbnail: Bitmap?
    get() = getParseFile(PROFILE_FILE)?.data?.let { BitmapFactory.decodeByteArray(it, 0, it.size) }
    set(value) = put(PROFILE_FILE, value!!)

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
    get() = getBoolean(EMAIL_VERIFIED_BOOL)

var ParseUser.familyName: String?
    get() = getString(FAMILY_NAME_STR)
    set(value) = putOrIgnore(FAMILY_NAME_STR, value)

var ParseUser.givenName: String?
    get() = getString(GIVEN_NAME_STR)
    set(value) = putOrIgnore(GIVEN_NAME_STR, value)

fun ParseUser.setPhone(phone: String): Boolean {
    val num = phone.filter { it.isDigit() }.toLongOrNull()
    val change = num != this.phone
    if (change) putOrIgnore(PHONE_LONG, phone.filter { it.isDigit() }.toLongOrNull())
    return change
}

val ParseUser.phone: String?
    get() = getString(PHONE_STR)?.let { "+" + PhoneNumberUtils.formatNumber(it, "US") }
