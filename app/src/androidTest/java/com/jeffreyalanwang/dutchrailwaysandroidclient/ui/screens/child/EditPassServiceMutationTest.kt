package com.jeffreyalanwang.dutchrailwaysandroidclient.ui.screens.child

import androidx.compose.ui.test.assertAny
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.filter
import androidx.compose.ui.test.filterToOne
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onLast
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.jeffreyalanwang.dutchrailwaysandroidclient.Trainset
import com.jeffreyalanwang.dutchrailwaysandroidclient.ui.MainActivity
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class EditPassServiceMutationTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun `duplicate and mutate a PassService`() {
        // 1. Navigate to the Edit tab
        composeTestRule.onNodeWithText("Edit").performClick()
        composeTestRule.waitForIdle()

        // 2. Search for a PassService
        composeTestRule.onAllNodes(hasSetTextAction()).onFirst().performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onAllNodes(hasSetTextAction()).onLast().performTextInput("2263")

        // 3. Select the search result
        composeTestRule.waitUntil(20000) {
            composeTestRule.onAllNodesWithText("2263", substring = true).filter(!hasSetTextAction()).fetchSemanticsNodes().isNotEmpty()
        }
        composeTestRule.onAllNodesWithText("2263", substring = true).filterToOne(!hasSetTextAction())
            .performScrollTo()
            .performClick()

        // 4. Click the "Duplicate" icon
        composeTestRule.waitUntil(20000) {
            composeTestRule.onAllNodesWithContentDescription("Duplicate").fetchSemanticsNodes().isNotEmpty()
        }
        composeTestRule.onNodeWithContentDescription("Duplicate").performClick()

        // 5. Change trainset to SLT
        composeTestRule.waitUntil(20000) {
            composeTestRule.onAllNodesWithTag("trainset_selector").fetchSemanticsNodes().isNotEmpty()
        }
        composeTestRule.onNodeWithTag("trainset_selector").performClick()
        composeTestRule.onNodeWithText(Trainset.SLT.name).performClick()

        // 6. Save changes
        composeTestRule.onNodeWithContentDescription("Finish & save").performClick()

        // 7. Assert navigation back to PassServiceDetailScreen with new data
        composeTestRule.waitUntil(20000) {
            composeTestRule.onAllNodesWithContentDescription("Train icon").fetchSemanticsNodes().isNotEmpty()
        }
        
        composeTestRule.onNodeWithContentDescription("Train icon").performClick()
        composeTestRule.onNodeWithText(Trainset.SLT.name).assertExists()
    }

    @Test
    fun `modify stops in place`() {
        // 1. Navigate to the Edit tab
        composeTestRule.onNodeWithText("Edit").performClick()
        composeTestRule.waitForIdle()

        // 2. Search for a PassService
        composeTestRule.onAllNodes(hasSetTextAction()).onFirst().performClick()
        composeTestRule.waitForIdle()
        
        composeTestRule.onAllNodes(hasSetTextAction()).onLast().performTextInput("2263")

        // 3. Select the search result
        composeTestRule.waitUntil(20000) {
            composeTestRule.onAllNodesWithText("2263", substring = true).filter(!hasSetTextAction()).fetchSemanticsNodes().isNotEmpty()
        }
        composeTestRule.onAllNodesWithText("2263", substring = true).filterToOne(!hasSetTextAction())
            .performScrollTo()
            .performClick()

        // 4. Click the "Edit" icon
        composeTestRule.waitUntil(20000) {
            composeTestRule.onAllNodesWithContentDescription("Edit").filter(hasClickAction()).fetchSemanticsNodes().isNotEmpty()
        }
        composeTestRule.onNode(hasContentDescription("Edit") and hasClickAction()).performClick()

        // 5. Modify stops
        composeTestRule.waitUntil(20000) {
            composeTestRule.onAllNodesWithTag("stop_station_1").fetchSemanticsNodes().isNotEmpty()
        }
        // Delete last stop (Rotterdam Centraal)
        composeTestRule.onAllNodes(hasAnyAncestor(hasTestTag("edit_stop_row_2"))).run {
            assertAny(hasText("Rotterdam Centraal"))
            filterToOne(hasContentDescription("Delete stop")).performClick()
            assertCountEquals(0)
        }
        
        // 6. Save changes
        composeTestRule.onNodeWithContentDescription("Finish & save").performClick()

        // 7. Assert navigation back
        composeTestRule.waitUntil(20000) {
            composeTestRule.onAllNodesWithContentDescription("Train icon").fetchSemanticsNodes().isNotEmpty()
        }

        composeTestRule.onNodeWithText("Amsterdam Centraal").assertExists()
        composeTestRule.onNodeWithText("Den Haag HS").assertExists()
        composeTestRule.onNodeWithText("Rotterdam Centraal").assertDoesNotExist()
    }

}
