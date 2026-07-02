package com.jeffreyalanwang.dutchrailwaysandroidclient.ui.screens.child

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.plus
import androidx.compose.foundation.layout.safeContent
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.foundation.text.input.setTextAndSelectAll
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SearchBarValue
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberSearchBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType.Companion.SegmentFrequentTick
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.lifecycle.viewmodel.compose.viewModel
import com.jeffreyalanwang.dutchrailwaysandroidclient.BackendApi
import com.jeffreyalanwang.dutchrailwaysandroidclient.PassService
import com.jeffreyalanwang.dutchrailwaysandroidclient.R
import com.jeffreyalanwang.dutchrailwaysandroidclient.Station
import com.jeffreyalanwang.dutchrailwaysandroidclient.StopPoint
import com.jeffreyalanwang.dutchrailwaysandroidclient.Trainset
import com.jeffreyalanwang.dutchrailwaysandroidclient.joinToString
import com.jeffreyalanwang.dutchrailwaysandroidclient.toKotlinLocalTime
import com.jeffreyalanwang.dutchrailwaysandroidclient.ui.PassServiceDetailNavArgs
import com.jeffreyalanwang.dutchrailwaysandroidclient.ui.components.DiscreteGridControl
import com.jeffreyalanwang.dutchrailwaysandroidclient.ui.components.DiscreteGridRow
import com.jeffreyalanwang.dutchrailwaysandroidclient.ui.components.EditAmenityBadgeSet
import com.jeffreyalanwang.dutchrailwaysandroidclient.ui.components.ElevatingReorderableItem
import com.jeffreyalanwang.dutchrailwaysandroidclient.ui.components.FormField
import com.jeffreyalanwang.dutchrailwaysandroidclient.ui.components.IconWithBadge
import com.jeffreyalanwang.dutchrailwaysandroidclient.ui.components.PredictiveBackDialog
import com.jeffreyalanwang.dutchrailwaysandroidclient.ui.components.ReorderDragHandle
import com.jeffreyalanwang.dutchrailwaysandroidclient.ui.components.TimePickerWithExtras
import com.jeffreyalanwang.dutchrailwaysandroidclient.ui.search.ExpandedSearch
import com.jeffreyalanwang.dutchrailwaysandroidclient.ui.util.AppIcons
import com.jeffreyalanwang.dutchrailwaysandroidclient.ui.util.AppStringFormats
import com.jeffreyalanwang.dutchrailwaysandroidclient.ui.util.horizontalOnly
import com.jeffreyalanwang.dutchrailwaysandroidclient.ui.util.providesWindowInsets
import com.jeffreyalanwang.dutchrailwaysandroidclient.ui.util.verticalOnly
import com.jeffreyalanwang.dutchrailwaysandroidclient.ui.viewmodel.EditPassServiceStopsModel
import com.jeffreyalanwang.dutchrailwaysandroidclient.ui.viewmodel.EditPassServiceViewModel
import kotlinx.coroutines.launch
import sh.calvin.reorderable.ReorderableColumn

@Preview
@Composable
private fun EditPassServiceScreenPreview(serviceId: Int = 119) {
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    fun notify(message: String) = scope.launch {
        snackbarHostState.showSnackbar(
            message,
            withDismissAction = true,
        )
    }

    val service = BackendApi.get_pass_service(serviceId)
    val viewModel: EditPassServiceViewModel =
        viewModel { EditPassServiceViewModel(service, serviceId) }

    EditPassServiceScreenBase(
        viewModel = viewModel,
        screenTitle = "Edit train service",
        onFinished = { viewModel.run {
            notify(
                joinToString( "\t\n",
                    "Finished: $title ($trainsetSelection)",
                        "$amenitiesMultiSelection",
                        "$stops",
                )
            )
        } },
        onCancelled = { notify("Cancelled") },
    )

    Box(Modifier.fillMaxSize()) {
        SnackbarHost(
            snackbarHostState,
            Modifier.align(Alignment.BottomCenter)
        )
    }
}


