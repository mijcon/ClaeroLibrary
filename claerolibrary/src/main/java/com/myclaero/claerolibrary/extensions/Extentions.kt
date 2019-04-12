package com.myclaero.claerolibrary.extensions

import android.content.res.Resources
import android.graphics.Bitmap
import android.graphics.Matrix
import android.widget.EditText
import com.myclaero.claerolibrary.BuildConfig
import com.parse.ParseException
import com.parse.ParseObject
import com.parse.ParseQuery
import com.parse.ParseUser
import com.parse.ktx.putOrIgnore
import org.json.JSONArray
import java.io.*
import java.text.SimpleDateFormat
import java.util.*
import javax.net.ssl.HttpsURLConnection

/**
 * Shorthand handling of EditText Strings.
 */
fun EditText.getString(): String {
	return text.toString()
}


/**
 * Sends information provided and the Exception itself to the Parse Server for error-reporting
 */
fun Exception.uploadAsync(loc: String, note: String? = null) {
	if (this !is ParseException || code != ParseException.CONNECTION_FAILED) {
		val sw = StringWriter()
		this.printStackTrace(PrintWriter(sw))

		ParseObject("Error").apply {
			put("codeSection", loc)
			put("owner", ParseUser.getCurrentUser())
			put("stackTrace", sw.toString())
			put("platform", "Android")
			put("version", BuildConfig.VERSION_CODE)
			putOrIgnore("extra", note)
			putOrIgnore("message", message)
			putOrIgnore("parseCode", if (this@uploadAsync is ParseException) code else null)
			saveEventually()
		}
	}
}

var Calendar.timeInSecs: Long
	get() = timeInMillis / 1000
	set(value) { timeInMillis = value * 1000 }

fun Exception.upload(loc: String, note: String? = null) {
	if (this !is ParseException || code != ParseException.CONNECTION_FAILED) {
		val sw = StringWriter()
		this.printStackTrace(PrintWriter(sw))

		ParseObject("Error").apply {
			put("codeSection", loc)
			put("owner", ParseUser.getCurrentUser())
			put("stackTrace", sw.toString())
			put("platform", "Android")
			put("version", BuildConfig.VERSION_CODE)
			putOrIgnore("extra", note)
			putOrIgnore("message", message)
			putOrIgnore("parseCode", if (this@upload is ParseException) code else null)
			save()
		}
	}
}

@Suppress("UNCHECKED_CAST")
fun <T> JSONArray.toList(): List<T> = List<T>(length()) { this[it] as T }


fun HttpsURLConnection.readAll(): String {
	val reader = java.io.BufferedReader(java.io.InputStreamReader(this.inputStream, "UTF-8"))

	var line = reader.readLine()
	var result = ""

	while (line != null) {
		result += line
		line = reader.readLine()
	}

	reader.close()
	disconnect()
	return result
}

fun Bitmap.rotateImage(degrees: Float): Bitmap {
	val matrix = Matrix()
	matrix.postRotate(degrees)
	return Bitmap.createBitmap(this, 0, 0, this.width, this.height, matrix, true)
}

fun Int.dpToPx(): Int {
	val scale = Resources.getSystem().displayMetrics.density
	return (this * scale + 0.5f).toInt()
}

fun HttpsURLConnection.setData(data: String) {
	val outputStream = BufferedOutputStream(outputStream)
	BufferedWriter(OutputStreamWriter(outputStream, "utf-8")).run {
		write(data)
		flush()
		close()
	}
	outputStream.close()
}

fun Date.getTimeString(halfHours: Int): String {
	val millisInTimeBlock = 1000 * 60 * 30
	val formatDate = SimpleDateFormat("EEEE, MMMM d", Locale.getDefault())
	val formatTime = SimpleDateFormat("h:mm a", Locale.getDefault())

	// Get start and end times
	val dateEnd = Date().also {
		it.time = (this.time + halfHours * millisInTimeBlock)
	}

	val stringStart = formatTime.format(this)
	val stringEnd = formatTime.format(dateEnd)
	val stringDate = formatDate.format(this).let {
		when {
			it[it.length - 2] == '1' -> it + "th"
			it.last() == '1' -> it + "st"
			it.last() == '2' -> it + "nd"
			it.last() == '3' -> it + "rd"
			else -> it + "th"
		}
	}

	return "$stringDate\n$stringStart \u2013 $stringEnd"
}


@Suppress("NOTHING_TO_INLINE", "unused")
inline fun <T : ParseObject> ParseQuery<T>.getFirstOrNull(): T? = try {
	first
} catch (e: ParseException) {
	if (e.code == ParseException.OBJECT_NOT_FOUND) null else throw e
}