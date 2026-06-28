package com.jeffreyalanwang.dutchrailwaysandroidclient.ui.components

import android.annotation.SuppressLint
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.AnimationSpec
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
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.Immutable
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
import androidx.compose.ui.graphics.Color.Companion.Red
import androidx.compose.ui.graphics.Color.Companion.Transparent
import androidx.compose.ui.graphics.Color.Companion.White
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.shadow.Shadow
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.Placeable
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.node.ParentDataModifierNode
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontStyle.Companion.Italic
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Density
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
import com.jeffreyalanwang.dutchrailwaysandroidclient.R
import com.jeffreyalanwang.dutchrailwaysandroidclient.TrainAmenity
import com.jeffreyalanwang.dutchrailwaysandroidclient.associateWithIndexed
import com.jeffreyalanwang.dutchrailwaysandroidclient.letIf
import com.jeffreyalanwang.dutchrailwaysandroidclient.letWith
import com.jeffreyalanwang.dutchrailwaysandroidclient.ui.components.ExpandableBadgeSetUtilScope.Target
import com.jeffreyalanwang.dutchrailwaysandroidclient.ui.util.AppIcons
import com.jeffreyalanwang.dutchrailwaysandroidclient.ui.util.animateIntOffsetAsState
import com.jeffreyalanwang.dutchrailwaysandroidclient.ui.util.asRectInWindow
import com.jeffreyalanwang.dutchrailwaysandroidclient.ui.util.copy
import com.jeffreyalanwang.dutchrailwaysandroidclient.ui.util.movedInto
import com.jeffreyalanwang.dutchrailwaysandroidclient.ui.util.shift
import com.jeffreyalanwang.dutchrailwaysandroidclient.zipOnKeys

@Preview(widthDp = 300, heightDp = 200)
@Composable
private fun AmenityBadgePreview() {
    var amenities by remember { mutableStateOf(TrainAmenity.entries.toSet()) }
    var isExpanded by remember { mutableStateOf(true) }
    Card(Modifier.size(300.dp, 200.dp)) {
        Box(
            Modifier
                .fillMaxSize()
                .clickable { isExpanded = !isExpanded },
            contentAlignment = Alignment.Center,
        ) {
            Row(
                verticalAlignment = Alignment.Bottom,
            ) {
                Icon(
                    painterResource(R.drawable.ic_dr_trainservice),
                    "Train icon",
                    Modifier
                        .size(72.dp + 20.dp)
                )

                // Test that we are not clipped by nested layout
                EditAmenityBadgeSet(
                    amenities,
                    onModify = { amenities = it },
                    isExpanded = isExpanded,
                    onSetExpanded = { isExpanded = it },
                    containerModifier = Modifier
                        .shift(-25.dp, -7.5.dp),
                    windowInsets = WindowInsets(top = 20.dp, right = 20.dp),
                )

                // Test that we display expanded badges on top
                Icon(
                    painterResource(R.drawable.ic_draghandle_vertical),
                    "Test content",
                    Modifier
                        .shift(-100.dp, 0.dp)
                        .size(72.dp + 20.dp),
                    tint = Color.Blue,
                )
            }
        }
    }
}

private const val badgeContentProportion = .7f

@SuppressLint("ModifierParameter")
@Composable
fun EditAmenityBadgeSet(
    amenities: Set<TrainAmenity>,
    onModify: ((Set<TrainAmenity>) -> Unit),
    isExpanded: Boolean,
    onSetExpanded: (Boolean) -> Unit,
    windowInsets: WindowInsets,
    contentModifier: Modifier = Modifier,
    containerModifier: Modifier = Modifier,
    collapsedBadgeSize: Dp = 15.dp,
    expandedBadgeSize: Dp = 30.dp,
    color: Color = LocalContentColor.current,
    bgColor: Color = MaterialTheme.colorScheme.background,
) = AmenityBadgeSetBase(
    amenities,
    isExpanded,
    onSetExpanded,
    windowInsets = windowInsets,
    contentModifier = contentModifier,
    containerModifier = containerModifier,
    onModify = onModify,
    collapsedBadgeSize = collapsedBadgeSize,
    expandedBadgeSize = expandedBadgeSize,
    color,
    bgColor,
)

