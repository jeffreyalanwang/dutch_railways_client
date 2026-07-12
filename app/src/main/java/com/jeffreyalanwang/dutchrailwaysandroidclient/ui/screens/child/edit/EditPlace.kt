package com.jeffreyalanwang.dutchrailwaysandroidclient.ui.screens.child.edit

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.foundation.text.input.setTextAndPlaceCursorAtEnd
import androidx.compose.material3.AppBarWithSearch
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SearchBarDefaults
import androidx.compose.material3.SearchBarValue
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberSearchBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.onLayoutRectChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.roundToIntRect
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.rememberCameraPositionState
import com.jeffreyalanwang.dutchrailwaysandroidclient.R
import com.jeffreyalanwang.dutchrailwaysandroidclient.backend.BackendApi
import com.jeffreyalanwang.dutchrailwaysandroidclient.backend.Geocoding
import com.jeffreyalanwang.dutchrailwaysandroidclient.letWith
import com.jeffreyalanwang.dutchrailwaysandroidclient.ui.components.CardContentScaffold
import com.jeffreyalanwang.dutchrailwaysandroidclient.ui.components.ExpandingHeroBox
import com.jeffreyalanwang.dutchrailwaysandroidclient.ui.components.NavBackButton
import com.jeffreyalanwang.dutchrailwaysandroidclient.ui.components.SaveChangesButton
import com.jeffreyalanwang.dutchrailwaysandroidclient.ui.screens.child.detail.AreaOnMap
import com.jeffreyalanwang.dutchrailwaysandroidclient.ui.screens.top.ON_MAP_SHADOW_ELEVATION
import com.jeffreyalanwang.dutchrailwaysandroidclient.ui.search.BaseSearchInputField
import com.jeffreyalanwang.dutchrailwaysandroidclient.ui.search.ExpandedSearch
import com.jeffreyalanwang.dutchrailwaysandroidclient.ui.search.LOCATION_SEARCH_PLACEHOLDER
import com.jeffreyalanwang.dutchrailwaysandroidclient.ui.search.LatLngResult
import com.jeffreyalanwang.dutchrailwaysandroidclient.ui.search.LocationResult
import com.jeffreyalanwang.dutchrailwaysandroidclient.ui.util.AppStringFormats
import com.jeffreyalanwang.dutchrailwaysandroidclient.ui.util.InitOrChangeEffect
import com.jeffreyalanwang.dutchrailwaysandroidclient.ui.util.OnChangeEffect
import com.jeffreyalanwang.dutchrailwaysandroidclient.ui.util.boundsForDisplay
import com.jeffreyalanwang.dutchrailwaysandroidclient.ui.util.getMapCameraUpdate
import com.jeffreyalanwang.dutchrailwaysandroidclient.ui.viewmodel.EditStationViewModel
import com.jeffreyalanwang.dutchrailwaysandroidclient.ui.viewmodel.LocationPickerModel
import kotlinx.coroutines.launch

@Preview
@Composable
private fun EditStationPreview() {
    EditStationScreen(
        id = 361,
        {},
        {},
    )
}

@Preview
@Composable
private fun EditAreaPreview() {
    EditAreaScreen(1, {}, {})
}

@Composable
fun EditStationScreen(
    id: Int,
    onCancelRequest: () -> Unit,
    onSaveFinished: () -> Unit,
) = EditStationScreen(
        viewModel = viewModel<EditStationViewModel> { EditStationViewModel(id) },
        onCancelRequest = onCancelRequest,
        onSaveFinished = onSaveFinished,
    )


/**
 * Too complicated to allow users to add or change regions, because
 * of strict backend constraints for [com.jeffreyalanwang.dutchrailwaysandroidclient.Area]
 * geometries.
 *
 * Instead, shows a non-editable map.
 */
