package com.myclaero.claerolibrary.extensions

import com.parse.ParseObject
import com.parse.ktx.putOrRemove

fun ParseObject.putAll(vararg data: Pair<String, *>): ParseObject {
	data.forEach { putOrRemove(it.first, it.second) }
	return this
}

fun ParseObject.putAll(map: MutableMap<String, *>): ParseObject {
	map.forEach { putOrRemove(it.key, it.value) }
	return this
}