package com.jeffreyalanwang.dutchrailwaysandroidclient.ui.components

import android.annotation.SuppressLint
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.dropShadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.shadow.Shadow
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.Placeable
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.round
import androidx.compose.ui.unit.times
import androidx.compose.ui.util.fastMap
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import com.jeffreyalanwang.dutchrailwaysandroidclient.letIf
import com.jeffreyalanwang.dutchrailwaysandroidclient.letWith
import com.jeffreyalanwang.dutchrailwaysandroidclient.ui.util.animateIntOffsetAsState
import com.jeffreyalanwang.dutchrailwaysandroidclient.ui.util.asRectInWindow
import com.jeffreyalanwang.dutchrailwaysandroidclient.ui.util.copy
import com.jeffreyalanwang.dutchrailwaysandroidclient.ui.util.id
import com.jeffreyalanwang.dutchrailwaysandroidclient.ui.util.movedInto
import com.jeffreyalanwang.dutchrailwaysandroidclient.zipOnKeys

private const val badgeContentProportion = .7f

/** Handles visual effects. */
@SuppressLint("ModifierParameter")
@Composable
fun ExpandableBadgeSet(
    isExpanded: Boolean,
    onSetExpanded: (Boolean) -> Unit,
    collapsedBadgeSize: Dp,
    expandedBadgeSize: Dp,
    windowInsets: WindowInsets,
    contentModifier: Modifier = Modifier.Companion,
    containerModifier: Modifier = Modifier.Companion,
    collapsedGap: Dp = 0.dp,
    expandedGap: Dp = 0.dp,
    badgesToLabels: Map<Badge, Pair<@Composable () -> Unit, @Composable () -> Unit>>,
) = with (ExpandableBadgeSetUtilScope) {

    var animatingTo by remember(isExpanded) {
        mutableStateOf(
            if (isExpanded) ExpandableBadgeSetUtilScope.Target.Expanded
            else ExpandableBadgeSetUtilScope.Target.Collapsed
            // [Target.None] is set by [onAnimComplete()]
        )
    }
    var containerInWindow by remember { mutableStateOf(IntRect.Zero) }
    var popupSize by remember { mutableStateOf(IntSize.Zero) }

    val (motionSpecX, motionSpecY, motionSpecSize) = getMotionSpecs(animatingTo)

    /** Coordinates (in Px) of each item. */
    val destLocations = remember { mutableStateMapOf<Any, IntOffset>() }
    val destBadgeSize = (if (!isExpanded) collapsedBadgeSize else expandedBadgeSize)
        .letWith(LocalDensity.current) { it.roundToPx() }
    val destPopupOffset = popupOffsetFromContainer(
        isExpanded,
        boundsInWindow = windowInsets
            .asRectInWindow(LocalDensity.current, LocalResources.current.displayMetrics),
        containerInWindow = containerInWindow.topLeft,
        popupSize = popupSize,
        desiredOffset =
            with (LocalDensity.current) {
                androidx.compose.ui.unit.IntOffset(
                    x = collapsedBadgeSize.roundToPx() / 2,
                    y = containerInWindow.height - popupSize.height +
                            if (badgesToLabels.size <= 2) 0
                            else (expandedGap + expandedBadgeSize * 2).roundToPx(),
                )
            },
    )

    lateinit var onAnimComplete: () -> Unit
    /**
     * The actual coordinates of each item on screen, as animated state values.
     * Best practice (performance & maintainability-wise) would be to develop
     * a way to directly animate [IntOffset] values with different [androidx.compose.animation.core.AnimationSpec]s
     * for x & y axes; however, that would require access to several complex
     * internal/private components behind Compose animation API.
     */
    val animLocations: Map<Any, IntOffset> =
        destLocations.mapValues { (id, coords) ->
            key(id) {
                animateIntOffsetAsState(coords, motionSpecX to motionSpecY)
                { onAnimComplete() }
            }
            .value
        }
    val animPopupOffset by
    animateIntOffsetAsState(destPopupOffset, motionSpecX to motionSpecY)
    { onAnimComplete() }
    val animBadgeSize by
    animateIntAsState(destBadgeSize, motionSpecSize) { onAnimComplete() }

    onAnimComplete = {
        val allComplete =
            (destLocations zipOnKeys animLocations).values
            .plus(destPopupOffset to animPopupOffset)
            .plus(destBadgeSize to animBadgeSize)
            .all { (dest, curr) -> dest == curr }
        if (allComplete) {
            animatingTo = ExpandableBadgeSetUtilScope.Target.Neither
        }
    }

    Box(
        containerModifier
            .size(
                // Always sized as if collapsed
                width = badgesToLabels.size * collapsedBadgeSize +
                        (badgesToLabels.size - 1) * collapsedGap,
                height = collapsedBadgeSize,
            )
            .onGloballyPositioned {
                containerInWindow =
                    IntRect(it.positionInWindow().round(), it.size)
            }
            .wrapContentSize(Alignment.TopStart, unbounded = true)
    ) {
        val content = @Composable {
            Layout(
                modifier = contentModifier
                    .clickable(null, null) { onSetExpanded(!isExpanded) },
                content = {
                    for ((id, composables) in badgesToLabels) {
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
                    width = if (!isExpanded) animBadgeSize else null,
                )
                val placeables =
                    measurables.fastMap { it.measure(itemConstraints) }
                val placeablesWithId = placeables.map { it.id!! to it }

                // Calculate destination (i.e. non-animated) position values
                val template =
                    basicLinearLayout(
                        layout = if (!isExpanded) Row else Column,
                        keyedPlaceables = placeablesWithId,
                        gapPx = if (!isExpanded)
                            collapsedGap.roundToPx()
                        else expandedGap.roundToPx()
                    )

                // Animate to those positions
                destLocations.putAll(template.first)
                popupSize = template.second

                layout(popupSize.width, height = popupSize.height) {
                    // zipOnKeys skips placing values in [placeables]
                    // if they are not yet in [animLocationStates]
                    placeablesWithId.toMap()
                        .zipOnKeys(animLocations) { placeable, offset ->
                            placeable.placeRelative(offset)
                        }
                }
            }
        }

        if (animPopupOffset == IntOffset.Zero) {
            content()
        } else {
            // [Popup()] allows us to display above all other elements on the screen,
            // and also allows us to listen for clicks outside the element.
            Popup(
                alignment = Alignment.TopStart,
                offset = animPopupOffset,
                properties = PopupProperties(
                    focusable = isExpanded, // seems required to use [onDismissRequest],
                    // but blocks any other click listeners on the screen
                    dismissOnClickOutside = true,
                    clippingEnabled = false,
                ),
                onDismissRequest = { if (isExpanded) onSetExpanded(false) },
            ) {
                content()
            }
        }
    }
}

@Suppress("ClassName")
internal object ExpandableBadgeSetUtilScope {
    enum class _target { Expanded, Collapsed, Neither }
    typealias Target = _target // allows visibility in scope

    /**
     * Build each item with animated glow + animated label visibility.
     *
     * Label does not leave the composable immediately when collapsed,
     * but it does immediately stop contributing to the composable's size.
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

    /** Draws and animates enter/exit of background for expanded [AmenityBadgeSet]. */
    @SuppressLint("ModifierParameter")
    @Composable
    private fun GlowBox(
        isEnabled: Boolean = true,
        modifier: Modifier = Modifier.Companion,
        content: @Composable BoxScope.() -> Unit,
    ) {
        var isEnabledState by remember { mutableStateOf(false) }
        DisposableEffect(isEnabled) {
            isEnabledState = isEnabled
            onDispose {
                isEnabledState = false
            }
        }

        val progress by animateFloatAsState(
            if (isEnabledState) 1f else 0f,
            MaterialTheme.motionScheme.slowSpatialSpec(),
        )

        Box(
            modifier.dropShadow(
                MaterialTheme.shapes.small,
                Shadow(
                    color = Color.White,
                    alpha = 1f * progress,
                    radius = 3.dp,
                    spread = 2.dp * progress,
                ),
            ),
            content = content
        )
    }

    /** @return motionSpecX, motionSpecY, motionSpecSize */
    @Composable
    fun getMotionSpecs(animatingTo: Target)
        = with (MaterialTheme.motionScheme) {
            when (animatingTo) {
                Target.Expanded ->
                    Triple( fastSpatialSpec<Int>(), slowSpatialSpec<Int>(), slowSpatialSpec<Int>() )
                Target.Collapsed ->
                    Triple( slowSpatialSpec(),      fastSpatialSpec(),      defaultSpatialSpec()   )
                Target.Neither ->
                    Triple( defaultSpatialSpec(),   defaultSpatialSpec(),   defaultSpatialSpec()   )
            }
        }

    /**
     * @param desiredOffset:
     *      The desired offset of the popup from the container,
     *      when it is expanded.
     */
    fun popupOffsetFromContainer(
        isExpanded: Boolean,
        boundsInWindow: IntRect,
        containerInWindow: IntOffset,
        popupSize: IntSize,
        desiredOffset: IntOffset,
    ): IntOffset {
        if (!isExpanded) return IntOffset.Zero

        // Declared below: offsets are relative to container
        val bounds = boundsInWindow.translate(-containerInWindow)
        val actual =
            IntRect(desiredOffset, popupSize)
                .movedInto(
                    bounds,
                    xOverflow = 0,
                    yOverflow = bounds.center.y - (popupSize.height / 2),
                )

        return actual.topLeft
    }

    enum class LayoutAxis { Row, Column }
    val Row get() = LayoutAxis.Row
    val Column get() = LayoutAxis.Column

    /**
     * Lays out [keyedPlaceables] (aligned to top or start).
     *
     * Takes [keyedPlaceables] as a list because it must be in order.
     * However, returns a map because placement locations are already absolute.
     *
     * @return Pair of:
     *      - Coordinates of each placeable
     *      - Total size of parent composable
     */
    @Suppress("FunctionName")
    fun <K> basicLinearLayout(
        layout: LayoutAxis,
        keyedPlaceables: List<Pair<K, Placeable>>,
        gapPx: Int,
    ): Pair<Map<K, IntOffset>, IntSize> {
        // width-height to axis-crossaxis
        fun xy_to_ac(x: Int, y: Int) = when (layout) {
            LayoutAxis.Row -> arrayOf(x, y)
            LayoutAxis.Column -> arrayOf(y, x)
        }
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

@Composable
fun Badge(
    icon: Int,
    contentDescription: String,
    modifier: Modifier,
    color: Color = LocalContentColor.current,
    bgColor: Color = Color.Transparent,
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