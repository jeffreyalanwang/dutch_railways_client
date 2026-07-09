package com.jeffreyalanwang.dutchrailwaysandroidclient.ui

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextClearance
import androidx.compose.ui.test.performTextInput
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
import com.jeffreyalanwang.dutchrailwaysandroidclient.onNodeAfterExactlyOneExists
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockkObject
import io.mockk.unmockkAll
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
            DutchRailwaysAndroidClientApp()
        }

        onNodeWithText("Edit").performClick()

        onAllNodesWithTag("search_input").onFirst()
            .performTextInput("Initial")
        onNodeAfterExactlyOneExists(hasText("Initial Name") and !hasSetTextAction())
            .performClick()

        onNodeWithText("Station").assertIsDisplayed()
        onNodeWithText("Initial Name").assertIsDisplayed()

        onNodeWithContentDescription("Edit").performClick()

        onNodeWithContentDescription("Text field: set name").assertIsDisplayed()
        onNodeWithContentDescription("Text field: set name").performTextClearance()
        onNodeWithContentDescription("Text field: set name").performTextInput("Updated Name")
        onNodeWithContentDescription("Finish & save").performClick()

        // Wait for the detail screen to reappear with updated info
        onNodeAfterExactlyOneExists(hasText("Updated Name")).assertIsDisplayed()
        onNodeWithText("Station").assertIsDisplayed()
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
            DutchRailwaysAndroidClientApp()
        }

        onNodeWithText("Edit").performClick()
        onAllNodesWithTag("search_input").onFirst()
            .performTextInput("Initial")
        onNodeAfterExactlyOneExists(hasText("Initial Area") and !hasSetTextAction())
            .performClick()

        onNodeWithText("Area").assertIsDisplayed()
        onNodeWithText("Initial Area").assertIsDisplayed()

        onNodeWithContentDescription("Edit").performClick()
        
        onNodeWithContentDescription("Text field: set name").assertIsDisplayed()
        onNodeWithContentDescription("Text field: set name").performTextClearance()
        onNodeWithContentDescription("Text field: set name").performTextInput("Updated Area")
        onNodeWithContentDescription("Finish & save").performClick()

        // Wait for the detail screen to reappear with updated info
        onNodeAfterExactlyOneExists(hasText("Updated Area"))
            .assertIsDisplayed()
        onNodeWithText("Area").assertIsDisplayed()
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
            DutchRailwaysAndroidClientApp()
        }

        onNodeWithText("Edit").performClick()
        onAllNodesWithTag("search_input").onFirst()
            .performTextInput("Initial")

        onNodeAfterExactlyOneExists(hasText("Initial Service") and !hasSetTextAction())
            .performScrollTo()
            .performClick()

        onNodeWithText("Initial Service").assertIsDisplayed()

        onNodeWithContentDescription("Edit").performClick()
        onNodeWithContentDescription("Finish & save").performClick()

        // Wait for the detail screen to reappear with updated info
        onNodeAfterExactlyOneExists(hasText("Updated Service"))
            .assertIsDisplayed()
        onNodeWithText("Updated Service").assertIsDisplayed()
    }
}
