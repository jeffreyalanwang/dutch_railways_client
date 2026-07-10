package com.jeffreyalanwang.dutchrailwaysandroidclient.ui.viewmodel

import androidx.compose.foundation.text.input.setTextAndPlaceCursorAtEnd
import com.google.android.gms.maps.model.LatLng
import com.jeffreyalanwang.dutchrailwaysandroidclient.Station
import backend.BackendApi
import com.jeffreyalanwang.dutchrailwaysandroidclient.ui.search.LocationResult
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.unmockkAll
import io.mockk.verify
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import kotlin.time.Duration.Companion.seconds

@OptIn(ExperimentalCoroutinesApi::class)
class LocationPickerModelTest {
    private val testDispatcher = StandardTestDispatcher()
    private val testScope = TestScope(testDispatcher)

    @Test
    fun `initial state is correct`() {
        val initialGeom = LatLng(1.0, 2.0)
        val initialAddress = "Initial Address"
        val model = LocationPickerModel(testScope, initialGeom, initialAddress)

        assertEquals(initialGeom, model.geom)
        assertEquals(initialAddress, model.address)
        assertEquals(initialAddress, model.displayString)
        assertFalse(model.isExpanded)
    }

    @Test
    fun `updateLocation updates geom and address on success`() = testScope.runTest {
        val model = LocationPickerModel(this, LatLng(0.0, 0.0), "Old")
        val result = mockk<LocationResult>()
        val newGeom = LatLng(10.0, 20.0)
        val newAddress = "New Address"

        val addressJob = CompletableDeferred<String>()
        every { result.latLng } returns newGeom
        coEvery { result.getAddress() } coAnswers { addressJob.await() }

        model.updateLocation(result)
        runCurrent()

        verify { result.latLng }
        coVerify { result.getAddress() }
        assertNull("Model should be in loading state", model.displayString)

        addressJob.complete(newAddress)
        advanceUntilIdle()

        assertEquals(newGeom, model.geom)
        assertEquals(newAddress, model.address)
        assertEquals(newAddress, model.displayString)
    }

    @Test
    fun `updateLocation handles null result`() = testScope.runTest {
        val model = LocationPickerModel(this, LatLng(0.0, 0.0), "Old")
        model.updateLocation(null)
        advanceUntilIdle()
        assertEquals("Old", model.address)
        assertEquals("Old", model.displayString)
    }

    @Test
    fun `updateLocation handles null address from geocoder`() = testScope.runTest {
        val model = LocationPickerModel(this, LatLng(0.0, 0.0), "Old")
        val result = mockk<LocationResult>()

        val job = CompletableDeferred<String?>()
        every { result.latLng } returns LatLng(10.0, 20.0)
        coEvery { result.getAddress() } coAnswers { job.await() }

        model.updateLocation(result)
        advanceUntilIdle()
        assertNull(model.displayString) // Loading state
        job.complete(null)

        // Address and geom should NOT have updated if getAddress returns null
        assertEquals(LatLng(0.0, 0.0), model.geom)
        assertEquals("Old", model.address)
        assertNull(model.displayString) // It stayed null because it returned@launch
    }

