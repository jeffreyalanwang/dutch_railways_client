package com.jeffreyalanwang.dutchrailwaysandroidclient.ui.screens.child

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.filter
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onLast
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.jeffreyalanwang.dutchrailwaysandroidclient.ui.MainActivity
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class DeletePassServiceTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun `delete pass service`() {

        // 1. Search for a PassService
        composeTestRule.onNodeWithText("Edit").performClick()
        composeTestRule.onAllNodes(hasSetTextAction()).onFirst().performClick()
        composeTestRule.onAllNodes(hasSetTextAction()).onLast().performTextInput("2263")
        with(hasText("2263", substring = true) and !hasSetTextAction()) {
            composeTestRule.waitUntilExactlyOneExists(this)
            composeTestRule.onNode(this).performScrollTo().performClick()
        }

        // 2. Duplicate to have two
        with(hasContentDescription("Duplicate")) {
            composeTestRule.waitUntilExactlyOneExists(this)
            composeTestRule.onNode(this).performClick()
        }
        composeTestRule.onNodeWithContentDescription("Finish & save").performClick()
        composeTestRule.waitForIdle()

        // 3. Search again and verify two results
        composeTestRule.onNodeWithText("Edit").performClick()
        composeTestRule.onAllNodes(hasSetTextAction()).onFirst().performClick()
        composeTestRule.onAllNodes(hasSetTextAction()).onLast().performTextInput("2263")
        composeTestRule.waitUntilNodeCount(hasText("2263", substring = true) and !hasSetTextAction(), 2)

        // 4. Delete one
        composeTestRule.onAllNodesWithText("2263", substring = true).filter(!hasSetTextAction())
            .onFirst()
            .performScrollTo().performClick()
        with(hasContentDescription("Delete")) {
            composeTestRule.waitUntilExactlyOneExists(this)
            composeTestRule.onNode(this).performClick()
        }
        
        composeTestRule.waitUntilExactlyOneExists(hasText("Delete", substring = true) and hasText("?", substring = true))
        composeTestRule.onNodeWithText("Yes").performClick()

        // 5. Verify only one remains
        composeTestRule.waitUntilExactlyOneExists(hasText("2263", substring = true))
    }
}
