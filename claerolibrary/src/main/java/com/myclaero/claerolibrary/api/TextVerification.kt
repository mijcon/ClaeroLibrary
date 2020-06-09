package com.myclaero.claerolibrary.api

import com.myclaero.claerolibrary.BuildConfig
import com.parse.ParseConfig
import com.parse.ParseException
import com.parse.ParseUser
import khttp.post
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.properties.Delegates

object TextVerification {

	const val REQUEST_CODE_VERIFY_TEXT = 177
	const val ACTION_CODE_RECEIVED = "com.myclaero.claerolibrary.SMS_AUTO_VERIFY"
	const val EXTRA_PHONE = "phoneNumber"
	const val EXTRA_CODE = "code"
	const val RESPONSE_KEY_SESSION_TOKEN = "sessionToken"

	private val url = BuildConfig.CLAERO_API_URL + "/verify/phone"

	private val headers = mapOf(
		"X-Api-Key" to ParseConfig.getCurrentConfig().getString("claero_api_key", BuildConfig.CLAERO_API_KEY)
	)

	/**
	 * The phone number to be verified.
	 */
	private var phone: String? by Delegates.observable<String?>(null) { prop, old, new ->
		if (old != new) {
			verificationId = null
			token = null
			isVerified = false
		}
	}

	/**
	 * The objectId for the Verification object stored on the server.
	 */
	private var verificationId: String? = null

	/**
	 * The AppSpecificSmsToken being used for the verification process.
	 *
	 * Only applies to Android Oreo and above.
	 */
	private var token: String? = null

	/**
	 * The verification status of the number.
	 */
	var isVerified = false
		private set

	fun requestVerificationCode(phone: String, token: String? = null): Exception? {
		this.phone = phone
		this.token = token

		val data = mapOf(
			"phone" to phone,
			"token" to token
		)
		val request = post(url, json = data, headers = headers)
		return request.jsonObject
	}

	fun requestVerificationCodeAsync(phone: String, token: String? = null, callback: (objectId: String?, e: Exception?) -> Unit) {

	}

	fun checkVerificationCode(code: String): ParseUser {
		val data = mapOf(
			"token" to token,
			"code" to code
		)
		val request = post(url, json = data, headers = headers)
		return ParseUser.become(request.jsonObject.getString(RESPONSE_KEY_SESSION_TOKEN))
	}

	fun checkVerificationCodeAsync(code: String) {
		GlobalScope.launch(Dispatchers.Main) {
			try {
				val user = withContext(Dispatchers.IO) { checkVerificationCode(code) }
				callback.done(user, null)
			} catch (e: ParseException) {
				callback.done(null, e)
			}
		}
	}

}