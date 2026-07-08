package com.jeffreyalanwang.dutchrailwaysandroidclient.ui

import androidx.compose.ui.test.*
import androidx.compose.ui.test.v2.runComposeUiTest
import com.google.android.gms.maps.model.LatLng
import com.jeffreyalanwang.dutchrailwaysandroidclient.Area
import com.jeffreyalanwang.dutchrailwaysandroidclient.PassService
import com.jeffreyalanwang.dutchrailwaysandroidclient.Place
import com.jeffreyalanwang.dutchrailwaysandroidclient.ServiceStop
import com.jeffreyalanwang.dutchrailwaysandroidclient.Station
import com.jeffreyalanwang.dutchrailwaysandroidclient.Trainset
import com.jeffreyalanwang.dutchrailwaysandroidclient.backend.BackendApi
import com.jeffreyalanwang.dutchrailwaysandroidclient.backend.Geocoding
import com.jeffreyalanwang.dutchrailwaysandroidclient.ui.theme.DutchRailwaysAndroidClientTheme
import io.mockk.*
import org.junit.After
import org.junit.Before
import org.junit.Test
import java.time.ZonedDateTime

class NavigationRefreshTest {

    @Before
    fun setup() {
        mockkObject(BackendApi)
        mockkObject(Geocoding)
        every { Geocoding.initialize(any()) } returns Unit
        coEvery { Geocoding.autocomplete_location(any()) } returns emptyList()
        coEvery { Geocoding.autocomplete_location(any(), any()) } returns emptyList()
        coEvery { Geocoding.closest_address(any()) } returns null
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun editStation_savesAndRefreshesStationDetail() = runComposeUiTest {
        val stationId = 1
        val initialStation = Station(stationId, "Initial Name", "Address", LatLng(0.0, 0.0))
        val updatedStation = Station(stationId, "Updated Name", "Address", LatLng(0.0, 0.0))

        every { BackendApi.get_station_info(stationId) } returns initialStation andThen updatedStation
        every { BackendApi.autocomplete_place(Place::class, any()) } returns listOf(initialStation)
        every { BackendApi.edit_station(any(), any(), any(), any()) } returns Unit

        setContent {
            DutchRailwaysAndroidClientTheme {
                DutchRailwaysAndroidClientApp()
            }
        }

        onNodeWithText("Edit").performClick()
        
        onNodeWithTag("edit_screen_search_input").performTextInput("Initial")
        waitUntil(timeoutMillis = 5000) {
            onAllNodesWithText("Initial Name").fetchSemanticsNodes().isNotEmpty()
        }
        onAllNodesWithText("Initial Name").filterToOne(!hasSetTextAction()).performClick()

        onNodeWithText("Station").assertIsDisplayed()
        onNodeWithText("Initial Name").assertIsDisplayed()

        onNodeWithContentDescription("Edit").performClick()
        
        onNodeWithTag("name_field").assertIsDisplayed()
        onNodeWithTag("name_field").performTextClearance()
        onNodeWithTag("name_field").performTextInput("Updated Name")
        onNodeWithContentDescription("Finish & save").performClick()

        // Wait for the detail screen to reappear with updated info
        waitUntil(timeoutMillis = 5000) {
            onAllNodesWithText("Updated Name").fetchSemanticsNodes().isNotEmpty()
        }
        onNodeWithText("Station").assertIsDisplayed()
        onNodeWithText("Updated Name").assertIsDisplayed()
    }

    @Test
    fun editArea_savesAndRefreshesAreaDetail() = runComposeUiTest {
        val areaId = 1
        val initialArea = Area(areaId, "Initial Area")
        val updatedArea = Area(areaId, "Updated Area")

        every { BackendApi.get_area_info(areaId) } returns initialArea andThen updatedArea
        every { BackendApi.autocomplete_place(Place::class, any()) } returns listOf(initialArea)
        every { BackendApi.edit_area(any(), any()) } returns Unit

        setContent {
            DutchRailwaysAndroidClientTheme {
                DutchRailwaysAndroidClientApp()
            }
        }

        onNodeWithText("Edit").performClick()
        onNodeWithTag("edit_screen_search_input").performTextInput("Initial")
        waitUntil(timeoutMillis = 5000) {
            onAllNodesWithText("Initial Area").fetchSemanticsNodes().isNotEmpty()
        }
        onAllNodesWithText("Initial Area").filterToOne(!hasSetTextAction()).performClick()

        onNodeWithText("Area").assertIsDisplayed()
        onNodeWithText("Initial Area").assertIsDisplayed()

        onNodeWithContentDescription("Edit").performClick()
        
        onNodeWithTag("name_field").assertIsDisplayed()
        onNodeWithTag("name_field").performTextClearance()
        onNodeWithTag("name_field").performTextInput("Updated Area")
        onNodeWithContentDescription("Finish & save").performClick()

        // Wait for the detail screen to reappear with updated info
        waitUntil(timeoutMillis = 5000) {
            onAllNodesWithText("Updated Area").fetchSemanticsNodes().isNotEmpty()
        }
        onNodeWithText("Area").assertIsDisplayed()
        onNodeWithText("Updated Area").assertIsDisplayed()
    }

    @Test
    fun editPassService_savesAndRefreshesPassServiceDetail() = runComposeUiTest {
        val serviceId = 1
        val initialService = PassService(serviceId, "Initial Service", Trainset.VIRM, emptySet())
        val updatedService = PassService(serviceId, "Updated Service", Trainset.VIRM, emptySet())
        
        // Use two stops to ensure validity
        val stop1 = ServiceStop(arrival = null, departure = ZonedDateTime.now(), passServiceId = serviceId, stationId = 1)
        val stop2 = ServiceStop(arrival = ZonedDateTime.now().plusMinutes(10), departure = null, passServiceId = serviceId, stationId = 2)
        val stops = listOf(stop1, stop2)

        every { BackendApi.get_pass_service(serviceId) } returns initialService andThen updatedService
        every { BackendApi.autocomplete_pass_service(any()) } returns listOf(initialService)
        every { BackendApi.update_pass_service(any(), any(), any(), any()) } returns Unit
        every { BackendApi.get_stops_of_service(any<PassService>()) } returns stops
        every { BackendApi.get_stops_of_service(any<Int>()) } returns stops
        every { BackendApi.get_station_info(any()) } returns Station(1, "S", "A", LatLng(0.0, 0.0))

        setContent {
            DutchRailwaysAndroidClientTheme {
                DutchRailwaysAndroidClientApp()
            }
        }

        onNodeWithText("Edit").performClick()
        onNodeWithTag("edit_screen_search_input").performTextInput("Initial")
        waitUntil(timeoutMillis = 5000) {
            onAllNodesWithText("Initial Service").fetchSemanticsNodes().isNotEmpty()
        }
        onAllNodesWithText("Initial Service").filterToOne(!hasSetTextAction()).performClick()

        onNodeWithText("Train").assertIsDisplayed()
        onNodeWithText("Initial Service").assertIsDisplayed()

        onNodeWithContentDescription("Edit").performClick()
        
        waitForIdle()
        onNodeWithContentDescription("Finish & save").performClick()

        // Wait for the detail screen to reappear with updated info
        waitUntil(timeoutMillis = 5000) {
            onAllNodesWithText("Updated Service").fetchSemanticsNodes().isNotEmpty()
        }
        onNodeWithText("Train").assertIsDisplayed()
        onNodeWithText("Updated Service").assertIsDisplayed()
    }
}
