package com.jeffreyalanwang.dutchrailwaysandroidclient.ui.screens.top

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.setTextAndPlaceCursorAtEnd
import androidx.compose.material3.BottomSheet
import androidx.compose.material3.ExpandedFullScreenSearchBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SearchBarDefaults
import androidx.compose.material3.SearchBarState
import androidx.compose.material3.SheetValue
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberStandardBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.Polygon
import com.google.maps.android.compose.rememberCameraPositionState
import com.google.maps.android.compose.rememberUpdatedMarkerState
import com.jeffreyalanwang.dutchrailwaysandroidclient.Area
import com.jeffreyalanwang.dutchrailwaysandroidclient.BackendApi
import com.jeffreyalanwang.dutchrailwaysandroidclient.Place
import com.jeffreyalanwang.dutchrailwaysandroidclient.R
import com.jeffreyalanwang.dutchrailwaysandroidclient.RoutePlan
import com.jeffreyalanwang.dutchrailwaysandroidclient.Station
import com.jeffreyalanwang.dutchrailwaysandroidclient.asCameraUpdate
import com.jeffreyalanwang.dutchrailwaysandroidclient.calculateBounds
import com.jeffreyalanwang.dutchrailwaysandroidclient.mapCameraUpdate
import com.jeffreyalanwang.dutchrailwaysandroidclient.points
import com.jeffreyalanwang.dutchrailwaysandroidclient.ui.NavRoute
import com.jeffreyalanwang.dutchrailwaysandroidclient.ui.components.AppBarWithDualSearch
import com.jeffreyalanwang.dutchrailwaysandroidclient.ui.components.ClearableTimePickerDialog
import com.jeffreyalanwang.dutchrailwaysandroidclient.ui.components.PlaceSearchResults
import com.jeffreyalanwang.dutchrailwaysandroidclient.ui.components.rememberDualSearchBarState
import com.jeffreyalanwang.dutchrailwaysandroidclient.ui.detailScreens.AreaDetailWithoutMap
import com.jeffreyalanwang.dutchrailwaysandroidclient.ui.detailScreens.StationDetailWithoutMap
import com.jeffreyalanwang.dutchrailwaysandroidclient.ui.util.AppStringFormats
import com.jeffreyalanwang.dutchrailwaysandroidclient.ui.util.bottomOnly
import com.jeffreyalanwang.dutchrailwaysandroidclient.ui.util.topOnly
import com.jeffreyalanwang.dutchrailwaysandroidclient.ui.viewmodel.RoutePlannerViewModel
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalTime
import java.util.TimeZone
import kotlin.time.ExperimentalTime
import kotlin.time.toJavaInstant
import kotlin.time.toKotlinInstant

val ON_MAP_SHADOW_ELEVATION = 12.dp

@Preview
@Composable
private fun RoutePlannerScreenPreview() {
    val snackbarHostState = remember { SnackbarHostState() }
    val snackbarEffectScope = rememberCoroutineScope()

    RoutePlannerScreen (
        viewModel = viewModel<RoutePlannerViewModel>(),
        onNavigate = { newRoute ->
            snackbarEffectScope.launch {
                snackbarHostState.showSnackbar(
                    newRoute.toString(),
                    withDismissAction = true
                )
            }
        }
    )

    SnackbarHost(hostState = snackbarHostState)
}

private enum class BottomSheetShowing { None, Origin, Destination, Routes }

