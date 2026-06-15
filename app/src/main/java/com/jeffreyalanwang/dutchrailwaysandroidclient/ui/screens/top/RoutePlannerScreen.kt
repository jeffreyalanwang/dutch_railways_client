package com.jeffreyalanwang.dutchrailwaysandroidclient.ui.screens.top

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.clearText
import androidx.compose.foundation.text.input.setTextAndPlaceCursorAtEnd
import androidx.compose.material3.BottomSheet
import androidx.compose.material3.ExpandedFullScreenSearchBar
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.android.gms.maps.CameraUpdate
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.Polygon
import com.google.maps.android.compose.rememberCameraPositionState
import com.google.maps.android.compose.rememberUpdatedMarkerState
import com.jeffreyalanwang.dutchrailwaysandroidclient.Area
import com.jeffreyalanwang.dutchrailwaysandroidclient.BackendApi
import com.jeffreyalanwang.dutchrailwaysandroidclient.PassService
import com.jeffreyalanwang.dutchrailwaysandroidclient.Place
import com.jeffreyalanwang.dutchrailwaysandroidclient.R
import com.jeffreyalanwang.dutchrailwaysandroidclient.RoutePlan
import com.jeffreyalanwang.dutchrailwaysandroidclient.Station
import com.jeffreyalanwang.dutchrailwaysandroidclient.calculateBounds
import com.jeffreyalanwang.dutchrailwaysandroidclient.letWith
import com.jeffreyalanwang.dutchrailwaysandroidclient.toLocalTime
import com.jeffreyalanwang.dutchrailwaysandroidclient.ui.CommonChildRoute
import com.jeffreyalanwang.dutchrailwaysandroidclient.ui.PlaceDetailRoute
import com.jeffreyalanwang.dutchrailwaysandroidclient.ui.RouteDetailRoute
import com.jeffreyalanwang.dutchrailwaysandroidclient.ui.RouteOptionsRoute
import com.jeffreyalanwang.dutchrailwaysandroidclient.ui.StationDetailRoute
import com.jeffreyalanwang.dutchrailwaysandroidclient.ui.TimePickerRoute
import com.jeffreyalanwang.dutchrailwaysandroidclient.ui.TrainQueryGraphChildRoute
import com.jeffreyalanwang.dutchrailwaysandroidclient.ui.TrainQueryGraphRoute
import com.jeffreyalanwang.dutchrailwaysandroidclient.ui.TrainQuerySelectionRoute
import com.jeffreyalanwang.dutchrailwaysandroidclient.ui.TrainServiceDetailRoute
import com.jeffreyalanwang.dutchrailwaysandroidclient.ui.components.AppBarWithDualSearch
import com.jeffreyalanwang.dutchrailwaysandroidclient.ui.components.ClearableTimePicker
import com.jeffreyalanwang.dutchrailwaysandroidclient.ui.components.PlaceSearchResults
import com.jeffreyalanwang.dutchrailwaysandroidclient.ui.components.rememberDualSearchBarState
import com.jeffreyalanwang.dutchrailwaysandroidclient.ui.detailScreens.AreaDetailWithoutMap
import com.jeffreyalanwang.dutchrailwaysandroidclient.ui.detailScreens.RouteDetailWithoutMap
import com.jeffreyalanwang.dutchrailwaysandroidclient.ui.detailScreens.StationDetailWithoutMap
import com.jeffreyalanwang.dutchrailwaysandroidclient.ui.detailScreens.TrainServiceDetail
import com.jeffreyalanwang.dutchrailwaysandroidclient.ui.screens.child.RouteOptionsList
import com.jeffreyalanwang.dutchrailwaysandroidclient.ui.util.AppStringFormats
import com.jeffreyalanwang.dutchrailwaysandroidclient.ui.util.boundsForDisplay
import com.jeffreyalanwang.dutchrailwaysandroidclient.ui.util.getMapCameraUpdate
import com.jeffreyalanwang.dutchrailwaysandroidclient.ui.util.paddedBelow
import com.jeffreyalanwang.dutchrailwaysandroidclient.ui.util.topOnly
import com.jeffreyalanwang.dutchrailwaysandroidclient.ui.viewmodel.DataState
import com.jeffreyalanwang.dutchrailwaysandroidclient.ui.viewmodel.Endpoint
import com.jeffreyalanwang.dutchrailwaysandroidclient.ui.viewmodel.RoutePlannerViewModel
import kotlinx.collections.immutable.ImmutableList
import kotlinx.coroutines.launch
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atDate
import kotlinx.datetime.todayIn
import kotlin.time.Clock
import kotlin.time.Instant

