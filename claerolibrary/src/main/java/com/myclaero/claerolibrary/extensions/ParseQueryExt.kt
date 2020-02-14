package com.myclaero.claerolibrary.extensions

import com.parse.ParseException
import com.parse.ParseObject
import com.parse.ParseQuery

val <T: ParseObject> ParseQuery<T>.firstOrNull : ParseObject?
	get() = try {
		first
	} catch (e: ParseException) {
		if (e.code == ParseException.OBJECT_NOT_FOUND) null else throw e
	}