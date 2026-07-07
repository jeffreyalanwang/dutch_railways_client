package com.jeffreyalanwang.dutchrailwaysandroidclient.ui.components

import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color.Companion.Green
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.isPopup
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.test.espresso.Espresso
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry.getInstrumentation
import androidx.test.uiautomator.UiDevice
import com.jeffreyalanwang.dutchrailwaysandroidclient.ui.theme.PurpleGrey40
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

class ExpandablePopupTest {

    @Test
    fun `collapsed popup should render its content directly`() = runComposeUiTest {
        setContent {
            ExpandablePopup(
                isExpanded = false,
                onCollapse = {},
                collapsedDimensions = DpSize(100.dp, 100.dp),
                uncoercedExpandedOffset = IntOffset(10, 10),
                windowInsets = WindowInsets(0.dp),
                animationSpec = tween<Int>() to tween<Int>()
            ) {
                Box(Modifier
                    .size(50.dp)
                    .testTag("content"))
            }
        }

        onNodeWithTag("content").assertIsDisplayed()
    }

    @Test
    fun `expanded popup should render its content within a popup`() = runComposeUiTest {
        setContent {
            ExpandablePopup(
                isExpanded = true,
                onCollapse = {},
                collapsedDimensions = DpSize(100.dp, 100.dp),
                uncoercedExpandedOffset = IntOffset(1, 1),
                windowInsets = WindowInsets(0.dp),
                animationSpec = tween<Int>(durationMillis = 1) to tween<Int>(durationMillis = 1)
            ) {
                Box(Modifier
                    .size(50.dp)
                    .testTag("content"))
            }
        }

        waitForIdle()
        // isPopup() is a matcher for nodes that are inside a Popup
        onNode(isPopup(), useUnmergedTree = true).assertExists()
        onNodeWithTag("content", useUnmergedTree = true).assertIsDisplayed()
    }

    @Test
    fun `toggling expansion should trigger the started listener`() = runComposeUiTest {
        var expanded by mutableStateOf(false)
        var started = false

        setContent {
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

        waitForIdle()
        expanded = true
        waitForIdle()
        assertTrue("Started listener should have been called", started)
    }

    @Test
    fun `toggling expansion should trigger the finished listener`() = runComposeUiTest {
        var expanded by mutableStateOf(false)
        var finished = false

        setContent {
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

        waitForIdle()
        expanded = true
        waitForIdle()
        assertTrue("Finished listener should have been called", finished)
    }

    @Test
    fun `pressing back should trigger the onCollapse callback`() = runComposeUiTest {
        var collapseCalled = false
        setContent {
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

        waitForIdle()
        Espresso.pressBack()
        waitForIdle()
        assertTrue("onCollapse should be called when back is pressed", collapseCalled)
    }

    @Test
    fun `clicking outside the popup should trigger the onCollapse callback`() = runComposeUiTest {
        var isExpanded by mutableStateOf(true)
        var collapseCalled = false
        setContent {
            Box(
                Modifier
                    .fillMaxSize()
                    .testTag("box")
            ) {
                ExpandablePopup(
                    isExpanded = isExpanded,
                    onCollapse = { isExpanded = false; collapseCalled = true },
                    collapsedDimensions = DpSize(200.dp, 200.dp),
                    uncoercedExpandedOffset = IntOffset(1, 1),
                    windowInsets = WindowInsets(0.dp),
                    animationSpec = tween<Int>(durationMillis = 1) to tween<Int>(durationMillis = 1)
                ) {
                    Box(Modifier
                        .background(if (isExpanded) Green else PurpleGrey40)
                        .size(200.dp)
                        .testTag("popup_content"))
                }
            }
        }

        waitForIdle()

        // The [Popup]'s collapse behavior seems to be triggered using an
        // Android API which is beyond the scope of Compose.
        // So, we manually click somewhere on the screen outside the popup.
        UiDevice.getInstance(getInstrumentation())
            .run {
                click(300, 300)
            }

        waitForIdle()

        assertFalse("Popup should have been collapsed", isExpanded)
        assertTrue("onCollapse should be called when clicking outside", collapseCalled)
    }
}
