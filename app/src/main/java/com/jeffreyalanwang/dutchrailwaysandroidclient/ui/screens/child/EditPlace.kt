package com.jeffreyalanwang.dutchrailwaysandroidclient.ui.screens.child

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.SharedTransitionScope.ResizeMode.Companion.RemeasureToBounds
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.clearText
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.foundation.text.input.setTextAndPlaceCursorAtEnd
import androidx.compose.material3.AppBarWithSearch
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ScaffoldDefaults
import androidx.compose.material3.SearchBarDefaults
import androidx.compose.material3.SearchBarState
import androidx.compose.material3.SearchBarValue
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberSearchBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onLayoutRectChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.rememberCameraPositionState
import com.jeffreyalanwang.dutchrailwaysandroidclient.R
import com.jeffreyalanwang.dutchrailwaysandroidclient.backend.BackendApi
import com.jeffreyalanwang.dutchrailwaysandroidclient.backend.Geocoding
import com.jeffreyalanwang.dutchrailwaysandroidclient.ui.AreaDetailNavArgs
import com.jeffreyalanwang.dutchrailwaysandroidclient.ui.StationDetailNavArgs
import com.jeffreyalanwang.dutchrailwaysandroidclient.ui.components.CardContentScaffold
import com.jeffreyalanwang.dutchrailwaysandroidclient.ui.components.NavBackButton
import com.jeffreyalanwang.dutchrailwaysandroidclient.ui.components.SaveChangesButton
import com.jeffreyalanwang.dutchrailwaysandroidclient.ui.screens.top.ON_MAP_SHADOW_ELEVATION
import com.jeffreyalanwang.dutchrailwaysandroidclient.ui.search.AddressResult
import com.jeffreyalanwang.dutchrailwaysandroidclient.ui.search.BaseSearchInputField
import com.jeffreyalanwang.dutchrailwaysandroidclient.ui.search.ExpandedSearch
import com.jeffreyalanwang.dutchrailwaysandroidclient.ui.search.LOCATION_SEARCH_PLACEHOLDER
import com.jeffreyalanwang.dutchrailwaysandroidclient.ui.search.LatLngResult
import com.jeffreyalanwang.dutchrailwaysandroidclient.ui.search.LocationResult
import com.jeffreyalanwang.dutchrailwaysandroidclient.ui.util.AppStringFormats
import com.jeffreyalanwang.dutchrailwaysandroidclient.ui.util.InitOrChangeEffect
import com.jeffreyalanwang.dutchrailwaysandroidclient.ui.util.OnChangeEffect
import com.jeffreyalanwang.dutchrailwaysandroidclient.ui.util.boundsForDisplay
import com.jeffreyalanwang.dutchrailwaysandroidclient.ui.util.copy
import com.jeffreyalanwang.dutchrailwaysandroidclient.ui.util.getMapCameraUpdate
import kotlinx.coroutines.launch

typealias StationLocation = Pair<LatLng, String>

@Preview
@Composable
private fun EditStationPreview() {
    LocalContext.current.let { context ->
        LaunchedEffect(Unit) {
            Geocoding.initialize(context)
        }
    }

    EditStationScreen(361, {}, {})
}

@Preview
@Composable
private fun EditAreaPreview() {
    EditAreaScreen(1, {}, {})
}

