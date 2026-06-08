package com.jeffreyalanwang.dutchrailwaysandroidclient.ui.detailScreens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
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
import androidx.compose.foundation.layout.wrapContentHeight
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.Polygon
import com.google.maps.android.compose.rememberCameraPositionState
import com.jeffreyalanwang.dutchrailwaysandroidclient.Area
import com.jeffreyalanwang.dutchrailwaysandroidclient.BackendApi
import com.jeffreyalanwang.dutchrailwaysandroidclient.R
import com.jeffreyalanwang.dutchrailwaysandroidclient.Station
import com.jeffreyalanwang.dutchrailwaysandroidclient.getMapCameraUpdate
import com.jeffreyalanwang.dutchrailwaysandroidclient.ui.NavRoute
import com.jeffreyalanwang.dutchrailwaysandroidclient.ui.StationDetailRoute
import com.jeffreyalanwang.dutchrailwaysandroidclient.ui.components.DiscreteGridRowScope.cellAlign
import com.jeffreyalanwang.dutchrailwaysandroidclient.ui.components.DiscreteGridRowScope.fill
import com.jeffreyalanwang.dutchrailwaysandroidclient.ui.util.AppIcons
import com.jeffreyalanwang.dutchrailwaysandroidclient.ui.util.horizontalOnly
import com.jeffreyalanwang.dutchrailwaysandroidclient.ui.util.verticalOnly
import kotlinx.coroutines.launch

@Preview
@Composable
private fun AreaDetailPreview() {
    val snackbarHostState = remember { SnackbarHostState() }
    val snackbarEffectScope = rememberCoroutineScope()

    AreaDetailScreen(
        BackendApi.get_area_info(1),
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
fun AreaDetailScreen(
    area: Area,
    onNavigate: (NavRoute) -> Unit,
    onNavigateBack: () -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Area") },
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
                AreaDetail(
                    area,
                    onNavigate,
                )
            }
        }
    }
}

@Composable
fun AreaDetailWithoutMap(
    area: Area,
    onNavigate: (NavRoute) -> Unit,
    modifier: Modifier = Modifier,
) = AreaDetailBase(area, onNavigate, modifier)

@Composable
fun AreaDetail(
    area: Area,
    onNavigate: (NavRoute) -> Unit,
    modifier: Modifier = Modifier,
) = AreaDetailBase(area, onNavigate, modifier, {
    val areaGeom = remember { area.getGeom() }
    val cameraPositionState = rememberCameraPositionState()
    var didInitPosition by remember { mutableStateOf(false) }

    HorizontalDivider(thickness = Dp.Hairline)

    GoogleMap(
        cameraPositionState = cameraPositionState,
        contentDescription = "Area on map",
        modifier = Modifier
            .fillMaxWidth()
            .sizeIn(minHeight = 200.dp, maxHeight = 400.dp),
        onMapLoaded = {
            if (!didInitPosition) {
                cameraPositionState.move(area.getMapCameraUpdate())
                didInitPosition = true
            }
        },
    ) {
        Polygon(
            tag = area.name,
            points = areaGeom.points,
            holes = areaGeom.holes,
            fillColor = Color.Transparent,
        )
    }

    HorizontalDivider(thickness = Dp.Hairline)
})

@Composable
private fun AreaDetailBase(
    area: Area,
    onNavigate: (NavRoute) -> Unit,
    modifier: Modifier = Modifier,
    googleMapsSlot: @Composable (() -> Unit)? = null,
) {
    val stations = remember { area.getStations() }

    Column (modifier.fillMaxWidth()) {
        Spacer(Modifier.height(20.dp))

        Icon(
            painterResource(R.drawable.ic_dr_station),
            "Station icon",
            Modifier.size(72.dp + 20.dp)
        )
        Text(area.name, style=MaterialTheme.typography.displaySmall, modifier=Modifier.padding(horizontal=10.dp))

        Spacer(Modifier.height(10.dp))
        googleMapsSlot?.invoke()
        Spacer(Modifier.height(10.dp))

        StationList(
            stations,
            padding = PaddingValues(horizontal=10.dp),
            onNavigate = onNavigate,
        )

        Spacer(Modifier.height(20.dp))
    }
}

@Composable
private fun StationList(
    stations: List<Station>,
    modifier: Modifier = Modifier,
    padding: PaddingValues = PaddingValues.Zero,
    onNavigate: (NavRoute) -> Unit
) {
    val gap = 10.dp

    Column(modifier
        .fillMaxWidth()
        .padding(padding.verticalOnly())
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(padding.horizontalOnly()),
            horizontalArrangement = Arrangement.spacedBy(gap, Alignment.Start),
        ) {
            Spacer(Modifier.width(24.dp))
            Text(
                "Station",
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .cellAlign(Alignment.Start)
                    .fill()
            )
        }
        for (station in stations) {
            Spacer(Modifier.height(5.dp))
            Row(
                modifier = modifier
                    .fillMaxWidth()
                    .height(IntrinsicSize.Min)
                    .clickable { onNavigate(StationDetailRoute(station.id)) }
                    .padding(padding.horizontalOnly()),
                horizontalArrangement = Arrangement.spacedBy(gap, Alignment.Start),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    painterResource(AppIcons.PlaceType(Station::class)),
                    contentDescription = null, // Explained by "Station" column
                    modifier = Modifier
                        .fillMaxHeight()
                        .width(24.dp)
                )
                Text(
                    station.name,
                    modifier = Modifier
                        .wrapContentHeight()
                        .cellAlign(Alignment.Start)
                        .fill()
                        .padding(vertical = 6.dp),
                )
            }
        }
    }
}