@Composable
fun NewPassServiceScreen(
    basedOnService: PassService? = null,
    onNavigateBack: () -> Unit,
    onNavigate: (PassServiceDetailNavArgs) -> Unit,
) {
    val viewModel: EditPassServiceViewModel = viewModel {
        EditPassServiceViewModel(
            basedOnService = basedOnService,
            destPassServiceId = null,
        )
    }

    EditPassServiceScreenBase(
        viewModel = viewModel,
        screenTitle = "New train service",
        onFinished = { id ->
            onNavigateBack()
            onNavigate(PassServiceDetailNavArgs(id)) // TODO you might need instead to refresh the previous page
        },
        onCancelled = onNavigateBack,
        modifier = Modifier.padding(vertical = 20.dp)
    )
}

@Composable
fun EditPassServiceScreen(
    id: Int,
    onNavigateBack: () -> Unit,
    onNavigate: (PassServiceDetailNavArgs) -> Unit,
) {
    val service = BackendApi.get_pass_service(id)
    val viewModel: EditPassServiceViewModel = viewModel {
        EditPassServiceViewModel(
            basedOnService = service,
            destPassServiceId = id,
        )
    }
    EditPassServiceScreenBase(
        viewModel = viewModel,
        screenTitle = "Edit train service",
        onFinished = { id ->
            onNavigateBack()
            onNavigate(PassServiceDetailNavArgs(id)) // TODO you might need instead to refresh the previos page
        },
        onCancelled = onNavigateBack,
        modifier = Modifier.padding(vertical = 20.dp)
    )
}

@Composable
private fun EditPassServiceScreenBase(
    viewModel: EditPassServiceViewModel,
    screenTitle: String,
    modifier: Modifier = Modifier,
    onFinished: (Int) -> Unit,
    onCancelled: () -> Unit,
) {
    var amenityBadgesBounds by WindowInsets.safeContent.let { remember { mutableStateOf(it) } }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(screenTitle) },
                navigationIcon = {
                    IconButton(onClick = onCancelled) {
                        Icon(
                            painterResource(R.drawable.ic_back),
                            contentDescription = "Back"
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.saveChanges()?.let { onFinished(it) } }) {
                        Icon(
                            painterResource(R.drawable.ic_done),
                            contentDescription = "Finish & save"
                        )
                    }
                },
            )
        },
        modifier = modifier.fillMaxSize(),
    ) { innerPadding ->

        Box(Modifier.verticalScroll(rememberScrollState())) {
            Card(
                Modifier
                    .padding(innerPadding + PaddingValues(10.dp))
                    .fillMaxSize()
                    .providesWindowInsets { amenityBadgesBounds = it }
            ) {
                Row(
                    Modifier.padding(start = 10.dp, top = 10.dp),
                    verticalAlignment = Alignment.Bottom
                ) {
                    EditRollingStockIcon(
                        viewModel.trainsetSelection,
                        !viewModel.trainsetValid,
                    ) { viewModel.trainsetSelection = it }

                    var areAmenitiesExpanded by remember { mutableStateOf(false) }
                    EditAmenityBadgeSet(
                        viewModel.amenitiesMultiSelection,
                        containerModifier = Modifier
                            .offset(x = -7.5.dp, y = -10.dp)
                            .testTag("amenity_badges"),
                        isExpanded = areAmenitiesExpanded,
                        onSetExpanded = { areAmenitiesExpanded = it },
                        onModify = { viewModel.amenitiesMultiSelection = it },
                        windowInsets = amenityBadgesBounds,
                    )
                }

                // Name
                Text(
                    viewModel.title,
                    style = MaterialTheme.typography.displaySmall,
                    modifier = Modifier
                        .padding(horizontal = 10.dp)
                        .testTag("screen_title_text")
                )

                Spacer(Modifier.height(10.dp))

                // Stops (arrive; depart; station)
                EditStops(
                    viewModel,
                    Modifier.padding(horizontal = 4.dp)
                )

                Spacer(Modifier.height(10.dp))
            }
        }

    }
}

