package com.jeffreyalanwang.dutchrailwaysandroidclient.ui.util

import androidx.compose.animation.BoundsTransform
import androidx.compose.animation.animateBounds
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.VisibilityThreshold
import androidx.compose.animation.core.spring
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.minus
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateSetOf
import androidx.compose.runtime.saveable.rememberSerializable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.layout.LookaheadScope
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.serialization.NavBackStackSerializer
import androidx.navigation3.runtime.serialization.NavKeySerializer
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.jeffreyalanwang.dutchrailwaysandroidclient.Area
import com.jeffreyalanwang.dutchrailwaysandroidclient.Place
import com.jeffreyalanwang.dutchrailwaysandroidclient.Station
import com.jeffreyalanwang.dutchrailwaysandroidclient.getBounds
import com.jeffreyalanwang.dutchrailwaysandroidclient.ui.AppNavArgs
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn

context (lookaheadScope: LookaheadScope)
fun Modifier.animateBounds(
    modifier: Modifier = Modifier,
    boundsTransform: BoundsTransform
        = BoundsTransform { _, _ ->
            spring(
                dampingRatio = Spring.DampingRatioNoBouncy,
                stiffness = Spring.StiffnessMediumLow,
                visibilityThreshold = Rect.VisibilityThreshold,
            )
        },
    animateMotionFrameOfReference: Boolean = false,
) = animateBounds(lookaheadScope, modifier, boundsTransform, animateMotionFrameOfReference)

inline fun <reified T> Set<T>.toMutableStateSet()
    = mutableStateSetOf(*toTypedArray())

fun <K, V> Map<K, V>.toMutableStateMap()
    = mutableStateMapOf(*entries.map { (k, v) -> k to v }.toTypedArray())

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

fun LatLng.copy(
    latitude: Double = this.latitude,
    longitude: Double = this.longitude,
) = LatLng(latitude, longitude)

fun LatLng.plus(
    latitude: Double = 0.0,
    longitude: Double = 0.0,
) = LatLng(this.latitude + latitude, this.longitude + longitude)

fun LatLng.minus(
    latitude: Double = 0.0,
    longitude: Double = 0.0,
) = LatLng(this.latitude - latitude, this.longitude - longitude)

/**
 * Create a new [LatLngBounds] in which the receiver [LatLngBounds]
 * is *roughly* in the top, padded with a proportion of its height.
 */
fun LatLngBounds.paddedBelow(proportion: Float): LatLngBounds {

    val originalBoundsHeight = this.northeast.latitude - this.southwest.latitude

    val newSouthWest = this.southwest.minus(
        latitude = originalBoundsHeight * proportion
    )
    return this.including(newSouthWest)
}

fun LatLngBounds.getMapCameraUpdate(padding: Int)
    = CameraUpdateFactory.newLatLngBounds(this, padding)

/**
 * Gets [LatLngBounds] with an area > 0,
 * even when the [Place] is a [Station]
 * (i.e. located at a point).
 */
fun Place.boundsForDisplay()
    = when (this) {
        is Station -> this.geom.let {
            val latitudePadding = 1/60f
            val longitudePadding = 1/90f
            LatLngBounds(
                LatLng( // southwest
                    it.latitude - latitudePadding,
                    it.longitude - longitudePadding,
                ),
                LatLng( // northeast
                    it.latitude + latitudePadding,
                    it.longitude + longitudePadding,
                ),
            )
        }
        is Area -> this.getGeom().getBounds()
        else -> throw NotImplementedError()
    }

@Composable
inline fun <reified T: AppNavArgs> rememberNavBackStack(
    vararg elements: T,
): NavBackStack<T> {
    return rememberSerializable(
        // Take advantage of the fact that [AppNavArgs] is a subclass of [NavKey]
        serializer = NavBackStackSerializer(NavKeySerializer()),
    ) {
        NavBackStack(*elements)
    }
}

fun <T: NavKey> NavBackStack<T>.clearToInitial() {
    while (size > 1) {
        removeAt(size - 1)
    }
}

context(viewModel: ViewModel)
fun <T> Flow<T>.asStateWithInitialValueOf(initialValue: T)
    = this.stateIn(
        viewModel.viewModelScope,
        SharingStarted.Lazily,
        initialValue = initialValue,
    )