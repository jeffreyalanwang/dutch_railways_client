package com.jeffreyalanwang.dutchrailwaysandroidclient.ui.components

import android.annotation.SuppressLint
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
inline fun ScrollableScaffold(
    noinline topBar: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    scrollState: ScrollState = rememberScrollState(),
    crossinline content: @Composable (PaddingValues) -> Unit,
) = Scaffold(
        topBar = topBar,
        modifier = modifier,
    ) { innerPadding ->
        Box(Modifier.verticalScroll(scrollState)) {
            content(innerPadding)
        }
    }

@Composable
fun CardContentScaffold(
    topBar: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    cardModifier: Modifier = Modifier,
    scrollState: ScrollState = rememberScrollState(),
    content: @Composable ColumnScope.() -> Unit,
) = ScrollableScaffold(
        topBar = topBar,
        modifier = modifier,
        scrollState = scrollState,
    ) { innerPadding ->
        Card(
            cardModifier
                .padding(innerPadding)
                .consumeWindowInsets(innerPadding)
                .padding(10.dp),
            content = content
        )
    }