package com.jeffreyalanwang.dutchrailwaysandroidclient.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.AlignmentLine
import androidx.compose.ui.layout.Placeable
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertWidthIsEqualTo
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.width
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.jeffreyalanwang.dutchrailwaysandroidclient.TrainAmenity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ExpandableBadgeSetTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun `collapsed badge set should render items in a row`() {
        val badgesToLabels: Map<Any, Pair<@Composable () -> Unit, @Composable () -> Unit>> = mapOf(
            Badge.Amenity(TrainAmenity.WIFI) to Pair(
                @Composable { Box(Modifier.size(20.dp).testTag("badge_0")) },
                @Composable { Text("Label 0", Modifier.testTag("label_0")) }
            ),
            Badge.Amenity(TrainAmenity.TOILET) to Pair(
                @Composable { Box(Modifier.size(20.dp).testTag("badge_1")) },
                @Composable { Text("Label 1", Modifier.testTag("label_1")) }
            )
        )

        composeTestRule.setContent {
            ExpandableBadgeSet(
                isExpanded = false,
                onSetExpanded = {},
                collapsedBadgeSize = 20.dp,
                expandedBadgeSize = 40.dp,
                windowInsets = WindowInsets(0.dp),
                keyedBadgesToLabels = badgesToLabels
            )
        }

        val badge0X = composeTestRule.onNodeWithTag("badge_0", useUnmergedTree = true).getUnclippedBoundsInRoot().left
        val badge1X = composeTestRule.onNodeWithTag("badge_1", useUnmergedTree = true).getUnclippedBoundsInRoot().left
        
        assertTrue("Badge 1 ($badge1X) should be to the right of Badge 0 ($badge0X)", badge1X > badge0X)
    }

    @Test
    fun `expanded badge set should render items in a column`() {
        val badgesToLabels: Map<Any, Pair<@Composable () -> Unit, @Composable () -> Unit>> = mapOf(
            Badge.Amenity(TrainAmenity.WIFI) to Pair(
                @Composable { Box(Modifier.size(20.dp).testTag("badge_0")) },
                @Composable { Text("Label 0", Modifier.testTag("label_0")) }
            ),
            Badge.Amenity(TrainAmenity.TOILET) to Pair(
                @Composable { Box(Modifier.size(20.dp).testTag("badge_1")) },
                @Composable { Text("Label 1", Modifier.testTag("label_1")) }
            )
        )

        composeTestRule.setContent {
            ExpandableBadgeSet(
                isExpanded = true,
                onSetExpanded = {},
                collapsedBadgeSize = 20.dp,
                expandedBadgeSize = 40.dp,
                windowInsets = WindowInsets(0.dp),
                keyedBadgesToLabels = badgesToLabels
            )
        }

        composeTestRule.waitForIdle()

        val badge0Y = composeTestRule.onNodeWithTag("badge_0", useUnmergedTree = true).getUnclippedBoundsInRoot().top
        val badge1Y = composeTestRule.onNodeWithTag("badge_1", useUnmergedTree = true).getUnclippedBoundsInRoot().top
        
        assertTrue("Badge 1 ($badge1Y) should be below Badge 0 ($badge0Y)", badge1Y > badge0Y)
    }

    @Test
    fun `basicLinearLayout utility should correctly position items in a row`() {
        val util = ExpandableBadgeSetUtilScope
        val mockPlaceable1 = MockPlaceable(50, 50)
        val mockPlaceable2 = MockPlaceable(60, 40)
        val keyedPlaceables = listOf("1" to mockPlaceable1, "2" to mockPlaceable2)
        val gap = 10

        val (positions, size) = util.basicLinearLayout(LayoutAxis.Row, keyedPlaceables, gap)

        assertEquals(IntSize(120, 50), size)
        assertEquals(IntOffset(0, 0), positions["1"])
        assertEquals(IntOffset(60, 0), positions["2"])
    }

    @Test
    fun `basicLinearLayout utility should correctly position items in a column`() {
        val util = ExpandableBadgeSetUtilScope
        val mockPlaceable1 = MockPlaceable(50, 50)
        val mockPlaceable2 = MockPlaceable(60, 40)
        val keyedPlaceables = listOf("1" to mockPlaceable1, "2" to mockPlaceable2)
        val gap = 10

        val (positions, size) = util.basicLinearLayout(LayoutAxis.Column, keyedPlaceables, gap)

        assertEquals(IntSize(60, 100), size)
        assertEquals(IntOffset(0, 0), positions["1"])
        assertEquals(IntOffset(0, 60), positions["2"])
    }

    @Test
    fun `badge size should animate correctly during expansion`() {
        val badgesToLabels: Map<Any, Pair<@Composable () -> Unit, @Composable () -> Unit>> = mapOf(
            Badge.Amenity(TrainAmenity.WIFI) to Pair(
                // Use aspectRatio and fillMaxSize so it takes the height provided by the parent
                @Composable { Box(Modifier.aspectRatio(1f).fillMaxSize().testTag("badge_0")) },
                @Composable { Text("Label 0") }
            )
        )

        var expanded by mutableStateOf(false)

        composeTestRule.setContent {
            ExpandableBadgeSet(
                isExpanded = expanded,
                onSetExpanded = { expanded = it },
                collapsedBadgeSize = 20.dp,
                expandedBadgeSize = 40.dp,
                windowInsets = WindowInsets(0.dp),
                keyedBadgesToLabels = badgesToLabels
            )
        }

        // Wait for initial layout
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("badge_0", useUnmergedTree = true).assertWidthIsEqualTo(20.dp)

        // Start animation
        composeTestRule.mainClock.autoAdvance = false
        expanded = true
        
        // Recompose and advance few frames
        composeTestRule.mainClock.advanceTimeByFrame() // First frame
        composeTestRule.mainClock.advanceTimeBy(100)
        
        val node = composeTestRule.onNodeWithTag("badge_0", useUnmergedTree = true)
        val intermediateWidth = node.getUnclippedBoundsInRoot().width
        
        assertTrue("Expected width $intermediateWidth to be greater than 20dp", intermediateWidth > 20.dp)
        assertTrue("Expected width $intermediateWidth to be less than 40dp", intermediateWidth < 40.dp)

        // Complete animation
        composeTestRule.mainClock.autoAdvance = true
        composeTestRule.waitForIdle()
        
        composeTestRule.onNodeWithTag("badge_0", useUnmergedTree = true).assertWidthIsEqualTo(40.dp)
    }

    private class MockPlaceable(width: Int, height: Int) : Placeable() {
        init {
            measurementConstraints = Constraints(width, width, height, height)
        }
        override fun get(alignmentLine: AlignmentLine): Int = AlignmentLine.Unspecified
        override fun placeAt(position: IntOffset, zIndex: Float, layerBlock: (androidx.compose.ui.graphics.GraphicsLayerScope.() -> Unit)?) {}
    }
}
