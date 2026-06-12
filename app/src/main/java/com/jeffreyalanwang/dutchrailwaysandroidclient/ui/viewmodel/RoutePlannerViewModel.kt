@file:OptIn(ExperimentalTime::class)

package com.jeffreyalanwang.dutchrailwaysandroidclient.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jeffreyalanwang.dutchrailwaysandroidclient.Area
import com.jeffreyalanwang.dutchrailwaysandroidclient.BackendApi
import com.jeffreyalanwang.dutchrailwaysandroidclient.Place
import com.jeffreyalanwang.dutchrailwaysandroidclient.RoutePlan
import com.jeffreyalanwang.dutchrailwaysandroidclient.Station
import com.jeffreyalanwang.dutchrailwaysandroidclient.ui.AreaDetailRoute
import com.jeffreyalanwang.dutchrailwaysandroidclient.ui.RouteOptionsRoute
import com.jeffreyalanwang.dutchrailwaysandroidclient.ui.StationDetailRoute
import com.jeffreyalanwang.dutchrailwaysandroidclient.ui.TrainQueryGraphChildRoute
import com.jeffreyalanwang.dutchrailwaysandroidclient.ui.TrainQueryGraphMajorRoute
import com.jeffreyalanwang.dutchrailwaysandroidclient.ui.TrainQuerySelectionRoute
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

enum class Endpoint { Origin, Destination }
enum class RoutePlannerStage { QueryBuilding, RoutesFound }

data class RoutePlannerState(
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
            lastSet = newOrigin?.let { Endpoint.Origin },
        )

    fun copyWithDestination(newDestination: Place?)
        = copyWithQueryParams(
            destination = newDestination,
            arriveTime = null,
            lastSet = newDestination?.let { Endpoint.Destination },
        )
}

class RoutePlannerViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(RoutePlannerState())
    val uiState: StateFlow<RoutePlannerState> = _uiState.asStateFlow()

    val navMajorRouteState: StateFlow<TrainQueryGraphMajorRoute> = uiState
        .map {
            if (it.routes == null) TrainQuerySelectionRoute
            else RouteOptionsRoute
        }
        .stateIn(
            viewModelScope,
            SharingStarted.Lazily,
            initialValue = TrainQuerySelectionRoute
        )

    /**
     * Requests zero or one minor routes to be pushed directly on top of
     * the value of [navMajorRouteState].
     */
    val navMinorRouteState: StateFlow<TrainQueryGraphChildRoute?> =
        uiState.map {
            if (it.routes != null) null
            else when (val place = it.lastSetPlace) {
                is Station -> StationDetailRoute(place.id)
                is Area -> AreaDetailRoute(place.id)
                null -> null

                else -> throw IllegalStateException()
            }
        }
        .stateIn(
            viewModelScope,
            SharingStarted.Lazily,
            initialValue = null
        )

    // [setOrigin()] and [setDestination()] need to be separate methods
    // so that we can implicitly set [RoutePlannerState.lastSet].

    fun setOrigin(origin: Place?) {
        if (origin != uiState.value.origin) {
            _uiState.update { it.copyWithOrigin(origin) }
        }
    }

    fun setDestination(destination: Place?) {
        if (destination != uiState.value.destination) {
            _uiState.update { it.copyWithDestination(destination) }
        }
    }

    fun setTimeConstraints(
        departTime: Instant? = uiState.value.departTime,
        arriveTime: Instant? = uiState.value.arriveTime,
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

    fun loadRoutes() {
        val routes =
            with(uiState.value) {
                BackendApi.get_routes(origin!!, destination!!, departTime, arriveTime)
            }
            .take(10)
            .toImmutableList()
        _uiState.update { it.copy(routes = routes) }
    }

    /**
     * Returns [false] instead of popping the eldest major ancestor route.
     */
    fun onPopMajorRoute(): Boolean {
        when (navMajorRouteState.value) {
            is TrainQuerySelectionRoute -> return false
            is RouteOptionsRoute -> {
                _uiState.update { it.copy(routes = null) }
                return true
            }

            else -> throw IllegalArgumentException()
        }
    }
}