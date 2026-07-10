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
import com.jeffreyalanwang.dutchrailwaysandroidclient.Area
import backend.BackendApi
import com.jeffreyalanwang.dutchrailwaysandroidclient.Place
import com.jeffreyalanwang.dutchrailwaysandroidclient.Station
import com.jeffreyalanwang.dutchrailwaysandroidclient.ui.util.AppIcons

const val PLACE_SEARCH_PLACEHOLDER = "Search places"
const val AREA_SEARCH_PLACEHOLDER = "Search areas"
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
    textFieldState: TextFieldState,
    searchBarState: SearchBarState,
    noinline onClose: () -> Unit,
    noinline onSelectResult: (T?) -> Unit,
    noinline onClearedText: () -> Unit = {
        textFieldState.clearText()
        onSelectResult(null)
    },
    modifier: Modifier = Modifier,
    placeholderText: String = when (T::class) {
        Station::class -> STATION_SEARCH_PLACEHOLDER
        Area::class -> AREA_SEARCH_PLACEHOLDER
        else -> PLACE_SEARCH_PLACEHOLDER
    }
) {
    ExpandedSearch(
        results = BackendApi.autocomplete_place(
            T::class,
            textFieldState.text.toString()
        ),
        resultToText = { it.name },
        placeholderText = placeholderText,
        textFieldState = textFieldState,
        searchBarState = searchBarState,
        modifier = modifier,
        onClose = onClose,
        onSelectResult = onSelectResult,
        onClearedText = onClearedText,
    ) { place, onClick ->
        SearchResult(place, onClick)
    }
}