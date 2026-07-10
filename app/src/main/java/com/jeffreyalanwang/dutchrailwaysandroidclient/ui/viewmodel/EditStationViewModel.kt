package com.jeffreyalanwang.dutchrailwaysandroidclient.ui.viewmodel

import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.gms.maps.model.LatLng
import com.jeffreyalanwang.dutchrailwaysandroidclient.Station
import backend.BackendApi
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
        }.apply {
            this.invokeOnCompletion {
                if (displayString == null) {
                    displayString = address // restore if cancelled
                }
            }
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

    /**
     * Odd return value allows us to mirror the behavior of [EditPassServiceViewModel].
     * @return `null` if data is invalid; otherwise, `Unit` (indicates saved successfully).
     * @see EditPassServiceViewModel.saveChanges
     */
    fun saveChanges(): Unit? {
        if (!isNameValid) return null

        BackendApi.edit_station(
            stationId,
            currentName,
            currAddress,
            currGeom,
        )
        return Unit
    }
}
