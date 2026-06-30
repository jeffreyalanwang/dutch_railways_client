package com.jeffreyalanwang.dutchrailwaysandroidclient.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimeInput
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.Placeable
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.constrainWidth
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastSumBy
import androidx.compose.ui.window.Dialog
import androidx.navigation3.runtime.result.LocalResultEventBus
import com.jeffreyalanwang.dutchrailwaysandroidclient.R
import com.jeffreyalanwang.dutchrailwaysandroidclient.dropAt
import com.jeffreyalanwang.dutchrailwaysandroidclient.letWith
import com.jeffreyalanwang.dutchrailwaysandroidclient.toLocalTime
import com.jeffreyalanwang.dutchrailwaysandroidclient.ui.TimePickerNavArgs
import com.jeffreyalanwang.dutchrailwaysandroidclient.ui.util.DialogResult
import com.jeffreyalanwang.dutchrailwaysandroidclient.ui.util.id
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import kotlin.time.Clock

/**
 * Requires [ResultEventBusNavEntryDecorator] to be used.
 */
@Composable
fun TimePicker(
    navArgs: TimePickerNavArgs<*>,
    onNavigateBack: () -> Unit,
) {
    val resultBus = LocalResultEventBus.current
    if (navArgs.clearable) {
        ClearableTimePicker(
            title = navArgs.title,
            initialTime = navArgs.initialTime,
            onDismiss = onNavigateBack,
            onConfirm = {
                resultBus.sendResult(
                    DialogResult(it, navArgs.tag)
                )
                onNavigateBack()
            },
            enableKeyboard = navArgs.enableKeyboard,
        )
    } else {
        NonClearableTimePicker(
            title = navArgs.title,
            initialTime = navArgs.initialTime,
            onDismiss = onNavigateBack,
            onConfirm = {
                resultBus.sendResult(
                    DialogResult(it, navArgs.tag)
                )
                onNavigateBack()
            },
            enableKeyboard = navArgs.enableKeyboard,
        )
    }
}

@Preview(widthDp = 350, heightDp = 500)
@Composable
private fun ClearableTimePickerDialogPreview() {
    var time by remember { mutableStateOf<LocalTime?>(null) }
    var openDialog by remember { mutableStateOf(true) }

    Column(
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(time.toString(), Modifier.padding(20.dp))
        Button(
            onClick = { openDialog = true }
        ) {
            Text("Open dialog")
        }
    }

    if (openDialog) {
        ClearableTimePickerDialog(
            initialTime =  time,
            onConfirm = { time = it; openDialog = false },
            onDismiss = { openDialog = false }
        )
    }
}

@Composable
fun ClearableTimePickerDialog(
    initialTime: LocalTime?,
    onConfirm: (LocalTime?) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    title: String? = null,
    enableKeyboard: Boolean = true,
) {
    Dialog(onDismissRequest = onDismiss) {
        ClearableTimePicker(
            initialTime,
            onConfirm,
            onDismiss,
            modifier,
            title,
            enableKeyboard,
        )
    }
}

@Composable
fun ClearableTimePicker(
    initialTime: LocalTime?,
    onConfirm: (LocalTime?) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    title: String? = null,
    enableKeyboard: Boolean = true,
) = TimePickerWithExtras(
        initialTime = initialTime,
        onConfirm = onConfirm,
        onDismiss = onDismiss,
        modifier = modifier,
        title = title,
        enableKeyboard = enableKeyboard,
    ) {
        TextButton(onClick = { onConfirm(null) }) {
            Text("Clear and exit")
        }
    }

@Composable
fun NonClearableTimePicker(
    initialTime: LocalTime?,
    onConfirm: (LocalTime) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    title: String? = null,
    enableKeyboard: Boolean = true,
) = TimePickerWithExtras(
        initialTime = initialTime,
        onConfirm = { onConfirm(it!!) },
        onDismiss = onDismiss,
        modifier = modifier,
        title = title,
        enableKeyboard = enableKeyboard,
        bottomRowExtras = {},
    )

@Composable
fun TimePickerWithExtras(
    initialTime: LocalTime?,
    onConfirm: (LocalTime) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    title: String? = null,
    enableKeyboard: Boolean = true,
    bottomRowExtras: @Composable RowScope.() -> Unit,
) {
    var isUsingDial by rememberSaveable { mutableStateOf(true) }
    val _initialTime = initialTime
        ?: Clock.System.now()
            .letWith(TimeZone.currentSystemDefault()) { it.toLocalTime() }

    val timePickerState = rememberTimePickerState(
        initialHour = _initialTime.hour,
        initialMinute = _initialTime.minute,
    )

    Surface(
        shape = MaterialTheme.shapes.extraLarge,
        tonalElevation = 6.dp,
        modifier = modifier,
    ) {
        Column(Modifier.padding(24.dp)) {
            title?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.labelMedium,
                )
                Spacer(Modifier.height(20.dp))
            }

            if (isUsingDial) {
                TimePicker(timePickerState)
            } else {
                TimeInput(timePickerState)
            }

            BottomRowLayout(Modifier.fillMaxWidth()) {
                if (enableKeyboard) {
                    IconButton(
                        onClick = { isUsingDial = !isUsingDial }
                    ) {
                        if (isUsingDial) Icon(
                            painterResource(R.drawable.ic_keyboard),
                            contentDescription = "Use keyboard",
                        ) else Icon(
                            painterResource(R.drawable.ic_clock),
                            contentDescription = "Use dial",
                        )
                    }
                }

                Extras(bottomRowExtras)

                Spacer(Modifier.fill())

                TextButton(onClick = onDismiss) {
                    Text("Cancel")
                }

                TextButton( onClick = { onConfirm(
                    LocalTime(timePickerState.hour, timePickerState.minute)
                ) } ) {
                    Text("OK")
                }
            }
        }
    }
}

@Composable
private fun Modifier.fill() = id(1)

@Composable
private fun Extras(content: @Composable RowScope.() -> Unit)
    = Row(
        Modifier.id(0),
        Arrangement.Start,
        Alignment.CenterVertically,
        content = content
    )

@Composable
private fun BottomRowLayout(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) = Layout(content, modifier) { measurables, constraints ->
        val placeables = measurables.map { it.measure(constraints.copyMaxDimensions()) }

        val extrasRowIndex = placeables.indexOfFirst { it.id == 0 }
        val (row1: Placeable?, row2: List<Placeable>) =
            if (placeables.fastSumBy { it.width } > constraints.maxWidth) {
                // wrap Extras up a row
                placeables[extrasRowIndex] to placeables.dropAt(extrasRowIndex)
            } else {
                null to placeables
            }

        val fillSpacerIndex = row2.indexOfFirst { it.id == 1 }
        val containerWidth =
            constraints.constrainWidth(
                maxOf(
                    row1?.width ?: 0,
                    row2.fastSumBy { it.width },
                )
            )

        layout(
            width = containerWidth,
            height =
                ( row1?.height ?: 0 ) +
                row2.maxOf { it.height },
        ) {

            var y = 0
            row1?.let {
                it.place(0, 0)
                y += it.height
            }

            row2.take(fillSpacerIndex + 1) // include fillSpacer
                .fold(0) { x, placeable ->
                    placeable.place(x, y)
                    x + placeable.width
                }

            row2.drop(fillSpacerIndex + 1)
                .foldRight(containerWidth) { placeable, x ->
                    val newX = x - placeable.width
                    placeable.place(newX, y)
                    newX
                }

        }
    }