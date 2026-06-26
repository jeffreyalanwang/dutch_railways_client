package com.jeffreyalanwang.dutchrailwaysandroidclient.ui.components

import android.annotation.SuppressLint
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.IntState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.asIntState
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
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.node.ParentDataModifierNode
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontStyle.Companion.Italic
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.times
import androidx.compose.ui.util.fastMap
import androidx.compose.ui.util.fastSumBy
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import com.jeffreyalanwang.dutchrailwaysandroidclient.R
import com.jeffreyalanwang.dutchrailwaysandroidclient.TrainAmenity
import com.jeffreyalanwang.dutchrailwaysandroidclient.associateWithIndexed
import com.jeffreyalanwang.dutchrailwaysandroidclient.letIf
import com.jeffreyalanwang.dutchrailwaysandroidclient.ui.components.ExpandableBadgeSetUtilScope.Target
import com.jeffreyalanwang.dutchrailwaysandroidclient.ui.util.AppIcons
import com.jeffreyalanwang.dutchrailwaysandroidclient.ui.util.shift
import com.jeffreyalanwang.dutchrailwaysandroidclient.zipOnKeys

@Preview(widthDp = 300, heightDp = 200)
@Composable
private fun AmenityBadgePreview() {
    val snackbarState = remember { SnackbarHostState() }
    var amenities by remember { mutableStateOf(TrainAmenity.entries.toSet()) }
    var isExpanded by remember { mutableStateOf(false) }
    Card {
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

                EditAmenityBadgeSet(
                    amenities,
                    onModify = { amenities = it },
                    isExpanded = isExpanded,
                    onSetExpanded = { isExpanded = it },
                    containerModifier = Modifier
                        .shift(-25.dp, -7.5.dp),
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

@Composable
fun EditAmenityBadgeSet(
    amenities: Set<TrainAmenity>,
    onModify: ((Set<TrainAmenity>) -> Unit),
    isExpanded: Boolean,
    onSetExpanded: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    containerModifier: Modifier = Modifier,
    height: Dp = 15.dp,
    color: Color = LocalContentColor.current,
    bgColor: Color = MaterialTheme.colorScheme.background,
) = AmenityBadgeSetBase(
    amenities,
    isExpanded,
    onSetExpanded,
    modifier = modifier,
    containerModifier = containerModifier,
    onModify = onModify,
    height,
    color,
    bgColor,
)

@Composable
fun AmenityBadgeSet(
    amenities: Set<TrainAmenity>,
    isExpanded: Boolean,
    onSetExpanded: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    containerModifier: Modifier = Modifier,
    height: Dp = 15.dp,
    color: Color = LocalContentColor.current,
    bgColor: Color = MaterialTheme.colorScheme.background,
) = AmenityBadgeSetBase(
    amenities,
    isExpanded,
    onSetExpanded,
    modifier = modifier,
    containerModifier = containerModifier,
    onModify = null,
    height,
    color,
    bgColor,
)

@Composable
private fun AmenityBadgeSetBase(
    amenities: Set<TrainAmenity>,
    isExpanded: Boolean,
    onSetExpanded: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    containerModifier: Modifier = Modifier,
    onModify: ((Set<TrainAmenity>) -> Unit)? = null,
    height: Dp = 15.dp,
    color: Color = LocalContentColor.current,
    bgColor: Color = MaterialTheme.colorScheme.background,
) {
    if (amenities.isEmpty()) return Text(
        "No amenities",
        color = color,
        fontStyle = Italic,
        modifier = modifier.padding(vertical = 5.dp),
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

    val gap = (-1 * (1 - badgeContentProportion) * height.value / 2).dp

    ExpandableBadgeSet(
        isExpanded,
        onSetExpanded,
        modifier = modifier,
        containerModifier = containerModifier,
        collapsedGap = gap,
        expandedGap = -gap,
        badgesToLabels =
            amenities.associateWithIndexed { i, it ->
                val isConfirmingDelete = (confirmDeleteOf == i)

                val badge = @Composable {
                    if (!isConfirmingDelete) AmenityBadge(
                        it,
                        modifier = Modifier.size(height)
                            .letIf(isExpanded && isModifiable) { m ->
                                m.clickable { toggleConfirmDelete(i) }
                            },
                        color = color,
                        bgColor = bgColor
                    )
                    else DeleteBadge(
                        Modifier
                            .size(height)
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

                        style = MaterialTheme.typography.labelSmall,
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

/** Handles visual effects. */
@Composable
private fun ExpandableBadgeSet(
    isExpanded: Boolean,
    onSetExpanded: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    containerModifier: Modifier = Modifier,
    collapsedGap: Dp = 0.dp,
    expandedGap: Dp = 0.dp,
    badgesToLabels: Map<TrainAmenity, Pair<@Composable () -> Unit, @Composable () -> Unit>>,
) = with (ExpandableBadgeSetUtilScope) {

    /** Used to set the AnimationSpecs (motionSpecX, motionSpecY). */
    var animatingTo by remember { mutableStateOf(Target.Neither) }
    LaunchedEffect(isExpanded) {
        animatingTo = if (isExpanded) Target.Expanded
                      else Target.Collapsed
        // [Target.None] is set by [onAnimComplete()]
    }
    val (motionSpecX, motionSpecY) = with (MaterialTheme.motionScheme) {
        when (animatingTo) {
            Target.Expanded ->
                fastSpatialSpec<Int>() to slowSpatialSpec<Int>()
            Target.Collapsed ->
                slowSpatialSpec<Int>() to fastSpatialSpec<Int>()
            Target.Neither ->
                defaultSpatialSpec<Int>() to defaultSpatialSpec<Int>()
        }
    }

    /** Coordinates (in Px) of each item. */
    val destLocations = remember { mutableStateMapOf<TrainAmenity, IntOffset>() }
    var collapsedHeight by remember { mutableStateOf(15.dp) }
    var destPopupOffset by remember { mutableStateOf(IntOffset.Zero) }

    lateinit var onAnimComplete: () -> Unit
    /** The actual coordinates of each item on screen, as animated states. */
    val animLocationStates: Map<TrainAmenity, Pair<IntState, IntState>> =
        destLocations.mapValues { (amenity, coords) ->
            key(amenity) {
                animateIntAsState(coords.x, motionSpecX) { onAnimComplete() }
                    .asIntState() to
                animateIntAsState(coords.y, motionSpecY) { onAnimComplete() }
                    .asIntState()
            }
        }
    val animatedPopupOffsetStates =
        animateIntAsState(destPopupOffset.x, motionSpecX) { onAnimComplete() }
            .asIntState() to
        animateIntAsState(destPopupOffset.y, motionSpecY) { onAnimComplete() }
            .asIntState()
    onAnimComplete = {
        val allComplete =
            (destLocations zipOnKeys animLocationStates).values
            .plus(destPopupOffset to animatedPopupOffsetStates)
            .all { (dest, curr) ->
                dest.x == curr.first.intValue &&
                dest.y == curr.second.intValue
            }
        if (allComplete) {
            animatingTo = Target.Neither
        }
    }

    Box(
        containerModifier
            .size(
                badgesToLabels.size * (collapsedHeight + collapsedGap) - collapsedGap,
                collapsedHeight,
            )
    ) {
        Popup(
            alignment = Alignment.TopStart,
            offset = animatedPopupOffsetStates
                .run { IntOffset(first.intValue, second.intValue) },
            properties = PopupProperties(
                focusable = isExpanded, // seems required to use [onDismissRequest],
                                        // but blocks any other click listeners on the screen
                dismissOnClickOutside = true,
            ),
            onDismissRequest = if (isExpanded) { { onSetExpanded(false) } } else null,
        ) {
            Layout(
                modifier = modifier.clickable { onSetExpanded(!isExpanded) },
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
                val placeables = measurables.fastMap { it.measure(constraints) }
                val placeablesWithId = placeables.map { it.amenity to it }

                // Prepare destination (i.e. non-animated) position values
                // TODO vertical position when expanded: find a place within window insets/parent composable

                val (locations, parentSize) =
                    basicLinearLayout(
                        layout = if (!isExpanded) Row else Column,
                        keyedPlaceables = placeablesWithId,
                        gapPx = if (!isExpanded)
                            collapsedGap.roundToPx()
                        else expandedGap.roundToPx()
                    )

                destLocations.putAll(locations)
                if (!isExpanded) {
                    collapsedHeight = parentSize.height.toDp()
                }
                destPopupOffset = if (!isExpanded) IntOffset.Zero
                    else IntOffset(
                        collapsedHeight.roundToPx() / 2,
                        placeables
                            .dropLast(2)
                            .fastSumBy { it.height + expandedGap.roundToPx() }
                            .unaryMinus()
                    )

                layout(
                    width = parentSize.width,
                    height = parentSize.height,
                ) {
                    // zipOnKeys skips placing values in [placeables]
                    // if they are not yet in [animLocationStates]
                    placeablesWithId.toMap()
                        .zipOnKeys(animLocationStates) { placeable, (x, y) ->
                            placeable.placeRelative(x.intValue, y.intValue)
                        }
                }
            }
        }
    }

}

private object ExpandableBadgeSetUtilScope {
    enum class _target { Expanded, Collapsed, Neither }
    typealias Target = _target // allows visibility in scope

    /** Build each item with animated glow + animated label visibility. */
    @Composable
    fun Item(
        id: TrainAmenity,
        badge: @Composable () -> Unit,
        label: @Composable () -> Unit,
        isExpanded: Boolean,
        badgeLabelGap: Dp,
    ) = with(AmenityModifierScope) {
        Row(Modifier.id(amenity = id)) {
            GlowBox(isExpanded) { badge() }
            AnimatedVisibility(isExpanded) {
                GlowBox(
                    modifier = Modifier.padding(start = badgeLabelGap)
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