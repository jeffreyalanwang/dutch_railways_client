package com.jeffreyalanwang.dutchrailwaysandroidclient.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class DiscreteGridRowTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun `cellAlign update should reposition item`() {
        var alignment by mutableStateOf(Alignment.Start)
        val control = DiscreteGridControl()

        composeTestRule.setContent {
            Column(Modifier.width(200.dp)) {
                // First row sets the column width to 100dp
                DiscreteGridRow(discreteGridControl = control) {
                    Box(Modifier.size(100.dp, 10.dp))
                }
                // Second row has a 50dp item that we'll align
                DiscreteGridRow(discreteGridControl = control) {
                    Box(
                        Modifier
                            .size(50.dp, 10.dp)
                            .cellAlign(alignment)
                            .testTag("item")
                    )
                }
            }
        }

        fun assertLeft(expected: Float) {
            val actual = composeTestRule.onNodeWithTag("item").getUnclippedBoundsInRoot().left.value
            assertEquals("Expected left $expected but was $actual", expected, actual, 1.1f)
        }

        // Column width is 100dp. Item is 50dp.
        // Start alignment -> 0dp
        assertLeft(0f)

        alignment = Alignment.End
        composeTestRule.waitForIdle()

        // End alignment -> 100dp - 50dp = 50dp
        assertLeft(50f)
        
        alignment = Alignment.CenterHorizontally
        composeTestRule.waitForIdle()
        
        // Center alignment -> (100dp - 50dp) / 2 = 25dp
        assertLeft(25f)
    }

    @Test
    fun `fill should take remaining space`() {
        val control = DiscreteGridControl()
        composeTestRule.setContent {
            Box(Modifier.width(300.dp)) {
                DiscreteGridRow(
                    discreteGridControl = control,
                    modifier = Modifier.fillMaxWidth(),
                    gap = 10.dp,
                    fillCellWidth = true
                ) {
                    Box(Modifier.size(50.dp).testTag("left"))
                    Box(Modifier.fill().testTag("center"))
                    Box(Modifier.size(50.dp).testTag("right"))
                }
            }
        }

        composeTestRule.waitForIdle()

        // total(300) - rigid(50+50) - gaps(10+10) = 180
        // Use a bit of tolerance because of pixel snapping and density
        val centerBounds = composeTestRule.onNodeWithTag("center").getUnclippedBoundsInRoot()
        val centerWidth = (centerBounds.right - centerBounds.left).value
        assertTrue("Expected width approx 180dp but was $centerWidth", centerWidth >= 178f && centerWidth <= 183f)
        
        fun assertLeft(tag: String, expected: Float) {
            val actual = composeTestRule.onNodeWithTag(tag).getUnclippedBoundsInRoot().left.value
            assertEquals("Expected left $expected for $tag but was $actual", expected, actual, 1.1f)
        }

        // Positions:
        // left: 0
        // center: 50 + 10 = 60
        // right: 300 - 50 = 250
        assertLeft("left", 0f)
        assertLeft("center", 60f)
        assertLeft("right", 250f)
    }
}
