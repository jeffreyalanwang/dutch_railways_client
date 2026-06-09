@file:OptIn(ExperimentalTime::class)

package com.jeffreyalanwang.dutchrailwaysandroidclient.ui.viewmodel

import androidx.lifecycle.ViewModel
import com.jeffreyalanwang.dutchrailwaysandroidclient.BackendApi
import com.jeffreyalanwang.dutchrailwaysandroidclient.Place
import com.jeffreyalanwang.dutchrailwaysandroidclient.RoutePlan
import com.jeffreyalanwang.dutchrailwaysandroidclient.calculateBounds
import com.jeffreyalanwang.dutchrailwaysandroidclient.getMapCameraUpdate
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

enum class Endpoint { Origin, Destination }
enum class RoutePlannerStage { NoneSelected, HasAnySelected, ListRouteOptions }

data class RoutePlannerState(
    val origin: Place? = null,
    val destination: Place? = null,
    val departTime: Instant? = null,
    val arriveTime: Instant? = null,

    /**
     * Used to display the most recently selected endpoint [Place].
     */
    val lastSet: Endpoint? = null,

    val routes: ImmutableList<RoutePlan>? = null,
) {
    val lastSetEndpoint = when (lastSet) {
        Endpoint.Origin -> origin
        Endpoint.Destination -> destination
        null -> null
    }

    val queryAllowed =
        (origin != null) && (destination != null) && (routes == null)

    val uiStage =
        if (routes != null)
            RoutePlannerStage.ListRouteOptions
        else if (lastSet != null)
            RoutePlannerStage.HasAnySelected
        else
            RoutePlannerStage.NoneSelected

    @get:Throws(NullPointerException::class)
    val mapCameraPosition by lazy {
        when (uiStage) {
            RoutePlannerStage.HasAnySelected
                -> lastSetEndpoint!!
                .getMapCameraUpdate()

            RoutePlannerStage.ListRouteOptions
                -> listOf(origin!!, destination!!)
                .calculateBounds()
                .getMapCameraUpdate(400)

            RoutePlannerStage.NoneSelected
                -> BackendApi.get_nl_area()
                .getMapCameraUpdate()
        }
    }

    fun copyWithQueryParams(
        origin: Place? = this.origin,
        destination: Place? = this.destination,
        departTime: Instant? = this.departTime,
        arriveTime: Instant? = this.arriveTime,
        lastSet: Endpoint? = this.lastSet,
    ) = copy(
        origin = origin,
        destination = destination,
        departTime = departTime,
        arriveTime = arriveTime,
        lastSet = lastSet,
        routes = null,
    )

    fun copyWithOrigin(newOrigin: Place?)
        = copyWithQueryParams(
            origin = newOrigin,
            departTime = null,
            lastSet =
                if (newOrigin != null) Endpoint.Origin
                else if (destination != null) Endpoint.Destination
                else null,
        )

    fun copyWithDestination(newDestination: Place?)
        = copyWithQueryParams(
            destination = newDestination,
            arriveTime = null,
            lastSet =
                if (newDestination != null) Endpoint.Destination
                else if (origin != null) Endpoint.Origin
                else null,
        )
}

class RoutePlannerViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(RoutePlannerState())
    val uiState: StateFlow<RoutePlannerState> = _uiState.asStateFlow()

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
        val routes = with(uiState.value) {
            BackendApi.get_routes(origin!!, destination!!, departTime, arriveTime)
        }.take(10).toImmutableList()
        _uiState.update { it.copy(routes = routes) }
    }
}