    @Test
    fun `subsequent updateLocation cancels previous one`() = testScope.runTest {
        val model = LocationPickerModel(this, LatLng(0.0, 0.0), "Old")
        val result1 = mockk<LocationResult>()
        val result2 = mockk<LocationResult>()

        every { result1.latLng } returns LatLng(1.0, 1.0)
        coEvery { result1.getAddress() } coAnswers {
            delay(1.seconds)
            "Address 1"
        }

        every { result2.latLng } returns LatLng(2.0, 2.0)
        coEvery { result2.getAddress() } returns "Address 2"

        model.updateLocation(result1)
        testDispatcher.scheduler.advanceTimeBy(500) // First job is suspended in delay
        
        model.updateLocation(result2) // Should cancel first job
        advanceUntilIdle()

        assertEquals("Address 2", model.address)
        assertEquals(LatLng(2.0, 2.0), model.geom)
    }
}

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
        unmockkAll()
    }

    @Test
    fun `initialization via station ID loads data from backend`() {
        val station = Station(123, "Test Station", "Test Address", LatLng(1.0, 2.0))
        every { BackendApi.get_station_info(123) } returns station

        val viewModel = EditStationViewModel(123)

        assertEquals(123, viewModel.stationId)
        assertEquals("Test Station", viewModel.currentName)
        assertEquals("Test Address", viewModel.currAddress)
        assertEquals(LatLng(1.0, 2.0), viewModel.currGeom)
        verify { BackendApi.get_station_info(123) }
    }

    @Test
    fun `isNameValid reacts to name changes`() {
        val station = Station(1, "Initial", "Addr", LatLng(0.0, 0.0))
        val viewModel = EditStationViewModel(station)

        assertTrue(viewModel.isNameValid)

        viewModel.nameFieldState.setTextAndPlaceCursorAtEnd("")
        assertFalse(viewModel.isNameValid)

        viewModel.nameFieldState.setTextAndPlaceCursorAtEnd("New Name")
        assertTrue(viewModel.isNameValid)
    }

    @Test
    fun `saveChanges calls backend when valid`() {
        val station = Station(1, "Name", "Addr", LatLng(0.0, 0.0))
        every { BackendApi.edit_station(any(), any(), any(), any()) } returns Unit
        
        val viewModel = EditStationViewModel(station)

        val result = viewModel.saveChanges()
        assertNotNull(result)

        verify { BackendApi.edit_station(1, "Name", "Addr", LatLng(0.0, 0.0)) }
    }

    @Test
    fun `saveChanges does nothing when invalid`() {
        val station = Station(1, "Name", "Addr", LatLng(0.0, 0.0))
        every { BackendApi.edit_station(any(), any(), any(), any()) } returns Unit
        
        val viewModel = EditStationViewModel(station)
        viewModel.nameFieldState.setTextAndPlaceCursorAtEnd("") // Invalid

        val result = viewModel.saveChanges()
        assertNull(result)

        verify(exactly = 0) { BackendApi.edit_station(any(), any(), any(), any()) }
    }
}

@OptIn(ExperimentalCoroutinesApi::class)
class EditStationViewModelIntegrationTest {
    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        mockkObject(BackendApi)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        unmockkAll()
    }

    @Test
    fun `onExpandLocationPicker updates delegate state`() {
        val station = Station(1, "Name", "Addr", LatLng(0.0, 0.0))
        val viewModel = EditStationViewModel(station)

        assertFalse(viewModel.locationPickerDelegate.isExpanded)
        viewModel.onExpandLocationPicker()
        assertTrue(viewModel.locationPickerDelegate.isExpanded)
    }

    @Test
    fun `viewModel reflects updates from location picker delegate`() = runTest {
        val station = Station(1, "Name", "Old Addr", LatLng(0.0, 0.0))
        val viewModel = EditStationViewModel(station)
        
        val result = mockk<LocationResult>()
        every { result.latLng } returns LatLng(5.0, 5.0)
        coEvery { result.getAddress() } returns "New Addr"

        viewModel.locationPickerDelegate.updateLocation(result)
        advanceUntilIdle()

        assertEquals("New Addr", viewModel.currAddress)
        assertEquals(LatLng(5.0, 5.0), viewModel.currGeom)
    }

    @Test
    fun `saveChanges uses values updated through location picker`() = runTest {
        val station = Station(1, "Name", "Old Addr", LatLng(0.0, 0.0))
        every { BackendApi.edit_station(any(), any(), any(), any()) } returns Unit
        
        val viewModel = EditStationViewModel(station)
        
        val result = mockk<LocationResult>()
        every { result.latLng } returns LatLng(5.0, 5.0)
        coEvery { result.getAddress() } returns "New Addr"

        viewModel.locationPickerDelegate.updateLocation(result)
        advanceUntilIdle()

        viewModel.saveChanges()

        verify { BackendApi.edit_station(1, "Name", "New Addr", LatLng(5.0, 5.0)) }
    }
}
