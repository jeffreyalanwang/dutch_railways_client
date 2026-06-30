@file:Suppress("FunctionName")

package com.jeffreyalanwang.dutchrailwaysandroidclient.ui.util

import kotlinx.datetime.LocalTime
import kotlinx.datetime.toJavaLocalTime
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.time.temporal.TemporalAccessor
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
}