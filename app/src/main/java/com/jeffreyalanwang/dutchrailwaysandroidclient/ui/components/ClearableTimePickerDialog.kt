package com.jeffreyalanwang.dutchrailwaysandroidclient.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.jeffreyalanwang.dutchrailwaysandroidclient.letWith
import com.jeffreyalanwang.dutchrailwaysandroidclient.toLocalTime
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

@Preview(widthDp = 350, heightDp = 500)
@Composable
private fun ClearableTimePickerDialogPreview() {
    var time by remember { mutableStateOf<LocalTime?>(null) }
    var openDialog by remember { mutableStateOf(false) }

    Column(
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(time.toString(), Modifier.padding(20.dp))
        Button(
            onClick = { openDialog = true }
        ) {
            Text("Open dialogue")
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
) {
    val _initialTime = initialTime
        ?: Clock.System.now()
            .letWith(TimeZone.currentSystemDefault()) { it.toLocalTime() }

    val timePickerState = rememberTimePickerState(
        initialHour = _initialTime.hour,
        initialMinute = _initialTime.minute,
    )

    Dialog(
        onDismissRequest = onDismiss
    ) {
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

                TimePicker(timePickerState)

                Row {
                    TextButton(onClick = { onConfirm(null) }) {
                        Text("Clear and exit")
                    }

                    Spacer(Modifier.weight(1f))

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
}