package com.jeffreyalanwang.dutchrailwaysandroidclient.ui.screens.child

import androidx.compose.runtime.Composable
import com.jeffreyalanwang.dutchrailwaysandroidclient.PassService
import com.jeffreyalanwang.dutchrailwaysandroidclient.ui.NewPassServiceNavArgs

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

@Composable
fun ConfirmDeletePassServiceDialog(
    id: Int
) {

}