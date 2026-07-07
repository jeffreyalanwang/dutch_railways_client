package com.jeffreyalanwang.dutchrailwaysandroidclient.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.height
import androidx.compose.ui.unit.width
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ExpandingHeroBoxTest {

    @Test
    fun expandingHeroBox_initialState_isCollapsed() = runComposeUiTest {
        setContent {
            ExpandingHeroBox(
                isExpanded = false,
                onDismissRequest = {},
                modifier = Modifier.size(100.dp)
            ) {
                Box(Modifier.testTag("content").fillMaxSize()) {
                    Text("Inside Content")
                }
            }
        }

        onNodeWithTag("content").assertIsDisplayed()
        onNodeWithText("Inside Content").assertIsDisplayed()
    }

    @Test
    fun expandingHeroBox_boundsInterpolation() = runComposeUiTest {
        var isExpanded by mutableStateOf(false)
        val collapsedBounds = IntRect(100, 200, 300, 400) // 200x200 at (100, 200) px
        
        setContent {
            ExpandingHeroBox(
                isExpanded = isExpanded,
                onDismissRequest = { isExpanded = false },
                collapsedBounds = { collapsedBounds },
                modifier = Modifier.testTag("hero_box")
            ) {
                Box(Modifier.fillMaxSize().testTag("inner_content"))
            }
        }

        mainClock.autoAdvance = false
        
        val rootBounds = onRoot().getUnclippedBoundsInRoot()
        val density = density

        // Initial state: Collapsed
        val initialBounds = onNodeWithTag("inner_content").getUnclippedBoundsInRoot()
        val densityVal = density
        println("Root bounds: $rootBounds")
        println("Initial bounds: $initialBounds")
        assertEquals(with(densityVal) { collapsedBounds.left.toDp().value }, initialBounds.left.value, 1f)
        assertEquals(with(densityVal) { collapsedBounds.top.toDp().value }, initialBounds.top.value, 1f)
        assertEquals(with(densityVal) { collapsedBounds.width.toDp().value }, initialBounds.width.value, 1f)
        assertEquals(with(densityVal) { collapsedBounds.height.toDp().value }, initialBounds.height.value, 1f)

        // Start Expansion
        isExpanded = true
        mainClock.advanceTimeByFrame() // Kick off the Animatable
        
        // Advance to a mid-point
        mainClock.advanceTimeBy(100) 
        val midBounds = onNodeWithTag("inner_content").getUnclippedBoundsInRoot()
        println("Mid bounds: $midBounds")
        
        // Verify it moved and resized from initial
        assertTrue("Width ${midBounds.width.value} should be greater than initial ${initialBounds.width.value}", midBounds.width.value > initialBounds.width.value)
        assertTrue("Left ${midBounds.left.value} should be less than initial ${initialBounds.left.value}", midBounds.left.value < initialBounds.left.value)

        // Finish Expansion
        mainClock.advanceTimeBy(1000)
        val finalBounds = onNodeWithTag("inner_content").getUnclippedBoundsInRoot()
        println("Final bounds: $finalBounds")
        
        // Verify it reached expanded state
        assertEquals(0f, finalBounds.left.value, 0.1f)
        assertEquals(0f, finalBounds.top.value, 0.1f)
        assertTrue("Final width ${finalBounds.width.value} should be >= mid width ${midBounds.width.value}", finalBounds.width.value >= midBounds.width.value)
        
        // Start Collapse
        isExpanded = false
        mainClock.advanceTimeByFrame()
        mainClock.advanceTimeBy(100)
        val midCollapseBounds = onNodeWithTag("inner_content").getUnclippedBoundsInRoot()
        
        // Verify it's shrinking back
        assertTrue("Collapse: Width ${midCollapseBounds.width.value} should be less than final ${finalBounds.width.value}", midCollapseBounds.width.value < finalBounds.width.value)
        assertTrue("Collapse: Left ${midCollapseBounds.left.value} should be greater than final ${finalBounds.left.value}", midCollapseBounds.left.value > finalBounds.left.value)

        // Finish Collapse
        mainClock.advanceTimeBy(1000)
        val endBounds = onNodeWithTag("inner_content").getUnclippedBoundsInRoot()
        
        assertEquals(with(densityVal) { collapsedBounds.left.toDp().value }, endBounds.left.value, 1f)
        assertEquals(with(densityVal) { collapsedBounds.width.toDp().value }, endBounds.width.value, 1f)
    }
}
