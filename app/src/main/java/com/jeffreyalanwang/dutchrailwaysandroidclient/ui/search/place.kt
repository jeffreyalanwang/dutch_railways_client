package com.jeffreyalanwang.dutchrailwaysandroidclient.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.clearText
import androidx.compose.foundation.text.input.setTextAndPlaceCursorAtEnd
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExpandedFullScreenSearchBar
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.SearchBarDefaults
import androidx.compose.material3.SearchBarState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.unit.dp
import com.jeffreyalanwang.dutchrailwaysandroidclient.Area
import com.jeffreyalanwang.dutchrailwaysandroidclient.BackendApi
import com.jeffreyalanwang.dutchrailwaysandroidclient.PassService
import com.jeffreyalanwang.dutchrailwaysandroidclient.Place
import com.jeffreyalanwang.dutchrailwaysandroidclient.R
import com.jeffreyalanwang.dutchrailwaysandroidclient.Station
import com.jeffreyalanwang.dutchrailwaysandroidclient.ui.util.AppIcons
import com.jeffreyalanwang.dutchrailwaysandroidclient.ui.util.AppStringFormats
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

const val STATION_SEARCH_PLACEHOLDER = "Search stations"

@Composable
fun <T: Place> SearchResult(
    item: T,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    ListItem(
        headlineContent = { Text(item.name) },
        supportingContent = when (item) {
            is Station -> {
                { Text(item.address) }
            }
            else -> null
        },
        leadingContent = {
            Icon(
                painterResource(AppIcons.PlaceType(item::class)),
                contentDescription = when (item) {
                    is Station -> "Station"
                    is Area -> "Area"
                    else -> "Place"
                },
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
inline fun <reified T: Place> ExpandedSearch(
    scope: CoroutineScope,
    textFieldState: TextFieldState,
    searchBarState: SearchBarState,
    noinline onResultSelected: (T?) -> Unit,
    noinline onClearedText: () -> Unit = {
        textFieldState.clearText()
        onResultSelected(null)
    }
) {
    ExpandedSearch(
        results = BackendApi.autocomplete_place(T::class, textFieldState.text.toString()),
        resultToText = { it.name },
        textFieldState = textFieldState,
        searchBarState = searchBarState,
        onClose = { scope.launch { searchBarState.animateToCollapsed() } },
        onResultSelected = onResultSelected,
        onClearedText = onClearedText,
    ) { station, onClick ->
        SearchResult(station, onClick)
    }
}

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
    scope: CoroutineScope,
    textFieldState: TextFieldState,
    searchBarState: SearchBarState,
    noinline onResultSelected: (PassService?) -> Unit,
    noinline onClearedText: () -> Unit = {
        textFieldState.clearText()
        onResultSelected(null)
    }
) {
    ExpandedSearch(
        results = BackendApi.autocomplete_pass_service(textFieldState.text.toString()),
        resultToText = { it.title },
        textFieldState = textFieldState,
        searchBarState = searchBarState,
        onClose = { scope.launch { searchBarState.animateToCollapsed() } },
        onResultSelected = onResultSelected,
        onClearedText = onClearedText,
    ) { station, onClick ->
        SearchResult(station, onClick)
    }
}