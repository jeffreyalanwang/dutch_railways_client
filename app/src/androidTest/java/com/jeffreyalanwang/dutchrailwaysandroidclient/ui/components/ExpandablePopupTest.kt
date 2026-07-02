package com.jeffreyalanwang.dutchrailwaysandroidclient.ui.components

import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.v2.createComposeRule
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
    fun `collapsed popup should render its content directly`() {
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
    fun `expanded popup should render its content within a popup`() {
        composeTestRule.setContent {
            ExpandablePopup(
                isExpanded = true,
                onCollapse = {},
                collapsedDimensions = DpSize(100.dp, 100.dp),
                uncoercedExpandedOffset = IntOffset(1, 1),
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
    fun `toggling expansion should trigger the started listener`() {
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
    fun `toggling expansion should trigger the finished listener`() {
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
    fun `pressing back should trigger the onCollapse callback`() {
        var collapseCalled = false
        composeTestRule.setContent {
            ExpandablePopup(
                isExpanded = true,
                onCollapse = { collapseCalled = true },
                collapsedDimensions = DpSize(10.dp, 10.dp),
                uncoercedExpandedOffset = IntOffset(1, 1),
                windowInsets = WindowInsets(0.dp),
                animationSpec = tween<Int>(durationMillis = 1) to tween<Int>(durationMillis = 1)
            ) {
                Box(Modifier.size(10.dp))
            }
        }

        composeTestRule.waitForIdle()
        androidx.test.espresso.Espresso.pressBack()
        composeTestRule.waitForIdle()
        assertTrue("onCollapse should be called when back is pressed", collapseCalled)
    }

    @Test
    fun `clicking outside the popup should trigger the onCollapse callback`() {
        var collapseCalled = false
        composeTestRule.setContent {
            Box(Modifier.fillMaxSize()) {
                ExpandablePopup(
                    isExpanded = true,
                    onCollapse = { collapseCalled = true },
                    collapsedDimensions = DpSize(10.dp, 10.dp),
                    uncoercedExpandedOffset = IntOffset(1, 1),
                    windowInsets = WindowInsets(0.dp),
                    animationSpec = tween<Int>(durationMillis = 1) to tween<Int>(durationMillis = 1)
                ) {
                    Box(Modifier.size(10.dp).testTag("popup_content"))
                }
            }
        }

        composeTestRule.waitForIdle()
        
        // We try to click outside by clicking on the root of the main window.
        composeTestRule.onAllNodes(isRoot()).onFirst().performTouchInput {
            click(bottomRight)
        }

        composeTestRule.waitForIdle()
        assertTrue("onCollapse should be called when clicking outside", collapseCalled)
    }
}
