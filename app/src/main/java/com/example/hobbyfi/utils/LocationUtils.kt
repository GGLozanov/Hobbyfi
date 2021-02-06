package com.example.hobbyfi.utils

import com.example.hobbyfi.R
import android.content.Context
import android.location.Location
import java.text.DateFormat
import java.util.*


object LocationUtils {
    fun getLocationText(location: Location?): String = if (location == null) "Unknown location" else "(" + location.getLatitude()
            .toString() + ", " + location.longitude.toString() + ")"

    fun getLocationTitle(context: Context): String = context.getString(
            R.string.location_updated,
            DateFormat.getDateTimeInstance().format(Date())
        )
}