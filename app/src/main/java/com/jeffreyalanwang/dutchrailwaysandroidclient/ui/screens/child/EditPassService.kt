package com.jeffreyalanwang.dutchrailwaysandroidclient.ui.screens.child

import android.util.Log
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.plus
import androidx.compose.foundation.layout.safeContent
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.foundation.text.input.setTextAndSelectAll
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SearchBarValue
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberSearchBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.runtime.toMutableStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType.Companion.SegmentFrequentTick
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation3.runtime.result.LocalResultEventBus
import androidx.navigation3.runtime.result.ResultEffect
import androidx.navigation3.runtime.result.ResultEventBus
import com.jeffreyalanwang.dutchrailwaysandroidclient.BackendApi
import com.jeffreyalanwang.dutchrailwaysandroidclient.PassService
import com.jeffreyalanwang.dutchrailwaysandroidclient.R
import com.jeffreyalanwang.dutchrailwaysandroidclient.ServiceStop
import com.jeffreyalanwang.dutchrailwaysandroidclient.Station
import com.jeffreyalanwang.dutchrailwaysandroidclient.StopPoint
import com.jeffreyalanwang.dutchrailwaysandroidclient.TrainAmenity
import com.jeffreyalanwang.dutchrailwaysandroidclient.Trainset
import com.jeffreyalanwang.dutchrailwaysandroidclient.applyAt
import com.jeffreyalanwang.dutchrailwaysandroidclient.map
import com.jeffreyalanwang.dutchrailwaysandroidclient.toKotlinInstant
import com.jeffreyalanwang.dutchrailwaysandroidclient.toKotlinLocalDateTime
import com.jeffreyalanwang.dutchrailwaysandroidclient.toZonedDateTime
import com.jeffreyalanwang.dutchrailwaysandroidclient.ui.CommonChildNavArgs
import com.jeffreyalanwang.dutchrailwaysandroidclient.ui.PassServiceDetailNavArgs
import com.jeffreyalanwang.dutchrailwaysandroidclient.ui.TimePickerNavArgs
import com.jeffreyalanwang.dutchrailwaysandroidclient.ui.components.DiscreteGridControl
import com.jeffreyalanwang.dutchrailwaysandroidclient.ui.components.DiscreteGridRow
import com.jeffreyalanwang.dutchrailwaysandroidclient.ui.components.EditAmenityBadgeSet
import com.jeffreyalanwang.dutchrailwaysandroidclient.ui.components.ElevatingReorderableItem
import com.jeffreyalanwang.dutchrailwaysandroidclient.ui.components.FormField
import com.jeffreyalanwang.dutchrailwaysandroidclient.ui.components.IconWithBadge
import com.jeffreyalanwang.dutchrailwaysandroidclient.ui.components.ReorderDragHandle
import com.jeffreyalanwang.dutchrailwaysandroidclient.ui.search.ExpandedSearch
import com.jeffreyalanwang.dutchrailwaysandroidclient.ui.util.AppIcons
import com.jeffreyalanwang.dutchrailwaysandroidclient.ui.util.AppStringFormats
import com.jeffreyalanwang.dutchrailwaysandroidclient.ui.util.DialogResult
import com.jeffreyalanwang.dutchrailwaysandroidclient.ui.util.providesWindowInsets
import com.jeffreyalanwang.dutchrailwaysandroidclient.ui.util.verticalOnly
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalTime
import kotlinx.datetime.atDate
import sh.calvin.reorderable.ReorderableColumn
import java.time.ZonedDateTime
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Instant

@Preview(widthDp = 500, heightDp = 1000)
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

    CompositionLocalProvider(LocalResultEventBus provides remember { ResultEventBus() }) {
        EditPassServiceScreenBase(
            basedOnService = BackendApi.get_pass_service(serviceId),
            onNavigate = { notify("Navigate to: $it") },
            onFinished = { notify("Finished: $it") },
            onCancelled = { notify("Cancelled") },
        )
    }

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
    onNavigate: (CommonChildNavArgs) -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("New train service") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            painterResource(R.drawable.ic_back),
                            contentDescription = "Back"
                        )
                    }
                },
            )
        }
    ) { innerPadding ->
        EditPassServiceScreenBase(
            basedOnService = basedOnService,
            onNavigate = onNavigate,
            onFinished = {
                BackendApi.add_pass_service(
                    title = it.title,
                    trainset = it.trainset,
                    amenities = it.amenities,
                    stops = it.getStops(),
                )
                onNavigateBack()
                onNavigate(
                    PassServiceDetailNavArgs(it.id)
                )
            },
            onCancelled = onNavigateBack,
            modifier = Modifier.padding(innerPadding)
        )
    }
}

