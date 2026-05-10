package com.jeffreyalanwang.dutchrailwaysandroidclient

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Color.Companion.Transparent
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontStyle.Companion.Italic
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.times
import java.time.Instant.now
import java.time.ZonedDateTime
import java.util.EnumSet
import kotlin.time.ExperimentalTime

@Preview
@Composable
fun TrainServiceDetailTest() {
    val scrollState = rememberScrollState()
    Box(Modifier
        .verticalScroll(scrollState)
        .height(1000.dp)
        .width(550.dp)
    ) {
        TrainServiceDetail(
            BackendApi.get_pass_service(119u),
            modifier = Modifier.padding(10.dp)
        )
    }
}

@Composable
fun TrainServiceDetail(service: PassService, modifier: Modifier = Modifier) {
    Card(modifier) {
        Spacer(Modifier.height(20.dp))

        Row(verticalAlignment = Alignment.Bottom) {
            // Icon (based on rolling stock)
            Icon(
                painterResource(AppIcons.Trainset(service.trainset)),
                "Train icon",
                Modifier
                    .size(72.dp + 20.dp)
            )

            // Amenities TODO add popup with names + rolling stock name
            AmenityBadgeSet(service.amenities, modifier=Modifier.offset(x=-25.dp, y=-7.5.dp))
        }
        // Name
        Text(
            service.title,
            style=MaterialTheme.typography.displaySmall,
            modifier=Modifier.padding(horizontal=10.dp)
        )

        Spacer(Modifier.height(10.dp))

        // Stops (arrive; depart; station)
        Stops(service.getStops(), Modifier.padding(horizontal=10.dp))

        Spacer(Modifier.height(20.dp))
    }
}

const val badgeContentProportion = .7f

@Composable
fun AmenityBadge(
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

@Composable
fun AmenityBadgeSet(
    amenities: EnumSet<TrainAmenity>,
    modifier: Modifier = Modifier,
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

    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(
            (-1 * (1 - badgeContentProportion) * height.value / 2).dp
        ),
    ) {
        amenities.forEach {
            AmenityBadge( it,
            modifier = Modifier.size(height),
            color = color,
            bgColor = bgColor
        )}
    }
}

@Composable
fun StopsLineSegment(
    color: Color, 
    thickness: Dp,
    start: Boolean,
    end: Boolean,
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.()->Unit
) = Box(
    modifier=modifier
        .fillMaxHeight()
        .drawWithCache {
            onDrawBehind {
                if (!start) drawLine(
                    color = color,
                    strokeWidth = thickness.value,
                    start = Offset(this.center.x, -1f),
                    end = this.center + Offset(0f, 1f),
                )
                if (!end) drawLine(
                    color = color,
                    strokeWidth = thickness.value,
                    start = this.center - Offset(0f, 1f),
                    end = Offset(this.center.x, this.size.height + 1),
                )
                if (start xor end) drawCircle(
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
fun StopsLinePoint(
    lineWidth: Dp,
    lineColor: Color,
    accentColor: Color,
    isCurr: Boolean,
    modifier: Modifier = Modifier,
) = Box(
    modifier=modifier
        .sizeIn(minWidth = 1.5 * lineWidth)
        .drawBehind {
            if (isCurr) {
                drawCircle(
                    color = lineColor,
                    radius = (1.5f * lineWidth.value) / 2,
                    style = Fill,
                )
                drawCircle(
                    color = accentColor,
                    radius = (.9f * lineWidth.value) / 2,
                    style = Fill,
                )
            } else {
                drawCircle(
                    color = accentColor,
                    radius = (lineWidth.value / 3) / 2,
                    style = Fill,
                )
            }
        }
)

@Composable
private fun Stop(
    stationName: String,
    arriveTime: ZonedDateTime?,
    departTime: ZonedDateTime?,
    isCurrStop: Boolean,
    modifier: Modifier = Modifier,
    itemPadding: Dp = 5.dp,
    lineWidth: Dp = 20.dp,
    lineColor: Color = MaterialTheme.colorScheme.primary,
    dotColor: Color = MaterialTheme.colorScheme.inversePrimary,
) {
    val isFirstStop = arriveTime == null
    val isLastStop  = departTime == null

    val textMeasurer = rememberTextMeasurer()
    val textLayoutResult = textMeasurer.measure(
        text = "Your potential text",
        style = MaterialTheme.typography.bodyLarge
    )
    val widthPx = textLayoutResult.size.width
    val widthDp = with(LocalDensity.current) { widthPx.toDp() }

    Row(modifier.height(IntrinsicSize.Min)) {
        StopsLineSegment(
            thickness=lineWidth,
            color=lineColor,
            start=isFirstStop,
            end=isLastStop
        ) {
            StopsLinePoint(
                lineWidth=lineWidth,
                lineColor=lineColor,
                accentColor=dotColor,
                isCurr=isCurrStop
            )
        }
        Spacer(Modifier.width(5.dp))
        Column(Modifier.weight(1f)) { // TODO text color by time
            if (!isFirstStop) Spacer(Modifier.height(itemPadding))
            Text(stationName, fontWeight = FontWeight.Bold)
            Row {
                if (!isFirstStop) Text(
                    "Arrival: ${UIStrings.Time(arriveTime)}",
                    Modifier
                        .alpha(.5f)
                        .weight(1f, fill = true),
                )
                if (!isLastStop) Text(
                    "Departure: ${UIStrings.Time(departTime)}",
                    Modifier
                        .alpha(.5f)
                        .weight(1f, fill = true),
                )
            }
            if (!isLastStop) Spacer(Modifier.height(itemPadding))
        }
    }
}

@OptIn(ExperimentalTime::class)
private fun getCurrStop(stops: List<ServiceStop>): IndexedValue<ServiceStop> {
    stops.forEachIndexed { index, stop ->
        if (stop.departure.toInstant() > now()) {
            return IndexedValue(index, stop)
        }
    }
    return IndexedValue(stops.size-1, stops.last())
}

@Composable
fun Stops(stops: List<ServiceStop>, modifier: Modifier = Modifier) {
    val currStopState by remember { mutableIntStateOf(getCurrStop(stops).index) }
    // TODO on stop departure, update currStopState and set the timer for the next stop down

    Column(modifier) {
        stops.forEachIndexed { i, stop ->
            Stop(
                arriveTime = if (i == 0) null else stop.arrival,
                departTime = if (i == stops.size-1) null else stop.departure,
                stationName = stop.getStation().name,
                isCurrStop = (i == currStopState),
                modifier=Modifier.fillMaxWidth(), // TODO make clickable
            )
        }
    }
}