@file:Suppress("FunctionName")

package com.jeffreyalanwang.dutchrailwaysandroidclient.ui.util

import com.google.android.gms.maps.model.LatLng
import com.jeffreyalanwang.dutchrailwaysandroidclient.map
import com.jeffreyalanwang.dutchrailwaysandroidclient.toParts
import kotlinx.datetime.LocalTime
import kotlinx.datetime.toJavaLocalTime
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.time.temporal.TemporalAccessor
import kotlin.math.absoluteValue
import kotlin.time.Duration

object AppStringFormats {
    fun Time(time: TemporalAccessor): String =
        DateTimeFormatter
            .ofLocalizedTime(FormatStyle.SHORT)
            .format(time)

    fun Time(time: LocalTime): String = Time(time.toJavaLocalTime())


    fun TripDuration(duration: Duration): String {
        val hours = duration.inWholeHours
        val minutes = duration.inWholeMinutes % 60

        val components = listOfNotNull(
            if (hours > 0)   "${hours}h"   else null,
            if (minutes > 0) "${minutes}m" else null,
        )

        return components.joinToString(" ")
    }

    fun LatLng(location: LatLng): String {
        val latitudeSign = if (location.latitude >= 0) "N" else "S"
        val longitudeSign = if (location.longitude >= 0) "E" else "W"

        val (latitudeStr, longitudeStr) =
            location.run { latitude to longitude }
            .map {
                // Get degrees, minutes, and seconds as Int
                val (degrees, rest1) = it.absoluteValue.toParts()
                val (minutes, rest2) = (rest1 * 60).toParts()
                val seconds = rest2.toInt()

                Triple(degrees, minutes, seconds)
            }
            .map {
                // Put into a string
                val (degrees, minutes, seconds) = it
                "${degrees}° ${minutes}' ${seconds}''"
            }

        return "$latitudeStr $latitudeSign, $longitudeStr $longitudeSign"
    }
}