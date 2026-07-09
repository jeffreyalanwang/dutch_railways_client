package com.jeffreyalanwang.dutchrailwaysandroidclient.ui.components

import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.painterResource
import com.jeffreyalanwang.dutchrailwaysandroidclient.R

@Composable
fun NavBackButton(
    onClick: () -> Unit,
    contentDescription: String = "Back",
) = IconButton(onClick = onClick) {
        Icon(
            painterResource(R.drawable.ic_back),
            contentDescription = contentDescription,
        )
    }

@Composable
fun SaveChangesButton(
    onClick: () -> Unit,
    contentDescription: String = "Finish & save",
) = IconButton(onClick) {
        Icon(
            painterResource(R.drawable.ic_done),
            contentDescription = contentDescription,
        )
    }