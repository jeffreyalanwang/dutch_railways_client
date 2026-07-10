package com.jeffreyalanwang.dutchrailwaysandroidclient.ui.search

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.clearText
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.SearchBarState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import backend.BackendApi
import com.jeffreyalanwang.dutchrailwaysandroidclient.PassService
import com.jeffreyalanwang.dutchrailwaysandroidclient.ui.util.AppIcons
import com.jeffreyalanwang.dutchrailwaysandroidclient.ui.util.AppStringFormats

const val PASS_SERVICE_SEARCH_PLACEHOLDER = "Search train services"

@Composable
fun SearchResult(
    item: PassService,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    ListItem(
        headlineContent = { Text(item.title) },
        supportingContent = {
            item.getStops()
                .first()
                .run {
                    "From ${getStation().name} at ${AppStringFormats.Time(departure!!)}"
                }
                .let { Text(it) }
        },
        leadingContent = {
            Icon(
                painterResource(AppIcons.Trainset(item.trainset)),
                contentDescription = item.trainset.name,
            )
        },
        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
        modifier = modifier
            .clickable(onClick = onClick)
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
    )
}

@Composable
inline fun <reified T: PassService> ExpandedSearch(
    textFieldState: TextFieldState,
    searchBarState: SearchBarState,
    noinline onClose: () -> Unit,
    noinline onSelectResult: (PassService?) -> Unit,
    noinline onClearedText: () -> Unit = {
        textFieldState.clearText()
        onSelectResult(null)
    }
) {
    ExpandedSearch(
        results = BackendApi.autocomplete_pass_service(textFieldState.text.toString()),
        resultToText = { it.title },
        placeholderText = PASS_SERVICE_SEARCH_PLACEHOLDER,
        textFieldState = textFieldState,
        searchBarState = searchBarState,
        onClose = onClose,
        onSelectResult = onSelectResult,
        onClearedText = onClearedText,
    ) { passService, onClick ->
        SearchResult(passService, onClick)
    }
}