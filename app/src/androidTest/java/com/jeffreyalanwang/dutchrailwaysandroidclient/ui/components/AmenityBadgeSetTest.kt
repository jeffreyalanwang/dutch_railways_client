package com.jeffreyalanwang.dutchrailwaysandroidclient.ui.components

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Card
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry.getInstrumentation
import androidx.test.uiautomator.UiDevice
import com.jeffreyalanwang.dutchrailwaysandroidclient.TrainAmenity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AmenityBadgeSetTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun `empty set should display no amenities text`() {
        composeTestRule.setContent {
            AmenityBadgeSet(
                amenities = emptySet(),
                isExpanded = false,
                onSetExpanded = { },
                windowInsets = WindowInsets(0.dp)
            )
        }
        composeTestRule.onNodeWithText("No amenities").assertIsDisplayed()
    }

    @Test
    fun `clicking no amenities text should toggle expansion`() {
        var expanded by mutableStateOf(false)
        composeTestRule.setContent {
            AmenityBadgeSet(
                amenities = emptySet(),
                isExpanded = expanded,
                onSetExpanded = { expanded = it },
                windowInsets = WindowInsets(0.dp)
            )
        }
        composeTestRule.onNodeWithText("No amenities").performClick()
        assertTrue("Expected isExpanded to be true after click", expanded)
    }

    @Test
    fun `set with amenities should display correct badges`() {
        val amenities = setOf(TrainAmenity.WIFI, TrainAmenity.TOILET)
        composeTestRule.setContent {
            AmenityBadgeSet(
                amenities = amenities,
                isExpanded = false,
                onSetExpanded = { },
                windowInsets = WindowInsets(0.dp)
            )
        }
        // Verify badges exist (they have content descriptions corresponding to friendly names)
        composeTestRule.onNodeWithContentDescription(TrainAmenity.WIFI.friendlyName).assertExists()
        composeTestRule.onNodeWithContentDescription(TrainAmenity.TOILET.friendlyName).assertExists()
    }

    @Test
    fun `clicking a badge should toggle expansion when read-only`() {
        var expanded by mutableStateOf(false)
        val amenities = setOf(TrainAmenity.WIFI)
        composeTestRule.setContent {
            AmenityBadgeSet(
                amenities = amenities,
                isExpanded = expanded,
                onSetExpanded = { expanded = it },
                windowInsets = WindowInsets(0.dp)
            )
        }
        
        // Find the badge and click it
        composeTestRule.onNodeWithContentDescription(TrainAmenity.WIFI.friendlyName).performClick()
        assertTrue("Expected isExpanded to be true after clicking badge", expanded)
    }

    @Test
    fun `editable set should display add button when expanded`() {
        composeTestRule.setContent {
            EditAmenityBadgeSet(
                amenities = setOf(TrainAmenity.WIFI),
                onModify = { },
                isExpanded = true,
                onSetExpanded = { },
                windowInsets = WindowInsets(0.dp)
            )
        }
        // "Add..." label is present in modifiable mode when expanded
        composeTestRule.onNodeWithText("Add...").assertIsDisplayed()
    }

    @Test
    fun `clicking outside the badge set should collapse`() {
        var expanded by mutableStateOf(true)
        val amenities = setOf(TrainAmenity.WIFI)
        composeTestRule.setContent {
            Card(
                modifier = Modifier.size(300.dp, 1000.dp)
                    .testTag("card")
            ) {
                AmenityBadgeSet(
                    amenities = amenities,
                    isExpanded = expanded,
                    onSetExpanded = { expanded = it },
                    windowInsets = WindowInsets(0.dp)
                )
            }
        }

        assertTrue(expanded)
        UiDevice.getInstance(getInstrumentation())
            .run {
                click(300, 300)
            }
        assertFalse("Expected isExpanded to be false after clicking away", expanded)
    }

    @Test
    fun `clicking a badge in edit mode should enter delete mode`() {
        composeTestRule.setContent {
            EditAmenityBadgeSet(
                amenities = setOf(TrainAmenity.WIFI),
                onModify = { },
                isExpanded = true,
                onSetExpanded = { },
                windowInsets = WindowInsets(0.dp)
            )
        }

        // Initially WIFI badge exists
        composeTestRule.onNodeWithContentDescription(TrainAmenity.WIFI.friendlyName).assertIsDisplayed()
        
        // Click WIFI badge
        composeTestRule.onNodeWithContentDescription(TrainAmenity.WIFI.friendlyName).performClick()
        
        // Should show delete prompt
        composeTestRule.onNodeWithText("Delete Wi-Fi?").assertIsDisplayed()
        // Icon description for delete is "Delete"
        composeTestRule.onNodeWithContentDescription("Delete").assertIsDisplayed()
    }

    @Test
    fun `confirming deletion should trigger onModify callback`() {
        val initialAmenities = setOf(TrainAmenity.WIFI)
        var resultAmenities: Set<TrainAmenity>? = null
        
        composeTestRule.setContent {
            EditAmenityBadgeSet(
                amenities = initialAmenities,
                onModify = { resultAmenities = it },
                isExpanded = true,
                onSetExpanded = { },
                windowInsets = WindowInsets(0.dp)
            )
        }

        // Enter delete mode
        composeTestRule.onNodeWithContentDescription(TrainAmenity.WIFI.friendlyName).performClick()
        
        // Click the delete icon
        composeTestRule.onNodeWithContentDescription("Delete").performClick()
        
        assertEquals(emptySet<TrainAmenity>(), resultAmenities)
    }

    @Test
    fun `adding an amenity should show options and trigger onModify`() {
        val initialAmenities = setOf(TrainAmenity.WIFI)
        var resultAmenities: Set<TrainAmenity>? = null
        
        composeTestRule.setContent {
            EditAmenityBadgeSet(
                amenities = initialAmenities,
                onModify = { resultAmenities = it },
                isExpanded = true,
                onSetExpanded = { },
                windowInsets = WindowInsets(0.dp)
            )
        }

        // Click "Add..."
        composeTestRule.onNodeWithText("Add...").performClick()
        
        // Should show other amenities, e.g., TOILET (Restrooms)
        composeTestRule.onNodeWithText("Add Restrooms").assertIsDisplayed()
        
        // Click to add
        composeTestRule.onNodeWithText("Add Restrooms").performClick()
        
        // Verify it was added
        assertTrue(resultAmenities?.contains(TrainAmenity.TOILET) == true)
        assertTrue(resultAmenities?.contains(TrainAmenity.WIFI) == true)
    }

    @Test
    fun `clicking add badge while collapsed should have no effect`() {
        var expanded by mutableStateOf(false)
        val initialAmenities = setOf(TrainAmenity.WIFI)
        
        composeTestRule.setContent {
            EditAmenityBadgeSet(
                amenities = initialAmenities,
                onModify = { },
                isExpanded = expanded,
                onSetExpanded = { expanded = it },
                windowInsets = WindowInsets(0.dp)
            )
        }

        // Find the "Add" badge (plus icon). Content description is "Add"
        val addButton = composeTestRule.onNodeWithContentDescription("Add")
        addButton.assertExists()
        
        // Click it while collapsed
        addButton.performClick()

        // Check
        composeTestRule.onNodeWithText("Add Restrooms").assertDoesNotExist()
    }
}
