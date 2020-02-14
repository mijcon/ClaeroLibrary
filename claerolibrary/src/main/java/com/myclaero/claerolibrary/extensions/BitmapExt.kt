package com.myclaero.claerolibrary.extensions

import android.graphics.Bitmap
import android.graphics.Matrix
import com.parse.ParseException
import com.parse.ParseFile
import java.io.ByteArrayOutputStream
import java.io.IOException

/**
 * A convenience function that converts the receiving [Bitmap] into a ByteArray and *synchronously*
 * uploads it to the Parse Server.
 *
 * @return The new ParseFile, after it has been successfully saved to the Parse Server.
 * @throws IOException
 * @throws ParseException
 */
fun Bitmap.upload(): ParseFile =
	ByteArrayOutputStream().let {
		compress(Bitmap.CompressFormat.PNG, 100, it)
		it.toByteArray()
	}.let {
		ParseFile("image.png", it).apply { save() }
	}

/**
 * A function which creates a resized [Bitmap] from the receiving Bitmap, based on the width and
 * height parameters.
 *
 * @param width The desired width of the new Bitmap, in pixels.
 * @param height The desired height of the new Bitmap, in pixels.
 * @return The resized Bitmap.
 * @throws IllegalArgumentException
 */
fun Bitmap.resize(width: Int, height: Int): Bitmap {
	// Create downsized image
	val scaleWidth = width / this.width.toFloat()
	val scaleHeight = height / this.height.toFloat()
	val scale = if (scaleWidth > scaleHeight) scaleHeight else scaleWidth
	return Matrix().let {
		it.postScale(scale, scale)
		Bitmap.createBitmap(this, 0, 0, this.width, this.height, it, false)
	}
}

/**
 * A function which rotates the given [Bitmap] by the given degrees.
 *
 * @param degrees The amount of clockwise rotation to apply.
 * @return The newly-rotated Bitmap.
 * @throws IllegalArgumentException
 */
fun Bitmap.rotateImage(degrees: Float): Bitmap =
	Matrix().let {
		it.postRotate(degrees)
		return Bitmap.createBitmap(this, 0, 0, this.width, this.height, it, true)
	}
