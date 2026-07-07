package com.jeffreyalanwang.dutchrailwaysandroidclient.ui.components

import androidx.activity.compose.PredictiveBackHandler
import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.GraphicsLayerScope
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.findRootCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.navigationevent.NavigationEvent
import androidx.navigationevent.NavigationEvent.Companion.EDGE_NONE
import com.jeffreyalanwang.dutchrailwaysandroidclient.interpolates
import com.jeffreyalanwang.dutchrailwaysandroidclient.toIntPercent
import kotlinx.coroutines.CancellationException

@Composable
fun PredictiveBackBox(
    enabled: Boolean,
    onDismissRequest: () -> Unit,
    content: @Composable () -> Unit,
) {
    var isFullScreen by remember { mutableStateOf(false) }
    val backProgress = remember { Animatable(initialValue = 0f) }

    PredictiveBackBox(
        snapBackProgressTo = { backProgress.snapTo(it) },
        animateBackProgressTo = {
            backProgress.animateTo(
                if (it) 1f else 0f,
            )
        },
        content,
        backHandlingEnabled = enabled,
        onDismissRequest = onDismissRequest,
        modifier = Modifier
            .onGloballyPositioned {
                isFullScreen = (it.size == it.findRootCoordinates().size)
            }
    ) { swipeEdge ->
        val backProgress by backProgress.asState()

        transformOrigin = TransformOrigin(
            pivotFractionX =
                when (swipeEdge) {
                    NavigationEvent.EDGE_LEFT -> 1f
                    NavigationEvent.EDGE_RIGHT -> 0f
                    else -> 0.5f
                },
            pivotFractionY = 0.5f
        )

        (backProgress interpolates (1f to 0f))
            .let {
                scaleX = it
                scaleY = it
                alpha = it
            }

        if (isFullScreen && backProgress > 0f) {
            clip = true
            shape = RoundedCornerShape(percent = backProgress.toIntPercent() / 2)
        }
    }
}

@Composable
fun PredictiveBackBox(
    snapBackProgressTo: suspend (Float) -> Unit,
    animateBackProgressTo: suspend (backComplete: Boolean) -> Unit,
    content: @Composable () -> Unit,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    backHandlingEnabled: Boolean = true,
    graphicsLayerBlock: GraphicsLayerScope.(swipeEdge: Int) -> Unit,
) {
    var swipeEdge by remember { mutableIntStateOf(EDGE_NONE) }

    PredictiveBackHandler(
        enabled = backHandlingEnabled,
    ) { progress ->
        try {
            progress.collect { backEvent ->
                snapBackProgressTo(backEvent.progress)
                swipeEdge = backEvent.swipeEdge
            }
            animateBackProgressTo(true)
            swipeEdge = EDGE_NONE
            onDismissRequest()
        } catch (e: CancellationException) {
            animateBackProgressTo(false)
            swipeEdge = EDGE_NONE
        }
    }

    Box(
        modifier
            .graphicsLayer {
                graphicsLayerBlock(swipeEdge)
            },
    ) {
        content()
    }
}