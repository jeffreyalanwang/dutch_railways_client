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
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SearchBarDefaults
import androidx.compose.material3.SearchBarState
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.rememberStandardBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
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
import com.jeffreyalanwang.dutchrailwaysandroidclient.ui.components.PlaceSearchResults
import com.jeffreyalanwang.dutchrailwaysandroidclient.ui.components.SearchBarId
import com.jeffreyalanwang.dutchrailwaysandroidclient.ui.components.rememberDualSearchBarState
import com.jeffreyalanwang.dutchrailwaysandroidclient.ui.detailScreens.AreaDetailWithoutMap
import com.jeffreyalanwang.dutchrailwaysandroidclient.ui.detailScreens.StationDetailWithoutMap
import com.jeffreyalanwang.dutchrailwaysandroidclient.ui.util.bottomOnly
import com.jeffreyalanwang.dutchrailwaysandroidclient.ui.util.topOnly
import com.jeffreyalanwang.dutchrailwaysandroidclient.ui.viewmodel.RoutePlannerViewModel
import kotlinx.coroutines.launch
import kotlin.time.ExperimentalTime

val SHADOW_ELEVATION_ON_MAP = 12.dp

@Preview
@Composable
private fun RoutePlannerScreenTest() {
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun inputFieldFactory(
    placeholderText: String,
    onFinishSearch: () -> Unit,
): @Composable (TextFieldState, SearchBarState, @Composable (() -> Unit)?) -> Unit {
    return { textFieldState, searchBarState, leadingIcon ->
        SearchBarDefaults.InputField(
            textFieldState = textFieldState,
            searchBarState = searchBarState,
            leadingIcon = leadingIcon,
            onSearch = { onFinishSearch() },
            placeholder = {
                Text(
                    modifier = Modifier.clearAndSetSemantics {},
                    text = placeholderText,
                )
            },
            trailingIcon = {
                Icon(painterResource(R.drawable.ic_search),
                    contentDescription="Search",
                )
            },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun expandedSearchFactory(
    onNewSelection: (Place) -> Unit,
    onFinishSearch: () -> Unit,
): @Composable (TextFieldState, SearchBarState, @Composable () -> Unit) -> Unit {
    return { textFieldState, searchBarState, inputField ->
        ExpandedFullScreenSearchBar(searchBarState, inputField = inputField) {
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

@OptIn(ExperimentalMaterial3Api::class, ExperimentalTime::class)
@Composable
fun RoutePlannerScreen(viewModel: RoutePlannerViewModel, onNavigate: (NavRoute)->Unit) {
    val scope = rememberCoroutineScope()
    val routePlannerState by viewModel.uiState.collectAsState()
    val dualSearchBarState = rememberDualSearchBarState()
    val cameraPositionState = rememberCameraPositionState()
    val bottomSheetState = rememberStandardBottomSheetState()

    // Will be [SearchBarId.None] only if no selection has been made yet
    var currViewing by rememberSaveable { mutableStateOf(SearchBarId.None) }
    var didInitPosition by remember { mutableStateOf(false) }

    fun closeSearch() { scope.launch { dualSearchBarState.animateToCollapsed() } }

    // origin/destination referenced by [value] should not be null
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
                shadowElevation = SHADOW_ELEVATION_ON_MAP,
                inputField1 = inputFieldFactory(
                    placeholderText = "Search departure",
                    onFinishSearch = { closeSearch() },
                ),
                inputField2 = inputFieldFactory(
                    placeholderText = "Search arrival",
                    onFinishSearch = { closeSearch() },
                ),
                expandedSearch1 = expandedSearchFactory(
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
                shadowElevation = SHADOW_ELEVATION_ON_MAP,
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
    }
}
