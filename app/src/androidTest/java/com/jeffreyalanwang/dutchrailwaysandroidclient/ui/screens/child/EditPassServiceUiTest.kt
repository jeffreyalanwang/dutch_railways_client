package com.jeffreyalanwang.dutchrailwaysandroidclient.ui.screens.child

import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasAnyDescendant
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.compose.ui.test.swipeDown
import com.jeffreyalanwang.dutchrailwaysandroidclient.PassService
import com.jeffreyalanwang.dutchrailwaysandroidclient.ServiceStop
import com.jeffreyalanwang.dutchrailwaysandroidclient.Station
import com.jeffreyalanwang.dutchrailwaysandroidclient.Trainset
import backend.BackendApi
import com.jeffreyalanwang.dutchrailwaysandroidclient.ui.screens.child.edit.EditPassServiceScreen
import com.jeffreyalanwang.dutchrailwaysandroidclient.ui.screens.child.edit.NewPassServiceScreen
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.unmockkAll
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import java.time.ZoneId
import java.time.ZonedDateTime

class EditPassServiceUiTest {

    private val ams = ZoneId.of("Europe/Amsterdam")

    @Before
    fun setUp() {
        mockkObject(BackendApi)
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun `creating a new pass service should allow adding stops and selecting stations`() = runComposeUiTest {
            val station1 = Station(1, "Amsterdam Centraal", "Address 1", mockk())
            val station2 = Station(2, "Rotterdam Centraal", "Address 2", mockk())
            
            every { BackendApi.autocomplete_place(Station::class, any()) } returns listOf(station1, station2)
            every { BackendApi.get_station_info(1) } returns station1
            every { BackendApi.get_station_info(2) } returns station2
            
            // Mock add_pass_service to return a dummy service
            every { BackendApi.add_pass_service(any(), any(), any(), any()) } returns PassService(123, "New Service", Trainset.SLT, emptySet())
    
            var idToNavigate: Int? = null
    
            setContent {
                NewPassServiceScreen(
                    onCancelRequest = {},
                    onSaveFinished = { idToNavigate = it }
                )
            }
    
            // Add 2 stops first so arrival/departure fields become visible
            onNodeWithText("Add stop").performClick()
            onNodeWithText("Add stop").performClick()
    
            // Select station for stop 0
            onNodeWithTag("stop_station_0").performClick()
            onNode(hasSetTextAction()).performTextInput("Amsterdam")
            onNodeWithText("Amsterdam Centraal").performClick()
            
            // Set departure time for stop 0 to 10:00
            onNodeWithTag("stop_departure_0").performClick()
            onNodeWithContentDescription("Use keyboard").performClick()
            onAllNodes(hasSetTextAction())[0].performTextReplacement("10")
            onAllNodes(hasSetTextAction())[1].performTextReplacement("00")
            onNodeWithText("OK").performClick()
    
            // Select Trainset
            onNodeWithTag("trainset_selector").performClick()
            onNodeWithText("SLT").performClick()
            
            // Select station for stop 1
            onNodeWithTag("stop_station_1").performClick()
            onNode(hasSetTextAction()).performTextInput("Rotterdam")
            onNodeWithText("Rotterdam Centraal").performClick()
            
            // Set arrival time for stop 1 to 11:00
            onNodeWithTag("stop_arrival_1").performClick()
            onNodeWithContentDescription("Use keyboard").performClick()
            onAllNodes(hasSetTextAction())[0].performTextReplacement("11")
            onAllNodes(hasSetTextAction())[1].performTextReplacement("00")
            onNodeWithText("OK").performClick()
    
            // Save
            onNodeWithContentDescription("Finish & save").performClick()
            
            // Verify navigation called with mocked ID
            waitForIdle()
            assertEquals(123, idToNavigate)
        }

    @Test
    fun `editing an existing pass service should display correctly pre-filled data`() = runComposeUiTest {
        val station = Station(1, "Den Haag Centraal", "Address", mockk())
        val stop = ServiceStop(null, ZonedDateTime.now(ams), 119, 1)
        val service = PassService(119, "Intercity to Den Haag Centraal", Trainset.VIRM, emptySet())
        
        every { BackendApi.get_pass_service(119) } returns service
        every { BackendApi.get_stops_of_service(any<PassService>()) } returns listOf(stop)
        every { BackendApi.get_station_info(1) } returns station

        setContent {
            EditPassServiceScreen(
                id = 119,
                onCancelRequest = {},
                onSaveFinished = {}
            )
        }

        // Check pre-filled data
        onNodeWithText("Edit train service").assertIsDisplayed()
        onNodeWithTag("stop_station_0").assert(hasAnyDescendant(hasText("Den Haag Centraal")))
    }

    @Test
    fun `reordering stops in edit mode should update their position in the list`() = runComposeUiTest {
        val ams = ZoneId.of("Europe/Amsterdam")
        val station1 = Station(1, "Station 1", "Address 1", mockk())
        val station2 = Station(2, "Station 2", "Address 2", mockk())
        val stop1 = ServiceStop(null, ZonedDateTime.now(ams), 119, 1)
        val stop2 = ServiceStop(ZonedDateTime.now(ams).plusMinutes(10), null, 119, 2)
        val service = PassService(119, "Test Service", Trainset.VIRM, emptySet())

        every { BackendApi.get_pass_service(119) } returns service
        every { BackendApi.get_stops_of_service(any<PassService>()) } returns listOf(stop1, stop2)
        every { BackendApi.get_station_info(1) } returns station1
        every { BackendApi.get_station_info(2) } returns station2

        setContent {
            EditPassServiceScreen(
                id = 119,
                onCancelRequest = {},
                onSaveFinished = {}
            )
        }

        // Verify initial order
        onNodeWithTag("stop_station_0").assert(hasAnyDescendant(hasText("Station 1")))
        onNodeWithTag("stop_station_1").assert(hasAnyDescendant(hasText("Station 2")))

        // Perform drag and drop: swipe handle 0 down significantly
        onNodeWithTag("drag_handle_0").performTouchInput {
            swipeDown(startY = centerY, endY = centerY + 500f, durationMillis = 1000)
        }
        
        waitForIdle()

        // After reorder
        onNodeWithTag("stop_station_0").assert(hasAnyDescendant(hasText("Station 2")))
        onNodeWithTag("stop_station_1").assert(hasAnyDescendant(hasText("Station 1")))
    }
}
