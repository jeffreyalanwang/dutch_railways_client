package com.jeffreyalanwang.dutchrailwaysandroidclient.ui.components

import androidx.annotation.FloatRange
import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.layout.layout
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.round
import androidx.navigationevent.NavigationEvent
import com.jeffreyalanwang.dutchrailwaysandroidclient.interpolates
import com.jeffreyalanwang.dutchrailwaysandroidclient.toIntPercent
import com.jeffreyalanwang.dutchrailwaysandroidclient.ui.util.displaySize
import com.jeffreyalanwang.dutchrailwaysandroidclient.ui.util.interpolates

/**
 * A component that switches between a normal box and a fullscreen popup.
 *
 * Animates in/out as a hero animation, including on predictive back.
 *
 * Does not move [content] around in the composition tree, allowing the use
 * of external [android.view.View]s without flicker.
 *
 * Make sure to use [modifier] or [collapsedSize] to constrain collapsed
 * size; otherwise, content will try to measure against infinity and throw.
 *
 * @param modifier          Size constraints wrap around node which
 *                          overrides size for expansion.
 * @param onDismissRequest  Responsible for setting [isExpanded].
 * @param collapsedSize     Size to which this composable should be measured.
 *                          Recommended if the composable will be
 *                          initially expanded.
 * @param expandedZIndex    Set to ensure this composable is drawn above
 *                          other items when it is expanded; when collapsed,
 *                          it defaults to 0f.
 *                          Note: zIndex only applies relatively to the nearest
 *                          ancestor Layout composable.
 */
@Composable
fun ExpandingHeroBox(
    isExpanded: Boolean,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    collapsedSize: (Constraints) -> IntSize = { IntSize(it.maxWidth, it.maxHeight) },
    expandedZIndex: Float = 100f,
    @FloatRange(0.0, 1.0) collapsedCornerRounding: Float = 0f,
    content: @Composable () -> Unit,
) {
    // [progress] should ideally only be read inside the MeasureScope and
    // GraphicsLayerScope lambdas, in order to prevent excessive recompositions.
    //
    // Value is a little counterintuitive because it matches
    // back-progress Animatable in [PredictiveBackBox].
    //  0f: totally expanded
    //  1f: totally collapsed
    val progressState = remember { Animatable(initialValue = 1f) }

    // [PredictiveBackBox] changes [isExpanded] once a back operation completes.
    // If [isExpanded] is programmatically changed, animation is performed here.
    LaunchedEffect(isExpanded) {
        if (isExpanded) progressState.animateTo(0f)
        else progressState.animateTo(1f)
    }

    // Offsets are from the [PredictiveBackBox]'s layout position before
    // the [layout] Modifier node overrides it.
    val expandedSize = LocalResources.current.displaySize
    var expandedOffset by remember { mutableStateOf(IntOffset.Zero) }
    var collapsedSizeCached by remember { mutableStateOf(IntSize.Zero) } // only used for [minScale], below
    val collapsedOffset = IntOffset.Zero

    val minScale by remember { derivedStateOf {
        maxOf (
            .5f,
            .8f * collapsedSizeCached.width / expandedSize.width,
            .8f * collapsedSizeCached.height / expandedSize.height,
        )
    } }

    PredictiveBackBox(
        content = content,
        snapBackProgressTo = progressState::snapTo,
        animateBackProgressTo = progressState::animateTo,
        backHandlingEnabled = isExpanded,
        onDismissRequest = onDismissRequest,
        modifier =
            modifier
                .onGloballyPositioned {
                    // This modifier element is applied to the outer node,
                    // which is sized as collapsed even when the child layout
                    // node (below) overrides size + offset.
                    expandedOffset = it.positionInWindow().round().unaryMinus()
                }
                .layout { measurable, constraints ->
                    val progressValue by progressState.asState()

                    // Here, we animate the hero popout/hide.
                    // This code only animates values for [progressValue > .5].

                    val (width, height) =
                        collapsedSize(constraints)
                            .let {
                                progressValue interpolates
                                        Triple(expandedSize, expandedSize, it)
                            }
                            .also {
                                collapsedSizeCached = it
                            }

                    val placeable =
                        measurable.measure(Constraints.fixed(width, height))
                    layout(width, height) {

                        placeable.place(
                            progressValue interpolates
                                    Triple(
                                        expandedOffset,
                                        expandedOffset,
                                        collapsedOffset
                                    ),
                            zIndex = if (isExpanded) expandedZIndex else 0f,
                        )
                    }
                }
    ) { swipeEdge ->
        val progressValue by progressState.asState()

        // Here, we animate the back gesture.
        // These effects animate in reverse when [progressValue > .5].

        transformOrigin = TransformOrigin(
            pivotFractionX =
                when (swipeEdge) {
                    NavigationEvent.EDGE_LEFT -> 1f
                    NavigationEvent.EDGE_RIGHT -> 0f
                    else -> 0.5f
                }
                .let { progressValue interpolates Triple(it, it, .5f) },
            pivotFractionY = 0.5f
        )

        minScale
            .let { progressValue interpolates Triple(1f, it, 1f) }
            .let {
                scaleX = it
                scaleY = it
            }

        collapsedCornerRounding
            .let { progressValue interpolates Triple(0f, .5f, it) }
            .let {
                clip = true
                shape = RoundedCornerShape(percent = it.toIntPercent() / 2)
            }
    }
}