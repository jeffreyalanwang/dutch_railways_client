package com.jeffreyalanwang.dutchrailwaysandroidclient

import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

object UIStrings {
    fun Time(time: ZonedDateTime)
        = time.format(DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT))
}