@SuppressLint("ModifierParameter")
@Composable
fun AmenityBadgeSet(
    amenities: Set<TrainAmenity>,
    isExpanded: Boolean,
    onSetExpanded: (Boolean) -> Unit,
    windowInsets: WindowInsets,
    contentModifier: Modifier = Modifier,
    containerModifier: Modifier = Modifier,
    collapsedBadgeSize: Dp = 15.dp,
    expandedBadgeSize: Dp = 30.dp,
    color: Color = LocalContentColor.current,
    bgColor: Color = MaterialTheme.colorScheme.background,
) = AmenityBadgeSetBase(
    amenities,
    isExpanded,
    onSetExpanded,
    windowInsets = windowInsets,
    contentModifier = contentModifier,
    containerModifier = containerModifier,
    onModify = null,
    collapsedBadgeSize = collapsedBadgeSize,
    expandedBadgeSize = expandedBadgeSize,
    color,
    bgColor,
)

@SuppressLint("ModifierParameter")
@Composable
private fun AmenityBadgeSetBase(
    amenities: Set<TrainAmenity>,
    isExpanded: Boolean,
    onSetExpanded: (Boolean) -> Unit,
    windowInsets: WindowInsets,
    contentModifier: Modifier = Modifier,
    containerModifier: Modifier = Modifier,
    onModify: ((Set<TrainAmenity>) -> Unit)? = null,
    collapsedBadgeSize: Dp = 15.dp,
    expandedBadgeSize: Dp = 30.dp,
    color: Color = LocalContentColor.current,
    bgColor: Color = MaterialTheme.colorScheme.background,
) {
    if (amenities.isEmpty()) return Text(
        "No amenities",
        color = color,
        fontStyle = Italic,
        modifier = contentModifier.padding(vertical = 5.dp),
    )

    val isModifiable = (onModify != null)
    var confirmDeleteOf by
        remember(
            isExpanded, // Reset to null when collapsed
            isModifiable,
            amenities.size
        ) { mutableStateOf<Int?>(null) }

    /** Flips the [confirmDeleteOf] state between null and not null. */
    fun toggleConfirmDelete(index: Int) {
        confirmDeleteOf = if (confirmDeleteOf == null) index
                          else null
    }

    val gap = (-1 * (1 - badgeContentProportion) * collapsedBadgeSize / 2)

    ExpandableBadgeSet(
        isExpanded,
        onSetExpanded,
        collapsedBadgeSize = collapsedBadgeSize,
        expandedBadgeSize = expandedBadgeSize,
        windowInsets = windowInsets,
        contentModifier = contentModifier,
        containerModifier = containerModifier,
        collapsedGap = gap,
        expandedGap = -gap,
        badgesToLabels =
            amenities.associateWithIndexed { i, it ->
                val isConfirmingDelete = (confirmDeleteOf == i)

                val badge = @Composable {
                    if (!isConfirmingDelete) AmenityBadge(
                        it,
                        modifier = Modifier
                            .letIf<Modifier>(isExpanded && isModifiable) { m ->
                                m.clickable { toggleConfirmDelete(i) }
                            },
                        color = color,
                        bgColor = bgColor
                    )
                    else DeleteBadge(
                        Modifier
                            // [DeleteBadge] can always assume
                            // [isExpanded == true && isModifiable == true]
                            .clickable {
                                onModify!!(amenities.minus(it))
                            }
                    )
                }

                val label = @Composable {
                    Text(
                        if (confirmDeleteOf != i) it.friendlyName
                        else "Delete ${it.friendlyName}?",

                        style = MaterialTheme.typography.labelLarge,
                        softWrap = false,
                        maxLines = 1,
                        modifier = Modifier
                            // Can always assume [isExpanded == true]
                            .letIf<Modifier>(isModifiable) { m ->
                                m.clickable { toggleConfirmDelete(i) }
                            },
                    )
                }

                badge to label
            }
    )
}

/**
 *  Handles visual effects.
 *  @param collapsedBadgeSize
 *  @param expandedBadgeSize
 *      These two parameters describe the size the badge slots will voluntarily
 *      be, not the size to which they should be coerced by this composable.
 */
