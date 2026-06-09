package com.jeffreyalanwang.dutchrailwaysandroidclient.ui.detailScreens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.plus
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.rememberCameraPositionState
import com.google.maps.android.compose.rememberUpdatedMarkerState
import com.jeffreyalanwang.dutchrailwaysandroidclient.BackendApi
import com.jeffreyalanwang.dutchrailwaysandroidclient.R
import com.jeffreyalanwang.dutchrailwaysandroidclient.RoutePlan
import com.jeffreyalanwang.dutchrailwaysandroidclient.RoutePlan.Companion.byLeg
import com.jeffreyalanwang.dutchrailwaysandroidclient.calculateBounds
import com.jeffreyalanwang.dutchrailwaysandroidclient.getCurrStop
import com.jeffreyalanwang.dutchrailwaysandroidclient.getMapCameraUpdate
import com.jeffreyalanwang.dutchrailwaysandroidclient.minus
import com.jeffreyalanwang.dutchrailwaysandroidclient.ui.CommonChildRoute
import com.jeffreyalanwang.dutchrailwaysandroidclient.ui.StationDetailRoute
import com.jeffreyalanwang.dutchrailwaysandroidclient.ui.TrainServiceDetailRoute
import com.jeffreyalanwang.dutchrailwaysandroidclient.ui.components.DiscreteGridControl
import com.jeffreyalanwang.dutchrailwaysandroidclient.ui.components.DiscreteGridRow
import com.jeffreyalanwang.dutchrailwaysandroidclient.ui.components.LineSegmentWithPoint
import com.jeffreyalanwang.dutchrailwaysandroidclient.ui.util.AppStringFormats
import com.jeffreyalanwang.dutchrailwaysandroidclient.ui.util.horizontalOnly
import com.jeffreyalanwang.dutchrailwaysandroidclient.ui.util.verticalOnly
import kotlinx.coroutines.launch
import java.time.ZonedDateTime
import kotlin.time.Duration
import kotlin.time.ExperimentalTime


@Preview
@Composable
private fun RouteDetailPreview() {
    val snackbarHostState = remember { SnackbarHostState() }
    val snackbarEffectScope = rememberCoroutineScope()

    RouteDetailScreen(
        BackendApi.get_routes(
            BackendApi.get_station_info(358),
            BackendApi.get_station_info(361),
        ).first(),
        onNavigate = { passServiceRoute ->
            snackbarEffectScope.launch {
                snackbarHostState.showSnackbar(
                    passServiceRoute.toString(),
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
fun RouteDetailScreen(
    route: RoutePlan,
    onNavigate: (CommonChildRoute) -> Unit,
    onNavigateBack: () -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Station") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            painterResource(R.drawable.ic_back),
                            contentDescription = "Back",
                        )
                    }
                }
            )
        },
        modifier = Modifier.fillMaxSize(),
    ) { innerPadding ->
        Box(Modifier.verticalScroll(rememberScrollState())) {
            Card(Modifier.padding(innerPadding + PaddingValues(10.dp))) {
                RouteDetail(
                    route,
                    onNavigate,
                    Modifier.padding(vertical = 20.dp),
                )
            }
        }
    }
}

@Composable
fun RouteDetailWithoutMap(
    route: RoutePlan,
    onNavigate: (CommonChildRoute) -> Unit,
    modifier: Modifier = Modifier,
) = RouteDetailBase(route, onNavigate, modifier)

@Composable
fun RouteDetail(
    route: RoutePlan,
    onNavigate: (CommonChildRoute) -> Unit,
    modifier: Modifier = Modifier,
) = RouteDetailBase(route, onNavigate, modifier, {
    val cameraPositionState = rememberCameraPositionState()

    HorizontalDivider(thickness = Dp.Hairline)

    GoogleMap(
        cameraPositionState = cameraPositionState,
        contentDescription = "Transfer points on map",
        onMapLoaded = {
            cameraPositionState.move(
                route.stops
                    .map { it.getStation().geom }
                    .calculateBounds()
                    .getMapCameraUpdate(64)
            )
        },
        modifier = Modifier
            .fillMaxWidth()
            .sizeIn(minHeight = 200.dp, maxHeight = 400.dp),
    ) {
        for (stops in route.stopsByLayover()) {
            val station = stops.first().stationId.let { BackendApi.get_station_info(it) }
            val timeString = listOfNotNull(
                stops.first().arrival,
                stops.last().departure,
            ).joinToString(" - ") { AppStringFormats.Time(it) }

            Marker(
                state = rememberUpdatedMarkerState(station.geom),
                title = "${timeString}: ${station.name}",
            )
        }
    }

    HorizontalDivider(thickness = Dp.Hairline)
})

