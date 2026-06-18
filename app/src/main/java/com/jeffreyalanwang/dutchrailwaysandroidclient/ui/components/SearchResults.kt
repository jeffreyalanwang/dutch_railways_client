package com.jeffreyalanwang.dutchrailwaysandroidclient.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.jeffreyalanwang.dutchrailwaysandroidclient.Area
import com.jeffreyalanwang.dutchrailwaysandroidclient.BackendApi
import com.jeffreyalanwang.dutchrailwaysandroidclient.PassService
import com.jeffreyalanwang.dutchrailwaysandroidclient.Place
import com.jeffreyalanwang.dutchrailwaysandroidclient.Station
import com.jeffreyalanwang.dutchrailwaysandroidclient.ui.util.AppIcons
import com.jeffreyalanwang.dutchrailwaysandroidclient.ui.util.AppStringFormats
import kotlin.reflect.KClass

@Composable
private fun <T: Place> ResultCaption(resultItem: T)
    = when (resultItem) {
        is Station -> Text(resultItem.address)
        else -> {}
    }

@Composable
private fun ResultCaption(resultItem: PassService)
    = resultItem
        .getStops()
        .first()
        .run {
            "From ${getStation().name} at ${AppStringFormats.Time(departure!!)}"
        }

@Composable
private fun <T: Place> ResultIcon(resultItem: T)
    = Icon(
        painterResource(AppIcons.PlaceType(resultItem::class)),
        contentDescription = when (resultItem) {
            is Station -> "Station"
            is Area -> "Area"
            else -> "Place"
        },
    )

@Composable
private fun ResultIcon(resultItem: PassService)
    = Icon(
        painterResource(AppIcons.Trainset(resultItem.trainset)),
        contentDescription = when (resultItem) {
            is Station -> "Station"
            is Area -> "Area"
            else -> "Place"
        },
    )

@Composable
fun <T: Place> PlaceSearchResults(
    cls: KClass<T>,
    query: String,
    onResultClick: (Int, String) -> Unit,
    itemModifier: Modifier = Modifier,
) {
    BackendApi.autocomplete_place(cls, query)
        .forEach {
            ListItem(
                headlineContent = { Text(it.name) },
                supportingContent = { ResultCaption(it) },
                leadingContent = { ResultIcon(it) },
                colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                modifier = itemModifier
                        .clickable { onResultClick(it.id, it.name) }
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp),
            )
        }
}

@Composable
fun PassServiceSearchResults(
    query: String,
    onResultClick: (Int, String) -> Unit,
    itemModifier: Modifier = Modifier
) {
    BackendApi.autocomplete_pass_service(query)
        .forEach {
            ListItem(
                headlineContent = { Text(it.title) },
                supportingContent = { ResultCaption(it) },
                leadingContent = { ResultIcon(it) },
                colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                modifier = itemModifier
                    .clickable { onResultClick(it.id, it.title) }
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
            )
        }
}