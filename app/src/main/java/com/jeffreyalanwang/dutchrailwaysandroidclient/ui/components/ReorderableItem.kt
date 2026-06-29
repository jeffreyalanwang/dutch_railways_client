package com.jeffreyalanwang.dutchrailwaysandroidclient.ui.components

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import sh.calvin.reorderable.ReorderableListItemScope
import sh.calvin.reorderable.ReorderableListScope

@Composable
fun ReorderableListScope.ElevatingReorderableItem(
    isDragging: Boolean,
    vararg keys: Any?,
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.surface,
    tonalElevation: Dp = 0.dp,
    content: @Composable ReorderableListItemScope.() -> Unit
) = ReorderableItem(keys) {
    val elevation by animateDpAsState(if (isDragging) 4.dp else 0.dp)

    Surface(
        modifier,
        color = color,
        tonalElevation = tonalElevation,
        shadowElevation = elevation,
    ) { content() }
}

@Composable
fun ReorderableListScope.ReorderableItem(
    vararg keys: Any?,
    modifier: Modifier = Modifier,
    content: @Composable ReorderableListItemScope.() -> Unit
) = key(keys) { ReorderableItem(modifier, content) }