val ON_MAP_SHADOW_ELEVATION = 12.dp

@Preview
@Composable
private fun RoutePlannerScreenPreview() {
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    val viewModel = viewModel<RoutePlannerViewModel>()

    val backStack by viewModel.backStack.collectAsState()

    RoutePlannerScreen (
        routeArgs = backStack.last(),
        viewModel = viewModel,
        onNavigateMinor = { newRoute ->
            scope.launch {
                snackbarHostState.showSnackbar(
                    newRoute.toString(),
                    withDismissAction = true
                )
            }
        },
    )

    Column(Modifier.fillMaxSize(), Arrangement.Bottom) {
        SnackbarHost(hostState = snackbarHostState)
    }
}

@Composable
fun RoutePlannerScreen(
    routeArgs: TrainQueryGraphRoute,
    viewModel: RoutePlannerViewModel,
    onNavigateMinor: (TrainQueryGraphChildRoute)->Unit,
) {
    // This composable can only navigate to major routes by triggering changes
    // in [viewModel]. However, it is allowed to navigate to minor routes
    // using [onNavigateMinor].

    val viewModelState by viewModel.uiState.collectAsStateWithLifecycle()
    var mapLoaded by remember { mutableStateOf(false) }
    val initialZoomExecuted = viewModel.initialZoomExecuted

    val subjectPlace: Place?
    val subjectRoute: RoutePlan?
    val subjectTrainService: PassService?
    when (routeArgs) {
        is PlaceDetailRoute -> {
            subjectPlace = remember(routeArgs) {
                when (routeArgs.id) {
                    viewModelState.origin?.id -> viewModelState.origin
                    viewModelState.destination?.id -> viewModelState.destination
                    else -> BackendApi.get_place_info(routeArgs.id)
                }
            }
            subjectRoute = null
            subjectTrainService = null
        }
        is RouteDetailRoute -> {
            subjectPlace = null
            subjectRoute = remember(routeArgs, viewModelState) {
                viewModelState.routes!![routeArgs.index]
            }
            subjectTrainService = null
        }
        is TrainServiceDetailRoute -> {
            subjectPlace = null
            subjectRoute = null
            subjectTrainService = remember() {
                BackendApi.get_pass_service(routeArgs.id)
            }
        }
        else -> {
            subjectPlace = null
            subjectRoute = null
            subjectTrainService = null
        }
    }

    // Extensive use of null-safe [?.run] and [?.let] serves to
    // allow discrepancies between viewModel's [routeArgs] state
    // and the one passed; this issue occurs when [NavDisplay]
    // handles predictive back by rendering two versions of
    // [RoutePlannerScreen] (one with old and new [routeArgs]).

    RoutePlannerScreen(
        viewModel = viewModel,
        viewModelState = viewModelState,

        // Short circuit on [mapLoaded], ensuring we never try to evaluate lazy property
        // [mapCameraUpdate] before it is allowed to use [CameraUpdateFactory].
        onMapLoaded = { mapLoaded = true },
        onZoomComplete = { viewModel.setInitialZoomExecuted() }, // technically only needed when TrainQuerySelectionRoute
        mapCameraUpdate = if (!mapLoaded) null
            else when (routeArgs) {
                is TrainQuerySelectionRoute
                    -> if (initialZoomExecuted) null else
                        BackendApi.get_nl_area()
                        .run {
                            remember { boundsForDisplay().paddedBelow(1/2f).getMapCameraUpdate(100) }
                        }
                is PlaceDetailRoute
                    -> subjectPlace?.run { remember(this) {
                        boundsForDisplay().paddedBelow(1/2f).getMapCameraUpdate(100)
                    } }
                is RouteOptionsRoute
                    -> listOfNotNull(viewModelState.origin, viewModelState.destination)
                    .calculateBounds().run { remember(this) {
                        paddedBelow(1/2f).getMapCameraUpdate(100)
                    } }
                is RouteDetailRoute
                    -> subjectRoute?.stopsByLayover()?.map { it.first().getStation() }
                    ?.calculateBounds()?.run { remember(this) {
                        paddedBelow(1/2f).getMapCameraUpdate(100)
                    } }
                is TrainServiceDetailRoute
                    -> subjectTrainService?.getStops()?.map { it.getStation() }
                    ?.calculateBounds()?.run { remember(this) {
                        paddedBelow(1/2f).getMapCameraUpdate(100)
                    } }

                else
                    -> throw IllegalArgumentException("Unrecognized routeArgs type: ${routeArgs::class}")
            },
        mapMarkers = when (routeArgs) {
                is TrainQuerySelectionRoute
                    -> emptyList<Place>()
                is PlaceDetailRoute
                    -> setOfNotNull(viewModelState.origin, viewModelState.destination, subjectPlace)
                is RouteOptionsRoute
                    -> setOfNotNull(viewModelState.origin, viewModelState.destination)
                is RouteDetailRoute
                    -> subjectRoute!!.stopsByLayover().map { it.first().getStation() }.toSet()
                is TrainServiceDetailRoute
                    -> subjectTrainService!!.getStops().map { it.getStation() }.toSet()

                else ->
                    throw IllegalArgumentException("Unrecognized routeArgs type: ${routeArgs::class}")
            },

        bottomSheetContent = when (routeArgs) {
            is TrainQuerySelectionRoute -> {
                null
            }
            is PlaceDetailRoute -> subjectPlace?.let{
                { BottomSheetContent(it, onNavigateMinor) }
            }
            is RouteOptionsRoute -> viewModelState.routes?.let {
                { BottomSheetContent(it, onNavigateMinor) }
            }
            is RouteDetailRoute -> subjectRoute?.let{
                { BottomSheetContent(it, onNavigateMinor) }
            }
            is TrainServiceDetailRoute -> subjectTrainService?.let{
                { BottomSheetContent(it, onNavigateMinor) }
            }
            else ->
                throw IllegalArgumentException("Unrecognized routeArgs type: ${routeArgs::class}")
        },
    )
}