@Composable
private fun RouteDetailBase(
    route: RoutePlan,
    onNavigate: (CommonChildRoute) -> Unit,
    modifier: Modifier = Modifier,
    googleMapsSlot: @Composable (() -> Unit)? = null,
) {
    Column (modifier.fillMaxWidth()) {
        Icon(
            painterResource(R.drawable.ic_dr_traintime),
            "Train route details",
            Modifier.size(72.dp + 20.dp)
        )
        Text(
            "Trip from ${route.origin.name} to ${route.destination.name}",
            style = MaterialTheme.typography.displaySmall,
            modifier = Modifier.padding(horizontal=10.dp),
        )
        Spacer(Modifier.height(10.dp))
        Text(
            "%s, %s to %s".format(
                AppStringFormats.TripDuration(route.duration),
                AppStringFormats.Time(route.departTime),
                AppStringFormats.Time(route.arriveTime),
            ),
            modifier = Modifier.padding(horizontal=10.dp)
        )

        Spacer(Modifier.height(10.dp))
        googleMapsSlot?.invoke()
        Spacer(Modifier.height(10.dp))

        Itinerary(
            route,
            padding = PaddingValues(horizontal=10.dp),
            onNavigate = onNavigate
        )
    }
}

@Composable
private fun Itinerary(
    route: RoutePlan,
    onNavigate: (CommonChildRoute)-> Unit,
    modifier: Modifier = Modifier,
    padding: PaddingValues = PaddingValues.Zero,
) {
    val currStopState by remember { mutableIntStateOf(getCurrStop(route.stops).index) }
    val timetableGridControl = remember(route) { DiscreteGridControl() }
    // TODO on stop departure, update currStopState and set the timer for the next stop down

    Column(modifier.padding(padding.verticalOnly())) {
        val lineWidth = 20.dp
        Row(Modifier.padding(padding.horizontalOnly())) {
            Spacer(Modifier.width(lineWidth * 1.5f))
            Spacer(Modifier.width(5.dp))
            Text(
                "Itinerary",
                style = MaterialTheme.typography.titleSmallEmphasized,
                modifier = Modifier.alpha(.75f),
            )
        }

        route.stops
            .withIndex()
            .byLeg { it.value.passServiceId }
            .let {
                it zip (
                    it.drop(1).map { nextLeg -> nextLeg.first }.plus(null)
                )
            }
            .forEach { (legStops, nextStopAfterLeg) ->
                Leg(
                    fromTime = legStops.first.value.departure!!,
                    toTime = legStops.second.value.arrival!!,
                    serviceName = legStops.first.value.getService().title,
                    isCurr = (legStops.second.index == currStopState)
                            || ( legStops.first.index == currStopState
                                 && currStopState == 0 ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            onNavigate(
                                TrainServiceDetailRoute(legStops.first.value.passServiceId)
                            )
                        }
                        .padding(padding.horizontalOnly()),
                    discreteGridControl = timetableGridControl,
                    lineWidth = lineWidth,
                )
                if (nextStopAfterLeg != null) {
                    val legEndStop = legStops.second
                    Layover(
                        duration = legEndStop.value.arrival!!
                                - nextStopAfterLeg.value.run { arrival ?: departure!! },
                        stationName = legEndStop.value.getStation().name,
                        isCurr = (nextStopAfterLeg.index == currStopState),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                onNavigate(StationDetailRoute(legEndStop.value.stationId))
                            }
                            .padding(padding.horizontalOnly()),
                        lineWidth = lineWidth,
                    )
                }
        }
    }
}

@Composable
private fun ColumnScope.Leg(
    serviceName: String,
    fromTime: ZonedDateTime,
    toTime: ZonedDateTime,
    isCurr: Boolean,
    discreteGridControl: DiscreteGridControl,
    modifier: Modifier = Modifier,
    itemPadding: Dp = 5.dp,
    lineWidth: Dp = 20.dp,
) {
    Row(modifier.height(IntrinsicSize.Min)) {
        LineSegmentWithPoint(
            lineThickness = lineWidth,
            isStart = true,
            isEnd = true,
            isPointHighlighted = isCurr,
        )
        Spacer(Modifier.width(5.dp))
        Column(Modifier.weight(1f)) { // TODO text color by time
            Spacer(Modifier.height(itemPadding))
            Text(serviceName, fontWeight = FontWeight.Bold)
            DiscreteGridRow(discreteGridControl, gap = 10.dp) {
                Text(
                    "Departure: ${AppStringFormats.Time(fromTime)}",
                    Modifier.alpha(.5f),
                )
                Text(
                    "Arrival: ${AppStringFormats.Time(toTime)}",
                    Modifier.alpha(.5f)
                )
            }
            Spacer(Modifier.height(itemPadding))
        }
    }
}

@Composable
private fun ColumnScope.Layover(
    stationName: String,
    duration: Duration,
    isCurr: Boolean,
    modifier: Modifier = Modifier,
    itemPadding: Dp = 5.dp,
    lineWidth: Dp = 20.dp,
) {
    Row(modifier.height(IntrinsicSize.Min)) {
        LineSegmentWithPoint(
            lineThickness = lineWidth,
            isStart = true,
            isEnd = true,
            isPointHighlighted = isCurr,
        )
        Spacer(Modifier.width(5.dp))
        Column(Modifier.weight(1f)) { // TODO text color by time
            Spacer(Modifier.height(itemPadding))
            Text(stationName, fontWeight = FontWeight.Bold)
            Text(
                "Wait ${AppStringFormats.TripDuration(duration)}",
                Modifier.alpha(.5f)
            )
            Spacer(Modifier.height(itemPadding))
        }
    }
}