@OptIn(ExperimentalMaterial3Api::class, ExperimentalTime::class)
@Composable
fun RoutePlannerScreen(viewModel: RoutePlannerViewModel, onNavigate: (NavRoute)->Unit) {
    val scope = rememberCoroutineScope()
    val routePlannerState by viewModel.uiState.collectAsState()
    val dualSearchBarState = rememberDualSearchBarState()
    val cameraPositionState = rememberCameraPositionState()
    var isDepartTimePickerOpen by remember { mutableStateOf(false) }
    var isArriveTimePickerOpen by remember { mutableStateOf(false) }
    val bottomSheetState = rememberStandardBottomSheetState(
        confirmValueChange = {
            when (it) {
                SheetValue.Hidden -> false // Do not allow user to hide a visible sheet
                else -> true
            }
        }
    )

    // Will be [SearchBarId.None] only if no selection has been made yet
    var currViewing by rememberSaveable { mutableStateOf(BottomSheetShowing.None) }
    var didInitPosition by remember { mutableStateOf(false) }
    val bothEndpointsSet by remember { derivedStateOf {
        routePlannerState.origin != null && routePlannerState.destination != null
    } }

    fun closeSearch() { scope.launch { dualSearchBarState.animateToCollapsed() } }

    // Launch transition animations for content when [currViewing] is changed.
    // The endpoint (origin/destination) referenced by [value] must not be null.
    fun setCurrViewing(value: BottomSheetShowing) {
        currViewing = value
        scope.launch {
            bottomSheetState.show()
        }
        scope.launch {
            cameraPositionState.animate(when(value) {
                BottomSheetShowing.Origin -> routePlannerState.origin!!.mapCameraUpdate
                BottomSheetShowing.Destination -> routePlannerState.destination!!.mapCameraUpdate
                BottomSheetShowing.Routes -> emptyList<LatLng>()
                        .plus(routePlannerState.origin!!.points)
                        .plus(routePlannerState.destination!!.points)
                        .calculateBounds()
                        .asCameraUpdate(168)
                BottomSheetShowing.None -> throw IllegalStateException()
            })
        }
    }

    Scaffold (
        modifier = Modifier.fillMaxSize(),
        topBar = {
            AppBarWithDualSearch(
                dualSearchBarState,
                colors = SearchBarDefaults.appBarWithSearchColors(
                    appBarContainerColor = Color.Transparent,
                    scrolledAppBarContainerColor = Color.White.copy(alpha=0.5f),
                ),
                shadowElevation = ON_MAP_SHADOW_ELEVATION,
                actionIcon = {
                    IconButton(
                        enabled = bothEndpointsSet,
                        colors = IconButtonDefaults.filledIconButtonColors(),
                        modifier = Modifier.padding(horizontal = 4.dp),
                        onClick = {
                            viewModel.loadRoutes()
                            scope.launch { setCurrViewing(BottomSheetShowing.Routes) }
                        },
                    ) {
                        Icon(
                            painterResource(R.drawable.ic_search),
                            contentDescription = "Search routes"
                        )
                    }
                },
                inputField1 = inputFieldFactory(
                    placeholderText = "Search departure",
                    onFinishSearch = { closeSearch() },
                    timeConstraintField = routePlannerState.departTime
                        ?.toJavaInstant()
                        ?.atZone(TimeZone.getDefault().toZoneId())
                        ?.toLocalTime(),
                    timeButtonContentDescription = "Select departure time",
                    onTimeConstraintButtonClick = { isDepartTimePickerOpen = true },
                ),
                inputField2 = inputFieldFactory(
                    placeholderText = "Search arrival",
                    onFinishSearch = { closeSearch() },
                    timeConstraintField = routePlannerState.arriveTime
                        ?.toJavaInstant()
                        ?.atZone(TimeZone.getDefault().toZoneId())
                        ?.toLocalTime(),
                    timeButtonContentDescription = "Select arrival time",
                    onTimeConstraintButtonClick = { isArriveTimePickerOpen = true },
                ),
                expandedSearch1 = expandedSearchFactory(
                    placeholderText = "Search departure",
                    onNewSelection = { place ->
                        viewModel.setEndpoints(
                            origin = place,
                            departTime = null,
                            destination = routePlannerState.destination,
                            arriveTime = routePlannerState.arriveTime,
                        )
                        scope.launch { setCurrViewing(BottomSheetShowing.Origin) }
                    },
                    onFinishSearch = { closeSearch() },
                ),
                expandedSearch2 = expandedSearchFactory(
                    placeholderText = "Search arrival",
                    onNewSelection = { place ->
                        viewModel.setEndpoints(
                            origin = routePlannerState.origin,
                            departTime = routePlannerState.departTime,
                            destination = place,
                            arriveTime = null,
                        )
                        scope.launch { setCurrViewing(BottomSheetShowing.Destination) }
                    },
                    onFinishSearch = { closeSearch() },
                ),
            )
        },
    ) { innerPadding ->

        GoogleMap(
            cameraPositionState = cameraPositionState,
            contentDescription = "Trip endpoints map",
            modifier = Modifier.fillMaxSize(),
            contentPadding = innerPadding,
            onMapLoaded = {
                if (!didInitPosition) { scope.launch {
                    cameraPositionState.animate(
                        BackendApi
                            .get_nl_area()
                            .mapCameraUpdate
                    )
                    didInitPosition = true
                } }
            },
        ) {
            for (place in listOfNotNull(
                routePlannerState.origin,
                routePlannerState.destination,
            )) {
                when (place) {
                    is Station ->
                        Marker(
                            title = place.name,
                            state = rememberUpdatedMarkerState(
                                position = place.geom
                            )
                        )
                    is Area ->
                        Polygon(
                            tag = place.name,
                            points = place.getGeom().points,
                            holes = place.getGeom().holes,
                            fillColor = Color.Transparent,
                        )
                    else ->
                        throw NotImplementedError()
                }
            }
        }

        if (currViewing != BottomSheetShowing.None) {
            BottomSheet(
                state = bottomSheetState,
                modifier = Modifier.padding(innerPadding.topOnly()),
                shadowElevation = ON_MAP_SHADOW_ELEVATION,
            ) {
                Box(
                    Modifier
                        .padding(innerPadding.bottomOnly())
                        .padding(bottom = 10.dp)
                ) {
                    if (currViewing == BottomSheetShowing.Routes) {
                        LazyColumn {
                            if (routePlannerState.routes!!.size == 0) {
                                item { NoRoutesPlaceholder() }
                            } else {
                                for (route in routePlannerState.routes!!) {
                                    item { RouteListing(route, {}) }
                                }
                            }
                        }
                    } else {
                        val place = when (currViewing) {
                            BottomSheetShowing.Origin -> routePlannerState.origin!!
                            BottomSheetShowing.Destination -> routePlannerState.destination!!
                            else -> throw IllegalStateException()
                        }
                        when (place) {
                            is Station -> StationDetailWithoutMap(
                                station = place,
                                onNavigate = onNavigate,
                                Modifier.padding(horizontal = 10.dp),
                            )

                            is Area -> AreaDetailWithoutMap(
                                area = place,
                                onNavigate = onNavigate,
                                Modifier.padding(horizontal = 10.dp),
                            )

                            else -> throw NotImplementedError()
                        }
                    }
                }
            }
        }

        if (isDepartTimePickerOpen) {
            ClearableTimePickerDialog(
                title = "Select depart time",
                initialTime = routePlannerState.departTime
                    ?.toJavaInstant()
                    ?.atZone(TimeZone.getDefault().toZoneId())
                    ?.toLocalTime(),
                onConfirm = {
                    viewModel.setEndpoints(
                        origin = routePlannerState.origin,
                        departTime = it
                            ?.atDate(LocalDate.now())
                            ?.atZone(TimeZone.getDefault().toZoneId())
                            ?.toInstant()
                            ?.toKotlinInstant(),
                        destination = routePlannerState.destination,
                        arriveTime = routePlannerState.arriveTime,
                    )
                    isDepartTimePickerOpen = false
                },
                onDismiss = { isDepartTimePickerOpen = false },
            )
        }
        if (isArriveTimePickerOpen) {
            ClearableTimePickerDialog(
                title = "Select arrive time",
                initialTime = routePlannerState.arriveTime
                    ?.toJavaInstant()
                    ?.atZone(TimeZone.getDefault().toZoneId())
                    ?.toLocalTime(),
                onConfirm = {
                    viewModel.setEndpoints(
                        origin = routePlannerState.origin,
                        departTime = routePlannerState.departTime,
                        destination = routePlannerState.destination,
                        arriveTime = it
                            ?.atDate(LocalDate.now())
                            ?.atZone(TimeZone.getDefault().toZoneId())
                            ?.toInstant()
                            ?.toKotlinInstant(),
                    )
                    isArriveTimePickerOpen = false
                },
                onDismiss = { isArriveTimePickerOpen = false },
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun inputFieldFactory(
    placeholderText: String,
    onFinishSearch: () -> Unit,
    onTimeConstraintButtonClick: () -> Unit,
    timeConstraintField: LocalTime?,
    timeButtonContentDescription: String,
): @Composable (TextFieldState, SearchBarState) -> Unit {
    return { textFieldState, searchBarState ->
        SearchBarDefaults.InputField(
            textFieldState = textFieldState,
            searchBarState = searchBarState,
            onSearch = { onFinishSearch() },
            placeholder = {
                Text(
                    modifier = Modifier.clearAndSetSemantics {},
                    text = placeholderText,
                )
            },
            trailingIcon = {
                if (timeConstraintField == null) {
                    IconButton(onClick = onTimeConstraintButtonClick) {
                        Icon(
                            painterResource(R.drawable.ic_time_add),
                            contentDescription = timeButtonContentDescription,
                        )
                    }
                } else {
                    TextButton(onClick = onTimeConstraintButtonClick) {
                        Text( AppStringFormats.Time(timeConstraintField) )
                    }
                }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun expandedSearchFactory(
    placeholderText: String,
    onNewSelection: (Place) -> Unit,
    onFinishSearch: () -> Unit,
): @Composable (TextFieldState, SearchBarState) -> Unit {
    return { textFieldState, searchBarState ->
        ExpandedFullScreenSearchBar(
            searchBarState,
            inputField = {
                SearchBarDefaults.InputField(
                    textFieldState = textFieldState,
                    searchBarState = searchBarState,
                    onSearch = { onFinishSearch() },
                    placeholder = {
                        Text(
                            modifier = Modifier.clearAndSetSemantics {},
                            text = placeholderText,
                        )
                    },
                )
            }
        ) {
            PlaceSearchResults(
                Place::class,
                query = textFieldState.text.toString(),
                onResultClick = { id, name ->
                    textFieldState.setTextAndPlaceCursorAtEnd(name)
                    onNewSelection(BackendApi.get_place_info(id))
                    onFinishSearch()
                },
            )
        }
    }
}

@Composable
private fun NoRoutesPlaceholder(modifier: Modifier = Modifier) {
    Column(
        modifier
            .alpha(.7f)
            .fillMaxSize(),
        Arrangement.Center,
        Alignment.CenterHorizontally,
    ) {
        Icon(
            painterResource(R.drawable.ic_directions),
            null,
            Modifier
                .size(96.dp)
                .padding(bottom = 12.dp)
        )
        Text(
            "No routes available",
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
fun RouteListing(
    route: RoutePlan,
    onClick: (RoutePlan) -> Unit,
    modifier: Modifier = Modifier,
) {
    // Row height is controlled by transfers column.
    // Element width is controlled by duration and transfers item/column.
    Row(
        modifier = modifier
            .clickable(onClick = { onClick(route) })
            .fillMaxWidth()
            .height(IntrinsicSize.Min)
            .padding(horizontal = 14.dp),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            AppStringFormats.TripDuration(route.duration),
            textAlign = TextAlign.Center,
            softWrap = false,
            modifier = Modifier.fillMaxWidth(
                .25f
            ),
            autoSize = TextAutoSize.StepBased(
                MaterialTheme.typography.titleMedium.fontSize,
                MaterialTheme.typography.displayMedium.fontSize,
                stepSize = 1.sp,
            ),
            style = MaterialTheme.typography.displayMedium,
        )

        Column(
            modifier = Modifier.fillMaxHeight(),
            verticalArrangement = Arrangement.SpaceEvenly,
            horizontalAlignment = Alignment.End
        ) {
            Text(
                AppStringFormats.Time(route.stops.first().departure!!),
                style = MaterialTheme.typography.titleSmallEmphasized,
                modifier = Modifier.alpha(0.5f),
            )
            Text(
                AppStringFormats.Time(route.stops.last().arrival!!),
                style = MaterialTheme.typography.titleSmallEmphasized,
                modifier = Modifier.alpha(0.5f),
            )
        }

        Column(
            modifier = Modifier
                .fillMaxHeight()
                .weight(1f),
            verticalArrangement = Arrangement.SpaceEvenly,
            horizontalAlignment = Alignment.Start
        ) {
            Text(
                route.stops.first()
                    .getStation().name,
                overflow = TextOverflow.MiddleEllipsis,
                style = MaterialTheme.typography.titleSmallEmphasized,
            )
            Text(
                route.stops.last()
                    .getStation().name,
                overflow = TextOverflow.MiddleEllipsis,
                style = MaterialTheme.typography.titleSmallEmphasized,
            )
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                route.transferCount.toString(),
                style = MaterialTheme.typography.displaySmall,
            )
            Text(
                "Transfers",
                style = MaterialTheme.typography.labelSmall,
            )
        }
    }
}