@Composable
fun EditPassServiceScreen(
    id: Int,
    onNavigateBack: () -> Unit,
    onNavigate: (CommonChildNavArgs) -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Edit train service") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            painterResource(R.drawable.ic_back),
                            contentDescription = "Back"
                        )
                    }
                },
            )
        },
    ) { innerPadding ->

        Box(Modifier.verticalScroll(rememberScrollState())) {
            Card(Modifier.padding(innerPadding + PaddingValues(10.dp))) {
                EditPassServiceScreenBase(
                    basedOnService = BackendApi.get_pass_service(id),
                    onNavigate = onNavigate,
                    onFinished = {
                        BackendApi.update_pass_service(
                            it.id,
                            trainset = it.trainset,
                            amenities = it.amenities,
                            stops = it.getStops(),
                        )
                        onNavigateBack()
                        onNavigate( PassServiceDetailNavArgs(it.id) )
                    },
                    onCancelled = onNavigateBack,
                    modifier = Modifier.padding(vertical = 20.dp)
                )
            }
        }

    }
}

@Composable
private fun EditPassServiceScreenBase(
    basedOnService: PassService?,
    modifier: Modifier = Modifier,
    onNavigate: (TimePickerNavArgs<StationTimeField>) -> Unit,
    onFinished: (PassService) -> Unit,
    onCancelled: () -> Unit,
) {
    var amenityBadgesBounds by WindowInsets.safeContent.let { remember { mutableStateOf(it) } }

    var trainsetSelection by rememberSaveable {
        mutableStateOf( basedOnService?.trainset )
    }
    var amenitiesMultiSelection by rememberSaveable {
        mutableStateOf(basedOnService?.amenities ?: emptySet<TrainAmenity>())
    }
    val stopsList = rememberSaveable {
        (basedOnService?.getStops() ?: emptyList())
            .toMutableStateList()
    }
    val title = AppStringFormats.passServiceTitle(
        oldTitle = basedOnService?.title,
        lastStop = stopsList.last(),
    )

    // We preserve the time zone currently used for the stop.
    // TODO handle pass services crossing midnight
    ResultEffect<DialogResult<LocalTime, StationTimeField>> { (resultTime, tag)  ->
        val (stationId, forPoint) = tag
        val (stopIndex, stop) = stopsList.withIndex().find { it.value.stationId == stationId }!!

        // Convert to a ZonedDateTime
        val date = stop.run { arrival ?: departure }!!.toKotlinLocalDateTime().date
        val resultZoned = resultTime
            .atDate(date)
            .toZonedDateTime(stop.timezone)

        stopsList[stopIndex] = when(forPoint) {
            StopPoint.Arrival -> stop.copy(arrival = resultZoned)
            StopPoint.Departure -> stop.copy(departure = resultZoned)
        }
    }

    Column(modifier
        .fillMaxWidth()
        .providesWindowInsets{ amenityBadgesBounds = it }
    ) {
        Row(verticalAlignment = Alignment.Bottom) {
            EditRollingStockIcon(
                trainsetSelection
            ) { trainsetSelection = it }

            var areAmenitiesExpanded by remember { mutableStateOf(false) }
            EditAmenityBadgeSet(
                amenitiesMultiSelection,
                containerModifier = Modifier
                    .offset(x=-25.dp, y=-7.5.dp),
                isExpanded = areAmenitiesExpanded,
                onSetExpanded = { areAmenitiesExpanded = it },
                onModify = { amenitiesMultiSelection = it },
                windowInsets = amenityBadgesBounds,
            )
        }

        // Name
        Text(
            title,
            style=MaterialTheme.typography.displaySmall,
            modifier=Modifier.padding(horizontal=10.dp)
        )

        Spacer(Modifier.height(10.dp))

        // Stops (arrive; depart; station)
        EditStops(
            stopsList,
            onEditTime = { stationId, forPoint, title, initial ->
                onNavigate(
                    TimePickerNavArgs(
                        title = title,
                        initialTime = initial.toKotlinLocalDateTime().time,
                        clearable = false,
                        tag = stationId to forPoint,
                    )
                )
            },
            padding = PaddingValues(horizontal=10.dp)
        )
    }
}

