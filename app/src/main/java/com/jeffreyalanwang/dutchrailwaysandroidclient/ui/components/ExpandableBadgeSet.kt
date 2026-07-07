package com.jeffreyalanwang.dutchrailwaysandroidclient.ui.components

import android.annotation.SuppressLint
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.Placeable
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.round
import androidx.compose.ui.util.fastMap
import com.jeffreyalanwang.dutchrailwaysandroidclient.letIf
import com.jeffreyalanwang.dutchrailwaysandroidclient.letWith
import com.jeffreyalanwang.dutchrailwaysandroidclient.ui.util.animateIntOffsetAsState
import com.jeffreyalanwang.dutchrailwaysandroidclient.ui.util.copy
import com.jeffreyalanwang.dutchrailwaysandroidclient.ui.util.id
import com.jeffreyalanwang.dutchrailwaysandroidclient.ui.util.toPx
import com.jeffreyalanwang.dutchrailwaysandroidclient.zipOnKeys

private const val badgeContentProportion = .7f

/**
 * A low-level component that handles the visual transitions and layout of a set of badges that can expand from a row to a column with labels.
 *
 * @param isExpanded Whether the badge set is currently expanded.
 * @param onSetExpanded Callback to toggle the expansion state.
 * @param collapsedBadgeSize The size of the badges when collapsed.
 * @param expandedBadgeSize The size of the badges when expanded.
 * @param windowInsets The insets used for positioning the expanded popup.
 * @param contentModifier Modifier applied to the inner layout.
 * @param containerModifier Modifier applied to the outer placeholder container.
 * @param collapsedGap The gap between badges when collapsed.
 * @param expandedGap The gap between items when expanded.
 * @param keyedBadgesToLabels A map of unique IDs to pairs of composables (badge and label) to be rendered.
 */
