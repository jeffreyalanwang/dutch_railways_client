package com.jeffreyalanwang.dutchrailwaysandroidclient.ui.util

import android.R.attr.x
import android.R.attr.y
import android.util.DisplayMetrics
import androidx.compose.animation.BoundsTransform
import androidx.compose.animation.animateBounds
import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.VisibilityThreshold
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.minus
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableStateSetOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSerializable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.LookaheadScope
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.layout
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.roundToIntRect
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
import com.jeffreyalanwang.dutchrailwaysandroidclient.from
import com.jeffreyalanwang.dutchrailwaysandroidclient.getBounds
import com.jeffreyalanwang.dutchrailwaysandroidclient.ui.AppNavArgs
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.stateIn
import kotlin.math.max

fun Constraints.copy(
    height: Int? = null,
    width: Int? = null,
) = this.copy(
    minHeight = height ?: this.minHeight,
    maxHeight = height ?: this.maxHeight,
    minWidth = width ?: this.minWidth,
    maxWidth = width ?: this.maxWidth,
)

context(density: Density)
fun DpOffset.toPx()
    = with(density) { Offset(x.toPx(), y.toPx()) }

context(density: Density)
fun IntSize.toDp()
    = with(density) { DpSize(x.toDp(), y.toDp()) }

val Color.Companion.Gold get() = Color(239, 191, 4)

fun Modifier.providesWindowInsets(block: (WindowInsets) -> Unit)
    = onGloballyPositioned {
        val bounds = it
            .boundsInWindow(clipBounds = true)
            .roundToIntRect()

        val insets = with (bounds) {
            WindowInsets(
                left = left,
                top = top,
                right = width - right,
                bottom = height - bottom,
            )
        }

        block(insets)
    }

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

infix operator fun IntRange.contains(other: IntRange)
    = this.first <= other.first && this.last >= other.last

@Composable
fun WindowInsets.asRectInWindow(density: Density, displayMetrics: DisplayMetrics): IntRect {
    val width = displayMetrics.widthPixels
    val height = displayMetrics.heightPixels

    return IntRect(
        left = this.getLeft(density = density, LayoutDirection.Ltr),
        right = width - getRight(density = density, LayoutDirection.Ltr),
        top = getTop(density),
        bottom = height - getBottom(density),
    )
}

val IntRange.size: Int
    get() = endInclusive - start + 1

/** Retain size and translate into bounds. */
fun IntRange.movedInto(bounds: IntRange, overflowStart: Int = 0): IntRange {
    if (this.size > bounds.size) {
        return IntRange.from(overflowStart, size = this.size)
    }
    if (this.first < bounds.first) {
        return IntRange.from(bounds.first, size = this.size)
    }
    if (this.last > bounds.last) {
        return IntRange.from(bounds.last - this.size + 1, size = this.size)
    }
    return this
}

val IntRect.xRange: IntRange
    get() = left..<right

val IntRect.yRange: IntRange
    get() = top..<bottom

infix operator fun IntRect.contains(other: IntRect)
    = this.xRange contains other.xRange && this.yRange contains other.yRange

fun IntRect.translatedTo(left: Int, top: Int)
    = this.copy(
        left = left,
        top = top,
        right = left + width,
        bottom = top + height,
    )

/**
 * Return [this], translated to fit in [bounds].
 * If [this] is too large to fit in [bounds] along a dimension,
 *     move its top-left to [xOverflow] or [yOverflow].
 */
context(layoutDirection: LayoutDirection)
fun IntRect.movedInto(
    bounds: IntRect,
    overflowX: Alignment.Horizontal,
    overflowY: Alignment.Vertical,
) = translatedTo(
    left =
        xRange
        .movedInto(
            bounds.xRange,
            overflowStart =
                bounds.left +
                overflowX.align(this.width, bounds.width, layoutDirection),
        )
        .first,
    top =
        yRange
        .movedInto(
            bounds.yRange,
            overflowY.align(this.height, bounds.height),
        )
        .first
)

fun IntRect.movedInto(
    bounds: IntRect,
    xOverflow: Int = 0,
    yOverflow: Int = 0,
) = if (this in bounds) this
    else this.translatedTo(
        left = xRange.movedInto(bounds.xRange, overflowStart = xOverflow).first,
        top = yRange.movedInto(bounds.yRange, overflowStart = yOverflow).first,
    )

/**
 * Similar to [Modifier.offset()], but also changes the size it takes up in
 * the parent layout; this way, items further along a row or column are also
 * shifted to preserve the original gap size.
 */
@Composable
fun Modifier.shift(x: Dp = 0.dp, y: Dp = 0.dp)
    = this.layout { measurable, constraints ->
        val placeable = measurable.measure(constraints)
        layout(
            width = max(0, placeable.width + x.roundToPx()),
            height = max(0, placeable.height + y.roundToPx()),
        ) {
            placeable.placeRelative(x.roundToPx(), y.roundToPx())
        }
    }

@Composable
fun animateIntOffsetAsState(
    targetValue: IntOffset,
    animationSpecs: Pair<AnimationSpec<Int>, AnimationSpec<Int>> =
        spring(visibilityThreshold = Int.VisibilityThreshold)
            .let { it to it },
    label: String = "IntOffsetAnimation",
    finishedListener: ((IntOffset) -> Unit)? = null,
): State<IntOffset> {
    val (targetX, targetY) = targetValue

    var doneAnimatingX by remember { mutableStateOf(false) }
    var doneAnimatingY by remember { mutableStateOf(false) }

    LaunchedEffect(targetX) { doneAnimatingX = false }
    LaunchedEffect(targetY) { doneAnimatingY = false }

    lateinit var onFinished: () -> Unit
    val currX by
        animateIntAsState(targetValue.x, animationSpecs.first, "${label}X") {
            doneAnimatingX = true
            if (doneAnimatingY) onFinished()
        }
    val currY by
        animateIntAsState(targetValue.y, animationSpecs.second, "${label}Y") {
            doneAnimatingY = true
            if (doneAnimatingX) onFinished()
        }

    val outState = remember { derivedStateOf { IntOffset(currX, currY) } }
    val out by outState

    onFinished = finishedListener
        ?.let {
            { finishedListener.invoke(out) }
        }
        ?: {}

    return outState
}

/** [LaunchedEffect] that ignores its first key. */
@Composable
fun OnChangeEffect(key: Any?, block: suspend CoroutineScope.() -> Unit) {
    val currentKey by rememberUpdatedState(key)
    LaunchedEffect(Unit) {
        snapshotFlow { currentKey }
            .drop(1)
            .collect { block() }
    }
}