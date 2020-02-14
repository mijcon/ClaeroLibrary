package com.myclaero.claerolibrary.extensions

import android.content.Context
import android.content.res.Resources
import android.graphics.Bitmap
import android.net.Uri
import android.widget.EditText
import androidx.exifinterface.media.ExifInterface
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


var Calendar.timeInSecs: Long
	get() = timeInMillis / 1000L
	set(value) { timeInMillis = value * 1000L }


@Suppress("UNCHECKED_CAST")
fun <T> JSONArray.toList(): List<T> = List(length()) { this[it] as T }


fun HttpsURLConnection.readAll(): String {
	val reader = BufferedReader(InputStreamReader(this.inputStream, "UTF-8"))

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

/**
 * Retrieves a [Bitmap] from the given [Uri]. Applies rotation information to image, returning a
 * rotated image based on EXIF data.
 *
 * @param uri The Uri pointing to the Bitmap.
 * @return The rotated Bitmap.
 */
fun Context.getBitmap(uri: Uri): Bitmap? {
	try {
		// Get the Bitmap
		contentResolver.notifyChange(uri, null)
		var imgFullBitmap = android.provider.MediaStore.Images.Media.getBitmap(contentResolver, uri)

		// Open EXIF Data
		val inputStream = contentResolver.openInputStream(uri)!!
		val exifInterface = ExifInterface(inputStream)
		inputStream.close()

		// Read orientation and rotate if needed
		val orientation =
			exifInterface.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)
		imgFullBitmap = when (orientation) {
			ExifInterface.ORIENTATION_ROTATE_90 -> imgFullBitmap.rotateImage(90f)
			ExifInterface.ORIENTATION_ROTATE_180 -> imgFullBitmap.rotateImage(180f)
			ExifInterface.ORIENTATION_ROTATE_270 -> imgFullBitmap.rotateImage(270f)
			else -> imgFullBitmap
		}

		return imgFullBitmap
	} catch (e: Exception) {
		e.upload()
		return null
	}
}

infix fun Calendar.isSameDayAs(calendar: Calendar) = when {
	this[Calendar.YEAR] != calendar[Calendar.YEAR] -> false
	this[Calendar.DAY_OF_YEAR] != calendar[Calendar.DAY_OF_YEAR] -> false
	else -> true
}

