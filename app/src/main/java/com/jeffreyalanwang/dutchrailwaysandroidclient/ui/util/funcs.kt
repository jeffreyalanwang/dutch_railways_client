package com.jeffreyalanwang.dutchrailwaysandroidclient.ui.util

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.minus
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavDestination.Companion.hierarchy
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.LatLngBounds
import com.jeffreyalanwang.dutchrailwaysandroidclient.Area
import com.jeffreyalanwang.dutchrailwaysandroidclient.Place
import com.jeffreyalanwang.dutchrailwaysandroidclient.Station
import com.jeffreyalanwang.dutchrailwaysandroidclient.getBounds
import kotlin.reflect.KClass

fun PaddingValues.topOnly()
    = PaddingValues(
        top = this.calculateTopPadding(),
    )

fun PaddingValues.bottomOnly()
    = PaddingValues(
        bottom = this.calculateBottomPadding(),
    )

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

fun LatLngBounds.getMapCameraUpdate(padding: Int)
        = CameraUpdateFactory.newLatLngBounds(this, padding)

fun Place.getMapCameraUpdate()
    = when (this) {
        is Station -> CameraUpdateFactory.newLatLng(this.geom)
        is Area -> CameraUpdateFactory.newLatLngBounds(
            this.getGeom().getBounds(),
            12
        )

        else -> throw NotImplementedError()
    }