@SuppressLint("ModifierParameter")
@Composable
fun ExpandableBadgeSet(
    isExpanded: Boolean,
    onSetExpanded: (Boolean) -> Unit,
    collapsedBadgeSize: Dp,
    expandedBadgeSize: Dp,
    windowInsets: WindowInsets,
    contentModifier: Modifier = Modifier,
    containerModifier: Modifier = Modifier,
    collapsedGap: Dp = 0.dp,
    expandedGap: Dp = 0.dp,
    keyedBadgesToLabels: Map<Any, Pair<@Composable () -> Unit, @Composable () -> Unit>>,
) = with (ExpandableBadgeSetUtilScope) {

    var animatingTo by remember(isExpanded) {
        mutableStateOf(
            if (isExpanded) AnimationTarget.Expanded
            else AnimationTarget.Collapsed
        )
    }
    fun onAnimationComplete() { animatingTo = AnimationTarget.Neither }

    val (motionSpecX, motionSpecY, motionSpecSize) = getMotionSpecs(animatingTo)

    // Below are all in Px.
    /** Coordinates of each item. */
    val destLocations = remember { mutableStateMapOf<Any, IntOffset>() } // calculated inside Layout, below
    val destBadgeSize =
        (if (!isExpanded) collapsedBadgeSize else expandedBadgeSize)
        .letWith(LocalDensity.current) { it.roundToPx() }

    lateinit var onSingleAnimationComplete: () -> Unit
    /**
     * The actual coordinates of each item on screen, as animated state values.
     * Best practice (performance & maintainability-wise) would be to develop
     * a way to directly animate 2D-vector values like [IntOffset] with
     * different [AnimationSpec]s for their components; however, that would
     * require access to several complex internal/private components behind
     * Compose animation API.
     */
    val animLocations: Map<Any, IntOffset> =
        destLocations.mapValues { (id, coords) ->
            key(id) {
                animateIntOffsetAsState(coords, motionSpecX to motionSpecY)
                    { onSingleAnimationComplete() }
            }
            .value
        }
    val animBadgeSize by animateIntAsState(destBadgeSize, motionSpecSize)
        { onSingleAnimationComplete() }

    var isPopupAnimating by remember { mutableStateOf(false) }
    onSingleAnimationComplete = {
        val allComplete =
            !isPopupAnimating &&
            (destLocations zipOnKeys animLocations).values
            .plus(destBadgeSize to animBadgeSize)
            .all { (dest, curr) -> dest == curr }

        if (allComplete) onAnimationComplete()
    }

    val collapsedSize =
        DpSize(
            width = collapsedBadgeSize * (keyedBadgesToLabels.size) +
                    collapsedGap       * (keyedBadgesToLabels.size - 1),
            height = collapsedBadgeSize,
        )
    val expandedPopupHeight =
        expandedBadgeSize * (keyedBadgesToLabels.size) +
        expandedGap       * (keyedBadgesToLabels.size - 1)

    ExpandablePopup(
        isExpanded,
        onCollapse = { onSetExpanded(false) },
        uncoercedExpandedOffset =
            // When expanded, offset slightly to right,
            // and align bottom to 1 badge + gap below the container
            // (unless list is small).
            DpOffset(
                x = collapsedBadgeSize / 2,
                y = (
                        if (keyedBadgesToLabels.size <= 2) 0.dp
                        else (expandedGap + expandedBadgeSize * 2)
                    ) +
                    // align placeholder container + popup by bottom edge
                    collapsedSize.height - expandedPopupHeight,
            )
            .letWith(LocalDensity.current) { it.toPx().round() },
        collapsedDimensions = collapsedSize,
        modifier = containerModifier,
        windowInsets = windowInsets,
        animationSpec = motionSpecX to motionSpecY,
        animationStartedListener = { isPopupAnimating = true },
        animationFinishedListener = {
            isPopupAnimating = false
            onSingleAnimationComplete()
        },
    ) {
        Layout(
            modifier = contentModifier
                .clickable(null, null) { onSetExpanded(!isExpanded) },
            content = {
                for ((id, composables) in keyedBadgesToLabels) {
                    Item(
                        id = id,
                        badge = composables.first,
                        label = composables.second,
                        isExpanded = isExpanded,
                        badgeLabelGap = expandedGap,
                    )
                }
            }
        ) { measurables, constraints ->
            val itemConstraints = constraints.copy(
                height = animBadgeSize,

                // Even if animating between states TODO actually how does this work if we only constrain when !isExpanded
                width = if (!isExpanded) animBadgeSize else null,
            )
            val placeables =
                measurables.fastMap { it.measure(itemConstraints) }
            val placeablesWithId = placeables.map { it.id!! to it }

            // Calculate destination (i.e. non-animated) position values
            val (locations, parentSize) =
                basicLinearLayout(
                    layout = if (!isExpanded) LayoutAxis.Row else LayoutAxis.Column,
                    keyedPlaceables = placeablesWithId,
                    gapPx = if (!isExpanded)
                        collapsedGap.roundToPx()
                    else expandedGap.roundToPx()
                )

            // Animate to those positions
            destLocations.putAll(locations)

            layout(parentSize.width, height = parentSize.height) {
                // zipOnKeys skips placing values in [placeables]
                // if they are not yet in [animLocationStates]
                placeablesWithId.toMap()
                    .zipOnKeys(animLocations) { placeable, offset ->
                        placeable.placeRelative(offset)
                    }
            }
        }
    }
}

/** Target states for expansion animations.  */
internal enum class AnimationTarget { Expanded, Collapsed, Neither }

internal enum class LayoutAxis { Row, Column }

internal object ExpandableBadgeSetUtilScope {

    /** @return motionSpecX, motionSpecY, motionSpecSize */
    @Composable
    fun getMotionSpecs(animatingTo: AnimationTarget)
            = with (MaterialTheme.motionScheme) {
        when (animatingTo) {
            AnimationTarget.Expanded ->
                Triple( fastSpatialSpec<Int>(), slowSpatialSpec<Int>(), slowSpatialSpec<Int>() )
            AnimationTarget.Collapsed ->
                Triple( slowSpatialSpec(),      fastSpatialSpec(),      defaultSpatialSpec()   )
            AnimationTarget.Neither ->
                Triple( defaultSpatialSpec(),   defaultSpatialSpec(),   defaultSpatialSpec()   )
        }
    }

