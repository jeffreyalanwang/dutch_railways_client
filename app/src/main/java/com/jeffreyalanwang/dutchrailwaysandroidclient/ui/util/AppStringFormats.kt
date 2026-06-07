package com.jeffreyalanwang.dutchrailwaysandroidclient.ui.util

import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.time.temporal.TemporalAccessor
import kotlin.time.Duration
import kotlin.time.ExperimentalTime

object AppStringFormats {
    fun Time(time: TemporalAccessor)
        = DateTimeFormatter
            .ofLocalizedTime(FormatStyle.SHORT)
            .format(time)

    @OptIn(ExperimentalTime::class)
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