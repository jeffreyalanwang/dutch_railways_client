@file:OptIn(ExperimentalTime::class)

package com.jeffreyalanwang.dutchrailwaysandroidclient.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jeffreyalanwang.dutchrailwaysandroidclient.Area
import com.jeffreyalanwang.dutchrailwaysandroidclient.BackendApi
import com.jeffreyalanwang.dutchrailwaysandroidclient.Place
import com.jeffreyalanwang.dutchrailwaysandroidclient.RoutePlan
import com.jeffreyalanwang.dutchrailwaysandroidclient.Station
import com.jeffreyalanwang.dutchrailwaysandroidclient.removeLast
import com.jeffreyalanwang.dutchrailwaysandroidclient.ui.AreaDetailRoute
import com.jeffreyalanwang.dutchrailwaysandroidclient.ui.RouteOptionsRoute
import com.jeffreyalanwang.dutchrailwaysandroidclient.ui.StationDetailRoute
import com.jeffreyalanwang.dutchrailwaysandroidclient.ui.TrainQueryGraphChildRoute
import com.jeffreyalanwang.dutchrailwaysandroidclient.ui.TrainQueryGraphMajorRoute
import com.jeffreyalanwang.dutchrailwaysandroidclient.ui.TrainQueryGraphRoute
import com.jeffreyalanwang.dutchrailwaysandroidclient.ui.TrainQuerySelectionRoute
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

enum class Endpoint { Origin, Destination }


data class DataState(
    val origin: Place? = null,
    val destination: Place? = null,
    val departTime: Instant? = null,
    val arriveTime: Instant? = null,

    /**
     * Used to display the most recently selected endpoint [Place].
     */
    val lastSetEndpoint: Endpoint? = null,

    val routes: ImmutableList<RoutePlan>? = null,
) {
    val lastSetPlace =
        when (lastSetEndpoint) {
            Endpoint.Origin -> origin
            Endpoint.Destination -> destination
            null -> null
        }
    val canSubmitQuery =
        (origin != null) && (destination != null) && (routes == null)

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
        routes = null,
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

interface RoutePlannerDataModel {
    val uiState: StateFlow<DataState>
    fun setOrigin(origin: Place?)
    fun setDestination(destination: Place?)
    fun setTimeConstraints(
        departTime: Instant? = uiState.value.departTime,
        arriveTime: Instant? = uiState.value.arriveTime,
    )
    fun loadRoutes()
    fun clearRoutes()
}

private class DataModelDelegate: RoutePlannerDataModel {

    private val _uiState = MutableStateFlow(DataState())
    override val uiState = _uiState.asStateFlow()

    // [setOrigin()] and [setDestination()] need to be separate methods
    // so that we can implicitly set [RoutePlannerState.lastSet].

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

    override fun loadRoutes() {
        val routes =
            with(uiState.value) {
                BackendApi.get_routes(origin!!, destination!!, departTime, arriveTime)
            }
            .take(10)
            .toImmutableList()
        _uiState.update { it.copy(routes = routes) }
    }

    override fun clearRoutes() {
        _uiState.update { it.copy(routes = null) }
    }
}

private typealias GraphRoute = TrainQueryGraphRoute
private typealias ChildGraphRoute = TrainQueryGraphChildRoute
private typealias MajorGraphRoute = TrainQueryGraphMajorRoute
private typealias BackStack = ImmutableList<GraphRoute>

interface RoutePlannerNavModel {
    val backStack: StateFlow<BackStack>
    fun pushUserRequested(route: GraphRoute)
    fun popBack(): Boolean
}

private class NavModelDelegate(
    stateRequestedRoutes: Flow<Pair<MajorGraphRoute, ChildGraphRoute?>>,
    val onPopMajorRoute: (MajorGraphRoute) -> Boolean,
): RoutePlannerNavModel {
    // Major nav routes are controlled by the ViewModel and maintain a set linear order.
    //      No minor routes exist between two major routes on the back stack.
    // Minor nav routes are all others in the graph; they are ephemeral w.r.t. the most recent major route.
    // (Note: Minor routes are always child routes, but not to be confused with [TrainQueryGraphChildRoute].)

    // The ViewModel can directly trigger navigation to/back from major routes.
    //      It can directly trigger navigation to a minor route, but only one, directly above a major route.
    // The composable itself can directly trigger navigation to minor routes.
    //      It can directly trigger navigation back from a major/minor route. (In practice we do not use this.)
    // The back key can trigger navigation back from major and minor routes.

    private val _backStack =
        MutableStateFlow( persistentListOf<GraphRoute>() )

    override val backStack =
        _backStack.asStateFlow()

    val toLaunchInViewModelScope =
        stateRequestedRoutes
        .onEach { (newMajor, newMinor) ->
            _backStack.update { value ->
                value.builder().apply {
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

    override fun pushUserRequested(route: GraphRoute)
        = _backStack.update { it.add(route) }

    override fun popBack(): Boolean {
        val toPop = _backStack.value.last()
        if (toPop is MajorGraphRoute) {
            return onPopMajorRoute(toPop)
        } else {
            _backStack.update { it.removeLast() }
            return true
        }
        // TODO we fail to handle popping the state-requested minor route of
        //  [PlaceDetailRoute]; it would really be a major route.
        //  find a way to rebuild, fixing this and without the
        //  notion of a minor/major route
    }
}

class RoutePlannerViewModel private constructor(
    private val dataModelDelegate: DataModelDelegate,
    private val navModelDelegate: NavModelDelegate,
) : RoutePlannerDataModel by dataModelDelegate,
    RoutePlannerNavModel by navModelDelegate,
    ViewModel()
{
    private constructor(dataModelDelegate: DataModelDelegate): this(
        dataModelDelegate,
        NavModelDelegate(
            stateRequestedRoutes = dataModelDelegate.uiState
                .map {
                    if (it.routes == null) {
                        TrainQuerySelectionRoute to
                            when (val place = it.lastSetPlace) {
                                is Station -> StationDetailRoute(place.id)
                                is Area -> AreaDetailRoute(place.id)
                                null -> null

                                else -> throw IllegalStateException()
                            }
                    }
                    else {
                        RouteOptionsRoute to
                                null
                    }
                },
            onPopMajorRoute = {
                when (it) {
                    is TrainQuerySelectionRoute -> {
                        false
                    }
                    is RouteOptionsRoute -> {
                        dataModelDelegate.clearRoutes()
                        true
                    }

                    else -> throw IllegalArgumentException()
                }
            }
        ),
    )

    init { navModelDelegate.toLaunchInViewModelScope.launchIn(viewModelScope) }

    constructor(): this(DataModelDelegate())
}