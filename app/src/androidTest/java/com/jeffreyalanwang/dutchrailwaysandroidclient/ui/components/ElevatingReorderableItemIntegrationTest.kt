package com.jeffreyalanwang.dutchrailwaysandroidclient.ui.components

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.runtime.toMutableStateList
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.runComposeUiTest
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.jeffreyalanwang.dutchrailwaysandroidclient.isSorted
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toPersistentList
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import sh.calvin.reorderable.ReorderableColumn

private val defaultList = persistentListOf("Item 0", "Item 1", "Item 2")

@Preview
@Composable
fun ReorderableListForTest(
    items: List<String> = defaultList,
    onSettle: (from: Int, to: Int) -> Unit = { _, _ -> },
    onMove: () -> Unit = { }
) {
    ReorderableColumn(
        list = items,
        onSettle = onSettle,
        onMove = onMove
    ) { i, item, isDragging ->
        ElevatingReorderableItem(
            isDragging = isDragging,
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp)
                .testTag("row_$item")
        ) {
            Row {
                ReorderDragHandle(
                    hapticFeedback = null,
                    modifier = Modifier.testTag("handle_$item")
                )
                Text(item)
            }
        }
    }
}

@OptIn(ExperimentalTestApi::class)
@RunWith(AndroidJUnit4::class)
class ElevatingReorderableItemIntegrationTest {

    private lateinit var items: SnapshotStateList<String>

    @Before
    fun setUp() {
        items = defaultList.toMutableStateList()
    }

    @Test
    fun `dragging an item should trigger the onMove callback`() = runComposeUiTest {
            var moveCalled = false

        setContent {
                ReorderableListForTest(
                    items = items,
                    onMove = {
                        moveCalled = true
                    },
                )
            }

        val handle = onNodeWithTag("handle_${"Item 0"}")
        val targetRow = onNodeWithTag("row_${"Item 1"}")
            val start = handle.fetchSemanticsNode().positionInRoot
            val end = targetRow.fetchSemanticsNode().positionInRoot

        with (handle) {

            assertFalse(moveCalled)
            performTouchInput {
                down(center)
                mainClock.advanceTimeBy(1000)
                moveTo(center + Offset(0f, end.y - start.y + 100f))
                up()
            }
            assertTrue("onMove should have been called at least once", moveCalled)

            }
        }

    @Test
    fun `releasing a dragged item should trigger the onSettle callback`() = runComposeUiTest {
        var moveCalled = false
        var settleCalled = false

        setContent {
            ReorderableListForTest(
                items = items,
                onMove = {
                    moveCalled = true
                },
                onSettle = { from, to ->
                    settleCalled = true
                    val item = items.removeAt(from)
                    items.add(to, item)
                },
            )
        }

        val handle = onNodeWithTag("handle_${"Item 0"}")
        val targetRow = onNodeWithTag("row_${"Item 1"}")
        val start = handle.fetchSemanticsNode().positionInRoot
        val end = targetRow.fetchSemanticsNode().positionInRoot

        with (handle) {

            assertFalse(moveCalled)
            performTouchInput {
                down(center)
                mainClock.advanceTimeBy(1000)
                moveTo(center + Offset(0f, end.y - start.y + 100f))
            }
            assertTrue(
                "onMove should have been called at least once",
                moveCalled
            )

            assertFalse(settleCalled)
            performTouchInput {
                up()
            }
            waitForIdle()
            assertTrue("onSettle was not called", settleCalled)

        }
    }

    @Test
    fun `reordering items should update both the underlying list and the UI`() = runComposeUiTest {
        var settleCalled = false
        var fromIndex = -1
        var toIndex = -1

        setContent {
            ReorderableListForTest(
                items = items,
                onSettle = { from, to ->
                    settleCalled = true
                    fromIndex = from
                    toIndex = to
                    val item = items.removeAt(from)
                    items.add(to, item)
                }
            )
        }

        val handle = onNodeWithTag("handle_${"Item 0"}")
        val targetRow = onNodeWithTag("row_${"Item 1"}")
        val start = handle.fetchSemanticsNode().positionInRoot
        val end = targetRow.fetchSemanticsNode().positionInRoot
        handle.performTouchInput {
            down(center)
            advanceEventTime(1000)
            moveTo(center + Offset(0f, end.y - start.y + 100f))
            up()
        }
        waitForIdle()

        // The assertions below verify the wiring.
        // Note: Real drag behavior may be sensitive to timing/distance in CI/Emulators.
        assertTrue("onSettle was not called", settleCalled)
        assertTrue("Item was not moved down in list", fromIndex < toIndex)

        defaultList
            .map { "row_$it" }
            .map { onNodeWithTag(it) }
            .map { it.fetchSemanticsNode().positionInRoot.y }
            .let {
                assertTrue("Items should have moved in UI", !it.isSorted())
            }

        assertTrue("Items should have moved in list", items.toPersistentList() != defaultList)
    }

}
