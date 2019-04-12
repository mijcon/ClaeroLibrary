package com.myclaero.claerolibrary

import com.parse.*
import com.parse.ktx.getBooleanOrNull

@ParseClassName(ParseHub.NAME)
class ParseHub constructor() : ParseObject() {

    companion object {
        const val NAME = "Hub"
        const val TAG = "ParseHub"

        const val ACTIVE_BOOL = "active"
        const val GEOPOINT_GEOPOINT = "geoPoint"
        const val NAME_STR = "name"
        const val TRAVEL_INT = "travel"
    }

    val isActive: Boolean?
        get() = getBooleanOrNull(ACTIVE_BOOL) ?: false

    val geoPoint: ParseGeoPoint
        get() = getParseGeoPoint(GEOPOINT_GEOPOINT)!!

    val name: String
        get() = getString(NAME_STR)!!

    val travelTime: Int
        get() = getInt(TRAVEL_INT)

}