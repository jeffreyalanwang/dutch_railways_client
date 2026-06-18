package com.jeffreyalanwang.dutchrailwaysandroidclient.ui.screens.child

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.jeffreyalanwang.dutchrailwaysandroidclient.BackendApi
import com.jeffreyalanwang.dutchrailwaysandroidclient.PassService
import com.jeffreyalanwang.dutchrailwaysandroidclient.R
import com.jeffreyalanwang.dutchrailwaysandroidclient.ui.NewPassServiceNavArgs
import com.jeffreyalanwang.dutchrailwaysandroidclient.ui.components.IconWithBadge
import kotlinx.coroutines.launch

@Composable
fun NewPassServiceScreen(
    basedOnService: PassService? = null,
    onNavigate: (NewPassServiceNavArgs) -> Unit,
) {

}

@Composable
fun EditPassServiceScreen(
    id: Int
) {

}

@Preview(widthDp = 500, heightDp = 1000)
@Composable
fun ConfirmDeletePassServicePreview(serviceId: Int = 119) {
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
            onNavigateBack = { notify("Back") },
            onClearStack = { notify("Clear stack") },
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
    onNavigateBack: () -> Unit,
    onClearStack: () -> Unit,
) {
    Card(shape = MaterialTheme.shapes.large) {
        Column(
            Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            IconWithBadge(
                icon = painterResource(R.drawable.ic_dr_trainservice),
                badge = painterResource(R.drawable.ic_trash),
                modifier = Modifier
                    .padding(top = 16.dp),
            )
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
                        onClearStack()
                    },
                ) { Text("Yes") }
                TextButton(
                    shape = MaterialTheme.shapes.medium,
                    onClick = { onNavigateBack() },
                ) { Text("No") }
            }
        }
    }
}