@Composable
fun EditAreaScreen(
    id: Int,
    onCancelRequest: () -> Unit,
    onSaveFinished: () -> Unit,
) {
    val area = BackendApi.get_area_info(id)
    val nameState = rememberTextFieldState(area.name)
    val isNameValid by remember { derivedStateOf { nameState.text.isNotEmpty() } }

    CardContentScaffold(
        topBar = {
            TopAppBar(
                title = { Text("Edit area: ${area.name}", softWrap = false, overflow = TextOverflow.Ellipsis) },
                navigationIcon = { NavBackButton(onCancelRequest) },
                actions = { SaveChangesButton({
                    if (isNameValid) {
                        BackendApi.edit_area(id, nameState.text.toString())
                        onSaveFinished()
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
            isError = !isNameValid,
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
fun EditStationScreen(
    viewModel: EditStationViewModel,
    onCancelRequest: () -> Unit,
    onSaveFinished: () -> Unit,
    horizontalContentPadding: Dp = 10.dp,
) {
    var collapsedLocationSelectorBounds by remember { mutableStateOf(IntRect.Zero) }
    var windowInsets by remember { mutableStateOf<WindowInsets?>(null) }

    EditStationScreen(
        oldName = viewModel.initialName,
        nameFieldState = viewModel.nameFieldState,
        isNameValid = viewModel.isNameValid,

        currentCoords = viewModel.currGeom,
        currentAddress = viewModel.currAddress,

        onCancelRequest = onCancelRequest,
        onSaveRequest = {
            viewModel.saveChanges()
                ?.let { onSaveFinished() }
        },

        horizontalContentPadding = horizontalContentPadding,

        onSetLocationPickerExpanded = viewModel::onExpandLocationPicker,
        onCollapsedLocationPickerGloballyPositioned = { collapsedLocationSelectorBounds = it },
    ) {
        ExpandingLocationSelector(
            viewModel.locationPickerDelegate,
            stationName = { viewModel.currentName },
            collapsedBounds = { collapsedLocationSelectorBounds },
            horizontalContentPadding = horizontalContentPadding,
        )
    }
}

/**
 * This overload is missing its map.
 * @param onSaveRequest     Responsible for performing any desired navigation after save.
 */
@Composable
private fun EditStationScreen(
    oldName: String,
    nameFieldState: TextFieldState,
    isNameValid: Boolean,

    currentCoords: LatLng,
    currentAddress: String,

    onCancelRequest: () -> Unit,
    onSaveRequest: () -> Unit,

    onSetLocationPickerExpanded: () -> Unit,
    onCollapsedLocationPickerGloballyPositioned: (IntRect) -> Unit,

    modifier: Modifier = Modifier,
    horizontalContentPadding: Dp = 10.dp,

    expandingLocationSelector: @Composable () -> Unit,
) {
    val horizontalContentPadding = PaddingValues(horizontal = horizontalContentPadding)

    LocalContext.current.let { context ->
        LaunchedEffect(Unit) {
            Geocoding.initialize(context)
        }
    }

    CardContentScaffold(
        topBar = {
            TopAppBar(
                title = { Text("Edit station: $oldName", softWrap = false, overflow = TextOverflow.Ellipsis) },
                navigationIcon = { NavBackButton(onCancelRequest) },
                actions = { SaveChangesButton(onSaveRequest) },
            )
        },
        modifier = modifier,
    ) {
        Spacer(Modifier.height(20.dp))

        Icon(
            painterResource(R.drawable.ic_dr_station),
            "Station icon",
            Modifier.size(72.dp + 20.dp)
        )
        EditNameField(
            nameFieldState,
            isError = !isNameValid,
            placeholder = "Station name",
            modifier = Modifier
                .padding(horizontalContentPadding)
        )

        Spacer(Modifier.height(20.dp))

        LocationSelectorCaption(
            latLng = currentCoords,
            address = currentAddress,
            Modifier
                .testTag("location_selector_caption")
                .clickable(onClick = onSetLocationPickerExpanded)
                .padding(vertical = 10.dp)
                .padding(horizontalContentPadding),
        )
        HorizontalDivider(thickness = Dp.Hairline)
        Box(
            Modifier
                .onGloballyPositioned {
                    onCollapsedLocationPickerGloballyPositioned(
                        it.boundsInRoot().roundToIntRect()
                    )
                }
                .fillMaxWidth()
                .height(400.dp),
        )
        HorizontalDivider(thickness = Dp.Hairline)

        Spacer(Modifier.height(20.dp))
    }

    expandingLocationSelector()
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
        modifier = modifier
            .semantics { contentDescription = "Text field: set name" },
    )

@Composable
fun ExpandingLocationSelector(
    viewModel: LocationPickerModel,
    stationName: () -> String,
    collapsedBounds: () -> IntRect,
    horizontalContentPadding: Dp = 0.dp,
) = ExpandingLocationSelector(
        stationName = stationName,
        currCoords = viewModel.geom,
        searchBarText = viewModel.displayString,
        onSelectSearchResult = {
            viewModel.updateLocation(it)
        },
        onSelectMapCoords = {
            val selection = LatLngResult(it)
            viewModel.updateLocation(selection)
        },

        isExpanded = viewModel.isExpanded,
        onSetExpanded = { viewModel.isExpanded = it },

        collapsedBounds = collapsedBounds,
        horizontalContentPadding = horizontalContentPadding,
    )

/**
 * @param searchBarText     The text to show in the search bar, or `null` for loading.
 */
@Composable
private fun ExpandingLocationSelector(
    stationName: () -> String,
    currCoords: LatLng,
    searchBarText: String?,
    onSelectSearchResult: (LocationResult?) -> Unit,
    onSelectMapCoords: (LatLng) -> Unit,

    isExpanded: Boolean,
    onSetExpanded: (Boolean) -> Unit,

    collapsedBounds: () -> IntRect,
    horizontalContentPadding: Dp = 0.dp,
) = ExpandingHeroBox(
        isExpanded = isExpanded,
        onDismissRequest = { onSetExpanded(false) },
        collapsedBounds = { collapsedBounds() },
    ) {
        val scope = rememberCoroutineScope()
        val density = LocalDensity.current
        var appBarHeight by remember { mutableStateOf(0.dp) }
        var mapBounds by remember { mutableStateOf<LatLngBounds?>(null) }

        val searchBarState = rememberSearchBarState()
        val collapsedTextFieldState = rememberTextFieldState()
            .apply {
                LaunchedEffect(searchBarText) {
                    val text = searchBarText ?: "Loading..."
                    setTextAndPlaceCursorAtEnd(text)
                }
            }
        val expandedTextFieldState = rememberTextFieldState()
            .apply {
                LaunchedEffect(searchBarState.targetValue) {
                    if (searchBarState.targetValue == SearchBarValue.Expanded) {
                        val text = collapsedTextFieldState.text
                        setTextAndPlaceCursorAtEnd(text.toString())
                    }
                }
            }

        EditableMarkerMap(
            location = currCoords,
            onLocationSelected =
                if (isExpanded) onSelectMapCoords
                else            null,
            onMapClick =
                if (isExpanded) null
                else {
                    { onSetExpanded(true) }
                },
            onMapSettle = { mapBounds = it },
            markerTitle = stationName(),
            contentDescription = "Select station location on map",
            contentPadding =
                if (!isExpanded) PaddingValues(horizontal = horizontalContentPadding)
                else             PaddingValues(top = appBarHeight)
        )

        AnimatedVisibility(
            isExpanded,
            enter = MaterialTheme.motionScheme.run { slideInVertically(slowSpatialSpec()) },
            exit = MaterialTheme.motionScheme.run { slideOutVertically(fastSpatialSpec()) },
        ) {
            AppBarWithSearch(
                searchBarState,
                colors = SearchBarDefaults.appBarWithSearchColors(
                    appBarContainerColor = Color.Transparent
                ),
                shadowElevation = ON_MAP_SHADOW_ELEVATION,
                modifier = Modifier
                    .testTag("location_search_bar")
                    .onLayoutRectChanged {
                        appBarHeight = it.height
                            .letWith(density) { h -> h.toDp() }
                    },
                inputField = {
                    BaseSearchInputField(
                        LOCATION_SEARCH_PLACEHOLDER,
                        collapsedTextFieldState,
                        searchBarState,
                        modifier = Modifier
                            .testTag("location_search_input")
                            .fillMaxWidth(),
                        leadingIcon = {
                            NavBackButton({ onSetExpanded(false) })
                        },
                    )
                },
            )
        }

        ExpandedSearch<LocationResult>(
            expandedTextFieldState,
            searchBarState,
            mapBounds = mapBounds,
            onClose = { scope.launch { searchBarState.animateToCollapsed() } },
            onSelectResult = onSelectSearchResult,
            onClearedText = {}, // The text in the text box does change, but we
                                // make no changes to data state
        )

    }

@Composable
private fun LocationSelectorCaption(
    latLng: LatLng,
    address: String,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier
            .padding(5.dp)
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
 * A [GoogleMap] which allows the user to select a location, and also
 * updates camera area and marker location via a [LaunchedEffect].
 *
 * @param onLocationSelected    Set to null to disable interactions
 *                              that select a location.
 */
@Composable
private fun EditableMarkerMap(
    location: LatLng?,
    onLocationSelected: ((LatLng) -> Unit)?,
    markerTitle: String,
    modifier: Modifier = Modifier,
    contentDescription: String? = null,
    onMapClick: (() -> Unit)? = null,
    onMapSettle: ((LatLngBounds) -> Unit)? = null,
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

    cameraPositionState.run {
        LaunchedEffect(isMoving) {
            if (!isMoving) projection?.run {
                onMapSettle?.invoke(visibleRegion.latLngBounds)
            }
        }
    }

    GoogleMap(
        cameraPositionState = cameraPositionState,
        onMapClick = { onMapClick?.invoke() },
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