@Composable
fun EditStationScreen(
    id: Int,
    onNavigate: (StationDetailNavArgs) -> Unit,
    onNavigateBack: () -> Unit,
) = SharedTransitionLayout {

    val station = BackendApi.get_station_info(id)
    val nameState = rememberTextFieldState(station.name)
    val isNameValid by remember { derivedStateOf { nameState.text.isEmpty() } }
    var location: StationLocation by rememberSaveable { mutableStateOf( station.run { geom to address } ) } // TODO use a viewmodel instead
    var isLocationSelectorExpanded by remember { mutableStateOf(false) }

    CardContentScaffold(
        topBar = {
            TopAppBar(
                title = { Text("Edit station: ${station.name}", softWrap = false, overflow = TextOverflow.Ellipsis) },
                navigationIcon = { NavBackButton(onNavigateBack) },
                actions = { SaveChangesButton({
                    if (isNameValid) {
                        BackendApi.edit_station(id, nameState.text.toString(), location.second, location.first)
                        onNavigateBack()

                        // Refresh the previous screen by closing + reopening
                        onNavigateBack()
                        onNavigate(StationDetailNavArgs(id))
                    }
                }) },
            )
        }
    ) {
        Spacer(Modifier.height(20.dp))

        Icon(
            painterResource(R.drawable.ic_dr_station),
            "Station icon",
            Modifier.size(72.dp + 20.dp)
        )
        EditNameField(
            nameState,
            isError = isNameValid,
            placeholder = "Station name",
            modifier = Modifier.padding(horizontal = 10.dp)
        )

        Spacer(Modifier.height(10.dp))

        LocationSelectorCaption(
            latLng = location.first,
            address = location.second,
            Modifier
                .clickable { isLocationSelectorExpanded = true }
                .padding(all = 10.dp),
        )

        HorizontalDivider(thickness = Dp.Hairline)
        AnimatedVisibility(true) {
            Box(
                Modifier.sharedBounds(
                    rememberSharedContentState("map"),
                    this@AnimatedVisibility,
                    resizeMode = RemeasureToBounds,
                    enter = fadeIn() + scaleIn(),
                    exit = fadeOut() + scaleOut(),
                )
            ) {
                EditableMarkerMap(
                    location = location.first,
                    onLocationSelected = null, // make it non-interactive TODO just borrow the map from StationDetail.kt
                    markerTitle = location.second,
                    onTap = { isLocationSelectorExpanded = true },
                    contentDescription = "Station on map (tap to select a new location)",
                    contentPadding = PaddingValues(horizontal = 10.dp),
                    modifier = Modifier.heightIn(200.dp, 400.dp),
                )
            }
        }
        HorizontalDivider(thickness = Dp.Hairline)

        Spacer(Modifier.height(20.dp))
    }

    AnimatedVisibility (isLocationSelectorExpanded) {
        FullScreenLocationSelector(
            station.name,
            location,
            onNewSelection = { location = it },
            onDismissRequest = { isLocationSelectorExpanded = false },
            modifier = Modifier.sharedBounds(
                rememberSharedContentState("map"),
                this@AnimatedVisibility,
                resizeMode = RemeasureToBounds,
                enter = fadeIn() + scaleIn(),
                exit = fadeOut() + scaleOut(),
            ),
        )
    }
}

