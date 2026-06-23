package com.jeffreyalanwang.dutchrailwaysandroidclient.ui.components

import androidx.compose.animation.core.animateFloatAsState
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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Color.Companion.Transparent
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontStyle.Companion.Italic
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.jeffreyalanwang.dutchrailwaysandroidclient.R
import com.jeffreyalanwang.dutchrailwaysandroidclient.TrainAmenity
import com.jeffreyalanwang.dutchrailwaysandroidclient.ui.util.AppIcons
import java.util.EnumSet

@Preview(widthDp = 300, heightDp = 200)
@Composable
private fun AmenityBadgePreview() {
    var isExpanded by remember { mutableStateOf(true) }
    Card(Modifier.clickable { isExpanded = !isExpanded }) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Row(verticalAlignment = Alignment.Bottom) {
                Icon(
                    painterResource(R.drawable.ic_dr_trainservice),
                    "Train icon",
                    Modifier
                        .size(72.dp + 20.dp)
                )

                AmenityBadgeSet(
                    EnumSet.allOf(TrainAmenity::class.java),
                    isExpanded = isExpanded,
                    modifier = Modifier.offset(x = -25.dp, y = -7.5.dp)
                )
            }
        }
    }
}

private const val badgeContentProportion = .7f

@Composable
fun AmenityBadgeSet(
    amenities: EnumSet<TrainAmenity>,
    modifier: Modifier = Modifier,
    isExpanded: Boolean = false,
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
                AmenityBadge(
                    it,
                    modifier = Modifier.size(height),
                    color = color,
                    bgColor = bgColor
                )
                if (progressX > 0f) {
                    Text(
                        it.friendlyName,
                        style = MaterialTheme.typography.labelSmall,
                        softWrap = false,
                        maxLines = 1,
                        modifier = Modifier
                            .alpha(progressX)
                            .sizeIn(maxWidth = 0.dp)
                            .wrapContentWidth(
                                unbounded = true,
                                align = Alignment.Start
                            )
                            .padding(start = 5.dp)
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
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .defaultMinSize(15.dp)
            .drawWithContent {
                val circleOuterRadius = size.minDimension / 2
                val borderThickness = size.minDimension / 12

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
            painterResource( AppIcons.Amenity(amenity) ),
            contentDescription = amenity.friendlyName,
            tint = color,
            modifier = Modifier.fillMaxSize(badgeContentProportion)
        )
    }
}