package com.jeffreyalanwang.dutchrailwaysandroidclient.ui.screens.child

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextClearance
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.v2.runComposeUiTest
import com.google.android.gms.maps.model.LatLng
import com.jeffreyalanwang.dutchrailwaysandroidclient.Area
import com.jeffreyalanwang.dutchrailwaysandroidclient.Station
import com.jeffreyalanwang.dutchrailwaysandroidclient.backend.BackendApi
import com.jeffreyalanwang.dutchrailwaysandroidclient.backend.Geocoding
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockkObject
import io.mockk.unmockkAll
import io.mockk.verify
import org.junit.After
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalTestApi::class)
class EditPlaceTest {

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
    fun editStationScreen_displaysInitialData() = runComposeUiTest {
        val station = Station(1, "Amsterdam Centraal", "Address 1", LatLng(52.37, 4.89))
        every { BackendApi.get_station_info(1) } returns station

        setContent {
            EditStationScreen(
                id = 1,
                onCancelRequest = {},
                onSaveFinished = {},
            )
        }

        onNodeWithText("Edit station: Amsterdam Centraal").assertIsDisplayed()
        onNodeWithText("Amsterdam Centraal").assertIsDisplayed()
        onNodeWithText("Address 1").assertIsDisplayed()
    }

    @Test
    fun editStationScreen_editNameAndSave() = runComposeUiTest {
        val station = Station(1, "S", "A", LatLng(0.0, 0.0))
        every { BackendApi.get_station_info(1) } returns station
        every { BackendApi.edit_station(any(), any(), any(), any()) } returns Unit

        setContent {
            EditStationScreen(
                id = 1,
                onCancelRequest = {},
                onSaveFinished = {},
            )
        }

        onNodeWithText("S").performTextClearance()
        onNodeWithText("").performTextInput("New Name")
        
        onNodeWithContentDescription("Finish & save").performClick()

        verify { BackendApi.edit_station(1, "New Name", "A", LatLng(0.0, 0.0)) }
    }

    @Test
    fun editStationScreen_expandLocationSelector() = runComposeUiTest {
        val station = Station(1, "S", "Address", LatLng(0.0, 0.0))
        every { BackendApi.get_station_info(1) } returns station

        setContent {
            EditStationScreen(
                id = 1,
                onCancelRequest = {},
                onSaveFinished = {},
            )
        }

        onNodeWithTag("location_selector_caption").performClick()
        
        // Wait for the animation to finish or at least for the search bar to appear
        waitUntil(timeoutMillis = 5000L) {
            onAllNodesWithTag("location_search_input").fetchSemanticsNodes().isNotEmpty()
        }
        
        onNodeWithTag("location_search_input").assertIsDisplayed()
        // The back button should be there (leading icon of search input)
        onNode(hasContentDescription("Back") and hasAnyAncestor(hasTestTag("location_search_bar")))
            .assertIsDisplayed()
    }

    @Test
    fun editAreaScreen_displaysInitialData() = runComposeUiTest {
        val area = Area(1, "Nederland")
        every { BackendApi.get_area_info(1) } returns area

        setContent {
            EditAreaScreen(
                id = 1,
                onCancelRequest = {},
                onSaveFinished = {},
            )
        }

        onNodeWithText("Edit area: Nederland").assertIsDisplayed()
        onNodeWithText("Nederland").assertIsDisplayed()
    }

    @Test
    fun editAreaScreen_editNameAndSave() = runComposeUiTest {
        val area = Area(1, "Nederland")
        every { BackendApi.get_area_info(1) } returns area
        every { BackendApi.edit_area(any(), any()) } returns Unit

        setContent {
            EditAreaScreen(
                id = 1,
                onCancelRequest = {},
                onSaveFinished = {},
            )
        }

        onNodeWithText("Nederland").performTextClearance()
        onNodeWithText("").performTextInput("New Area")
        
        onNodeWithContentDescription("Finish & save").performClick()

        verify { BackendApi.edit_area(1, "New Area") }
    }
}
