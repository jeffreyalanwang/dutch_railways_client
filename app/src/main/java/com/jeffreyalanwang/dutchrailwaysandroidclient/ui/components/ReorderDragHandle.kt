package com.jeffreyalanwang.dutchrailwaysandroidclient.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType.Companion.GestureEnd
import androidx.compose.ui.hapticfeedback.HapticFeedbackType.Companion.GestureThresholdActivate
import androidx.compose.ui.res.painterResource
import com.jeffreyalanwang.dutchrailwaysandroidclient.R
import sh.calvin.reorderable.ReorderableListItemScope

@Composable
fun ReorderableListItemScope.ReorderDragHandle(
    hapticFeedback: HapticFeedback?,
    modifier: Modifier = Modifier
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .draggableHandle(
                onDragStarted = {
                    hapticFeedback?.performHapticFeedback(
                        GestureThresholdActivate
                    )
                },
                onDragStopped = {
                    hapticFeedback?.performHapticFeedback(
                        GestureEnd
                    )
                },
            )
            .fillMaxHeight()
    ) {
        Icon(
            painterResource(R.drawable.ic_draghandle_vertical),
            contentDescription = "Reorder",
        )
    }
}
