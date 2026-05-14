package com.jeffreyalanwang.dutchrailwaysandroidclient

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
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.node.ParentDataModifierNode
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
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
    private val widths = mutableStateListOf<Int?>()

    /**
     * Index of the one element that grows
     *  (i.e. is marked with [Fill] modifier).
     * Null indicates that there is no such composable.
     */
    private var centerIdx by mutableStateOf<Int?>(null)

    private var _totalWidth by mutableIntStateOf(0)
    /**
     * In px.
     */
    public var totalWidth: Int
        get() = _totalWidth
        private set(value) { _totalWidth = value }

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

    private fun placeHorizontalAligned( // TODO-TEST does this work on update
        width: Int,
        alignment: Alignment.Horizontal,
        left: Int, // pos at left edge of available space
        right: Int,
    ) = when(alignment) {
        Alignment.Start -> left
        Alignment.End   -> right - width
        Alignment.CenterHorizontally -> (
                left + (right - left - width) / 2
                )
        else -> throw IllegalArgumentException() // doesn't exist anyway
    }

    /**
     * Number of px within the parent
     *  to the left/above the subchild to align.
     * Calculated based on current values in [widths].
     * Must not access before the first call
     *  to update().
     * [itemWidths] here should be passed the real width,
     *  even of the center fill item. It might not be as wide
     *  as in [this.widths] (the maximum across the column).
     */
    internal fun positions(alignments: List<Alignment.Horizontal>, itemWidths: List<Int>): List<Int> {
        val out = MutableList<Int>(elementCount) { 0 }

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
    internal fun update(gap: Int, totalWidth: Int, centerIdx: Int?, incomingWidths: List<Int?>): Unit {
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

private data class SubchildParentData(
    var fill: Boolean = false,
    var cellAlign: Alignment.Horizontal = Alignment.Start
)

private data object FillModifierElement : ModifierNodeElement<FillModifierNode>() {
    override fun create() = FillModifierNode()
    override fun update(node: FillModifierNode) {}
}

private class FillModifierNode : ParentDataModifierNode, Modifier.Node() {
    override fun Density.modifyParentData(parentData: Any?): Any {
        return ((parentData as? SubchildParentData) ?: SubchildParentData()).also {
            it.fill = true
        }
    }
}

@Immutable
private data class CellAlignModifierElement(
    val alignment: Alignment.Horizontal
) : ModifierNodeElement<CellAlignModifierNode>() {
    override fun create() = CellAlignModifierNode(alignment)
    override fun update(node: CellAlignModifierNode) { //TODO-TEST what if this gets modified?
        node.alignment = this.alignment
    }
}

private class CellAlignModifierNode(
    var alignment: Alignment.Horizontal
) : ParentDataModifierNode, Modifier.Node() {
    override fun Density.modifyParentData(parentData: Any?): Any {
        return ((parentData as? SubchildParentData) ?: SubchildParentData()).also {
            it.cellAlign = alignment
        }
    }
}

@Immutable
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
    fun Modifier.fill(): Modifier
            = this.then(FillModifierElement)

    /**
     * Alignment of an item within its "grid cell"
     *  (i.e. the horizontal space available to it
     *  and its cousins in that column).
     */
    @Stable
    fun Modifier.cellAlign(alignment: Alignment.Horizontal): Modifier
            = this.then(CellAlignModifierElement(alignment))
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
    //TODO because this is a row, give it the parameters of Row (or at least, verticalAlignment)
    content: @Composable DiscreteGridRowScope.()->Unit,
) {
    Layout(
        content = { DiscreteGridRowScope.content() },
        modifier = modifier,
    ) { measurables, constraints ->
        if (measurables.isEmpty()) {
            return@Layout layout(0, 0) {}
        }

        // Identify center fill item
        val centerIdx: Int? = measurables
            .indexOfFirst { (it.parentData as? SubchildParentData)?.fill == true }
            .takeIf { it != -1 }

        // Determine preferredHeight (max of intrinsics of all children)
        val rigidsMaxTotalW = constraints.maxWidth - (
                centerIdx
                    ?.let { i -> measurables[i] }
                    ?.minIntrinsicWidth(constraints.maxHeight)
                    ?: 0
                )
        val rigidsMaxH = max(
            measurables.maxOf { it.minIntrinsicHeight(rigidsMaxTotalW) },
            constraints.minHeight
        )

        // Measure non-center (rigid) items
        val rigidPlaceables = measurables.mapIndexed { i, measurable ->
            if (i == centerIdx) null
            else measurable.measure(
                constraints.copy(
                    minWidth = 0,
                    maxWidth = measurable.maxIntrinsicWidth(rigidsMaxH),
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
            measurables[i].measure(
                constraints.copy(
                    minWidth = 0,
                    maxWidth = discreteGridControl.centerFillWidth(),
                )
            )
        }

        // Get position values TODO if this doesn't work, try moving into layout
        val alignments: List<Alignment.Horizontal> = measurables
            .map {
                (it.parentData as? SubchildParentData)?.cellAlign
                    ?: Alignment.Start
            }
        val widths = rigidPlaceables.map { (it ?: fillPlaceable)!!.width }
        val positions = discreteGridControl.positions(alignments, widths)

        layout(
            width = discreteGridControl.totalWidth,
            height = max(rigidsMaxH, fillPlaceable?.height ?: 0)
        ) {
            positions.forEachIndexed { i, pos ->
                if (i == centerIdx) {
                    fillPlaceable!!.place(pos, 0)
                } else {
                    rigidPlaceables[i]!!.place(pos, 0)
                }
            }
        }
    }
}
