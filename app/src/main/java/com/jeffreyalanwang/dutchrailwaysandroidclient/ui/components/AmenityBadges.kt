package com.jeffreyalanwang.dutchrailwaysandroidclient.ui.components

import android.annotation.SuppressLint
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontStyle.Companion.Italic
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastMaxOfOrDefault
import androidx.compose.ui.util.fastRoundToInt
import androidx.compose.ui.util.fastSumBy
import com.jeffreyalanwang.dutchrailwaysandroidclient.R
import com.jeffreyalanwang.dutchrailwaysandroidclient.TrainAmenity
import com.jeffreyalanwang.dutchrailwaysandroidclient.letIf
import com.jeffreyalanwang.dutchrailwaysandroidclient.runReversed
import com.jeffreyalanwang.dutchrailwaysandroidclient.ui.util.AppIcons

@Preview(widthDp = 300, heightDp = 200)
@Composable
private fun AmenityBadgePreview() {
    var amenities by remember { mutableStateOf(TrainAmenity.entries.toSet()) }
    var isExpanded by remember { mutableStateOf(true) }
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

                Box { // Test that we display expanded badges on top
                    EditAmenityBadgeSet(
                        amenities,
                        isExpanded = isExpanded,
                        onModify = { amenities = it },
                        modifier = Modifier.offset(x = -25.dp, y = -7.5.dp)
                    )

                    Text("Test content", color = Color.Blue)
                }
            }
        }
    }
}

private const val badgeContentProportion = .7f

@Composable
fun EditAmenityBadgeSet(
    amenities: Set<TrainAmenity>,
    onModify: ((Set<TrainAmenity>) -> Unit),
    modifier: Modifier = Modifier,
    isExpanded: Boolean = false,
    height: Dp = 15.dp,
    color: Color = LocalContentColor.current,
    bgColor: Color = MaterialTheme.colorScheme.background,
) = AmenityBadgeSetBase(
    amenities,
    modifier,
    isExpanded,
    onModify,
    height,
    color,
    bgColor,
)

@Composable
fun AmenityBadgeSet(
    amenities: Set<TrainAmenity>,
    modifier: Modifier = Modifier,
    isExpanded: Boolean = false,
    height: Dp = 15.dp,
    color: Color = LocalContentColor.current,
    bgColor: Color = MaterialTheme.colorScheme.background,
) = AmenityBadgeSetBase(
    amenities,
    modifier,
    isExpanded,
    null,
    height,
    color,
    bgColor,
)

@Composable
private fun AmenityBadgeSetBase(
    amenities: Set<TrainAmenity>,
    modifier: Modifier = Modifier,
    isExpanded: Boolean = false,
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
        modifier = modifier,
        collapsedGap = gap,
        expandedGap = -gap,
        badgesToLabels =
            amenities.mapIndexed { i, it ->
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
    modifier: Modifier = Modifier,
    collapsedGap: Dp = 0.dp,
    expandedGap: Dp = 0.dp,
    badgesToLabels: List<Pair<@Composable () -> Unit, @Composable () -> Unit>>,
) {
    @Composable
    fun item(
        badge: @Composable () -> Unit,
        label: @Composable () -> Unit,
    ) {
        Row {
            GlowBox(isExpanded) { badge() }
            Row(
                Modifier
                    .width(0.dp)
                    .wrapContentWidth(
                        Alignment.Start,
                        unbounded = true
                    )
            ) {
                Spacer(Modifier.width(expandedGap))
                AnimatedVisibility(isExpanded) {
                    GlowBox { label() }
                }
            }
        }
    }

    val progressX by animateFloatAsState(
        if (isExpanded) 1f else 0f,
        if (isExpanded) MaterialTheme.motionScheme.fastSpatialSpec()
            else MaterialTheme.motionScheme.slowSpatialSpec(),
    )
    val progressY by animateFloatAsState(
        if (isExpanded) 1f else 0f,
        if (isExpanded) MaterialTheme.motionScheme.slowSpatialSpec()
            else MaterialTheme.motionScheme.fastSpatialSpec(),
    )

    Layout(
        modifier = modifier,
        content = {
            badgesToLabels
                .forEach { (badge, label) -> item(badge, label) }
        }
    ) { measurables, constraints ->
        val placeables = measurables.map { it.measure(constraints) }
        val collapsedGap = collapsedGap.toPx().fastRoundToInt()
        val expandedGap = expandedGap.toPx().fastRoundToInt()

        // These will be the bounds seen by parent composables
        // regardless of whether we have expanded outside of them.
        val collapsedHeight = placeables.fastMaxOfOrDefault(0) { it.height }
        val collapsedWidth = placeables
            .run { fastSumBy { it.width } + (collapsedGap * (size - 1)) }

//        val expandedHeight = placeables
//            .run { fastSumBy { it.height } + (expandedGap * (size - 1)) }

        layout(collapsedWidth, collapsedHeight) {
            // TODO vertical position when expanded: find a place within window insets/parent composable

            // Collapsed layout is a row, expanded is a column;
            // so these are constants
            val collapsedY = 0
            val expandedX = (placeables.firstOrNull()?.width ?: 0) / 2

            val xPos = if (progressX == 1f) null // use [expandedX]
                else placeables
                    .dropLast(1)
                    .runningFold(0) { acc, placeable ->
                        acc + placeable.width + collapsedGap
                    }
                    .map { collapsedX ->
                        collapsedX +
                        progressX * (expandedX - collapsedX)
                    }
            val yPos = if (progressY == 0f) null // use [collapsedY]
                else placeables
                    .drop(1) // drop the top item, since we work from bottom
                    .runReversed {
                        runningFold(
                            if (size < 2) 0     // <, not <=, since we already dropped 1
                            else first().height // start one item-height lower than collapsed-row
                        ) { acc, placeable ->
                            acc - placeable.height - expandedGap
                        }
                    }
                    .map { expandedY ->
                        collapsedY +
                        progressY * (expandedY - collapsedY)
                    }

            placeables.forEachIndexed { i, placeable ->
                placeable.placeRelative(
                    xPos?.get(i)?.fastRoundToInt() ?: expandedX,
                    yPos?.get(i)?.fastRoundToInt() ?: collapsedY,
                )
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
                alpha = .75f * progress,
                radius = 2.5.dp,
                spread = 2.5.dp * progress,
                offset = DpOffset.Zero,
            ),
        ),
        content = content
    )
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