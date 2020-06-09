package com.myclaero.claerolibrary.extensions

import android.graphics.Bitmap
import com.parse.ParseObject
import com.parse.ktx.putOrRemove

internal fun ParseObject.putAll(vararg data: Pair<String, *>): ParseObject {
	data.forEach { putOrRemove(it.first, it.second) }
	return this
}

internal fun ParseObject.putAll(map: MutableMap<String, *>): ParseObject {
	map.forEach { putOrRemove(it.key, it.value) }
	return this
}

internal inline fun <reified E: ParseObject> ParseObject.getSet(key: String): Set<E> =
	getList<E>(key)?.toSet() ?: setOf()

interface GetBitmapCallback {
	fun done(bitmap: Bitmap?, e: Exception?)
}
