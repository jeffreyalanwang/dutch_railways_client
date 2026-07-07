package com.jeffreyalanwang.dutchrailwaysandroidclient.ui.viewmodel

import androidx.compose.material3.SearchBarState
import androidx.compose.material3.SearchBarValue
import com.google.android.gms.maps.model.LatLng
import com.jeffreyalanwang.dutchrailwaysandroidclient.Station
import com.jeffreyalanwang.dutchrailwaysandroidclient.backend.BackendApi
import com.jeffreyalanwang.dutchrailwaysandroidclient.ui.search.LocationResult
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.verify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class EditStationViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        mockkObject(BackendApi)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `initialization loads station data`() {
        val station = Station(1, "Test Station", "Test Address", LatLng(1.0, 2.0))
        every { BackendApi.get_station_info(1) } returns station

        val viewModel = EditStationViewModel(1)

        assertEquals("Test Station", viewModel.nameState.text.toString())
        assertEquals(LatLng(1.0, 2.0), viewModel.location.first)
        assertEquals("Test Address", viewModel.location.second)
    }

    @Test
    fun `isNameValid updates correctly`() {
        val station = Station(1, "S", "A", LatLng(0.0, 0.0))
        every { BackendApi.get_station_info(1) } returns station

        val viewModel = EditStationViewModel(1)

        assertTrue(viewModel.isNameValid)
        viewModel.nameState.edit { replace(0, 1, "") }
        assertFalse(viewModel.isNameValid)
    }

    @Test
    fun `updateLocation updates location and search text`() = runTest {
        val station = Station(1, "S", "A", LatLng(0.0, 0.0))
        every { BackendApi.get_station_info(1) } returns station

        val viewModel = EditStationViewModel(1)
        val result = mockk<LocationResult>()
        val searchBarState = mockk<SearchBarState>()
        
        every { result.latLng } returns LatLng(10.0, 20.0)
        coEvery { result.getAddress() } returns "New Address"
        every { searchBarState.targetValue } returns SearchBarValue.Collapsed

        viewModel.updateLocation(result, searchBarState)
        
        // Since we are using StandardTestDispatcher, the coroutine won't run until we yield or advance
        // But TextFieldState might be updated immediately if not careful? No, it's inside viewModelScope.launch.
        
        advanceUntilIdle()

        assertEquals(LatLng(10.0, 20.0), viewModel.location.first)
        assertEquals("New Address", viewModel.location.second)
        assertEquals("New Address", viewModel.searchTextFieldState.text.toString())
    }

    @Test
    fun `saveChanges calls backend api when valid`() {
        val station = Station(1, "S", "A", LatLng(0.0, 0.0))
        every { BackendApi.get_station_info(1) } returns station
        every { BackendApi.edit_station(any(), any(), any(), any()) } returns Unit

        val viewModel = EditStationViewModel(1)
        var successCalled = false
        
        viewModel.saveChanges { successCalled = true }

        verify { BackendApi.edit_station(1, "S", "A", LatLng(0.0, 0.0)) }
        assertTrue(successCalled)
    }
}