@Composable
private fun EditRollingStockIcon(
    trainsetSelection: Trainset?,
    isError: Boolean,
    onSelectTrainset: (Trainset) -> Unit,
) {
    var isDropdownOpen by remember { mutableStateOf(false) }
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        AnimatedContent(
            if (isDropdownOpen) trainsetSelection else null,
            Modifier
                .sizeIn(maxHeight = 0.dp)
                .wrapContentHeight(unbounded = true)
                .padding(bottom = 4.dp),
            transitionSpec = {
                fadeIn() + slideInVertically { it / 2 } togetherWith
                        fadeOut() + slideOutVertically { it / 2 }
            },
        ) { trainset ->
            trainset?.run { Text(name) }
        }
        ExposedDropdownMenuBox(
            expanded = isDropdownOpen,
            onExpandedChange = { isDropdownOpen = it },
        ) {
            IconWithBadge(
                badge = painterResource(
                    if (isDropdownOpen) R.drawable.ic_dropdown_up
                    else R.drawable.ic_dropdown
                ),
                sizeRatio = 1 / 2f,
                overlapRatio = 4 / 5f,
                modifier = Modifier
                    .size(72.dp + 20.dp)
                    .clip(MaterialTheme.shapes.large) // clip the ripple
                    .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable)
                    .clickable {} // Just to get the styling; click handled by .menuAnchor()
                    .testTag("trainset_selector")
            ) {
                Icon(
                    painterResource(AppIcons.Trainset(trainsetSelection)),
                    "Trainset: ${trainsetSelection ?: "none selected"}",
                    tint = if (isError)
                                MaterialTheme.colorScheme.error
                           else LocalContentColor.current
                )
            }
            ExposedDropdownMenu(
                expanded = isDropdownOpen,
                onDismissRequest = { isDropdownOpen = false },
                matchAnchorWidth = false,
            ) {
                for (option in Trainset.entries) {
                    DropdownMenuItem(
                        leadingIcon = {
                            Icon(
                                painterResource(AppIcons.Trainset(option)),
                                contentDescription = null,
                            )
                        },
                        text = { Text(option.name) },
                        onClick = {
                            onSelectTrainset(option)
                            isDropdownOpen = false
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun EditStops(
    viewModel: EditPassServiceStopsModel,
    modifier: Modifier = Modifier,
    padding: PaddingValues = PaddingValues.Zero,
    hapticFeedback: HapticFeedback? = LocalHapticFeedback.current,
) {
    val hPadding = padding.horizontalOnly()
    val vPadding = padding.verticalOnly()
    val stops = viewModel.stops

    var selectingStationForIndex by remember { mutableStateOf<Int?>(null) }
    var selectingTimeForIndex by remember { mutableStateOf<Pair<Int, StopPoint>?>(null) }

    val discreteGridControl = remember { DiscreteGridControl() }

    Column(modifier.padding(vPadding)) {
        ReorderableColumn(
            list = stops,
            onSettle = { iFrom, iTo -> viewModel.reorderStops(iFrom, iTo) },
            onMove = { hapticFeedback?.performHapticFeedback(SegmentFrequentTick) },
            modifier = Modifier.zIndex(1f),
        ) { i, stop, isDragging ->
            ElevatingReorderableItem(
                isDragging,
                contentPadding = hPadding,
                shape = MaterialTheme.shapes.small,
            ) {
                DiscreteGridRow(
                    discreteGridControl = discreteGridControl,
                    Modifier
                        .height(IntrinsicSize.Min)
                        .fillMaxWidth(),
                    gap = 2.dp,
                    verticalAlignment = Alignment.CenterVertically,
                    fillCellWidth = true,
                ) {
                    ReorderDragHandle(
                        hapticFeedback,
                        Modifier
                            .padding(horizontal = 4.dp)
                            .testTag("drag_handle_$i"),
                    )

                    FormField(
                        onClick = { selectingStationForIndex = i },
                        isError = !viewModel.stationValidity[i],
                        modifier = Modifier
                            .fill()
                            .testTag("stop_station_$i")
                    ) {

                        Text(
                            stop.getStation()?.name ?: "",
                            softWrap = false,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }

                    if (i == 0 && stop.arrival == null) Spacer(Modifier) else {
                        val enabled = i != 0
                        FormField(
                            enabled = enabled,
                            isError = enabled && !viewModel.arrivalTimeValidity[i],
                            onClick = {
                                selectingTimeForIndex = i to StopPoint.Arrival
                            },
                            modifier = Modifier
                                .alpha(if (enabled) 1f else .5f)
                                .testTag("stop_arrival_$i")
                        ) {
                            Text(
                                stop.arrival
                                    ?.let { AppStringFormats.Time(it) }
                                    ?: ""
                            )
                        }
                    }

                    if (i == stops.lastIndex && stop.departure == null) Spacer(Modifier) else {
                        val enabled = i != stops.lastIndex
                        FormField(
                            enabled = enabled,
                            isError = enabled && !viewModel.departureTimeValidity[i],
                            onClick = {
                                selectingTimeForIndex = i to StopPoint.Departure
                            },
                            modifier = Modifier
                                .alpha(if (enabled) 1f else .5f)
                                .testTag("stop_departure_$i")
                        ) {
                            Text(
                                stop.departure
                                    ?.let { AppStringFormats.Time(it) }
                                    ?: ""
                            )
                        }
                    }

                    IconButton(
                        onClick = { viewModel.removeStop(i) },
                        Modifier.size(
                            IconButtonDefaults.smallContainerSize(
                                IconButtonDefaults.IconButtonWidthOption.Narrow
                            )
                        )
                    ) {
                        Icon(
                            painterResource(R.drawable.ic_trash),
                            contentDescription = "Delete stop",
                        )
                    }
                }
            }

        }

        TextButton(
            onClick = { viewModel.addStop() },
            contentPadding = PaddingValues(horizontal = 4.dp),
        ) {
            Row(
                Modifier.fillMaxWidth(),
                Arrangement.spacedBy(2.dp),
                Alignment.CenterVertically,
            ) {
                Icon(
                    painterResource(R.drawable.ic_add),
                    contentDescription = "Add stop",
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    "Add stop",
                )
            }
        }
    }

    // Popup station search
    val textFieldState = rememberTextFieldState()
    val searchState = rememberSearchBarState(SearchBarValue.Expanded)
    selectingStationForIndex
        ?.also { i ->
            LaunchedEffect(i) {
                textFieldState.setTextAndSelectAll(stops[i].getStation()?.name ?: "")
            }
        }.let { i ->
            AnimatedVisibility(i != null) {
                ExpandedSearch<Station>(
                    textFieldState = textFieldState,
                    searchBarState = searchState,
                    onClose = { selectingStationForIndex = null },
                    onSelectResult = { newStation ->
                        if (newStation != null && i != null) {
                            viewModel.updateStation(i, newStation)
                        }
                    },
                    modifier = Modifier.animateEnterExit(),
                )
            }
        }

    // Popup time select TODO hide amenity badges at this time.
    selectingTimeForIndex?.let { (i, forPoint) ->

        val stop = stops[i]
        val hadPreviousValue =
            null != when (forPoint) {
                    StopPoint.Arrival -> stop.arrival
                    StopPoint.Departure -> stop.departure
                }
        var shiftFollowing by remember { mutableStateOf(hadPreviousValue) }

        PredictiveBackDialog(onDismissRequest = { selectingTimeForIndex = null }) {
            TimePickerWithExtras(
                initialTime = with (viewModel) { stop.suggestedTime(forPoint) }.toKotlinLocalTime(),
                onConfirm = {
                    viewModel.updateStopTime(i, forPoint, it, shiftFollowing)
                    selectingTimeForIndex = null
                },
                onDismiss = { selectingTimeForIndex = null },
                title =
                    "Edit ${
                        when (forPoint) {
                            StopPoint.Arrival ->   "arrival"
                            StopPoint.Departure -> "departure"
                        }
                    } time for ${
                        stop.getStation()?.name ?: "stop"
                    }",
                enableKeyboard = true,
            ) {
                if (hadPreviousValue) {
                    Checkbox(shiftFollowing, { shiftFollowing = it })
                    Text(
                        "Shift following times",
                        Modifier.clickable { shiftFollowing = !shiftFollowing }
                    )
                }
            }
        }
    }
}
