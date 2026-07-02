package com.jeffreyalanwang.dutchrailwaysandroidclient.ui.components

import android.annotation.SuppressLint
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.dropShadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.shadow.Shadow
import androidx.compose.ui.unit.dp

/** Draws and animates enter/exit of background, serving as a scrim. */
@SuppressLint("ModifierParameter")
@Composable
fun GlowBox(
    isEnabled: Boolean = true,
    modifier: Modifier = Modifier.Companion,
    content: @Composable BoxScope.() -> Unit,
) {
    var isEnabledState by remember { mutableStateOf(false) }
    DisposableEffect(isEnabled) {
        isEnabledState = isEnabled
        onDispose {
            isEnabledState = false
        }
    }

    val progress by animateFloatAsState(
        if (isEnabledState) 1f else 0f,
        MaterialTheme.motionScheme.slowSpatialSpec(),
    )

    Box(
        modifier.dropShadow(
            MaterialTheme.shapes.small,
            Shadow(
                color = Color.White,
                alpha = 1f * progress,
                radius = 3.dp,
                spread = 2.dp * progress,
            ),
        ),
        content = content
    )
}