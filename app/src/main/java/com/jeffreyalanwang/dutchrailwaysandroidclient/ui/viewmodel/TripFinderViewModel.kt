package com.jeffreyalanwang.dutchrailwaysandroidclient.ui.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jeffreyalanwang.dutchrailwaysandroidclient.Area
import com.jeffreyalanwang.dutchrailwaysandroidclient.backend.BackendApi
import com.jeffreyalanwang.dutchrailwaysandroidclient.Endpoint
import com.jeffreyalanwang.dutchrailwaysandroidclient.Journey
import com.jeffreyalanwang.dutchrailwaysandroidclient.Place
import com.jeffreyalanwang.dutchrailwaysandroidclient.Station
import com.jeffreyalanwang.dutchrailwaysandroidclient.removeLast
import com.jeffreyalanwang.dutchrailwaysandroidclient.ui.AreaDetailNavArgs
import com.jeffreyalanwang.dutchrailwaysandroidclient.ui.JourneyListNavArgs
import com.jeffreyalanwang.dutchrailwaysandroidclient.ui.StationDetailNavArgs
import com.jeffreyalanwang.dutchrailwaysandroidclient.ui.TripFinderGraphMajorNavArgs
import com.jeffreyalanwang.dutchrailwaysandroidclient.ui.TripFinderGraphNavArgs
import com.jeffreyalanwang.dutchrailwaysandroidclient.ui.TripFinderStartNavArgs
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.time.Clock
import kotlin.time.Instant

data class DataState(
    val origin: Place? = null,
    val destination: Place? = null,
    val departTime: Instant? = null,
    val arriveTime: Instant? = null,

    /**
     * Used to display the most recently selected endpoint [Place].
     */
    val lastSetEndpoint: Endpoint? = null,

    val journeys: ImmutableList<Journey>? = null,
) {
    val lastSetPlace =
        when (lastSetEndpoint) {
            Endpoint.Origin -> origin
            Endpoint.Destination -> destination
            null -> null
        }
    val canSubmitQuery =
        (origin != null) && (destination != null) && (journeys == null)

    fun copyWithQueryParams(
        origin: Place? = this.origin,
        destination: Place? = this.destination,
        departTime: Instant? = this.departTime,
        arriveTime: Instant? = this.arriveTime,
        lastSet: Endpoint? = this.lastSetEndpoint,
    ) = copy(
        origin = origin,
        destination = destination,
        departTime = departTime,
        arriveTime = arriveTime,
        lastSetEndpoint = lastSet,
        journeys = null,
    )

    fun copyWithOrigin(newOrigin: Place?)
        = copyWithQueryParams(
            origin = newOrigin,
            departTime = null,
            lastSet = if (newOrigin == null) null else Endpoint.Origin,
        )

    fun copyWithDestination(newDestination: Place?)
        = copyWithQueryParams(
            destination = newDestination,
            arriveTime = null,
            lastSet = if (newDestination == null) null else Endpoint.Destination ,
        )
}

interface TripFinderDataModel {
    val uiState: StateFlow<DataState>
    fun setOrigin(origin: Place?)
    fun setDestination(destination: Place?)
    fun setTimeConstraints(
        departTime: Instant? = uiState.value.departTime,
        arriveTime: Instant? = uiState.value.arriveTime,
    )
    fun loadJourneys()
    fun clearJourneys()
}

private class DataModelDelegate: TripFinderDataModel {

    private val _uiState = MutableStateFlow(DataState(
        departTime = Clock.System.now(),
    ))
    override val uiState = _uiState.asStateFlow()

    // [setOrigin()] and [setDestination()] need to be separate methods
    // so that we can implicitly set [DataState.lastSet].

    override fun setOrigin(origin: Place?) {
        if (origin != uiState.value.origin) {
            _uiState.update { it.copyWithOrigin(origin) }
        }
    }

    override fun setDestination(destination: Place?) {
        if (destination != uiState.value.destination) {
            _uiState.update { it.copyWithDestination(destination) }
        }
    }

    override fun setTimeConstraints(
        departTime: Instant?,
        arriveTime: Instant?,
    ) {
        if (departTime != uiState.value.departTime || arriveTime != uiState.value.arriveTime) {
            _uiState.update {
                it.copyWithQueryParams(
                    departTime = departTime,
                    arriveTime = arriveTime,
                )
            }
        }
    }

