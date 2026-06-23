package com.jeffreyalanwang.dutchrailwaysandroidclient.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun FormField(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    isError: Boolean = false,
    trailingIcon: @Composable (() -> Unit)? = null,
    content: @Composable () -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    Box(
        modifier
            .height(IntrinsicSize.Min)
            .width(IntrinsicSize.Max)
    ) {
        OutlinedTextFieldDefaults.Container(
            enabled = true,
            interactionSource = interactionSource,
            isError = isError,
            modifier = Modifier
                .fillMaxSize()
                .clickable(
                    interactionSource = interactionSource,
                    indication = null,
                    onClick = onClick,
                    enabled = enabled,
                ),
        )
        Row(
            Modifier.padding(4.dp),
            Arrangement.spacedBy(4.dp),
            Alignment.CenterVertically,
        ) {
            Box(Modifier.padding(4.dp)) {
                content()
                trailingIcon?.invoke()
            }
        }
    }
}