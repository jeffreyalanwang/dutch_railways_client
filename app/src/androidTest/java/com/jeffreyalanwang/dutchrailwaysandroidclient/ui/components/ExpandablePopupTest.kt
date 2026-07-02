package com.jeffreyalanwang.dutchrailwaysandroidclient.ui.components

import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.isPopup
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ExpandablePopupTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun collapsed_rendersContent() {
        composeTestRule.setContent {
            ExpandablePopup(
                isExpanded = false,
                onCollapse = {},
                collapsedDimensions = DpSize(100.dp, 100.dp),
                uncoercedExpandedOffset = IntOffset(10, 10),
                windowInsets = WindowInsets(0.dp),
                animationSpec = tween<Int>() to tween<Int>()
            ) {
                Box(Modifier.size(50.dp).testTag("content"))
            }
        }

        composeTestRule.onNodeWithTag("content").assertIsDisplayed()
    }

    @Test
    fun expanded_rendersContentInPopup() {
        composeTestRule.setContent {
            ExpandablePopup(
                isExpanded = true,
                onCollapse = {},
                collapsedDimensions = DpSize(100.dp, 100.dp),
                uncoercedExpandedOffset = IntOffset(10, 10),
                windowInsets = WindowInsets(0.dp),
                animationSpec = tween<Int>(durationMillis = 1) to tween<Int>(durationMillis = 1)
            ) {
                Box(Modifier.size(50.dp).testTag("content"))
            }
        }

        composeTestRule.waitForIdle()
        // isPopup() is a matcher for nodes that are inside a Popup
        composeTestRule.onNode(isPopup(), useUnmergedTree = true).assertExists()
        composeTestRule.onNodeWithTag("content", useUnmergedTree = true).assertIsDisplayed()
    }

    @Test
    fun toggleExpansion_callsStartedListener() {
        var expanded by mutableStateOf(false)
        var started = false

        composeTestRule.setContent {
            ExpandablePopup(
                isExpanded = expanded,
                onCollapse = {},
                collapsedDimensions = DpSize(100.dp, 100.dp),
                uncoercedExpandedOffset = IntOffset(10, 10),
                windowInsets = WindowInsets(0.dp),
                animationSpec = tween<Int>(durationMillis = 100) to tween<Int>(durationMillis = 100),
                animationStartedListener = { started = true }
            ) {
                Box(Modifier.size(50.dp))
            }
        }

        composeTestRule.waitForIdle()
        expanded = true
        composeTestRule.waitForIdle()
        assertTrue("Started listener should have been called", started)
    }

    @Test
    fun toggleExpansion_callsFinishedListener() {
        var expanded by mutableStateOf(false)
        var finished = false

        composeTestRule.setContent {
            ExpandablePopup(
                isExpanded = expanded,
                onCollapse = {},
                collapsedDimensions = DpSize(100.dp, 100.dp),
                uncoercedExpandedOffset = IntOffset(10, 10),
                windowInsets = WindowInsets(0.dp),
                animationSpec = tween<Int>(durationMillis = 10) to tween<Int>(durationMillis = 10),
                animationFinishedListener = { finished = true }
            ) {
                Box(Modifier.size(50.dp))
            }
        }

        composeTestRule.waitForIdle()
        expanded = true
        composeTestRule.waitForIdle()
        assertTrue("Finished listener should have been called", finished)
    }

    @Test
    fun dismissPopup_callsOnCollapse() {
        var collapseCalled = false
        composeTestRule.setContent {
            Box(Modifier.fillMaxSize().testTag("background")) {
                ExpandablePopup(
                    isExpanded = true,
                    onCollapse = { collapseCalled = true },
                    collapsedDimensions = DpSize(10.dp, 10.dp),
                    uncoercedExpandedOffset = IntOffset(0, 0),
                    windowInsets = WindowInsets(0.dp),
                    animationSpec = tween<Int>(durationMillis = 1) to tween<Int>(durationMillis = 1)
                ) {
                    Box(Modifier.size(10.dp).testTag("popup_content"))
                }
            }
        }

        composeTestRule.waitForIdle()
        
        // Popup uses a separate window. In tests, we can find it with isPopup().
        // To click outside, we can click a node that is NOT the popup.
        
        // We'll perform click on the background node.
        composeTestRule.onNodeWithTag("background").performClick()

        composeTestRule.waitForIdle()
        assertTrue("onCollapse should be called when clicking outside", collapseCalled)
    }
}
