package com.jeffreyalanwang.dutchrailwaysandroidclient.ui.util

import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

object AppStringFormats {
    fun Time(time: ZonedDateTime)
        = time.format(DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT))
}