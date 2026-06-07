package com.jeffreyalanwang.dutchrailwaysandroidclient.ui.screens.top

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.setTextAndPlaceCursorAtEnd
import androidx.compose.material3.BottomSheet
import androidx.compose.material3.ExpandedFullScreenSearchBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.Polygon
import com.google.maps.android.compose.rememberCameraPositionState
import com.google.maps.android.compose.rememberUpdatedMarkerState
import com.jeffreyalanwang.dutchrailwaysandroidclient.Area
import com.jeffreyalanwang.dutchrailwaysandroidclient.BackendApi
import com.jeffreyalanwang.dutchrailwaysandroidclient.Place
import com.jeffreyalanwang.dutchrailwaysandroidclient.R
import com.jeffreyalanwang.dutchrailwaysandroidclient.Station
import com.jeffreyalanwang.dutchrailwaysandroidclient.mapCameraUpdate
import com.jeffreyalanwang.dutchrailwaysandroidclient.ui.NavRoute
import com.jeffreyalanwang.dutchrailwaysandroidclient.ui.components.AppBarWithDualSearch
import com.jeffreyalanwang.dutchrailwaysandroidclient.ui.components.ClearableTimePickerDialog
import com.jeffreyalanwang.dutchrailwaysandroidclient.ui.components.PlaceSearchResults
import com.jeffreyalanwang.dutchrailwaysandroidclient.ui.components.SearchBarId
import com.jeffreyalanwang.dutchrailwaysandroidclient.ui.components.rememberDualSearchBarState
import com.jeffreyalanwang.dutchrailwaysandroidclient.ui.detailScreens.AreaDetailWithoutMap
import com.jeffreyalanwang.dutchrailwaysandroidclient.ui.detailScreens.StationDetailWithoutMap
import com.jeffreyalanwang.dutchrailwaysandroidclient.ui.util.bottomOnly
import com.jeffreyalanwang.dutchrailwaysandroidclient.ui.util.topOnly
import com.jeffreyalanwang.dutchrailwaysandroidclient.ui.viewmodel.RoutePlannerViewModel
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
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
    var currViewing by rememberSaveable { mutableStateOf(SearchBarId.None) }
    var didInitPosition by remember { mutableStateOf(false) }
    val bothEndpointsSet by remember { derivedStateOf {
        routePlannerState.origin != null && routePlannerState.destination != null
    } }

    fun closeSearch() { scope.launch { dualSearchBarState.animateToCollapsed() } }

    // Launch transition animations for content when [currViewing] is changed.
    // The endpoint (origin/destination) referenced by [value] must not be null.
    fun setCurrViewing(value: SearchBarId) {
        currViewing = value
        val place = when(value) {
            SearchBarId.First -> routePlannerState.origin!!
            SearchBarId.Second -> routePlannerState.destination!!
            else -> throw NotImplementedError()
        }
        scope.launch {
            bottomSheetState.show()
        }
        scope.launch {
            cameraPositionState.animate(place.mapCameraUpdate)
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
                        onClick = { viewModel.loadRoutes() },
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
                        scope.launch { setCurrViewing(SearchBarId.First) }
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
                        scope.launch { setCurrViewing(SearchBarId.Second) }
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

        if (currViewing != SearchBarId.None) {
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
                    val place = when(currViewing) {
                        SearchBarId.First -> routePlannerState.origin
                        SearchBarId.Second -> routePlannerState.destination
                        else -> throw NotImplementedError()
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
                        Text(
                            DateTimeFormatter
                                .ofLocalizedTime(FormatStyle.SHORT)
                                .format(timeConstraintField)
                        )
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