    /**
     * Build each item with animated glow + animated label visibility.
     *
     * Label does not leave the composable immediately when collapsed,
     * but it does immediately stop contributing to the composable's size.
     *
     * @param id Unique ID for the item.
     * @param badge Composable to render as the badge.
     * @param label Composable to render as the label.
     * @param isExpanded Whether the item is expanded.
     * @param badgeLabelGap The gap between the badge and its label.
     */
    @Composable
    fun Item(
        id: Any,
        badge: @Composable () -> Unit,
        label: @Composable () -> Unit,
        isExpanded: Boolean,
        badgeLabelGap: Dp,
    ) {
        Row(
            Modifier.id(id),
            Arrangement.Start,
            Alignment.CenterVertically,
        ) {
            GlowBox(isExpanded) { badge() }
            AnimatedVisibility(isExpanded) {
                GlowBox(
                    modifier = Modifier.padding(start = badgeLabelGap)
                        .letIf(!isExpanded) {
                            // during transition to collapsed,
                            // measure at final width (i.e. 0) for accurate animation
                            it
                                .width(0.dp)
                                .wrapContentSize(unbounded = true)
                        }
                ) {
                    label()
                }
            }
        }
    }

    /**
     * Lays out [keyedPlaceables] (aligned to top or start).
     *
     * Takes [keyedPlaceables] as a list because it must be in order.
     * However, returns a map because placement locations are already absolute.
     *
     * @param layout The axis along which to layout items.
     * @param keyedPlaceables List of keyed placeables to layout.
     * @param gapPx The gap between items in pixels.
     * @return Pair containing:
     *      - A map of IDs to absolute coordinates for each placeable.
     *      - The total size of the layout.
     */
    fun <K> basicLinearLayout(
        layout: LayoutAxis,
        keyedPlaceables: List<Pair<K, Placeable>>,
        gapPx: Int,
    ): Pair<Map<K, IntOffset>, IntSize> {
        // width-height to axis-crossaxis
        @Suppress("FunctionName")
        fun xy_to_ac(x: Int, y: Int) = when (layout) {
            LayoutAxis.Row -> arrayOf(x, y)
            LayoutAxis.Column -> arrayOf(y, x)
        }
        @Suppress("FunctionName")
        fun ac_to_xy(a: Int, c: Int) = xy_to_ac(a, c) // happens to be the same swap

        val startPos = 0

        val positions = mutableMapOf<K, IntOffset>()
        var currAxisPos = startPos
        var maxCrossDim = 0
        for ((id, placeable) in keyedPlaceables) {
            val (xPos, yPos) = ac_to_xy(a = currAxisPos, c = 0)
            positions[id] = IntOffset(xPos, yPos)

            val (axisDim, crossDim) = xy_to_ac(x = placeable.width, y = placeable.height)
            currAxisPos += axisDim + gapPx
            maxCrossDim = maxOf(maxCrossDim, crossDim)
        }

        val endPos = currAxisPos - gapPx
        val totalAxisDim = endPos - startPos

        val (parentWidth, parentHeight) = ac_to_xy(totalAxisDim, maxCrossDim)
        return positions to IntSize(parentWidth, parentHeight)
    }
}

/**
 * A circular badge component with an icon.
 *
 * @param icon The resource ID of the icon to display.
 * @param contentDescription The accessibility description for the icon.
 * @param modifier Modifier applied to the badge layout.
 * @param color The color of the icon and the border.
 * @param bgColor The background color of the badge circle.
 * @param borderThickness The thickness of the border as a proportion of the badge size.
 */
@Composable
fun Badge(
    icon: Int,
    contentDescription: String,
    modifier: Modifier,
    color: Color = LocalContentColor.current,
    bgColor: Color = MaterialTheme.colorScheme.background,
    borderThickness: Float = 1 / 12f,
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .aspectRatio(1f)
            .defaultMinSize(15.dp)
            .drawWithContent {
                val circleOuterRadius = size.minDimension / 2
                val borderThickness = size.minDimension * borderThickness

                drawCircle(bgColor, radius = circleOuterRadius, style = Fill)
                drawContent()
                drawCircle(
                    color,
                    radius = (circleOuterRadius - (borderThickness / 2)),
                    style = Stroke(borderThickness),
                )
            }
    ) {
        Icon(
            painterResource(icon),
            contentDescription = contentDescription,
            tint = color,
            modifier = Modifier.fillMaxSize(badgeContentProportion)
        )
    }
}