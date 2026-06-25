package com.jeffreyalanwang.dutchrailwaysandroidclient.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.spring
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.dropShadow
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Color.Companion.Red
import androidx.compose.ui.graphics.Color.Companion.Transparent
import androidx.compose.ui.graphics.Color.Companion.White
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.shadow.Shadow
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontStyle.Companion.Italic
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.jeffreyalanwang.dutchrailwaysandroidclient.R
import com.jeffreyalanwang.dutchrailwaysandroidclient.TrainAmenity
import com.jeffreyalanwang.dutchrailwaysandroidclient.letIf
import com.jeffreyalanwang.dutchrailwaysandroidclient.ui.util.AppIcons

@Preview(widthDp = 300, heightDp = 200)
@Composable
private fun AmenityBadgePreview() {
    var amenities by remember { mutableStateOf(TrainAmenity.entries.toSet()) }
    var isExpanded by remember { mutableStateOf(true) }
    Card {
        Box(
            Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center,
        ) {
            Row(
                Modifier.onFocusChanged { isExpanded = it.hasFocus },
                verticalAlignment = Alignment.Bottom,
            ) {
                Icon(
                    painterResource(R.drawable.ic_dr_trainservice),
                    "Train icon",
                    Modifier
                        .size(72.dp + 20.dp)
                )

                AmenityBadgeSet(
                    amenities,
                    isExpanded = isExpanded,
                    onModify = { amenities = it },
                    modifier = Modifier.offset(x = -25.dp, y = -7.5.dp)
                )
            }
        }
    }
}

private const val badgeContentProportion = .7f

/** Placed behind [AmenityBadgeSet] when it is in expanded state. */
@Composable
fun Modifier.glow()
    = this.dropShadow(
        MaterialTheme.shapes.small,
        Shadow(
            color = White,
            alpha = .5f,
            radius = 10.dp,
            spread = 5.dp,
            offset = DpOffset.Zero,
        ),
    )

@Composable
fun EditAmenityBadgeSet(
    amenities: Set<TrainAmenity>,
    modifier: Modifier = Modifier,
    isExpanded: Boolean = false,
    onModify: ((Set<TrainAmenity>) -> Unit)? = null,
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

    val gap = (-1 * (1 - badgeContentProportion) * height.value / 2).dp

    val badgeSizePx = with (LocalDensity.current) { height.toPx() }
    val gapPx = with (LocalDensity.current) {
        gap.toPx()
    }

    val isModifiable = (onModify != null)
    var confirmDeleteOf by remember { mutableStateOf<TrainAmenity?>(null) }

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

    fun xOffset(i: Int) = progressX * -(badgeSizePx + gapPx) * (i - 1)
    fun yOffset(i: Int) = progressY * -(badgeSizePx - gapPx) * (amenities.size - 2 - i)

    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(gap),
    ) {
        amenities.forEachIndexed { i, it ->
            Row(
                if (progressX == 0f) Modifier
                else Modifier
                    .zIndex(100f)
                    .offset {
                        IntOffset(
                            x = xOffset(i).toInt(),
                            y = yOffset(i).toInt(),
                        )
                    },
                verticalAlignment = Alignment.CenterVertically
            ) {
                // The [DeleteBadge] will only become visible
                // in the case that [isModifiable == true].
                if (confirmDeleteOf != it) {
                    AmenityBadge(
                        it,
                        modifier = Modifier
                            .size(height)
                            .letIf(isExpanded) { m -> m.glow() }
                            .letIf(isExpanded && isModifiable) { m ->
                                m.clickable {
                                    confirmDeleteOf =
                                        if (confirmDeleteOf == null) it
                                            else null
                                }
                            },
                        color = color,
                        bgColor = bgColor
                    )
                } else {
                    DeleteBadge(
                        Modifier
                            .glow()
                            .size(height)
                            .clickable {
                                onModify!!(amenities - it)
                            }
                    )
                }
                AnimatedVisibility (
                    isExpanded,
                    Modifier
                        .sizeIn(maxWidth = 0.dp)
                        .wrapContentWidth(
                            unbounded = true,
                            align = Alignment.Start
                        )
                        .padding(start = 5.dp),
                    fadeIn() +
                        expandHorizontally(
                            spring(
                                Spring.DampingRatioLowBouncy,
                                Spring.StiffnessLow
                            )
                        ),
                    fadeOut(
                        keyframes {
                            durationMillis = 100
                            1f at 0 using FastOutSlowInEasing
                            .25f at 25 using LinearOutSlowInEasing
                            0f at 100
                        }
                    ) + shrinkHorizontally(
                        spring(
                            Spring.DampingRatioLowBouncy,
                            Spring.StiffnessLow
                        )
                    ),
                ) {
                    Text(
                        if (confirmDeleteOf != it) it.friendlyName
                            else "Delete ${it.friendlyName}?",
                        style = MaterialTheme.typography.labelSmall,
                        softWrap = false,
                        maxLines = 1,
                        modifier = Modifier.glow()
                            // This only shows when [isExpanded == true]
                            .letIf<Modifier>(isModifiable) { m ->
                                m.clickable {
                                    confirmDeleteOf =
                                        if (confirmDeleteOf == null) it
                                        else null
                                }
                            },
                    )
                }
            }
        }
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