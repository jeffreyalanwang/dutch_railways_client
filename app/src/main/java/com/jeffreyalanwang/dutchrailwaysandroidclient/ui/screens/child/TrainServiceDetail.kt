package com.jeffreyalanwang.dutchrailwaysandroidclient.ui.detailScreens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.plus
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Color.Companion.Transparent
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontStyle.Companion.Italic
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.jeffreyalanwang.dutchrailwaysandroidclient.BackendApi
import com.jeffreyalanwang.dutchrailwaysandroidclient.PassService
import com.jeffreyalanwang.dutchrailwaysandroidclient.R
import com.jeffreyalanwang.dutchrailwaysandroidclient.ServiceStop
import com.jeffreyalanwang.dutchrailwaysandroidclient.TrainAmenity
import com.jeffreyalanwang.dutchrailwaysandroidclient.getCurrStop
import com.jeffreyalanwang.dutchrailwaysandroidclient.ui.StationDetailRoute
import com.jeffreyalanwang.dutchrailwaysandroidclient.ui.components.DiscreteGridControl
import com.jeffreyalanwang.dutchrailwaysandroidclient.ui.components.DiscreteGridRow
import com.jeffreyalanwang.dutchrailwaysandroidclient.ui.components.LineSegmentWithPoint
import com.jeffreyalanwang.dutchrailwaysandroidclient.ui.util.AppIcons
import com.jeffreyalanwang.dutchrailwaysandroidclient.ui.util.AppStringFormats
import com.jeffreyalanwang.dutchrailwaysandroidclient.ui.util.horizontalOnly
import com.jeffreyalanwang.dutchrailwaysandroidclient.ui.util.verticalOnly
import kotlinx.coroutines.launch
import java.time.ZonedDateTime
import java.util.EnumSet

@Preview
@Composable
private fun TrainServiceDetailPreview() {
    val snackbarHostState = remember { SnackbarHostState() }
    val snackbarEffectScope = rememberCoroutineScope()

    TrainServiceDetailScreen(
        BackendApi.get_pass_service(119),
        onNavigate = { stationRoute ->
            snackbarEffectScope.launch {
                snackbarHostState.showSnackbar(
                    stationRoute.toString(),
                    withDismissAction = true
                )
            }
        },
        onNavigateBack = {
            snackbarEffectScope.launch {
                snackbarHostState.showSnackbar(
                    "Back",
                    withDismissAction = true
                )
            }
        },
    )

    SnackbarHost(hostState = snackbarHostState)
}

@Composable
fun TrainServiceDetailScreen(
    service: PassService,
    onNavigate: (StationDetailRoute) -> Unit,
    onNavigateBack: () -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Train") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            painterResource(R.drawable.ic_back),
                            contentDescription = "Back"
                        )
                    }

                }
            )
        },
        modifier = Modifier.fillMaxSize(),
    ) { innerPadding ->
        Box(Modifier.verticalScroll(rememberScrollState())) {
            Card(Modifier.padding(innerPadding + PaddingValues(10.dp))) {
                TrainServiceDetail(
                    service,
                    onNavigate,
                    Modifier.padding(vertical = 20.dp)
                )
            }
        }

    }
}

@Composable
fun TrainServiceDetail(
    service: PassService,
    onNavigate: (StationDetailRoute) -> Unit,
    modifier: Modifier = Modifier
) {
    val stops = remember { service.getStops() }

    Column(modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.Bottom) {
            // Icon (based on rolling stock)
            Icon(
                painterResource(AppIcons.Trainset(service.trainset)),
                "Train icon",
                Modifier
                    .size(72.dp + 20.dp)
            )

            // Amenities TODO add popup with names + rolling stock name
            AmenityBadgeSet(
                service.amenities,
                modifier=Modifier.offset(x=-25.dp, y=-7.5.dp)
            )
        }

        // Name
        Text(
            service.title,
            style=MaterialTheme.typography.displaySmall,
            modifier=Modifier.padding(horizontal=10.dp)
        )

        Spacer(Modifier.height(10.dp))

        // Stops (arrive; depart; station)
        Stops(stops, onNavigate, padding=PaddingValues(horizontal=10.dp))
    }
}

private const val badgeContentProportion = .7f

@Composable
private fun AmenityBadgeSet(
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
            AmenityBadge(
                it,
                modifier = Modifier.size(height),
                color = color,
                bgColor = bgColor
            )
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

@Composable
private fun Stops(
    stops: List<ServiceStop>,
    onNavigate: (StationDetailRoute)-> Unit,
    modifier: Modifier = Modifier,
    padding: PaddingValues = PaddingValues.Zero,
) {
    val currStopState by remember { mutableIntStateOf(getCurrStop(stops).index) }
    val timetableGridControl = remember(stops) { DiscreteGridControl() }
    // TODO on stop departure, update currStopState and set the timer for the next stop down

    Column(modifier.padding(padding.verticalOnly())) {
        stops.forEachIndexed { i, stop ->
            Stop(
                arriveTime = if (i == 0)            null else stop.arrival,
                departTime = if (i == stops.size-1) null else stop.departure,
                stationName = stop.getStation().name,
                isCurrStop = (i == currStopState),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        onNavigate(StationDetailRoute(stop.stationId))
                    }
                    .padding(padding.horizontalOnly()),
                discreteGridControl = timetableGridControl,
            )
        }
    }
}

@Composable
private fun Stop(
    stationName: String,
    arriveTime: ZonedDateTime?,
    departTime: ZonedDateTime?,
    isCurrStop: Boolean,
    discreteGridControl: DiscreteGridControl,
    modifier: Modifier = Modifier,
    itemPadding: Dp = 5.dp,
    lineWidth: Dp = 20.dp,
) {
    val isFirstStop = arriveTime == null
    val isLastStop  = departTime == null

    Row(modifier.height(IntrinsicSize.Min)) {
        LineSegmentWithPoint(
            lineThickness = lineWidth,
            isStart = isFirstStop,
            isEnd = isLastStop,
            isPointHighlighted = isCurrStop,
        )
        Spacer(Modifier.width(5.dp))
        Column(Modifier.weight(1f)) { // TODO text color by time
            if (!isFirstStop) Spacer(Modifier.height(itemPadding))
            Text(stationName, fontWeight = FontWeight.Bold)
            DiscreteGridRow(discreteGridControl, gap = 10.dp) {
                if (isFirstStop) Spacer(Modifier) else Text(
                    "Arrival: ${AppStringFormats.Time(arriveTime)}",
                    Modifier.alpha(.5f),
                )
                if (isLastStop) Spacer(Modifier) else Text(
                    "Departure: ${AppStringFormats.Time(departTime)}",
                    Modifier.alpha(.5f)
                )
            }
            if (!isLastStop) Spacer(Modifier.height(itemPadding))
        }
    }
}
