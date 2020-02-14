package com.myclaero.claerolibrary.extensions

import com.myclaero.claerolibrary.BuildConfig
import com.parse.ParseException
import com.parse.ParseObject
import java.io.PrintWriter
import java.io.StringWriter

const val EXCEPTION_CLASS = "Error"
const val APPLICATION_ID_STR = "applicationId"
const val BUILD_TYPE_STR = "buildType"
const val FLAVOR_STR = "flavor"
const val VERSION_CODE_INT = "versionCode"
const val VERSION_NAME_STR = "versionName"
const val MESSAGE_STR = "message"
const val MESSAGE_LOCAL_STR = "localizedMessage"
const val STACKTRACE_STR = "stacktrace"
const val PARSE_CODE_INT = "code"

/**
 * Creates and uploads the Exception to our ParseServer.
 *
 * @return If true, this Exception was uploaded. If false, it was an Exception that isn't needed
 * to be uploaded (such as [java.io.IOException], or [ParseException.CONNECTION_FAILED]).
 */
fun Exception.upload(): Exception? {

	// Going to ignore connection errors... Not really something we need to report
	if (this is ParseException && code != ParseException.CONNECTION_FAILED) return null

	// Build a String of the Stacktrace
	val sw = StringWriter()
	this.printStackTrace(PrintWriter(sw))

	// Construct and save a new Error object
	ParseObject(EXCEPTION_CLASS).putAll(
		APPLICATION_ID_STR to BuildConfig.LIBRARY_PACKAGE_NAME,
		BUILD_TYPE_STR to BuildConfig.BUILD_TYPE,
		FLAVOR_STR to BuildConfig.FLAVOR,
		VERSION_CODE_INT to BuildConfig.VERSION_CODE,
		VERSION_NAME_STR to BuildConfig.VERSION_NAME,
		MESSAGE_STR to message,
		MESSAGE_LOCAL_STR to localizedMessage,
		STACKTRACE_STR to sw.toString(),
		PARSE_CODE_INT to if (this is ParseException) this.code else -1
	).saveEventually()

	return this
}
