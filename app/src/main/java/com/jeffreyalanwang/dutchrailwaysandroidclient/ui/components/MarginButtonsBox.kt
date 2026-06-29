package com.jeffreyalanwang.dutchrailwaysandroidclient.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.widthIn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onLayoutRectChanged
import androidx.compose.ui.platform.LocalDensity
import com.jeffreyalanwang.dutchrailwaysandroidclient.ui.components.DiscreteGridRowScope.fill
import kotlin.math.max

/**
 * A box that holds a button on potentially either side of the content.
 *
 * If a button is only present on the left or right, the other side
 * receives enough padding to visually balance the content.
 */
@Composable
fun MarginButtonsBox(
    left: @Composable (BoxScope.() -> Unit)?,
    right: @Composable (BoxScope.() -> Unit)?,
    modifier: Modifier = Modifier,
    verticalAlignment: Alignment.Vertical = Alignment.Top,
    content: @Composable BoxScope.() -> Unit,
) {
    Row(
        modifier,
        verticalAlignment = verticalAlignment,
        horizontalArrangement = Arrangement.Center,
    ) {
        if ((left ?: right) == null) {
            Box(content = content)
        } else {
            var minMarginWidth by remember { mutableIntStateOf(0) } // in px; used to adjust when button only exists on one side

            Box(
                Modifier
                    .onLayoutRectChanged { rect ->
                        minMarginWidth = max(minMarginWidth, rect.width / 2)
                    }
                    .fillMaxHeight()
                    .widthIn(min = with(LocalDensity.current) { minMarginWidth.toDp() }),
                content = left ?: {}
            )
            Box(
                modifier = Modifier.fill(),
                content = content,
            )
            Box(
                Modifier
                    .onLayoutRectChanged { rect ->
                        minMarginWidth = max(minMarginWidth, rect.width / 2)
                    }
                    .fillMaxHeight()
                    .widthIn(min = with(LocalDensity.current) { minMarginWidth.toDp() }),
                content = right ?: {}
            )
        }
    }
}