@Composable
private fun RoutePlannerScreen(
    viewModel: RoutePlannerViewModel,
    viewModelState: DataState,

    onMapLoaded: () -> Unit,
    onZoomComplete: () -> Unit,
    mapCameraUpdate: CameraUpdate?,
    mapMarkers: Collection<Place>,

    bottomSheetContent: @Composable (BoxScope.() -> Unit)?,
) {
    RoutePlannerScreen(
        setOrigin = { viewModel.setOrigin(it) },
        setDestination = { viewModel.setDestination(it) },

        departTime = viewModelState.departTime,
        arriveTime = viewModelState.arriveTime,

        onMapLoaded = onMapLoaded,
        onZoomComplete = onZoomComplete,
        mapMarkers = mapMarkers,

        mapCameraUpdate = mapCameraUpdate,

        isSubmitQueryAllowed = viewModelState.canSubmitQuery,
        onSubmitQuery = viewModel::loadRoutes,

        onOpenTimePicker = { viewModel.pushUserRequested(TimePickerRoute(forEndpoint = it)) },

        bottomSheetContent = bottomSheetContent,
    )
}

@Composable
private fun RoutePlannerScreen(
    setOrigin: (Place?) -> Unit,
    setDestination: (Place?) -> Unit,

    departTime: Instant?,
    arriveTime: Instant?,

    mapCameraUpdate: CameraUpdate?,

    mapMarkers: Collection<Place>,
    onZoomComplete: () -> Unit,
    onMapLoaded: () -> Unit,

    isSubmitQueryAllowed: Boolean,
    onSubmitQuery: () -> Unit,

    onOpenTimePicker: (Endpoint) -> Unit,

    bottomSheetContent: @Composable (BoxScope.() -> Unit)?,
) {
    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            PersistentTopBar(
                isSubmitQueryAllowed = isSubmitQueryAllowed,
                arriveTime = arriveTime,
                departTime = departTime,
                onOpenTimePicker = onOpenTimePicker,
                setOrigin = setOrigin,
                setDestination = setDestination,
                onSubmitQuery = onSubmitQuery,
            )
         },
    ) { innerPadding ->

        PersistentGoogleMap(
            markers = mapMarkers,

            camera = mapCameraUpdate,
            onMapLoaded = onMapLoaded,
            onZoomComplete = onZoomComplete,

            contentPadding = innerPadding,
        )

        RevealableBottomSheet(
            isVisible = bottomSheetContent != null,
            scaffoldPadding = innerPadding,
            content = bottomSheetContent ?: {},
        )
    }
}

