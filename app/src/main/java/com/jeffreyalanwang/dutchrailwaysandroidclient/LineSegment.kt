package com.jeffreyalanwang.dutchrailwaysandroidclient

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.times

/**
 * A vertical line segment which can be used to
 * represent a transit route.
 */
@Composable
fun LineSegment(
    color: Color,
    thickness: Dp,
    isStart: Boolean,
    isEnd: Boolean,
    modifier: Modifier = Modifier.Companion,
    content: @Composable BoxScope.()->Unit
) = Box(
    modifier = modifier
        .fillMaxHeight()
        .drawWithCache {
            onDrawBehind {
                if (!isStart) drawLine(
                    color = color,
                    strokeWidth = thickness.value,
                    start = Offset(this.center.x, -1f),
                    end = this.center + Offset(0f, 1f),
                )
                if (!isEnd) drawLine(
                    color = color,
                    strokeWidth = thickness.value,
                    start = this.center - Offset(0f, 1f),
                    end = Offset(this.center.x, this.size.height + 1),
                )
                if (isStart xor isEnd) drawCircle(
                    // Cap
                    color = color,
                    radius = thickness.value / 2,
                    center = this.center,
                    style = Fill,
                )
            }
        },
    contentAlignment = Alignment.Center,
    content = content
)

@Composable
fun LineSegmentPoint(
    color: Color,
    lineWidth: Dp,
    lineColor: Color,
    highlight: Boolean,
    modifier: Modifier = Modifier,
) = Box(
    modifier = modifier
        .sizeIn(minWidth = 1.5 * lineWidth)
        .drawBehind {
            if (highlight) {
                drawCircle(
                    color = lineColor,
                    radius = (1.5f * lineWidth.value) / 2,
                    style = Fill,
                )
                drawCircle(
                    color = color,
                    radius = (.9f * lineWidth.value) / 2,
                    style = Fill,
                )
            } else {
                drawCircle(
                    color = color,
                    radius = (lineWidth.value / 3) / 2,
                    style = Fill,
                )
            }
        }
)

/**
 * See [LineSegment] and [LineSegmentPoint].
 */
@Composable
fun LineSegmentWithPoint(
    pointColor: Color = MaterialTheme.colorScheme.inversePrimary,
    lineColor: Color = MaterialTheme.colorScheme.primary,
    lineThickness: Dp,
    isStart: Boolean,
    isEnd: Boolean,
    highlightPoint: Boolean,
    modifier: Modifier = Modifier,
    pointModifier: Modifier = Modifier,
) = LineSegment(
    color = lineColor,
    thickness = lineThickness,
    isStart = isStart,
    isEnd = isEnd,
    modifier = modifier,
) {
    LineSegmentPoint(
        color = pointColor,
        lineWidth = lineThickness,
        lineColor = lineColor,
        highlight = highlightPoint,
        modifier = pointModifier,
    )
}