package com.jeffreyalanwang.dutchrailwaysandroidclient.ui.util

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.minus
import androidx.compose.runtime.Composable
import androidx.compose.runtime.saveable.rememberSerializable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.serialization.NavBackStackSerializer
import androidx.navigation3.runtime.serialization.NavKeySerializer
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.LatLngBounds
import com.jeffreyalanwang.dutchrailwaysandroidclient.Area
import com.jeffreyalanwang.dutchrailwaysandroidclient.Place
import com.jeffreyalanwang.dutchrailwaysandroidclient.Station
import com.jeffreyalanwang.dutchrailwaysandroidclient.getBounds
import com.jeffreyalanwang.dutchrailwaysandroidclient.ui.NavRoute
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn

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

@Composable
inline fun <reified T: NavRoute> rememberNavBackStack(
    vararg elements: T,
): NavBackStack<T> {
    return rememberSerializable(
        // Take advantage of the fact that NavRoute is a subclass of NavKey
        serializer = NavBackStackSerializer(NavKeySerializer()),
    ) {
        NavBackStack(*elements)
    }
}

context(viewModel: ViewModel)
private fun <T> Flow<T>.asStateWithInitialValueOf(initialValue: T)
    = this.stateIn(
        viewModel.viewModelScope,
        SharingStarted.Lazily,
        initialValue = initialValue,
    )