@Composable
context(sharedTransitionScope: SharedTransitionScope, animatedVisibilityScope: AnimatedVisibilityScope)
private fun FullScreenLocationSelector(
    stationName: String,
    initialLocation: StationLocation,
    onNewSelection: (StationLocation) -> Unit,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
) = with (sharedTransitionScope) {
    val scope = rememberCoroutineScope()
    val textFieldState = rememberTextFieldState()
    val searchBarState = rememberSearchBarState()

    var result by remember { mutableStateOf<LocationResult?>(
        // [AddressResult] will not require a call to the geocoder
        initialLocation.run { AddressResult(first, second) }
    ) }
    val value by rememberUpdatedGeocoderResult(result, searchBarState, textFieldState)
    value?.let {
        LaunchedEffect(it) { onNewSelection(it) }
    }

    Box(modifier) {
        var appBarHeight by remember { mutableIntStateOf(0) }

        EditableMarkerMap(
            location = value?.first, // use of [value] instead of [result] means no
                                     // pan until the geocoder successfully resolves
            onLocationSelected = { result = LatLngResult(it) },
            markerTitle = stationName,
            contentDescription = "Select station location on map",
            contentPadding =
                ScaffoldDefaults.contentWindowInsets
                    .asPaddingValues()
                    .copy(
                        top = with(LocalDensity.current) { appBarHeight.toDp() }
                    ),
        )

        AnimatedVisibility(
            visible = true, //TODO fix
            enter = with(MaterialTheme.motionScheme) {
                fadeIn(
                    defaultEffectsSpec()
                ) + slideInVertically(defaultSpatialSpec())
            },
            exit = with(MaterialTheme.motionScheme) {
                fadeOut(
                    defaultEffectsSpec()
                ) + slideOutVertically(defaultSpatialSpec())
            },
        ) {
            AppBarWithSearch(
                searchBarState,
                colors = SearchBarDefaults.appBarWithSearchColors(
                    appBarContainerColor = Color.Transparent
                ),
                shadowElevation = ON_MAP_SHADOW_ELEVATION,
                modifier = Modifier.onLayoutRectChanged {
                    appBarHeight = it.height
                },
                inputField = {
                    BaseSearchInputField(
                        LOCATION_SEARCH_PLACEHOLDER,
                        textFieldState,
                        searchBarState,
                        modifier = Modifier.fillMaxWidth(),
                        leadingIcon = {
                            NavBackButton({ onDismissRequest() })
                        },
                    )
                },
            )
        }

        ExpandedSearch<LocationResult>(
            textFieldState,
            searchBarState,
            onClose = { scope.launch { searchBarState.animateToCollapsed() } },
            onSelectResult = {
                it?.let {
                    result = it
                }
            },
            onClearedText = {}, // The UI does change in this situation, but we
                                // retain the previous location when saving changes
        )
    }
}

@Composable
private fun LocationSelectorCaption(
    latLng: LatLng,
    address: String,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier
            .padding(horizontal = 5.dp)
            .height(IntrinsicSize.Min),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(
            Modifier.weight(1f)
        ) {
            Text(
                address,
                style = MaterialTheme.typography.titleSmallEmphasized,
                overflow = TextOverflow.Ellipsis,
                softWrap = false,
            )
            Text(
                AppStringFormats.LatLng(latLng),
                style = MaterialTheme.typography.bodySmall,
                overflow = TextOverflow.Ellipsis,
                softWrap = false,
            )
        }
        Icon(
            painterResource(R.drawable.ic_edit),
            contentDescription = "Edit",
        )
    }
}

/**
 * @param onLocationSelected    Set to null to disable interactions
 *                              that select a location.
 */
@Composable
private fun EditableMarkerMap(
    location: LatLng?,
    onLocationSelected: ((LatLng) -> Unit)?,
    markerTitle: String,
    modifier: Modifier = Modifier,
    onTap: (() -> Unit)? = null,
    contentDescription: String? = null,
    contentPadding: PaddingValues,
) {
    val onLocationSelected by rememberUpdatedState(onLocationSelected)
    val markerState = remember { MarkerState(location ?: LatLng(0.0, 0.0)) }

    // Marshall the two competing sources of truth for the marker's position
    location?.let {
        LaunchedEffect(it) { markerState.position = it }
    }
    OnChangeEffect(markerState.isDragging) {
        if (!markerState.isDragging) { // we just settled
            onLocationSelected?.invoke(markerState.position)
        }
    }

    var isMapInitialized by remember { mutableStateOf(false) }
    val cameraPositionState = rememberCameraPositionState()

    if (isMapInitialized) InitOrChangeEffect(location) { isFirstTime ->
        if (location == null) return@InitOrChangeEffect
        if (isFirstTime) {
            // Snap to location at good zoom level
            val cameraUpdate = location.boundsForDisplay().getMapCameraUpdate(200)
            cameraPositionState.move(cameraUpdate)
        } else {
            // Animate to location, no zoom change
            val cameraUpdate = CameraUpdateFactory.newLatLng(location)
            cameraPositionState.animate(cameraUpdate)
        }
    }

    Column {
        GoogleMap(
            cameraPositionState = cameraPositionState,
            onMapClick = { onTap?.invoke() },
            onMapLongClick = { onLocationSelected?.invoke(it) },
            onMapLoaded = { isMapInitialized = true },
            contentDescription = contentDescription,
            modifier = modifier,
            contentPadding = contentPadding,
        ) {
            Marker(
                markerState,
                title = markerTitle,
                draggable = onLocationSelected != null,
            )
        }
    }
}

