@file:SuppressLint("ModifierNodeInspectableProperties")

package com.jeffreyalanwang.dutchrailwaysandroidclient.ui.components

import android.annotation.SuppressLint
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.Measurable
import androidx.compose.ui.layout.Placeable
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.node.ParentDataModifierNode
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.constrainHeight
import androidx.compose.ui.unit.dp
import com.jeffreyalanwang.dutchrailwaysandroidclient.ui.components.ModifierUtils.CellAlignModifierElement
import com.jeffreyalanwang.dutchrailwaysandroidclient.ui.components.ModifierUtils.FillModifierElement
import com.jeffreyalanwang.dutchrailwaysandroidclient.ui.components.ModifierUtils.cellAlign
import com.jeffreyalanwang.dutchrailwaysandroidclient.ui.components.ModifierUtils.isFill
import com.jeffreyalanwang.dutchrailwaysandroidclient.ui.util.copy
import kotlin.math.max
import kotlin.math.roundToInt

@Stable
class DiscreteGridControl {
    private var propsInitialized by mutableStateOf(false)

    /**
     * Ensure the same gap value was provided between all layout instances.
     * In px.
     */
    private var gap by mutableIntStateOf(0)

    /**
     * The required (i.e. highest value of) width of each "column"
     *  (across all layout instances).
     * Null for element at [centerIdx].
     * In px.
     */
    internal val widths = mutableStateListOf<Int?>()

    /**
     * Index of the one element that grows
     *  (i.e. is marked with [Modifier.fill]).
     * Null indicates that there is no such composable.
     */
    private var centerIdx by mutableStateOf<Int?>(null)

    /**
     * In px.
     */
    var totalWidth by mutableIntStateOf(0)
        private set

    private var elementCount by mutableIntStateOf(0)

    /**
     * Must not access before the first call
     *  to update().
     * In px.
     */
    internal fun centerFillWidth(): Int {
        if (centerIdx == null) {
            throw IllegalStateException("No center fill item exists.")
        }

        val gapsWidth = gap * (elementCount - 1)
        val othersWidth = widths.sumOf { it ?: 0 }
        return totalWidth - (othersWidth + gapsWidth)
    }

    private fun placeHorizontalAligned(
        width: Int,
        alignment: Alignment.Horizontal,
        left: Int, // pos at left edge of available space
        right: Int,
    ) = when(alignment) {
            Alignment.Start ->
                left
            Alignment.End   ->
                right - width
            Alignment.CenterHorizontally ->
                left + (right - left - width) / 2
            else ->
                throw IllegalArgumentException() // doesn't exist anyway
        }

    /**
     * Number of px within the parent
     *  to the left/above the subchild to align.
     * Calculated based on current values in [widths].
     * Must not access before the first call
     *  to update().
     * [itemWidths] here should be passed the real width,
     *  even of the center fill item. It might not be as wide
     *  as in [widths] (the maximum across the column).
     */
    internal fun positions(alignments: List<Alignment.Horizontal>, itemWidths: List<Int>): List<Int> {
        val out = MutableList(elementCount) { 0 }

        var leftPos = 0
        for (i in 0..<(centerIdx ?: elementCount)) {
            val leftColEdge  = leftPos
            val rightColEdge = leftPos + this.widths[i]!!

            out[i] = placeHorizontalAligned(
                itemWidths[i],
                alignments[i],
                leftColEdge,
                rightColEdge,
            )
            leftPos = rightColEdge + gap
        }

        var rightPos = totalWidth
        for (i in elementCount - 1 downTo (centerIdx?.plus(1) ?: elementCount)) {
            val leftColEdge  = rightPos - this.widths[i]!!
            val rightColEdge = rightPos

            out[i] = placeHorizontalAligned(
                itemWidths[i],
                alignments[i],
                leftColEdge,
                rightColEdge,
            )
            rightPos = leftColEdge - gap
        }

        if (centerIdx != null) {
            val leftColEdge  = leftPos
            val rightColEdge = rightPos

            out[centerIdx!!] = placeHorizontalAligned(
                itemWidths[centerIdx!!],
                alignments[centerIdx!!],
                leftColEdge,
                rightColEdge,
            )
        }

        return out
    }

    /**
     * Set or check all parameters except [incomingWidths].
     * These parameters should be consistent between linked
     *  instances of [DiscreteGridRow].
     * Resize subchildren in previous instances as needed by
     *  new values from [incomingWidths].
     * [incomingWidths]: contains `null` for the centerFill element, if present.
     */
    internal fun update(gap: Int, totalWidth: Int, centerIdx: Int?, incomingWidths: List<Int?>) {
        if (propsInitialized) {
            check(this.gap == gap)
            check(this.totalWidth == totalWidth)
            check(this.centerIdx == centerIdx)
            check(this.elementCount == incomingWidths.size)

            for (i in incomingWidths.indices) {
                val newW = incomingWidths[i]
                val oldW = widths.getOrNull(i)
                if (newW != null && newW > oldW!!) {
                    widths[i] = newW
                }
            }
        } else {
            this.gap = gap
            this.totalWidth = totalWidth
            this.centerIdx = centerIdx
            this.elementCount = incomingWidths.size
            this.widths.addAll(incomingWidths)
            propsInitialized = true
        }
    }
}

/**
 * Holds a component whose children align with their cousins.
 * Cannot be used with lazy containers.
 * It is assumed that instances of this Composable will be the same
 *  width, and themselves horizontally aligned.
 */
