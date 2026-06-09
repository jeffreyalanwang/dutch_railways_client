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
import androidx.compose.runtime.key
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
import com.jeffreyalanwang.dutchrailwaysandroidclient.letWith
import com.jeffreyalanwang.dutchrailwaysandroidclient.toLocalTime
import com.jeffreyalanwang.dutchrailwaysandroidclient.ui.CommonChildRoute
import com.jeffreyalanwang.dutchrailwaysandroidclient.ui.RouteDetailRoute
import com.jeffreyalanwang.dutchrailwaysandroidclient.ui.TrainQueryGraphRoute
import com.jeffreyalanwang.dutchrailwaysandroidclient.ui.TrainQueryRoute
import com.jeffreyalanwang.dutchrailwaysandroidclient.ui.components.AppBarWithDualSearch
import com.jeffreyalanwang.dutchrailwaysandroidclient.ui.components.ClearableTimePickerDialog
import com.jeffreyalanwang.dutchrailwaysandroidclient.ui.components.PlaceSearchResults
import com.jeffreyalanwang.dutchrailwaysandroidclient.ui.components.rememberDualSearchBarState
import com.jeffreyalanwang.dutchrailwaysandroidclient.ui.detailScreens.AreaDetailWithoutMap
import com.jeffreyalanwang.dutchrailwaysandroidclient.ui.detailScreens.StationDetailWithoutMap
import com.jeffreyalanwang.dutchrailwaysandroidclient.ui.util.AppStringFormats
import com.jeffreyalanwang.dutchrailwaysandroidclient.ui.util.bottomOnly
import com.jeffreyalanwang.dutchrailwaysandroidclient.ui.util.topOnly
import com.jeffreyalanwang.dutchrailwaysandroidclient.ui.viewmodel.Endpoint
import com.jeffreyalanwang.dutchrailwaysandroidclient.ui.viewmodel.RoutePlannerStage
import com.jeffreyalanwang.dutchrailwaysandroidclient.ui.viewmodel.RoutePlannerViewModel
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atDate
import kotlinx.datetime.todayIn
import kotlin.time.Clock
import kotlin.time.Instant

val ON_MAP_SHADOW_ELEVATION = 12.dp

@Preview
@Composable
private fun RoutePlannerScreenPreview() {
    val snackbarHostState = remember { SnackbarHostState() }
    val snackbarEffectScope = rememberCoroutineScope()

    RoutePlannerScreen (
        routeArgs = TrainQueryRoute,
        viewModel = viewModel<RoutePlannerViewModel>(),
        onNavigate = { newRoute ->
            snackbarEffectScope.launch {
                snackbarHostState.showSnackbar(
                    newRoute.toString(),
                    withDismissAction = true
                )
            }
        },
        onNavigateBack = {
            snackbarEffectScope.launch {
                snackbarHostState.showSnackbar(
                    "Back activated",
                    withDismissAction = true
                )
            }
        },
    )

    SnackbarHost(hostState = snackbarHostState)
}


@Composable
fun RoutePlannerScreen(
    routeArgs: TrainQueryGraphRoute,
    viewModel: RoutePlannerViewModel,
        // currently, the state we are in reacts to a state calculated in viewModel...
        //  We link onNavigate to a listener on RoutePlannerViewModel
        //  * LaunchedEffect(routePlannerState.uiStage) to either
        //          pop back to the previous main stage (see below)
        //          or pop to the current main stage and then navigate to the next main stage
        //  * LaunchedEffect(navRoute) to either
        //          base route -> display nothing
        //          station/area -> display station/area
        //          TODO ? -> display route list
        //          routeDetail -> display route
        //          [station/area from routeDetail]
        //
    // TODO the above: how are we going to make sure we do not bounce between the two effects?
        //  Back stack (by bottom sheet showing):
        //    Nothing -> one place (-> extra station/service) -> routes (-> route (-> extra station/service))
    onNavigate: (CommonChildRoute)->Unit,
    onNavigateBack: ()->Unit
) {
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
    fun closeSearch() { scope.launch { dualSearchBarState.animateToCollapsed() } }
    val isBottomSheetVisible = routeArgs !is TrainQueryRoute // all other routes require bottomSheet
    var timePickerTarget by remember { mutableStateOf<Endpoint?>(null) }

    LaunchedEffect(isBottomSheetVisible) {
        if (isBottomSheetVisible) bottomSheetState.expand()
        else bottomSheetState.hide()
    }

    // Sync changes in ViewModel state with GoogleMap
    runCatching {
        // If this throws an error, we will do it later using [onMapLoaded]
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
                        ?.letWith(TimeZone.currentSystemDefault()) { it.toLocalTime() },
                    timeButtonContentDescription = "Select departure time",
                    onTimeConstraintButtonClick = { timePickerTarget = Endpoint.Origin },
                ),
                inputField2 = inputFieldFactory(
                    placeholderText = "Search arrival",
                    onFinishSearch = { closeSearch() },
                    timeConstraintField = routePlannerState.arriveTime
                        ?.letWith(TimeZone.currentSystemDefault()) { it.toLocalTime() },
                    timeButtonContentDescription = "Select arrival time",
                    onTimeConstraintButtonClick = { timePickerTarget = Endpoint.Destination },
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
                    RoutePlannerStage.ListRouteOptions -> {
                        val routes = routePlannerState.routes!!
                        LazyColumn {
                            if (routes.isEmpty()) {
                                item { NoRoutesPlaceholder() }
                            } else routes.forEachIndexed { i, it ->
                                item {
                                    RouteListing(it,
                                        { onNavigate(RouteDetailRoute(i)) }
                                    )
                                }
                            }
                        }
                    }

                    RoutePlannerStage.HasAnySelected ->
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

        timePickerTarget?.let { target ->
            key(target) {
                val title = when (target) {
                    Endpoint.Origin -> "Select depart time"
                    Endpoint.Destination -> "Select arrive time"
                }
                val initialInstant = when (target) {
                    Endpoint.Origin -> routePlannerState.departTime
                    Endpoint.Destination -> routePlannerState.arriveTime
                }
                val setTime = when (target) {
                    Endpoint.Origin -> { it: Instant? ->
                        { viewModel.setTimeConstraints(departTime = it) }
                    }
                    Endpoint.Destination -> { it: Instant? ->
                        { viewModel.setTimeConstraints(arriveTime = it) }
                    }
                }
                ClearableTimePickerDialog(
                        title = title,
                        initialTime = initialInstant
                            ?.letWith(TimeZone.currentSystemDefault()) { it.toLocalDateTime() }
                            ?.time,
                    onConfirm = { selectedTime ->
                        setTime(
                            with(TimeZone.currentSystemDefault()) {
                                selectedTime
                                    ?.atDate(Clock.System.todayIn(this))
                                    ?.toInstant()
                            }
                        )
                        timePickerTarget = null
                    },
                    onDismiss = { timePickerTarget = null },
                )
            }
        }
    }
}


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