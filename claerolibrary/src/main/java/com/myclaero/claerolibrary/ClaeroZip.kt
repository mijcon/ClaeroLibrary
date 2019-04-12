package com.myclaero.claerolibrary

import com.myclaero.claerolibrary.extensions.readAll
import org.jetbrains.anko.doAsync
import org.json.JSONObject
import java.net.URL
import javax.net.ssl.HttpsURLConnection

class ClaeroZip(zip: String) {

	companion object {
	    const val TAG = "ClaeroZip"
		var json: JSONObject? = null
	}

	init {
		doAsync {
			try {
				// Open connection the ZIP Decoder API
				val zipCxn = URL(String.format(BuildConfig.REDLINE_API_URL, zip)).openConnection() as HttpsURLConnection
				zipCxn.apply {
					requestMethod = "GET"
					addRequestProperty("X-Mashape-Key", BuildConfig.MASHAPE_API_KEY)
					addRequestProperty("Accept", "application/specs")
				}

				if (zipCxn.responseCode == 200) {
					json = JSONObject(zipCxn.readAll())
				} else {
					zipCxn.disconnect()
				}
			} catch (e: Exception) {
				// Something went wrong. Give up.
				// e.upload(TAG)
			}
		}
	}

	fun asString(): String? {
		return json?.let {
			"${it.getString("city")}, ${it.getString("state")}"
		}
	}
}