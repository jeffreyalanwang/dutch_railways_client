package com.jeffreyalanwang.dutchrailwaysandroidclient.ui.screens.child

import androidx.compose.ui.test.filter
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isDisplayed
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onLast
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.v2.runAndroidComposeUiTest
import androidx.compose.ui.test.waitUntilExactlyOneExists
import androidx.compose.ui.test.waitUntilNodeCount
import com.jeffreyalanwang.dutchrailwaysandroidclient.onNodeAfterExactlyOneExists
import com.jeffreyalanwang.dutchrailwaysandroidclient.ui.MainActivity
import org.junit.Test

class DeletePassServiceTest {

    @Test
    fun `delete pass service`() = runAndroidComposeUiTest<MainActivity> {

        // 1. Search for a PassService
        onNodeWithText("Edit").performClick()
        onNodeWithText("Tap to unlock")
            .run { if ( isDisplayed() ) performClick() }
        onAllNodes(hasSetTextAction()).onFirst().performClick()
        onAllNodes(hasSetTextAction()).onLast().performTextInput("2263")
        with(hasText("2263", substring = true) and !hasSetTextAction()) {
            waitUntilExactlyOneExists(this)
            onNode(this).performScrollTo().performClick()
        }

        // 2. Duplicate to have two
        with(hasContentDescription("Duplicate")) {
            waitUntilExactlyOneExists(this)
            onNode(this).performClick()
        }
        onNodeWithContentDescription("Finish & save").performClick()
        waitForIdle()

        // 3. Search again and verify two results
        onNodeWithText("Edit").performClick()
        onAllNodes(hasSetTextAction()).onFirst().performClick()
        onAllNodes(hasSetTextAction()).onLast().performTextInput("2263")
        waitUntilNodeCount(hasText("2263", substring = true) and !hasSetTextAction(), 2)

        // 4. Delete one
        onAllNodesWithText("2263", substring = true).filter(!hasSetTextAction())
            .onFirst()
            .performScrollTo().performClick()
        onNodeAfterExactlyOneExists(hasContentDescription("Delete"), 5000)
            .performClick()

        waitUntilExactlyOneExists(hasText("Delete", substring = true) and hasText("?", substring = true))
        onNodeWithText("Yes").performClick()

        // 5. Verify only one remains
        waitUntilExactlyOneExists(hasText("2263", substring = true))
    }
}
