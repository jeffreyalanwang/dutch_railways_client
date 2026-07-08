package com.jeffreyalanwang.dutchrailwaysandroidclient.ui.viewmodel

import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.material3.SearchBarState
import androidx.compose.material3.SearchBarValue
import androidx.compose.foundation.text.input.clearText
import androidx.compose.foundation.text.input.setTextAndPlaceCursorAtEnd
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.gms.maps.model.LatLng
import com.jeffreyalanwang.dutchrailwaysandroidclient.Station
import com.jeffreyalanwang.dutchrailwaysandroidclient.backend.BackendApi
import com.jeffreyalanwang.dutchrailwaysandroidclient.ui.search.LocationResult
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

class LocationPickerModel(
    private val viewModelScope: CoroutineScope,
    initialGeom: LatLng,
    initialAddress: String,
) {
    var isExpanded by mutableStateOf(false)
        internal set

    var geom by mutableStateOf(initialGeom)
        private set
    var address by mutableStateOf(initialAddress)
        private set

    /** Null if loading. */
    var displayString by mutableStateOf<String?>(address)
        private set

    private var geocodeJob: Job? = null
    fun updateLocation(result: LocationResult?) {
        if (result == null) return

        geocodeJob?.cancel()
        geocodeJob = viewModelScope.launch {
            this@LocationPickerModel.displayString = null
            val latLng = result.latLng
            val address = result.getAddress() // this line may suspend
                ?: return@launch
            this@LocationPickerModel.geom = latLng
            this@LocationPickerModel.address = address
            this@LocationPickerModel.displayString = address
        }
    }
}

class EditStationViewModel(
    initialStationDetails: Station,
) : ViewModel() {

    constructor(stationId: Int)
        : this( BackendApi.get_station_info(stationId) )

    val stationId = initialStationDetails.id

    val initialName = initialStationDetails.name
    val nameFieldState = TextFieldState(initialName)
    val currentName get() = nameFieldState.text.toString()
    val isNameValid by derivedStateOf { nameFieldState.text.isNotEmpty() }

    val locationPickerDelegate = LocationPickerModel(viewModelScope, initialStationDetails.geom, initialStationDetails.address)
    val currGeom get() = locationPickerDelegate.geom
    val currAddress get() = locationPickerDelegate.address
    fun onExpandLocationPicker() {
        locationPickerDelegate.isExpanded = true
    }

    fun saveChanges(onSuccess: () -> Unit) {
        if (isNameValid) {
            BackendApi.edit_station(
                stationId,
                currentName,
                currAddress,
                currGeom,
            )
            onSuccess()
        }
    }
}
