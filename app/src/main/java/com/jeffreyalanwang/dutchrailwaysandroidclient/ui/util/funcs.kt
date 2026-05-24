package com.jeffreyalanwang.dutchrailwaysandroidclient.ui.util

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.minus
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavDestination.Companion.hierarchy
import kotlin.reflect.KClass

fun PaddingValues.verticalOnly()
    = PaddingValues(
    top = this.calculateTopPadding(),
    bottom = this.calculateBottomPadding(),
)

fun PaddingValues.horizontalOnly()
    = this.minus(this.verticalOnly())

fun <T : Any> NavBackStackEntry.hasRoute(route: KClass<T>)
    = this.destination.hierarchy.any {
        it.hasRoute(route)
    }