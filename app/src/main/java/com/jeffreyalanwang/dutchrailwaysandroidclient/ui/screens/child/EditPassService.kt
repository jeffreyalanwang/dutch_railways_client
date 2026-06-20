package com.jeffreyalanwang.dutchrailwaysandroidclient.ui.screens.child

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.plus
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
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
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType.Companion.GestureEnd
import androidx.compose.ui.hapticfeedback.HapticFeedbackType.Companion.GestureThresholdActivate
import androidx.compose.ui.hapticfeedback.HapticFeedbackType.Companion.SegmentFrequentTick
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.jeffreyalanwang.dutchrailwaysandroidclient.BackendApi
import com.jeffreyalanwang.dutchrailwaysandroidclient.PassService
import com.jeffreyalanwang.dutchrailwaysandroidclient.R
import com.jeffreyalanwang.dutchrailwaysandroidclient.ServiceStop
import com.jeffreyalanwang.dutchrailwaysandroidclient.Trainset
import com.jeffreyalanwang.dutchrailwaysandroidclient.map
import com.jeffreyalanwang.dutchrailwaysandroidclient.toKotlinInstant
import com.jeffreyalanwang.dutchrailwaysandroidclient.toZonedDateTime
import com.jeffreyalanwang.dutchrailwaysandroidclient.ui.PassServiceDetailNavArgs
import com.jeffreyalanwang.dutchrailwaysandroidclient.ui.components.DiscreteGridControl
import com.jeffreyalanwang.dutchrailwaysandroidclient.ui.components.DiscreteGridRow
import com.jeffreyalanwang.dutchrailwaysandroidclient.ui.components.IconWithBadge
import com.jeffreyalanwang.dutchrailwaysandroidclient.ui.util.AppIcons
import com.jeffreyalanwang.dutchrailwaysandroidclient.ui.util.AppStringFormats
import com.jeffreyalanwang.dutchrailwaysandroidclient.ui.util.toMutableStateSet
import com.jeffreyalanwang.dutchrailwaysandroidclient.ui.util.verticalOnly
import kotlinx.coroutines.launch
import sh.calvin.reorderable.ReorderableColumn
import sh.calvin.reorderable.ReorderableListItemScope
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

    EditPassServiceScreenBase(
        basedOnService = BackendApi.get_pass_service(serviceId),
        onFinished = { notify("Finished: $it") },
        onCancelled =  { notify("Cancelled") },
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
    onNavigate: (PassServiceDetailNavArgs) -> Unit,
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
    onFinished: (PassService) -> Unit,
    onCancelled: () -> Unit,
) {
    var trainsetSelection by rememberSaveable {
        mutableStateOf( basedOnService?.trainset )
    }
    val amenitiesMultiSelection = rememberSaveable {
        (basedOnService?.amenities ?: emptySet())
            .toMutableStateSet()
    }
    val stopsList = rememberSaveable {
        (basedOnService?.getStops() ?: emptyList())
            .toMutableStateList()
    }
    val title = AppStringFormats.passServiceTitle(
        oldTitle = basedOnService?.title,
        lastStop = stopsList.last(),
    )

    Column(modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.Bottom) {
            EditRollingStockIcon(
                trainsetSelection
            ) { trainsetSelection = it }

            Text(
                "No amenities",
                fontStyle = FontStyle.Italic,
                modifier = Modifier.alpha(0.5f)
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

@Composable
private fun EditStops(
    stops: SnapshotStateList<ServiceStop>,
    modifier: Modifier = Modifier,
    padding: PaddingValues = PaddingValues.Zero,
    hapticFeedback: HapticFeedback? = LocalHapticFeedback.current,
) {

    // Intermediately, first + last stops are allowed to have times;
    // however, TODO these will be removed upon save.

    val discreteGridControl = remember { DiscreteGridControl() }

    ReorderableColumn(
        list = stops,
        onSettle = { iFrom, iTo ->
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
        key(stop.stationId) {
            ReorderableItem {
                val elevation by animateDpAsState(if (isDragging) 4.dp else 0.dp)

                Surface(shadowElevation = elevation) {
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
//                        horizontalArrangement = Arrangement.spacedBy(8.dp), TODO
//                        verticalAlignment = Alignment.CenterVertically
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

                        Text(
                            stop.getStation().name,
                            Modifier.fill(),
                        )

                        stop.arrival
                            ?.let {
                                Text(
                                    AppStringFormats.Time(it),
                                    Modifier.alpha(
                                        if (i == 0) .5f else 1f
                                    ),
                                )
                            }
                            ?: Spacer(Modifier)

                        stop.departure
                            ?.let {
                                Text(
                                    AppStringFormats.Time(it),
                                    Modifier.alpha(
                                        if (i == stops.lastIndex) .5f else 1f
                                    ),
                                )
                            }
                            ?: Spacer(Modifier)
                    }
                }
            }
        }

    }
}

@Composable
fun ReorderableListItemScope.ReorderDragHandle(
    hapticFeedback: HapticFeedback?,
    modifier: Modifier = Modifier
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .draggableHandle(
                onDragStarted = {
                    hapticFeedback?.performHapticFeedback(
                        GestureThresholdActivate
                    )
                },
                onDragStopped = {
                    hapticFeedback?.performHapticFeedback(
                        GestureEnd
                    )
                },
            )
            .fillMaxHeight()
    ) {
        Icon(
            painterResource(R.drawable.ic_draghandle_vertical),
            contentDescription = "Reorder",
        )
    }
}

/**
 * Make up new stop times that fit within provided bounds.
 *
 * Even if this stop is the first or last, both [arrival]
 * and [departure] will be non-null values.
 *
 * @param earliest
 *      The earliest allowed arrival time.
 *      Null value represents no stop before.
 * @param latest See [earliest].
 */
private fun ServiceStop.withTimesWithin(earliest: Instant?, latest: Instant?): ServiceStop {
    val timezone = (arrival ?: departure)!!.zone // One stop => one station => one time zone
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