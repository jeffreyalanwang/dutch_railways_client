package com.jeffreyalanwang.dutchrailwaysandroidclient.ui.detailScreens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.google.android.gms.maps.model.CameraPosition
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.rememberCameraPositionState
import com.google.maps.android.compose.rememberUpdatedMarkerState
import com.jeffreyalanwang.dutchrailwaysandroidclient.BackendApi
import com.jeffreyalanwang.dutchrailwaysandroidclient.R
import com.jeffreyalanwang.dutchrailwaysandroidclient.ServiceStop
import com.jeffreyalanwang.dutchrailwaysandroidclient.Station
import com.jeffreyalanwang.dutchrailwaysandroidclient.ui.PassServiceDetailNavArgs
import com.jeffreyalanwang.dutchrailwaysandroidclient.ui.components.DiscreteGridControl
import com.jeffreyalanwang.dutchrailwaysandroidclient.ui.components.DiscreteGridRow
import com.jeffreyalanwang.dutchrailwaysandroidclient.ui.util.AppIcons
import com.jeffreyalanwang.dutchrailwaysandroidclient.ui.util.AppStringFormats
import com.jeffreyalanwang.dutchrailwaysandroidclient.ui.util.horizontalOnly
import com.jeffreyalanwang.dutchrailwaysandroidclient.ui.util.verticalOnly
import kotlinx.coroutines.launch
import java.time.ZonedDateTime

private const val EM_DASH = "—"

@Preview
@Composable
private fun StationDetailPreview() {
    val snackbarHostState = remember { SnackbarHostState() }
    val snackbarEffectScope = rememberCoroutineScope()

    StationDetailScreen(
        BackendApi.get_station_info(358),
        onNavigate = { passServiceNavArgs ->
            snackbarEffectScope.launch {
                snackbarHostState.showSnackbar(
                    passServiceNavArgs.toString(),
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
fun StationDetailScreen(
    station: Station,
    onNavigate: (PassServiceDetailNavArgs) -> Unit,
    onNavigateBack: () -> Unit,
    actionsSlot: @Composable (RowScope.() -> Unit)? = null,
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
                },
                actions = actionsSlot ?: {}
            )
        },
        modifier = Modifier.fillMaxSize(),
    ) { innerPadding ->

        Box(Modifier.verticalScroll(rememberScrollState())) {
            Card(Modifier.padding(innerPadding + PaddingValues(10.dp))) {
                StationDetail(
                    station,
                    onNavigate,
                    Modifier.padding(vertical = 20.dp),
                )
            }
        }
    }
}

@Composable
fun StationDetailWithoutMap(
    station: Station,
    onNavigate: (PassServiceDetailNavArgs) -> Unit,
    modifier: Modifier = Modifier,
) = StationDetailBase(station, onNavigate, modifier)

@Composable
fun StationDetail(
    station: Station,
    onNavigate: (PassServiceDetailNavArgs) -> Unit,
    modifier: Modifier = Modifier,
) = StationDetailBase(station, onNavigate, modifier, {
    val stationMarkerState = rememberUpdatedMarkerState(position = station.geom)
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(station.geom, 14f)
    }

    HorizontalDivider(thickness = Dp.Hairline)

    GoogleMap(
        cameraPositionState = cameraPositionState,
        contentDescription = "Station on map",
        modifier = Modifier
            .fillMaxWidth()
            .sizeIn(minHeight = 200.dp, maxHeight = 400.dp),
    ) {
        Marker(
            state = stationMarkerState,
            title = station.name,
        )
    }

    HorizontalDivider(thickness = Dp.Hairline)
})

@Composable
private fun StationDetailBase(
    station: Station,
    onNavigate: (PassServiceDetailNavArgs) -> Unit,
    modifier: Modifier = Modifier,
    googleMapsSlot: @Composable (() -> Unit)? = null,
) {
    val stops = remember { station.getStops() }

    Column (modifier.fillMaxWidth()) {
        Icon(
            painterResource(R.drawable.ic_dr_station),
            "Station icon",
            Modifier.size(72.dp + 20.dp)
        )
        Text(station.name, style=MaterialTheme.typography.displaySmall, modifier=Modifier.padding(horizontal=10.dp))
        Spacer(Modifier.height(10.dp))
        Text(station.address, Modifier.padding(horizontal=10.dp))

        Spacer(Modifier.height(10.dp))
        googleMapsSlot?.invoke()
        Spacer(Modifier.height(10.dp))

        StationTimetable(
            stops,
            padding = PaddingValues(horizontal=10.dp),
            onNavigate = onNavigate
        )
    }
}

@Composable
private fun StationTimetable(
    stops: List<ServiceStop>,
    modifier: Modifier = Modifier,
    padding: PaddingValues = PaddingValues.Zero,
    onNavigate: (PassServiceDetailNavArgs) -> Unit,
) {
    val gap = 10.dp
    val gridControl = remember(stops) { DiscreteGridControl() }

    Column(modifier
        .fillMaxWidth()
        .padding(padding.verticalOnly())
    ) {
        DiscreteGridRow(
            gridControl,
            Modifier
                .fillMaxWidth()
                .padding(padding.horizontalOnly()),
            gap,
            Alignment.CenterVertically,
        ) {
            Spacer(Modifier.width(24.dp))
            Text(
                "Train",
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .cellAlign(Alignment.Start)
                    .fill()
            )
            Text(
                "Arrival",
                fontWeight = FontWeight.Bold,
                modifier = Modifier.cellAlign(Alignment.CenterHorizontally)
            )
            Text(
                "Departure",
                fontWeight = FontWeight.Bold,
                modifier = Modifier.cellAlign(Alignment.CenterHorizontally)
            )
        }
        for (stop in stops) {
            Spacer(Modifier.height(5.dp))
            StationRow(
                painterResource(AppIcons.Trainset(stop.getService().trainset)),
                stop.getService().title,
                arriveTime = stop.arrival,
                departTime = stop.departure,
                Modifier
                    .fillMaxWidth()
                    .clickable { onNavigate(PassServiceDetailNavArgs(stop.passServiceId)) }
                    .padding(padding.horizontalOnly()),
                discreteGridControl = gridControl,
                gap = gap,
            )
        }
    }
}

@Composable
private fun StationRow(
    icon: Painter,
    title: String,
    arriveTime: ZonedDateTime?,
    departTime: ZonedDateTime?,
    modifier: Modifier = Modifier,
    discreteGridControl: DiscreteGridControl,
    gap: Dp,
) {
    DiscreteGridRow(
        discreteGridControl,
        modifier = modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min),
        gap = gap,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            icon, // TODO be more efficient with getting trainset (don't get all properties of the service)
            contentDescription = null, // Explained by "Train" column
            modifier = Modifier
                .fillMaxHeight()
                .width(24.dp)
        )
        Text(
            title,
            modifier = Modifier
//                .wrapContentHeight()
                .cellAlign(Alignment.Start)
                .fill()
        )
        Text(
            if (arriveTime == null) EM_DASH else AppStringFormats.Time(arriveTime),
            softWrap = false,
            modifier = Modifier
//                .wrapContentHeight()
                .cellAlign(Alignment.CenterHorizontally)
        )
        Text(
            if (departTime == null) EM_DASH else AppStringFormats.Time(departTime),
            softWrap = false,
            modifier = Modifier
//                .wrapContentHeight()
                .cellAlign(Alignment.CenterHorizontally)
        )
    }
}