@Composable
private fun EditRollingStockIcon(
    trainsetSelection: Trainset? = null,
    onSelectTrainset: (Trainset) -> Unit,
) {
    var isDropdownOpen by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(
        expanded = isDropdownOpen,
        onExpandedChange = { isDropdownOpen = it },
    ) {
        IconWithBadge(
            badge = painterResource(
                if (isDropdownOpen) R.drawable.ic_dropdown_up
                else R.drawable.ic_dropdown
            ),
            sizeRatio = 1/2f,
            overlapRatio = 1/2f,
            modifier = Modifier
                .size(72.dp + 20.dp)
                .clip(MaterialTheme.shapes.large) // clip the ripple
                .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable)
                .clickable {} // Just to get the styling; click handled by .menuAnchor()
        ) {
            Icon(
                painterResource(AppIcons.Trainset(trainsetSelection)),
                "Trainset: ${trainsetSelection ?: "none selected"}",
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

typealias StationTimeField = Pair<Int, StopPoint>


@Composable
private fun EditStops(
    stops: SnapshotStateList<ServiceStop>,
    onEditTime: (
        stationId: Int,
        forPoint: StopPoint,
        title: String,
        initial: ZonedDateTime,
    ) -> Unit,
    modifier: Modifier = Modifier,
    padding: PaddingValues = PaddingValues.Zero,
    hapticFeedback: HapticFeedback? = LocalHapticFeedback.current,
) {
    var selectingStationForIndex by remember { mutableStateOf<Int?>(null) }

    // Intermediately, first + last stops are allowed to have times;
    // however, TODO these will be removed upon save.

    val discreteGridControl = remember { DiscreteGridControl() }

    ReorderableColumn(
        list = stops,
        onSettle = { iFrom, iTo ->
            // TODO this is terrible
            with (stops) {
                val item = removeAt(iFrom)

                // Change to respect our new neighbors + new position
                val newItem = item.withTimesWithin(
                    earliest = getOrNull(iTo - 1)
                        ?.run { departure!!.plusMinutes(1).toKotlinInstant() },
                    latest = getOrNull(iTo + 1)
                        ?.run { arrival!!.minusMinutes(1).toKotlinInstant() },
                )

                add(iTo, newItem)
            }
        },
        modifier = modifier.padding(padding.verticalOnly()),
        onMove = {
            hapticFeedback?.performHapticFeedback(SegmentFrequentTick)
        }
    ) { i, stop, isDragging ->

        // TODO remove arrival or departure field + clear it for the first or last
        // TODO find an ID that does not change no matter how we modify the stop
        ElevatingReorderableItem(isDragging, stop.stationId) {
            DiscreteGridRow(
                discreteGridControl = discreteGridControl,
                Modifier
                    .height(IntrinsicSize.Min)
                    .fillMaxWidth()
                    .padding(
                        end =
                            with(LocalLayoutDirection.current) {
                                padding.calculateEndPadding(this)
                            }
                    ),
                gap = 8.dp,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                ReorderDragHandle(
                    hapticFeedback,
                    Modifier
                        .padding(
                            horizontal = 8.dp,
                            vertical = 12.dp,
                        )
                        .sizeIn(
                            minWidth =
                                with(LocalLayoutDirection.current) {
                                    padding.calculateStartPadding(this)
                                }
                        ),
                )

                FormField(
                    onClick = { selectingStationForIndex = i },
                    isError = stops.count { it.stationId == stop.stationId } > 1,
                    modifier = Modifier.fill()
                ) {
                    Text(
                        stop.getStation().name,
                        softWrap = false,
                        overflow = TextOverflow.Ellipsis,
                    )
                }

                stop.arrival
                    ?.let {
                        val enabled = (i != 0)
                        FormField(
                            enabled = enabled,
                            isError = enabled && !checkStopTimeValidity(i, stops),
                            onClick = {
                                onEditTime(
                                    stop.stationId,
                                    StopPoint.Arrival,
                                    "Edit arrival time for ${stop.getStation().name}",
                                    it,
                                )
                            },
                            modifier = Modifier.alpha( if (enabled) 1f else .5f )
                        ) {
                            Text(AppStringFormats.Time(it))
                        }
                    }
                    ?: Spacer(Modifier)

                stop.departure
                    ?.let {
                        val enabled = (i != stops.lastIndex)
                        FormField(
                            enabled = enabled,
                            isError = enabled && !checkStopTimeValidity(i, stops),
                            onClick = {
                                onEditTime(
                                    stop.stationId,
                                    StopPoint.Departure,
                                    "Edit departure time for ${stop.getStation().name}",
                                    it,
                                )
                            },
                            modifier = Modifier.alpha( if (enabled) 1f else .5f )
                        ) {
                            Text(AppStringFormats.Time(it))
                        }
                    }
                    ?: Spacer(Modifier)
            }
        }

    }

    val textFieldState = rememberTextFieldState()
    val searchState = rememberSearchBarState(SearchBarValue.Expanded)
    selectingStationForIndex
        ?.also { i ->
            LaunchedEffect(i) {
                textFieldState.setTextAndSelectAll(stops[i].getStation().name)
            }
        }.let { i ->
            AnimatedVisibility(i != null) {
                ExpandedSearch<Station>(
                    textFieldState = textFieldState,
                    searchBarState = searchState,
                    onClose = { selectingStationForIndex = null },
                    onSelectResult = { newStation ->
                        if (newStation != null && i != null) {
                            stops.applyAt(i) { stop ->
                                stop.copy(stationId = newStation.id)
                            }
                        }
                    },
                    modifier = Modifier.animateEnterExit(),
                )
            }
        }
}

/**
 * Make up new stop times that fit within provided bounds.
 *
 * Even if this stop is the first or last, both [arrival]
 * and [departure] will be non-null value
 *
 * @param earliest
 *      The earliest allowed arrival time.
 *      Null value represents no stop before.
 * @param latest See [earliest].
 */
private fun ServiceStop.withTimesWithin(earliest: Instant?, latest: Instant?): ServiceStop {
    val oldTimes = arrival to departure
    val newTimes =
        generateStopTimes(
            oldTimes.map { it?.toKotlinInstant() },
            earliest to latest,
        )
        .map { it.toZonedDateTime(timezone) }

    return this.copy(
        arrival = newTimes.first,
        departure = newTimes.second,
    )
}

/** One stop => one station => one time zone */
private val ServiceStop.timezone
    get() = (arrival ?: departure)!!.zone

private fun generateStopTimes(
    oldTimes: Pair<Instant?, Instant?>,
    bounds: Pair<Instant?, Instant?>,
): Pair<Instant, Instant> {
    val (oldArrival, oldDeparture) = oldTimes
    val (startBound, endBound) = bounds

    // If both bounds are provided, center the stop between them
    if (startBound != null && endBound != null) {
        // New stop's duration should equal the old, if possible
        val duration =
            if (oldArrival != null && oldDeparture != null)
                oldDeparture - oldArrival
            else 1.minutes

        val newArrival = startBound +
                (endBound - startBound - duration) / 2
        val newDeparture = newArrival + duration

        return newArrival to newDeparture

    }

    // If one bound is null, use the other
    if (startBound != null) {
        return startBound to startBound + 1.minutes
    }
    if (endBound != null) {
        return endBound - 1.minutes to endBound
    }

    // If both bounds are null, use the old times
    if (oldArrival != null && oldDeparture != null) {
        return oldArrival to oldDeparture
    }
    if (oldArrival != null) {
        return oldArrival to oldArrival + 1.minutes
    }
    if (oldDeparture != null) {
        return oldDeparture - 1.minutes to oldDeparture
    }
    throw IllegalArgumentException()
}

private fun checkStopTimeValidity(index: Int, stops: List<ServiceStop>): Boolean {
    val stop = stops[index]
    val stopArrival = stop.arrival
    val stopDeparture = stop.departure

    val isFirst = index == 0
    val isLast = index == stops.lastIndex

    if (
        stopArrival != null && stopDeparture != null
        && stopArrival >= stopDeparture
    )
        return false

    Log.d("asdf", "a $index")

    if (isFirst) {
        if (stopArrival != null) return false
    } else {
        if (stopArrival == null) return false

        val closestTimeBefore = stops
            .take(index)
            .map { it.departure ?: it.arrival }
            .findLast { it != null }

        if (closestTimeBefore.let { it != null && it >= stopArrival })
            return false
    }

    Log.d("asdf", "b $index")

    if (isLast) {
        if (stopDeparture != null) return false
    } else {
        if (stopDeparture == null) return false

        val closestTimeAfter = stops
            .drop(index + 1)
            .map { it.arrival ?: it.departure }
            .findLast { it != null }

        if (closestTimeAfter.let { it != null && stopDeparture >= it })
            return false
    }

    Log.d("asdf", "c $index")

    return true
}