@SuppressLint("ModifierParameter")
@Composable
private fun ExpandableBadgeSet(
    isExpanded: Boolean,
    onSetExpanded: (Boolean) -> Unit,
    collapsedBadgeSize: Dp,
    expandedBadgeSize: Dp,
    windowInsets: WindowInsets,
    contentModifier: Modifier = Modifier,
    containerModifier: Modifier = Modifier,
    collapsedGap: Dp = 0.dp,
    expandedGap: Dp = 0.dp,
    badgesToLabels: Map<TrainAmenity, Pair<@Composable () -> Unit, @Composable () -> Unit>>,
) = with (ExpandableBadgeSetUtilScope) {

    var animatingTo by remember(isExpanded) {
        mutableStateOf(
            if (isExpanded) Target.Expanded
            else Target.Collapsed
            // [Target.None] is set by [onAnimComplete()]
        )
    }
    var containerInWindow by remember { mutableStateOf(IntRect.Zero) }
    var popupSize by remember { mutableStateOf(IntSize.Zero) }

    val (motionSpecX, motionSpecY, motionSpecSize) = getMotionSpecs(animatingTo)

    /** Coordinates (in Px) of each item. */
    val destLocations = remember { mutableStateMapOf<TrainAmenity, IntOffset>() }
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
                IntOffset(
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
     * a way to directly animate [IntOffset] values with different [AnimationSpec]s
     * for x & y axes; however, that would require access to several complex
     * internal/private components behind Compose animation API.
     */
    val animLocations: Map<TrainAmenity, IntOffset> =
        destLocations.mapValues { (amenity, coords) ->
            key(amenity) {
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
            animatingTo = Target.Neither
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
            Layout(
                modifier = contentModifier
                    .clickable(null, null) { onSetExpanded(!isExpanded) },
                content = {
                    for ((amenity, composables) in badgesToLabels) {
                        Item(
                            id = amenity,
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
                val placeables = measurables.fastMap { it.measure(itemConstraints) }
                val placeablesWithId = placeables.map { it.amenity to it }

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
    }
}

@Suppress("ClassName")
private object ExpandableBadgeSetUtilScope {
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
        id: TrainAmenity,
        badge: @Composable () -> Unit,
        label: @Composable () -> Unit,
        isExpanded: Boolean,
        badgeLabelGap: Dp,
    ) = with(AmenityModifierScope) {
        Row(
            Modifier.id(amenity = id),
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
        modifier: Modifier = Modifier,
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
                    color = White,
                    alpha = 1f * progress,
                    radius = 3.dp,
                    spread = 2.dp * progress,
                ),
            ),
            content = content
        )
    }

    val Placeable.amenity
        get() = this.parentData as TrainAmenity

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

    private object AmenityModifierScope {
        fun Modifier.id(amenity: TrainAmenity) =
            this then AmenityIdModifierElement(amenity)

        @Immutable
        data class AmenityIdModifierElement(val amenity: TrainAmenity) :
            ModifierNodeElement<AmenityIdModifierNode>() {
            override fun create() = AmenityIdModifierNode(amenity)
            override fun update(node: AmenityIdModifierNode) {
                node.amenity = this.amenity
            }
        }

        class AmenityIdModifierNode(var amenity: TrainAmenity) :
            ParentDataModifierNode, Modifier.Node() {
            override fun Density.modifyParentData(parentData: Any?): Any =
                amenity
        }
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
    fun <K> basicLinearLayout(
        layout: LayoutAxis,
        keyedPlaceables: List<Pair<K, Placeable>>,
        gapPx: Int,
    ): Pair<MutableMap<K, IntOffset>, IntSize> {
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

        val endPos = currAxisPos
        val totalAxisDim = endPos - startPos

        val (parentWidth, parentHeight) = ac_to_xy(totalAxisDim, maxCrossDim)
        return positions to IntSize(parentWidth, parentHeight)
    }
}

@Composable
private fun AmenityBadge(
    amenity: TrainAmenity,
    modifier: Modifier = Modifier,
    color: Color = LocalContentColor.current,
    bgColor: Color = Transparent,
) = Badge(AppIcons.Amenity(amenity), amenity.friendlyName, modifier, color, bgColor)

@Composable
private fun DeleteBadge(
    modifier: Modifier = Modifier,
) = Badge(R.drawable.ic_close, "Delete", modifier, White, Red, 0f)

@Composable
private fun Badge(
    icon: Int,
    contentDescription: String,
    modifier: Modifier,
    color: Color,
    bgColor: Color,
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
            painterResource( icon ),
            contentDescription = contentDescription,
            tint = color,
            modifier = Modifier.fillMaxSize(badgeContentProportion)
        )
    }
}