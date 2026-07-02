package com.jeffreyalanwang.dutchrailwaysandroidclient.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import sh.calvin.reorderable.ReorderableListItemScope
import sh.calvin.reorderable.ReorderableListScope

@Composable
fun ReorderableListScope.ElevatingReorderableItem(
    isDragging: Boolean,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues.Zero,
    shape: Shape = RectangleShape,
    restingBgColor: Color = Color.Transparent,
    draggingBgColor: Color = MaterialTheme.colorScheme.surface,
    tonalElevation: Dp = 0.dp,
    content: @Composable ReorderableListItemScope.() -> Unit
) {
    val elevation by animateDpAsState(
        if (isDragging) 4.dp else 0.dp,
        animationSpec = MaterialTheme.motionScheme.slowEffectsSpec(),
    )
    val color by animateColorAsState(
        if (isDragging) draggingBgColor else restingBgColor,
        animationSpec = MaterialTheme.motionScheme.fastEffectsSpec()
    )

    ReorderableItem {
        Surface(
            modifier,
            color = color,
            shape = shape,
            tonalElevation = tonalElevation,
            shadowElevation = elevation,
        ) {
            Box(Modifier.padding(contentPadding)) {
                content()
            }
        }
    }
}