@Composable
private fun PersistentTopBar(
    modifier: Modifier = Modifier,
    isSubmitQueryAllowed: Boolean,
    arriveTime: Instant?,
    departTime: Instant?,
    onOpenTimePicker: (Endpoint) -> Unit,
    setOrigin: (Place?) -> Unit,
    setDestination: (Place?) -> Unit,
    onSubmitQuery: () -> Unit,
) {
    val scope = rememberCoroutineScope()
    val dualSearchBarState = rememberDualSearchBarState()
    fun closeSearch() { scope.launch { dualSearchBarState.animateToCollapsed() } }

    AppBarWithDualSearch(
        dualSearchBarState,
        modifier = modifier,
        colors = SearchBarDefaults.appBarWithSearchColors(
            appBarContainerColor = Color.Transparent,
        ),
        shadowElevation = ON_MAP_SHADOW_ELEVATION,
        actionIcon = {
            IconButton(
                enabled = isSubmitQueryAllowed,
                colors = IconButtonDefaults.filledIconButtonColors(),
                modifier = Modifier.padding(horizontal = 4.dp),
                onClick = onSubmitQuery,
            ) {
                Icon(
                    painterResource(R.drawable.ic_search),
                    contentDescription = "Search routes"
                )
            }
        },
        inputField1 = inputFieldFactory(
            placeholderText = "Search departure",
            onFinishSearch = ::closeSearch,
            timeConstraintField = departTime,
            timeButtonContentDescription = "Select departure time",
            onTimeConstraintButtonClick = { onOpenTimePicker(Endpoint.Origin) },
        ),
        inputField2 = inputFieldFactory(
            placeholderText = "Search arrival",
            onFinishSearch = ::closeSearch,
            timeConstraintField = arriveTime,
            timeButtonContentDescription = "Select arrival time",
            onTimeConstraintButtonClick = { onOpenTimePicker(Endpoint.Destination) },
        ),
        expandedSearch1 = expandedSearchFactory(
            placeholderText = "Search departure",
            onNewSelection = { setOrigin(it) },
            onFinishSearch = ::closeSearch,
        ),
        expandedSearch2 = expandedSearchFactory(
            placeholderText = "Search arrival",
            onNewSelection = { setDestination(it) },
            onFinishSearch = ::closeSearch,
        ),
    )
}

