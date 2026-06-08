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
import androidx.compose.foundation.text.input.clearText
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
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
import com.jeffreyalanwang.dutchrailwaysandroidclient.ui.viewmodel.RoutePlannerStage
import com.jeffreyalanwang.dutchrailwaysandroidclient.ui.viewmodel.RoutePlannerViewModel
import kotlinx.coroutines.launch
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atDate
import kotlinx.datetime.toJavaLocalTime
import kotlinx.datetime.toKotlinLocalTime
import kotlinx.datetime.toLocalDateTime
import kotlinx.datetime.todayIn
import java.time.LocalTime
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

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
    val routePlannerState by viewModel.uiState.collectAsState()

    val scope = rememberCoroutineScope()
    val dualSearchBarState = rememberDualSearchBarState()
    val mapCameraState = rememberCameraPositionState()
    val bottomSheetState = rememberStandardBottomSheetState(
        initialValue = SheetValue.Hidden,
        skipHiddenState = false,
        confirmValueChange = {
            when (it) {
                // Do not allow user to hide a visible sheet; only programmatically
                SheetValue.Hidden -> (routePlannerState.uiStage == RoutePlannerStage.NoneSelected)
                else -> true
            }
        }
    )

    var isDepartTimePickerOpen by remember { mutableStateOf(false) }
    var isArriveTimePickerOpen by remember { mutableStateOf(false) }

    fun closeSearch() { scope.launch { dualSearchBarState.animateToCollapsed() } }

    // Sync changes in ViewModel state with BottomSheet and GoogleMap

    routePlannerState.uiStage.let {
        LaunchedEffect(it) { when (it) {
            RoutePlannerStage.NoneSelected,
                -> bottomSheetState.hide() // TODO this doesn't work?

            RoutePlannerStage.Selecting,
            RoutePlannerStage.Routes,
                -> bottomSheetState.partialExpand()
        } }
    }

    runCatching { // If this throws an error, we will move to it [onMapLoaded]
        routePlannerState.mapCameraPosition
    }.getOrNull()?.let {
        LaunchedEffect(it) { mapCameraState.animate(it) }
    }

    // Build UI

    Scaffold (
        modifier = Modifier.fillMaxSize(),
        topBar = {
            AppBarWithDualSearch(
                dualSearchBarState,
                colors = SearchBarDefaults.appBarWithSearchColors(
                    appBarContainerColor = Color.Transparent,
                ),
                shadowElevation = ON_MAP_SHADOW_ELEVATION,
                actionIcon = {
                    IconButton(
                        enabled = routePlannerState.queryAllowed,
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
                        ?.toLocalDateTime(TimeZone.currentSystemDefault())
                        ?.time
                        ?.toJavaLocalTime(),
                    timeButtonContentDescription = "Select departure time",
                    onTimeConstraintButtonClick = { isDepartTimePickerOpen = true },
                ),
                inputField2 = inputFieldFactory(
                    placeholderText = "Search arrival",
                    onFinishSearch = { closeSearch() },
                    timeConstraintField = routePlannerState.arriveTime
                        ?.toLocalDateTime(TimeZone.currentSystemDefault())
                        ?.time
                        ?.toJavaLocalTime(),
                    timeButtonContentDescription = "Select arrival time",
                    onTimeConstraintButtonClick = { isArriveTimePickerOpen = true },
                ),
                expandedSearch1 = expandedSearchFactory(
                    placeholderText = "Search departure",
                    onNewSelection = { viewModel.setOrigin(it) },
                    onFinishSearch = { closeSearch() },
                ),
                expandedSearch2 = expandedSearchFactory(
                    placeholderText = "Search arrival",
                    onNewSelection = { viewModel.setDestination(it) },
                    onFinishSearch = { closeSearch() },
                ),
            )
        },
    ) { innerPadding ->

        GoogleMap(
            cameraPositionState = mapCameraState,
            contentDescription = "Trip endpoints map",
            modifier = Modifier.fillMaxSize(),
            contentPadding = innerPadding,
            onMapLoaded = {
                mapCameraState.move(routePlannerState.mapCameraPosition)
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
                when (routePlannerState.uiStage) {
                    RoutePlannerStage.Routes ->
                        LazyColumn {
                            val routeOptions = routePlannerState.routes!!
                            if (routeOptions.isEmpty()) {
                                item { NoRoutesPlaceholder() }
                            } else for (route in routeOptions) {
                                item { RouteListing(route, {}) }
                            }
                        }
                    RoutePlannerStage.Selecting  ->
                        when (routePlannerState.lastSetEndpoint) {
                            is Station -> StationDetailWithoutMap(
                                station = routePlannerState.lastSetEndpoint as Station,
                                onNavigate = onNavigate,
                                Modifier.padding(horizontal = 10.dp),
                            )

                            is Area -> AreaDetailWithoutMap(
                                area = routePlannerState.lastSetEndpoint as Area,
                                onNavigate = onNavigate,
                                Modifier.padding(horizontal = 10.dp),
                            )

                            else -> throw NotImplementedError()
                        }
                    RoutePlannerStage.NoneSelected -> {} // in this case, sheet is hidden
                }
            }
        }

        if (isDepartTimePickerOpen) {
            ClearableTimePickerDialog(
                title = "Select depart time",
                initialTime = routePlannerState.departTime
                    ?.toLocalDateTime(TimeZone.currentSystemDefault())
                    ?.time
                    ?.toJavaLocalTime(),
                onConfirm = { selectedTime ->
                    viewModel.setTimeConstraints(
                        departTime = with(TimeZone.currentSystemDefault()) {
                            selectedTime?.toKotlinLocalTime()
                                ?.atDate(Clock.System.now().toLocalDateTime().date)
                                ?.toInstant()
                        },
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
                    ?.toLocalDateTime(TimeZone.currentSystemDefault())
                    ?.time
                    ?.toJavaLocalTime(),
                onConfirm = { selectedTime ->
                    viewModel.setTimeConstraints(
                        arriveTime = with(TimeZone.currentSystemDefault()) {
                            selectedTime?.toKotlinLocalTime()
                                ?.atDate(Clock.System.todayIn(this))
                                ?.toInstant()
                        },
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
    onNewSelection: (Place?) -> Unit,
    onFinishSearch: () -> Unit,
): @Composable (TextFieldState, SearchBarState) -> Unit {
    return { textFieldState, searchBarState ->
        ExpandedFullScreenSearchBar(
            searchBarState,
            inputField = {
                SearchBarDefaults.InputField(
                    textFieldState = textFieldState,
                    searchBarState = searchBarState,
                    leadingIcon = {
                        IconButton(onClick = { onFinishSearch() }) {
                            Icon(
                                painterResource(R.drawable.ic_back),
                                "Clear search",
                            )
                        }
                    },
                    trailingIcon = {
                        IconButton(
                            onClick = {
                                textFieldState.clearText()
                                onNewSelection(null)
                            },
                        ) {
                            Icon(
                                painterResource(R.drawable.ic_close),
                                "Clear search",
                            )
                        }
                    },
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