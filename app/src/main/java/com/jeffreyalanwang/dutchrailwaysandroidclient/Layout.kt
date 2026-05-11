package com.jeffreyalanwang.dutchrailwaysandroidclient

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.max
import kotlin.math.roundToInt

//TODO this doesn't belong here
@Preview
@Composable
fun Test_1() {
    val scrollState = rememberScrollState()
    Box(Modifier
        .verticalScroll(scrollState)
        .width(550.dp)
    ) {
        TrainServiceDetail(
            BackendApi.get_pass_service(119u),
            modifier = Modifier.padding(10.dp)
        )
    }
}

@Immutable
class AlignSynchronizer() {
    /**
     * Number of px within the parent
     * to the left/above the subchild to align
     */
    private var _pos: Int? = null
    internal val pos: Int
        get() { return _pos ?: 0 }
    private val listeners = ArrayList<(Int)->Unit>()

    internal fun update(incomingPos: Int) {
        if (incomingPos > (_pos ?: 0)) {
            _pos = incomingPos
        }
    }
    internal fun registerOnUpdate(callback: (Int)->Unit)
        = listeners.add(callback)
    internal fun unregister(callback: (Int)->Unit) // currently unused (functions without; unclear where to call)
        = listeners.remove(callback)
}

//TODO which works with variable-length list of subchildren
//TODO give it a scope so we can create: leftAlignedItem(), growItem(), rightAlignedItem()

/**
 * Holds a component whose leftmost edge aligns with its cousins.
 * Cannot be used with lazy containers.
 */
@Composable
fun HorizontallyAlignedSubchildren(
    alignSynchronizer: AlignSynchronizer,
    modifier: Modifier = Modifier,
    gap: Dp = 0.dp,
    //TODO because this is a row, give it the parameters of Row (or at least, verticalAlignment)
    content: @Composable ()->Unit,
) {
    Layout(content, modifier) { measurables, constraints ->
        check(measurables.size == 2)
        val preferredHeight = max(
            measurables.maxOf { it.minIntrinsicHeight(constraints.maxWidth) },
            constraints.minHeight,
        )

        val placeables = measurables.map {
            it.measure(
                constraints = constraints.copy(
                    maxWidth = it.maxIntrinsicWidth(preferredHeight)
                )
            )
        }

        alignSynchronizer.update(placeables[0].width)

        layout(constraints.maxWidth, placeables.maxOf { it.measuredHeight }) {
            placeables[0].place(0, 0)

            val place1 = { pos: Int ->
                placeables[1].place(pos + gap.toPx().roundToInt(), 0)
            }
            place1(alignSynchronizer.pos)
            alignSynchronizer.registerOnUpdate(place1)
        }
    }
}
