package com.jeffreyalanwang.dutchrailwaysandroidclient.ui.viewmodel

import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.material3.SearchBarState
import androidx.compose.material3.SearchBarValue
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.foundation.text.input.clearText
import androidx.compose.foundation.text.input.setTextAndPlaceCursorAtEnd
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.gms.maps.model.LatLng
import com.jeffreyalanwang.dutchrailwaysandroidclient.backend.BackendApi
import com.jeffreyalanwang.dutchrailwaysandroidclient.ui.search.AddressResult
import com.jeffreyalanwang.dutchrailwaysandroidclient.ui.search.LocationResult
import com.jeffreyalanwang.dutchrailwaysandroidclient.ui.screens.child.StationLocation
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

class EditStationViewModel(
    val stationId: Int
) : ViewModel() {
    private val station = BackendApi.get_station_info(stationId)

    val nameState = TextFieldState(station.name)
    val isNameValid by derivedStateOf { nameState.text.isNotEmpty() }

    var location: StationLocation by mutableStateOf(station.geom to station.address)
        private set

    var isLocationPickerExpanded by mutableStateOf(false)
        private set

    val searchTextFieldState = TextFieldState()

    private var geocodeJob: Job? = null

    fun updateLocation(result: LocationResult?, searchBarState: SearchBarState) {
        if (result == null) return

        val isSearchBarExpanding = searchBarState.targetValue != SearchBarValue.Collapsed
        if (isSearchBarExpanding) return

        geocodeJob?.cancel()
        geocodeJob = viewModelScope.launch {
            searchTextFieldState.setTextAndPlaceCursorAtEnd("Loading...")
            val latLng = result.latLng
            val address = result.getAddress()
            if (address != null) {
                searchTextFieldState.setTextAndPlaceCursorAtEnd(address)
                location = latLng to address
            } else {
                searchTextFieldState.clearText()
            }
        }
    }

    fun saveChanges(onSuccess: () -> Unit) {
        if (isNameValid) {
            BackendApi.edit_station(
                stationId,
                nameState.text.toString(),
                location.second,
                location.first
            )
            onSuccess()
        }
    }

    fun updateLocationPickerExpanded(expanded: Boolean) {
        isLocationPickerExpanded = expanded
    }
}
