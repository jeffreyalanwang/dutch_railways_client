package com.jeffreyalanwang.dutchrailwaysandroidclient.ui.screens.child.edit

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.jeffreyalanwang.dutchrailwaysandroidclient.backend.BackendApi
import com.jeffreyalanwang.dutchrailwaysandroidclient.R
import com.jeffreyalanwang.dutchrailwaysandroidclient.ui.components.IconWithBadge
import kotlinx.coroutines.launch

@Preview(widthDp = 500, heightDp = 1000)
@Composable
private fun ConfirmDeletePassServicePreview(serviceId: Int = 119) {
    val service = BackendApi.get_pass_service(serviceId)

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    fun notify(message: String) = scope.launch {
        snackbarHostState.showSnackbar(
            message,
            withDismissAction = true,
        )
    }
    Dialog(onDismissRequest = {}) {
        ConfirmDeletePassService(
            id = service.id,
            serviceName = service.title,
            onCancelRequest = { notify("Back") },
            onDeleteFinished = { notify("Delete finished") },
        )
    }
    Box(Modifier.fillMaxSize()) {
        SnackbarHost(
            snackbarHostState,
            Modifier.align(Alignment.BottomCenter)
        )
    }
}

@Composable
fun ConfirmDeletePassService(
    id: Int,
    serviceName: String,
    onCancelRequest: () -> Unit,
    onDeleteFinished: () -> Unit,
) {
    Card(shape = MaterialTheme.shapes.large) {
        Column(
            Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            IconWithBadge(
                badge = painterResource(R.drawable.ic_close),
                sizeRatio = 2/3f,
                overlapRatio = .87f,
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .padding(top = 16.dp)
                    .alpha(0.875f)
                    .size(128.dp),
            ) {
                Icon(
                    painterResource(R.drawable.ic_dr_trainservice),
                    contentDescription = null,
                )
            }
            Text(
                text = "Delete the following train service?",
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .padding(horizontal = 8.dp),
            )
            Text(
                text = serviceName,
                style = MaterialTheme.typography.titleMediumEmphasized,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .padding(horizontal = 8.dp),
            )
            Row(
                modifier = Modifier.align(Alignment.End),
            ) {
                TextButton(
                    shape = MaterialTheme.shapes.medium,
                    onClick = {
                        BackendApi.delete_pass_service(id)
                        onDeleteFinished()
                    },
                ) { Text("Yes") }
                TextButton(
                    shape = MaterialTheme.shapes.medium,
                    onClick = onCancelRequest,
                ) { Text("No") }
            }
        }
    }
}