@Composable
fun DiscreteGridRow(
    discreteGridControl: DiscreteGridControl,
    modifier: Modifier = Modifier,
    gap: Dp = 0.dp,
    verticalAlignment: Alignment.Vertical = Alignment.Top,
    fillCellWidth: Boolean = false,
    content: @Composable DiscreteGridRowScope.() -> Unit,
) = Layout(
        content = { DiscreteGridRowScope.content() },
        modifier = modifier,
    ) { measurables, constraints ->

        // Identify center fill item
        val centerIdx: Int? = measurables
            .indexOfFirst { it.isFill }
            .takeIf { it >= 0 }

        // Determine preferredHeight (max of intrinsics of all children)
        val rigidsHeight = constraints.constrainHeight(
            measurables.maxOf { it.minIntrinsicHeight(constraints.maxHeight) }
        )

        // Measure non-center (rigid) items
        val rigidPlaceables = measurables.mapIndexed { i, measurable ->
            if (i == centerIdx) null
            else measurable.measure(
                constraints.copyMaxDimensions().copy(
                    width = max(
                        measurable.maxIntrinsicWidth(rigidsHeight),
                        discreteGridControl.widths.getOrElse(i) { 0 }!!
                            .let { if (fillCellWidth) it else 0 }
                    )
                )
            )
        }

        // Update synchronizer. This performs side effects:
        //  We calculate the positions which we use immediately below in layout.
        //  If position values are calculated to require change, we also move
        //      earlier-processed cousins as needed.
        discreteGridControl.update(
            gap = gap.toPx().roundToInt(),
            centerIdx = centerIdx,
            totalWidth = constraints.maxWidth,
            incomingWidths = rigidPlaceables.map { it?.width }
        )

        // Measure fill item
        val fillPlaceable = centerIdx?.let { i ->
            val spaceWidth = discreteGridControl.centerFillWidth()

            measurables[i].measure(
                constraints.copyMaxDimensions().copy(
                    minWidth = if (fillCellWidth) spaceWidth else 0,
                    maxWidth = spaceWidth,
                )
            )
        }

        // Get position values
        val placeables = rigidPlaceables.map { (it ?: fillPlaceable)!! }
        val positions = discreteGridControl.positions(
            alignments = placeables.map { it.cellAlign },
            itemWidths = placeables.map { it.width },
        )

        val containerHeight = max(rigidsHeight, fillPlaceable?.height ?: 0)
        layout(
            width = discreteGridControl.totalWidth,
            height = containerHeight
        ) {
            positions.forEachIndexed { i, pos ->
                val placeable = if (i == centerIdx) {
                    fillPlaceable!!
                } else {
                    rigidPlaceables[i]!!
                }
                val yPos = alignedYPos(verticalAlignment, placeable.height, containerHeight)
                placeable.place(pos, yPos)
            }
        }
    }

object DiscreteGridRowScope {
    /**
     * Apply to zero or one items in the row.
     * This item will take the remaining space after others
     *  have been sized.
     * Items before it (or all items, if this modifier is never used)
     *  will be positioned based on the widths of those before them.
     * Items after it will be positioned based on
     *  the widths of those after them.
     */
    @Stable
    fun Modifier.fill(): Modifier = this then FillModifierElement

    /**
     * Alignment of an item within its "grid cell"
     *  (i.e. the horizontal space available to it
     *  and its cousins in that column).
     */
    @Stable
    fun Modifier.cellAlign(alignment: Alignment.Horizontal): Modifier =
        this then CellAlignModifierElement(alignment)
}

private object ModifierUtils {

    private data class SubchildParentData(
        var fill: Boolean = false,
        var cellAlign: Alignment.Horizontal = Alignment.Start
    )

    val Measurable.isFill get() = parentData.orDefaultParentData().fill
    val Measurable.cellAlign get() = parentData.orDefaultParentData().cellAlign
    val Placeable.fill get() = parentData.orDefaultParentData().fill
    val Placeable.cellAlign get() = parentData.orDefaultParentData().cellAlign

    private fun Any?.orDefaultParentData() =
        this as? SubchildParentData ?: SubchildParentData()

    @Immutable
    data object FillModifierElement :
        ModifierNodeElement<FillModifierNode>() {
        override fun create() = FillModifierNode()
        override fun update(node: FillModifierNode) {}
    }

    class FillModifierNode : ParentDataModifierNode, Modifier.Node() {
        override fun Density.modifyParentData(parentData: Any?): Any {
            return ((parentData as? SubchildParentData)
                ?: SubchildParentData()).also {
                it.fill = true
            }
        }
    }

    @Immutable
    data class CellAlignModifierElement(
        val alignment: Alignment.Horizontal
    ) : ModifierNodeElement<CellAlignModifierNode>() {
        override fun create() = CellAlignModifierNode(alignment)
        override fun update(node: CellAlignModifierNode) {
            node.alignment = this.alignment
        }
    }

    class CellAlignModifierNode(
        var alignment: Alignment.Horizontal
    ) : ParentDataModifierNode, Modifier.Node() {
        override fun Density.modifyParentData(parentData: Any?): Any {
            return ((parentData as? SubchildParentData)
                ?: SubchildParentData()).also {
                it.cellAlign = alignment
            }
        }
    }

}

private fun alignedYPos(
    verticalAlignment: Alignment.Vertical,
    itemHeight: Int,
    containerHeight: Int
): Int =
    when (verticalAlignment) {
        Alignment.Top -> 0
        Alignment.CenterVertically -> (containerHeight - itemHeight) / 2
        Alignment.Bottom -> containerHeight - itemHeight
        else -> throw IllegalArgumentException() // doesn't exist anyway
    }