private inline fun inputFieldFactory(
    placeholderText: String,
    crossinline onFinishSearch: () -> Unit,
    crossinline onTimeConstraintButtonClick: () -> Unit,
    timeConstraintField: Instant?,
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
                    IconButton(onClick = { onTimeConstraintButtonClick() }) {
                        Icon(
                            painterResource(R.drawable.ic_time_add),
                            contentDescription = timeButtonContentDescription,
                        )
                    }
                } else {
                    TextButton(onClick = { onTimeConstraintButtonClick() }) {
                        Text( timeConstraintField
                            .letWith ( TimeZone.currentSystemDefault() )
                                { it.toLocalTime() }
                            .let { AppStringFormats.Time(it) } )
                    }
                }
            }
        )
    }
}
private inline fun expandedSearchFactory(
    placeholderText: String,
    crossinline onNewSelection: (Place?) -> Unit,
    crossinline onFinishSearch: () -> Unit,
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
                                "Close search",
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
private fun PersistentGoogleMap(
    markers: Collection<Place>,

    camera: CameraUpdate?,
    onMapLoaded: () -> Unit,
    onZoomComplete: () -> Unit,

    contentPadding: PaddingValues,
) {
    val cameraState = rememberCameraPositionState()

    camera?.let {
        LaunchedEffect(it) {
            cameraState.animate(it)
            onZoomComplete()
        }
    }

    GoogleMap(
        cameraPositionState = cameraState,
        contentDescription = "Map",
        modifier = Modifier.fillMaxSize(),
        contentPadding = contentPadding,
        onMapLoaded = onMapLoaded,
    ) {
        markers.forEach { when (it) {
            is Station ->
                Marker(
                    title = it.name,
                    state = rememberUpdatedMarkerState(
                        position = it.geom
                    )
                )
            is Area ->
                Polygon(
                    tag = it.name,
                    points = it.getGeom().points,
                    holes = it.getGeom().holes,
                    fillColor = Color.Transparent,
                )
            else ->
                throw NotImplementedError()
        } }
    }
}

@Composable
private fun RevealableBottomSheet(
    isVisible: Boolean,
    scaffoldPadding: PaddingValues,
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit,
) {
    val sheetState = rememberStandardBottomSheetState(
        skipHiddenState = false,
        initialValue = SheetValue.PartiallyExpanded,
        confirmValueChange = { when (it) {
            // Do not allow user to request hidden if currently visible,
            // but allow when triggered by [LaunchedEffect(isVisible)]
            SheetValue.Hidden -> !isVisible

            // User wouldn't be able to request partiallyExpanded/expanded if currently hidden
            else -> true
        } }
    )

    LaunchedEffect(isVisible) {
        if (isVisible) sheetState.expand()
        else sheetState.hide()
    }

    BottomSheet(
        state = sheetState,
        shadowElevation = ON_MAP_SHADOW_ELEVATION,
        backHandlerEnabled = false,

        // When fully expanded, do not cover scaffold's app bar
        modifier = modifier.padding(scaffoldPadding.topOnly()),
    ) {
        Box(
            modifier = Modifier
                .padding(bottom = 25.dp),
            content = content,
        )
    }
}

@Composable
private fun BottomSheetContent(
    route: RoutePlan,
    onNavigate: (CommonChildRoute) -> Unit,
) = RouteDetailWithoutMap(
        route = route,
        onNavigate = onNavigate,
        Modifier.padding(horizontal = 10.dp),
    )

@Composable
private fun BottomSheetContent(
    routes: ImmutableList<RoutePlan>,
    onNavigate: (RouteDetailRoute) -> Unit,
) = RouteOptionsList(
        routes = routes,
        onNavigate = onNavigate,
        Modifier.padding(horizontal = 10.dp),
    )

@Composable
private fun BottomSheetContent(
    place: Place,
    onNavigate: (CommonChildRoute) -> Unit,
) = when(place) {
        is Station ->
            StationDetailWithoutMap(
                station = place,
                onNavigate = onNavigate,
                Modifier.padding(horizontal = 10.dp),
            )

        is Area ->
            AreaDetailWithoutMap(
                area = place,
                onNavigate = onNavigate,
                Modifier.padding(horizontal = 10.dp),
            )

        else ->
            throw NotImplementedError()
    }

@Composable
private fun BottomSheetContent(
    passService: PassService,
    onNavigate: (StationDetailRoute) -> Unit,
) = TrainServiceDetail(
    service = passService,
    onNavigate = onNavigate,
    Modifier.padding(horizontal = 10.dp)
)

@Composable
fun EndpointTimePicker(
    forEndpoint: Endpoint,
    viewModel: RoutePlannerViewModel,
    onDismiss: () -> Unit,
) {
    EndpointTimePicker(
        forEndpoint = forEndpoint,
        onDismiss = onDismiss,
        departTime = viewModel.uiState.collectAsState().value.departTime,
        arriveTime = viewModel.uiState.collectAsState().value.arriveTime,
        setDepartTime = { viewModel.setTimeConstraints(departTime = it) },
        setArriveTime = { viewModel.setTimeConstraints(arriveTime = it) },
    )
}

@Composable
fun EndpointTimePicker(
    forEndpoint: Endpoint,
    onDismiss: () -> Unit,

    departTime: Instant?,
    setDepartTime: (Instant?) -> Unit,

    arriveTime: Instant?,
    setArriveTime: (Instant?) -> Unit,
) {
    val title = when (forEndpoint) {
        Endpoint.Origin -> "Select depart time"
        Endpoint.Destination -> "Select arrive time"
    }
    val initialInstant = when (forEndpoint) {
        Endpoint.Origin -> departTime
        Endpoint.Destination -> arriveTime
    }
    val setTime = when (forEndpoint) {
        Endpoint.Origin -> { it: Instant? -> setDepartTime(it) }
        Endpoint.Destination -> { it: Instant? -> setArriveTime(it) }
    }
    ClearableTimePicker(
        title = title,
        initialTime = with(TimeZone.currentSystemDefault()) { initialInstant?.toLocalTime() },
        onConfirm = { selectedTime ->
            setTime(
                with(TimeZone.currentSystemDefault()) {
                    selectedTime?.atDate(Clock.System.todayIn(this))?.toInstant()
                }
            )
            onDismiss()
        },
        onDismiss = onDismiss,
    )
}