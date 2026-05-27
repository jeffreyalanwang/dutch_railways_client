@file:OptIn(ExperimentalTime::class)

package com.jeffreyalanwang.dutchrailwaysandroidclient.ui.viewmodel

import androidx.lifecycle.ViewModel
import com.jeffreyalanwang.dutchrailwaysandroidclient.BackendApi
import com.jeffreyalanwang.dutchrailwaysandroidclient.Place
import com.jeffreyalanwang.dutchrailwaysandroidclient.RoutePlan
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

data class RoutePlannerState (
    val origin: Place? = null,
    val destination: Place? = null,
    val departTime: Instant? = null,
    val arriveTime: Instant? = null,
    val routes: ImmutableList<RoutePlan>? = null,
)

class RoutePlannerViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(RoutePlannerState())
    val uiState: StateFlow<RoutePlannerState> = _uiState.asStateFlow()

    fun setEndpoints(
        origin: Place?,
        destination: Place?,
        departTime: Instant? = null,
        arriveTime: Instant? = null,
    ) {
        _uiState.update { currentState ->
            currentState.copy(
                origin = origin,
                destination = destination,
                departTime = departTime,
                arriveTime = arriveTime,
                routes = null,
            )
        }
    }

    private fun setRoutes(routes: List<RoutePlan>) {
        _uiState.update { currentState ->
            currentState.copy(
                routes = routes.toImmutableList(),
            )
        }
    }

    fun loadRoutes() {
        with(uiState.value) {
            check(origin != null)
            check(destination != null)
            setRoutes(
                BackendApi.get_routes(
                    origin,
                    destination,
                    departTime,
                    arriveTime,
                ).take(10).toList()
            )
        }


    }
}