package com.jeffreyalanwang.dutchrailwaysandroidclient.ui.util

import com.jeffreyalanwang.dutchrailwaysandroidclient.ServiceStop
import kotlinx.datetime.LocalTime
import kotlinx.datetime.toJavaLocalTime
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.time.temporal.TemporalAccessor
import kotlin.time.Duration

object AppStringFormats {
    fun Time(time: TemporalAccessor)
        = DateTimeFormatter
            .ofLocalizedTime(FormatStyle.SHORT)
            .format(time)

    fun Time(time: LocalTime)
        = Time(time.toJavaLocalTime())


    fun TripDuration(duration: Duration): String {
        val hours = duration.inWholeHours
        val minutes = duration.inWholeMinutes % 60

        val components = listOfNotNull(
            if (hours > 0)   "${hours}h"   else null,
            if (minutes > 0) "${minutes}m" else null,
        )

        return components.joinToString(" ")
    }

    fun passServiceTitle(
        oldTitle: String?,
        lastStop: ServiceStop?,
    ): String {
        val trainName = oldTitle
                ?.indexOf(" to ") // "Intercity 2398 to Rotterdam Centraal"
                ?.let { index ->
                    if (index < 0) oldTitle
                    else oldTitle.substring(0, index)
                }
                ?: "Train"

        return lastStop
            ?.getStation()
            ?.run { "$trainName to $name" }
            ?: trainName
    }
}