package com.jeffreyalanwang.dutchrailwaysandroidclient.ui.components

import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color.Companion.Green
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.round
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import com.jeffreyalanwang.dutchrailwaysandroidclient.letWith
import com.jeffreyalanwang.dutchrailwaysandroidclient.ui.theme.PurpleGrey40
import com.jeffreyalanwang.dutchrailwaysandroidclient.ui.util.OnChangeEffect
import com.jeffreyalanwang.dutchrailwaysandroidclient.ui.util.animateIntOffsetAsState
import com.jeffreyalanwang.dutchrailwaysandroidclient.ui.util.asRectInWindow
import com.jeffreyalanwang.dutchrailwaysandroidclient.ui.util.movedInto
import com.jeffreyalanwang.dutchrailwaysandroidclient.ui.util.toDp

/**
 * An expandable popup that automatically calculates its collapsed dimensions based on its initial size.
 *
 * This override must only be used if the popup will not be expanded on initial composition,
 * as it relies on measuring the content in its collapsed state first.
 *
 * @param isExpanded Whether the popup is currently expanded.
 * @param onCollapse Callback invoked to request collapsing the popup.
 * @param uncoercedExpandedOffset The target offset relative to the placeholder when expanded (before window coercion).
 * @param windowInsets The window insets used to keep the popup within safe bounds.
 * @param modifier Modifier applied to the placeholder container.
 * @param animationSpec Pair of animation specs for X and Y coordinate transitions.
 * @param animationStartedListener Optional callback invoked when an expansion/collapse animation starts.
 * @param animationFinishedListener Optional callback invoked when an expansion/collapse animation finishes.
 * @param content The composable content to be rendered inside the popup.
 */
@Composable
fun ExpandablePopup(
    isExpanded: Boolean,
    onCollapse: () -> Unit,
    uncoercedExpandedOffset: IntOffset,
    windowInsets: WindowInsets,
    modifier: Modifier = Modifier,
    animationSpec: Pair<FiniteAnimationSpec<Int>, FiniteAnimationSpec<Int>>,
    animationStartedListener: (() -> Unit)? = null,
    animationFinishedListener: (() -> Unit)? = null,
    content: @Composable () -> Unit,
) {
    var collapsedDimensions by remember { mutableStateOf(IntSize.Zero) }
    var popupSize by remember { mutableStateOf(IntSize.Zero) }

    ExpandablePopup(
        isExpanded,
        onCollapse,
        with (LocalDensity.current) { collapsedDimensions.toDp() },
        uncoercedExpandedOffset,
        popupSize,
        windowInsets,
        modifier,
        animationSpec,
        animationStartedListener,
        animationFinishedListener,
    ) {
        Box(
            Modifier.onSizeChanged {
                popupSize = it
                if (!isExpanded) collapsedDimensions = it
            }
        ) {
            content()
        }
    }
}

/**
 * An expandable popup that takes explicit collapsed dimensions.
 *
 * @param isExpanded Whether the popup is currently expanded.
 * @param onCollapse Callback invoked to request collapsing the popup.
 * @param collapsedDimensions The fixed size of the placeholder area when collapsed.
 * @param uncoercedExpandedOffset The target offset relative to the placeholder when expanded (before window coercion).
 * @param windowInsets The window insets used to keep the popup within safe bounds.
 * @param modifier Modifier applied to the placeholder container.
 * @param animationSpec Pair of animation specs for X and Y coordinate transitions.
 * @param animationStartedListener Optional callback invoked when an expansion/collapse animation starts.
 * @param animationFinishedListener Optional callback invoked when an expansion/collapse animation finishes.
 * @param content The composable content to be rendered inside the popup.
 */
@Composable
fun ExpandablePopup(
    isExpanded: Boolean,
    onCollapse: () -> Unit,
    collapsedDimensions: DpSize,
    uncoercedExpandedOffset: IntOffset,
    windowInsets: WindowInsets,
    modifier: Modifier = Modifier,
    animationSpec: Pair<FiniteAnimationSpec<Int>, FiniteAnimationSpec<Int>>,
    animationStartedListener: (() -> Unit)? = null,
    animationFinishedListener: (() -> Unit)? = null,
    content: @Composable () -> Unit,
) {
    var popupSize by remember { mutableStateOf(IntSize.Zero) }

    ExpandablePopup(
        isExpanded,
        onCollapse,
        collapsedDimensions,
        uncoercedExpandedOffset,
        popupSize,
        windowInsets,
        modifier,
        animationSpec,
        animationStartedListener,
        animationFinishedListener,
    ) {
        Box(
            Modifier.onSizeChanged {
                popupSize = it
            }
        ) {
            content()
        }
    }
}

@Composable
private fun ExpandablePopup(
    isExpanded: Boolean,
    onCollapse: () -> Unit,
    collapsedDimensions: DpSize,
    uncoercedExpandedOffset: IntOffset,
    popupSize: IntSize,
    windowInsets: WindowInsets,
    modifier: Modifier = Modifier,
    animationSpec: Pair<FiniteAnimationSpec<Int>, FiniteAnimationSpec<Int>>,
    animationStartedListener: (() -> Unit)? = null,
    animationFinishedListener: (() -> Unit)? = null,
    content: @Composable () -> Unit,
) {
    // Note, if we start providing [animationStartedListener]
    // during an animation, it will not be invoked.
    animationStartedListener?.let {
        OnChangeEffect(isExpanded) { it() }
    }

    val boundsInWindow = windowInsets
        .asRectInWindow(LocalDensity.current, LocalResources.current.displayMetrics)
    var containerInWindow by remember { mutableStateOf(IntOffset.Zero) }

    val popupOffset by animateIntOffsetAsState(
        if (!isExpanded) IntOffset.Zero
        else IntRect(uncoercedExpandedOffset, popupSize)
            .letWith(LocalLayoutDirection.current) {
                val boundsFromContainer = boundsInWindow.translate(-containerInWindow)
                it.movedInto(boundsFromContainer)
            }
            .topLeft,
        animationSpec,
        "ExpandablePopupAnimation",
        animationFinishedListener?.let { { it() } },
    )

    // Serves as a placeholder when the content expands
    Box(
        modifier
            .size(collapsedDimensions)
            .onGloballyPositioned {
                containerInWindow = it.positionInWindow().round()
            }
            .wrapContentSize(Alignment.TopStart, unbounded = true)
    ) {
        if ( popupOffset == IntOffset.Zero ) {
            // Avoid using popup when collapsed so composables
            // with higher z-index are not obscured.
            content()
        } else {
            // [Popup()] allows us to display above all other
            // elements on the screen, and also allows us to
            // listen for clicks outside the element.
            //
            // By placing inside the placeholder box, we can
            // provide offset values relative to our parent layout.
            Popup(
                onDismissRequest = { onCollapse() },
                offset = popupOffset,
                alignment = Alignment.TopStart,
                properties = PopupProperties(
                    dismissOnClickOutside = true,
                    clippingEnabled = false,

                    // [focusable] seems required to use [onDismissRequest],
                    // but blocks any other click listeners on the screen
                    focusable = isExpanded,
                ),
            ) {
                content()
            }
        }
    }
}