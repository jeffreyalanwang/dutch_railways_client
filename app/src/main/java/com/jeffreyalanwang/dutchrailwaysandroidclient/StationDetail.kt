package com.jeffreyalanwang.dutchrailwaysandroidclient

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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

@Preview
@Composable
fun StationDetailTest() {
    val scrollState = rememberScrollState()
    Box(Modifier
        .verticalScroll(scrollState)
        .height(1000.dp)
        .width(550.dp)
    ) {
        StationDetail(
            BackendApi.get_station_info(358u),
            modifier = Modifier.padding(10.dp)
        )
    }
}

@Composable
fun StationDetail(station: Station, modifier: Modifier = Modifier) {

    key(station.id) { //TODO3 will this ever matter (and if so, put it everywhere)

        val stationMarkerState = rememberUpdatedMarkerState(position = station.geom)
        val cameraPositionState = rememberCameraPositionState {
            position = CameraPosition.fromLatLngZoom(station.geom, 10f)
        }

        Card (modifier.fillMaxWidth()) {
            Spacer(Modifier.height(20.dp))

            Icon(
                painterResource(R.drawable.ic_dr_station),
                "Station icon",
                Modifier.size(72.dp + 20.dp)
            )
            Text(station.name, style=MaterialTheme.typography.displaySmall, modifier=Modifier.padding(horizontal=10.dp))
            Spacer(Modifier.height(10.dp))
            Text(station.address, Modifier.padding(horizontal=10.dp))

            Spacer(Modifier.height(10.dp))
            HorizontalDivider(thickness = Dp.Hairline)

            GoogleMap(
                cameraPositionState = cameraPositionState,
                contentDescription = "Station on map",
                modifier = Modifier
                    .fillMaxWidth()
                    .sizeIn(minHeight = 200.dp)
            ) {
                Marker(
                    state = stationMarkerState,
                    title = station.name,
                )
            }

            HorizontalDivider(thickness = Dp.Hairline)
            Spacer(Modifier.height(10.dp))

            StationTimetable(station.getStops(), Modifier.padding(horizontal=10.dp))

            Spacer(Modifier.height(20.dp))
        }
    }
}

@Composable
fun TimetableRow(
    stop: ServiceStop,
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
    ) {
        Icon(
            painterResource(AppIcons.Trainset(stop.getService().trainset)), // TODO be more efficient with getting trainset (don't get all properties of the service)
            contentDescription = null,
            modifier=Modifier
                .fillMaxHeight()
                .width(24.dp)
        )
        Text(
            stop.getService().title,
            modifier=Modifier
                .wrapContentHeight()
                .cellAlign(Alignment.Start)
                .fill()
        )
        Text(
            UIStrings.Time(stop.arrival),
            softWrap = false,
            modifier=Modifier
                .cellAlign(Alignment.CenterHorizontally)
                .wrapContentHeight()
        )
        Text(
            UIStrings.Time(stop.departure),
            softWrap = false,
            modifier=Modifier
                .cellAlign(Alignment.CenterHorizontally)
                .wrapContentHeight()
        )
    }
}

@Composable
fun StationTimetable(stops: List<ServiceStop>, modifier: Modifier = Modifier) {
    val gap = 10.dp
    val gridControl = remember(stops) { DiscreteGridControl() }

    Column(modifier.fillMaxWidth()) {
        DiscreteGridRow(
            gridControl,
            Modifier.fillMaxWidth(),
            gap,
        ) {
            Spacer(Modifier.width(24.dp))
            Text(
                "Train",
                fontWeight = FontWeight.Bold,
                modifier=Modifier.cellAlign(Alignment.Start).fill()
            )
            Text(
                "Arrival",
                fontWeight = FontWeight.Bold,
                modifier=Modifier.cellAlign(Alignment.CenterHorizontally)
            )
            Text(
                "Departure",
                fontWeight = FontWeight.Bold,
                modifier=Modifier.cellAlign(Alignment.CenterHorizontally)
            )
        }
        for (stop in stops) {
            Spacer(Modifier.height(5.dp))
            TimetableRow(
                stop,
                discreteGridControl = gridControl,
                gap = gap,
            )
        }
    }
}