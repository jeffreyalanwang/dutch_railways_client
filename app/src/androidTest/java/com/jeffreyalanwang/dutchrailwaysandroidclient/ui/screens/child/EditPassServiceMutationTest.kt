package com.jeffreyalanwang.dutchrailwaysandroidclient.ui.screens.child

import androidx.compose.ui.test.assertAny
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.filter
import androidx.compose.ui.test.filterToOne
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasNoClickAction
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
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
import androidx.compose.ui.test.v2.runAndroidComposeUiTest
import com.jeffreyalanwang.dutchrailwaysandroidclient.Trainset
import com.jeffreyalanwang.dutchrailwaysandroidclient.onNodeAfterExactlyOneExists
import com.jeffreyalanwang.dutchrailwaysandroidclient.ui.MainActivity
import org.junit.Test

class EditPassServiceMutationTest {

    @Test
    fun `duplicate and mutate a PassService`() = runAndroidComposeUiTest<MainActivity> {
        // 1. Navigate to the Edit tab
        onNodeWithText("Edit").performClick()
        waitForIdle()

        // 2. Search for a PassService
        onAllNodes(hasSetTextAction()).onLast().performTextInput("2263")

        // 3. Select the search result
        onNodeAfterExactlyOneExists(hasText("2263", substring = true) and !hasSetTextAction())
            .performScrollTo()
            .performClick()

        // 4. Click the "Duplicate" icon
        waitUntil("Duplicate icon to appear", 5000) {
            onAllNodesWithContentDescription("Duplicate").fetchSemanticsNodes().isNotEmpty()
        }
        onNodeWithContentDescription("Duplicate").performClick()

        // 5. Change trainset to SLT
        onNodeAfterExactlyOneExists(hasTestTag("trainset_selector"), 5000)
            .performClick()
        onNodeWithText(Trainset.SLT.name)
            .performScrollTo()
            .performClick()

        onNodeWithTag("trainset_selector")
            .performClick()
        onAllNodesWithText(Trainset.SLT.name)
            .filterToOne(hasNoClickAction())
            .assertExists()

        // 6. Save changes
        onNodeWithContentDescription("Finish & save").performClick()

        // 7. Navigate to duplicated PassService
        onAllNodes(hasSetTextAction()).onLast().performTextInput("2263")

        // 3. Select the search result
        onNodeAfterExactlyOneExists(hasText("2263", substring = true) and !hasSetTextAction())
            .performScrollTo()
            .performClick()

        // 8. Assert navigation back to PassServiceDetailScreen with new data
        onNodeAfterExactlyOneExists(hasContentDescription("Train icon"), 5000)
            .performClick()

        onNodeWithText(Trainset.SLT.name).assertExists()
    }

    @Test
    fun `modify stops in place`() = runAndroidComposeUiTest<MainActivity> {
        // 1. Navigate to the Edit tab
        onNodeWithText("Edit").performClick()
        waitForIdle()

        // 2. Search for a PassService
        onAllNodes(hasSetTextAction()).onFirst().performClick()
        waitForIdle()
        
        onAllNodes(hasSetTextAction()).onLast().performTextInput("2263")

        // 3. Select the search result
        waitUntil("Search result to appear", 5000) {
            onAllNodesWithText("2263", substring = true).filter(!hasSetTextAction()).fetchSemanticsNodes().isNotEmpty()
        }
        onAllNodesWithText("2263", substring = true).filterToOne(!hasSetTextAction())
            .performScrollTo()
            .performClick()

        // 4. Click the "Edit" icon
        waitUntil("Edit icon to appear", 5000) {
            onAllNodesWithContentDescription("Edit").filter(hasClickAction()).fetchSemanticsNodes().isNotEmpty()
        }
        onNode(hasContentDescription("Edit") and hasClickAction()).performClick()

        // 5. Modify stops
        waitUntil("Stop station selector to appear", 5000) {
            onAllNodesWithTag("stop_station_1").fetchSemanticsNodes().isNotEmpty()
        }
        // Delete last stop (Rotterdam Centraal)
        onAllNodes(hasAnyAncestor(hasTestTag("edit_stop_row_2"))).run {
            assertAny(hasText("Rotterdam Centraal"))
            filterToOne(hasContentDescription("Delete stop")).performClick()
            assertCountEquals(0)
        }
        
        // 6. Save changes
        onNodeWithContentDescription("Finish & save").performClick()

        // 7. Assert navigation back
        waitUntil("Train icon to appear", 5000) {
            onAllNodesWithContentDescription("Train icon").fetchSemanticsNodes().isNotEmpty()
        }

        onNodeWithText("Amsterdam Centraal").assertExists()
        onNodeWithText("Den Haag HS").assertExists()
        onNodeWithText("Rotterdam Centraal").assertDoesNotExist()
    }

}
