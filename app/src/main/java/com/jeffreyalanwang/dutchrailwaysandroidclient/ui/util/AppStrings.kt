package com.jeffreyalanwang.dutchrailwaysandroidclient.ui.util

import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

object AppStrings {
    fun Time(time: ZonedDateTime)
        = time.format(DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT))
}