    override fun loadJourneys() {
        val journeys =
            with(uiState.value) {
                BackendApi.get_journeys(origin!!, destination!!, departTime, arriveTime)
            }
            .take(10)
            .toImmutableList()
        _uiState.update { it.copy(journeys = journeys) }
    }

    override fun clearJourneys() {
        _uiState.update { it.copy(journeys = null) }
    }
}

private typealias GraphNavArgs = TripFinderGraphNavArgs
private typealias MajorGraphNavArgs = TripFinderGraphMajorNavArgs
private typealias BackStack = ImmutableList<GraphNavArgs>

interface TripFinderNavModel {
    val backStack: StateFlow<BackStack>
    fun pushUserRequested(navArgs: GraphNavArgs)
    fun popBack(): Boolean
}

private class NavModelDelegate(
    stateRequestedRoutes: Flow<Pair<MajorGraphNavArgs, GraphNavArgs?>>,
    val onPopMajorRoute: (MajorGraphNavArgs) -> Boolean,
): TripFinderNavModel {
    // Major nav routes are controlled by the ViewModel and maintain a set linear order.
    //      No minor routes exist between two major routes on the back stack.
    // Minor nav routes are all others in the graph; they are ephemeral w.r.t. the most recent major route.
    // (Note: Minor routes are always child routes, but not to be confused with [TripFinderGraphChildRoute].)

    // The ViewModel can directly trigger navigation to/back from major routes.
    //      It can directly trigger navigation to a minor route, but only one,
    //      directly above a major route; even if it has one indicated,
    //      the user may have removed it by triggering [popBack].
    // The composable itself can directly trigger navigation to minor routes.
    //      It can directly trigger navigation back from a major/minor route.
    //      (In practice we do not use this.)
    // The back key can trigger navigation back from major and minor routes.

    private val _backStack =
        MutableStateFlow( persistentListOf<GraphNavArgs>() )

    override val backStack =
        _backStack.asStateFlow()

    val toLaunchInViewModelScope: suspend () -> Unit = {
        stateRequestedRoutes
        .collect { (newMajor, newMinor) ->
            _backStack.update { value ->
                value.builder()
                .apply {
                    val newMajorIndex = indexOf(newMajor)
                    val newMinorIndex = newMinor?.let { indexOf(it) }

                    if (newMajorIndex < 0) {
                        add(newMajor)
                        newMinor?.let { add(it) }
                    } else if (newMinorIndex == null || newMinorIndex < newMajorIndex) {
                        while (size != newMajorIndex + 1) removeLast()
                        newMinor?.let { add(it) }
                    } else {
                        while (newMinorIndex != size - 1) removeLast()
                    }
                }.build()
            }
        }
    }

    override fun pushUserRequested(navArgs: GraphNavArgs)
        = _backStack.update { it.add(navArgs) }

    override fun popBack(): Boolean {
        val toPop = _backStack.value.last()
        if (toPop is MajorGraphNavArgs) {
            return onPopMajorRoute(toPop)
        } else {
            _backStack.update { it.removeLast() }
            return true
        }
    }
}

class TripFinderViewModel private constructor(
    private val dataModelDelegate: DataModelDelegate,
    private val navModelDelegate: NavModelDelegate,
) : TripFinderDataModel by dataModelDelegate,
    TripFinderNavModel by navModelDelegate,
    ViewModel()
{
    var initialZoomExecuted: Boolean by mutableStateOf(false)
        private set
    fun setInitialZoomExecuted() { initialZoomExecuted = true }

    private constructor(dataModelDelegate: DataModelDelegate): this(
        dataModelDelegate,
        NavModelDelegate(
            stateRequestedRoutes = dataModelDelegate.uiState
                .map {
                    if (it.journeys == null) {
                        TripFinderStartNavArgs to
                            when (val place = it.lastSetPlace) {
                                is Station -> StationDetailNavArgs(place.id)
                                is Area -> AreaDetailNavArgs(place.id)
                                null -> null

                                else -> throw IllegalStateException()
                            }
                    }
                    else {
                        JourneyListNavArgs to
                                null
                    }
                },
            onPopMajorRoute = {
                when (it) {
                    is TripFinderStartNavArgs -> {
                        false
                    }
                    is JourneyListNavArgs -> {
                        dataModelDelegate.clearJourneys()
                        true
                    }

                    else -> throw IllegalArgumentException()
                }
            }
        ),
    )

    init { viewModelScope.launch { navModelDelegate.toLaunchInViewModelScope() } }

    constructor(): this(DataModelDelegate())
}