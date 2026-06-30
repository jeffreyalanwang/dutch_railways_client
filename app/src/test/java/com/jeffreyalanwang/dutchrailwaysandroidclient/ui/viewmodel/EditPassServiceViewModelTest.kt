package com.jeffreyalanwang.dutchrailwaysandroidclient.ui.viewmodel

import android.util.Log
import com.jeffreyalanwang.dutchrailwaysandroidclient.BackendApi
import com.jeffreyalanwang.dutchrailwaysandroidclient.PassService
import com.jeffreyalanwang.dutchrailwaysandroidclient.ServiceStop
import com.jeffreyalanwang.dutchrailwaysandroidclient.Station
import com.jeffreyalanwang.dutchrailwaysandroidclient.StopPoint
import com.jeffreyalanwang.dutchrailwaysandroidclient.TrainAmenity
import com.jeffreyalanwang.dutchrailwaysandroidclient.Trainset
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import io.mockk.verify
import kotlinx.datetime.LocalTime
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.ZoneId
import java.time.ZonedDateTime

class EditPassServiceViewModelTest {

    private val ams = ZoneId.of("Europe/Amsterdam")

    @Before
    fun setUp() {
        mockkObject(BackendApi)
        mockkStatic(Log::class)
        every { Log.d(any(), any()) } returns 0
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun `initial state with null service`() {
        val viewModel = EditPassServiceViewModel(null, null)
        assertEquals(null, viewModel.trainsetSelection)
        assertEquals(emptySet<TrainAmenity>(), viewModel.amenitiesMultiSelection)
        assertEquals("Train", viewModel.title)
        assertEquals(0, viewModel.stops.size)
    }

    @Test
    fun `initial state with basedOnService`() {
        val station = Station(1, "Test Station", "Address", mockk())
        val stop = ServiceStop(null, ZonedDateTime.now(ams), 100, 1)
        
        val service = PassService(100, "Intercity to Somewhere", Trainset.VIRM, setOf(TrainAmenity.WIFI), listOf(stop))
        
        every { BackendApi.get_stops_of_service(any<PassService>()) } returns listOf(stop)
        every { BackendApi.get_station_info(1) } returns station

        val viewModel = EditPassServiceViewModel(service, 100)
        
        assertEquals(Trainset.VIRM, viewModel.trainsetSelection)
        assertEquals(setOf(TrainAmenity.WIFI), viewModel.amenitiesMultiSelection)
        assertEquals("Intercity to Test Station", viewModel.title)
        assertEquals(1, viewModel.stops.size)
        assertEquals(1, viewModel.stops[0].stationId)
    }

    @Test
    fun `addStop adds a new TentativeStop`() {
        val viewModel = EditPassServiceViewModel(null, null)
        viewModel.addStop()
        assertEquals(1, viewModel.stops.size)
        assertNull(viewModel.stops[0].stationId)
    }

    @Test
    fun `removeStop removes stop at index`() {
        val viewModel = EditPassServiceViewModel(null, null)
        viewModel.addStop()
        viewModel.addStop()
        assertEquals(2, viewModel.stops.size)
        
        viewModel.removeStop(0)
        assertEquals(1, viewModel.stops.size)
    }

    @Test
    fun `updateStation updates stationId`() {
        val viewModel = EditPassServiceViewModel(null, null)
        viewModel.addStop()
        
        val station = Station(1, "New Station", "Address", mockk())
        viewModel.updateStation(0, station)
        
        assertEquals(1, viewModel.stops[0].stationId)
    }

    @Test
    fun `reorderStops changes stops order`() {
        val viewModel = EditPassServiceViewModel(null, null)
        viewModel.addStop() // index 0
        viewModel.addStop() // index 1
        
        val id0 = viewModel.stops[0].id
        val id1 = viewModel.stops[1].id
        
        viewModel.reorderStops(0, 1)
        
        assertEquals(id1, viewModel.stops[0].id)
        assertEquals(id0, viewModel.stops[1].id)
    }

    @Test
    fun `updateStopTime updates arrival time`() {
        val stop = ServiceStop(ZonedDateTime.now(ams), ZonedDateTime.now(ams), 100, 1)
        val service = PassService(100, "Train", Trainset.VIRM, emptySet(), listOf(stop))
        
        every { BackendApi.get_stops_of_service(any<PassService>()) } returns listOf(stop)
        
        val viewModel = EditPassServiceViewModel(service, 100)
        val newTime = LocalTime(10, 30)
        
        viewModel.updateStopTime(0, StopPoint.Arrival, newTime)
        
        assertEquals(10, viewModel.stops[0].arrival?.hour)
        assertEquals(30, viewModel.stops[0].arrival?.minute)
    }

    @Test
    fun `stationValidity detects missing stationId`() {
        val viewModel = EditPassServiceViewModel(null, null)
        viewModel.addStop()
        assertFalse(viewModel.stationValidity[0])
        
        val station = Station(1, "S1", "A1", mockk())
        viewModel.updateStation(0, station)
        assertTrue(viewModel.stationValidity[0])
    }

    @Test
    fun `stationValidity detects duplicate stationId`() {
        val viewModel = EditPassServiceViewModel(null, null)
        val station = Station(1, "S1", "A1", mockk())
        
        viewModel.addStop()
        viewModel.updateStation(0, station)
        assertTrue(viewModel.stationValidity[0])
        
        viewModel.addStop()
        viewModel.updateStation(1, station)
        assertFalse(viewModel.stationValidity[0])
        assertFalse(viewModel.stationValidity[1])
    }

    @Test
    fun `time validity for single stop`() {
        val viewModel = EditPassServiceViewModel(null, null)
        viewModel.addStop()
        // First and last stop times are always considered valid (they'll be removed on save)
        assertTrue(viewModel.arrivalTimeValidity[0])
        assertTrue(viewModel.departureTimeValidity[0])
    }

    @Test
    fun `suggestedTime returns existing time if available`() {
        val now = ZonedDateTime.now(ams)
        val stop = ServiceStop(now, now.plusMinutes(5), 100, 1)
        val service = PassService(100, "Train", Trainset.VIRM, emptySet(), listOf(stop))
        every { BackendApi.get_stops_of_service(any<PassService>()) } returns listOf(stop)
        
        val viewModel = EditPassServiceViewModel(service, 100)
        val tentativeStop = viewModel.stops[0]
        
        with(viewModel) {
            assertEquals(now, tentativeStop.suggestedTime(StopPoint.Arrival))
            assertEquals(now.plusMinutes(5), tentativeStop.suggestedTime(StopPoint.Departure))
        }
    }

    @Test
    fun `saveChanges calls add_pass_service when destPassServiceId is null`() {
        val station = Station(1, "S1", "A1", mockk())
        val station2 = Station(2, "S2", "A2", mockk())
        
        every { BackendApi.get_station_info(1) } returns station
        every { BackendApi.get_station_info(2) } returns station2
        
        val viewModel = EditPassServiceViewModel(null, null)
        viewModel.trainsetSelection = Trainset.VIRM
        
        viewModel.addStop()
        viewModel.updateStation(0, station)
        viewModel.updateStopTime(0, StopPoint.Departure, LocalTime(10, 0))
        
        viewModel.addStop()
        viewModel.updateStation(1, station2)
        viewModel.updateStopTime(1, StopPoint.Arrival, LocalTime(11, 0))
        
        // Mock BackendApi.add_pass_service
        every { BackendApi.add_pass_service(any(), any(), any(), any()) } returns PassService(200, "Title", Trainset.VIRM, emptySet())
        
        val result = viewModel.saveChanges()
        assertEquals(200, result)
        
        verify { BackendApi.add_pass_service(any(), Trainset.VIRM, any(), any()) }
    }
}