/**
 * Too complicated to allow users to add or change regions, because
 * of strict backend constraints for [Area] geometries.
 *
 * Instead, shows a non-editable map.
 */
@Composable
fun EditAreaScreen(
    id: Int,
    onNavigate: (AreaDetailNavArgs) -> Unit,
    onNavigateBack: () -> Unit,
) {
    val area = BackendApi.get_area_info(id)
    val nameState = rememberTextFieldState(area.name)
    val isNameValid by remember { derivedStateOf { nameState.text.isEmpty() } }

    CardContentScaffold(
        topBar = {
            TopAppBar(
                title = { Text("Edit area: ${area.name}", softWrap = false, overflow = TextOverflow.Ellipsis) },
                navigationIcon = { NavBackButton(onNavigateBack) },
                actions = { SaveChangesButton({
                    if (isNameValid) {
                        BackendApi.edit_area(id, nameState.text.toString())
                        onNavigateBack()

                        // Refresh the previous screen by closing + reopening
                        onNavigateBack()
                        onNavigate(AreaDetailNavArgs(id))
                    }
                }) },
            )
        }
    ) {
        Spacer(Modifier.height(20.dp))

        Icon(
            painterResource(R.drawable.ic_dr_area),
            "Area icon",
            Modifier.size(72.dp + 20.dp)
        )
        EditNameField(
            nameState,
            isError = isNameValid,
            placeholder = "Area name",
            modifier = Modifier.padding(horizontal = 10.dp)
        )

        Spacer(Modifier.height(10.dp))

        HorizontalDivider(thickness = Dp.Hairline)
        AreaOnMap(
            remember { area.getGeom() },
            name = area.name,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(200.dp, 400.dp),
        )
        HorizontalDivider(thickness = Dp.Hairline)

        Spacer(Modifier.height(20.dp))
    }
}

@Composable
private fun EditNameField(
    state: TextFieldState,
    placeholder: String,
    modifier: Modifier = Modifier,
    isError: Boolean = false,
) = TextField(
        state,
        isError = isError,
        placeholder = {
            Text(
                placeholder,
                style = MaterialTheme.typography.displaySmall,
            )
        },
        textStyle = MaterialTheme.typography.displaySmall,
        contentPadding = PaddingValues.Zero,
        modifier = modifier,
    )

/**
 * Queries the Geocoder to convert [result] to a [StationLocation]
 * and returns it as an updated state.
 *
 * Syncs the address to [textFieldState], except when the
 * user is interacting with the search bar.
 *
 * This is a bit of a hack; to properly follow top-down data flow,
 * all search bars in the app should really use a custom [TextFieldState]
 * or similar, which displays the currently-selected address/etc. when
 * the search bar is collapsed, and otherwise acts as a normal text field.
 */
@Composable
private fun rememberUpdatedGeocoderResult(
    result: LocationResult?,
    searchBarState: SearchBarState,
    textFieldState: TextFieldState,
): State<StationLocation?> {
    val scope = rememberCoroutineScope()
    val valueState = remember { mutableStateOf<StationLocation?>(null) }

    DisposableEffect(result, searchBarState.targetValue) {
        val isSearchBarExpanding = searchBarState.targetValue != SearchBarValue.Collapsed

        if (result == null || isSearchBarExpanding) {
            return@DisposableEffect onDispose {}
        }

        val job =
            scope.launch {
                textFieldState.setTextAndPlaceCursorAtEnd("Loading...")
                val address = result.getAddress()
                    ?: throw Exception("No address found") // skips to block below
                textFieldState.setTextAndPlaceCursorAtEnd(address)
                valueState.value = result.latLng to address
            }.apply {
                invokeOnCompletion { throwable ->
                    throwable?.let { textFieldState.clearText() }
                }
            }

        onDispose {
            job.cancel()
        }

    }

    return valueState
}