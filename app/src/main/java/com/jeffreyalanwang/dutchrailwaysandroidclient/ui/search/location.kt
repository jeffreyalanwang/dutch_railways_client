package com.jeffreyalanwang.dutchrailwaysandroidclient.ui.search

import android.location.Address
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.google.android.gms.maps.model.LatLng
import com.jeffreyalanwang.dutchrailwaysandroidclient.R
import com.jeffreyalanwang.dutchrailwaysandroidclient.SuspendLazy
import com.jeffreyalanwang.dutchrailwaysandroidclient.addNotNull
import com.jeffreyalanwang.dutchrailwaysandroidclient.addressString
import com.jeffreyalanwang.dutchrailwaysandroidclient.backend.Geocoding
import com.jeffreyalanwang.dutchrailwaysandroidclient.backend.parseLatLng
import com.jeffreyalanwang.dutchrailwaysandroidclient.equalOn
import com.jeffreyalanwang.dutchrailwaysandroidclient.latLng
import com.jeffreyalanwang.dutchrailwaysandroidclient.ui.util.AppStringFormats
import com.jeffreyalanwang.dutchrailwaysandroidclient.ui.util.producedStateFrom

const val LOCATION_SEARCH_PLACEHOLDER = "Search coordinates or address"

sealed class LocationResult(
    val latLng: LatLng,
) {
    abstract suspend fun getAddress(): String?

    override fun equals(other: Any?): Boolean
      = (other is LocationResult) &&
        with(this to other) {
            equalOn { it::class } && equalOn { it.latLng }
        }

    override fun hashCode() = latLng.hashCode()
}

class LatLngResult(
    latLng: LatLng
): LocationResult(latLng) {
    private val addressDelegate = SuspendLazy { Geocoding.closest_address(latLng)?.addressString }
    override suspend fun getAddress() = addressDelegate.getValue()
}

class AddressResult(
    latLng: LatLng,
    val address: String,
): LocationResult(latLng) {

    override suspend fun getAddress() = address

    constructor(
        geocoderResult: Address,
    ) : this(
        latLng = geocoderResult.latLng,
        address = geocoderResult.addressString,
    )

    override fun equals(other: Any?)
      = other is AddressResult
        && super.equals(other)
        && (this to other).equalOn { it.address }
}

@Composable
fun SearchResult(
    item: LocationResult,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    ListItem(
        headlineContent = { Text(
            when (item) {
                is LatLngResult -> AppStringFormats.LatLng(item.latLng)
                is AddressResult -> item.address
            },
            softWrap = false,
            overflow = TextOverflow.Ellipsis,
        ) },
        supportingContent = { Text(
            when (item) {
                is LatLngResult -> producedStateFrom("Loading...") { item.getAddress() ?: "No address" }
                is AddressResult -> AppStringFormats.LatLng(item.latLng)
            }
        ) },
        leadingContent = {
            Icon(
                painterResource(
                    when (item) {
                        is LatLngResult -> R.drawable.ic_coordinate
                        is AddressResult -> R.drawable.ic_map_pin
                    }
                ),
                contentDescription = null,
            )
        },
        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
        modifier = modifier
            .clickable(onClick = onClick)
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
    )
}

/**
 * For this overload of [ExpandedSearch], after [onSelectResult] is called,
 * the caller is responsible for populating the text field with the result's
 * address.
 */
@Composable
fun <T: LocationResult> ExpandedSearch(
    textFieldState: TextFieldState,
    searchBarState: SearchBarState,
    onClose: () -> Unit,
    onSelectResult: (LocationResult?) -> Unit,
    onClearedText: () -> Unit = {
        textFieldState.clearText()
        onSelectResult(null)
    },
    modifier: Modifier = Modifier,
) {
    val addressResults by produceState(emptyList(), textFieldState.text) {
        value = textFieldState.text
            .let { query -> Geocoding.autocomplete_location(query) }
            .map { results -> AddressResult(results) }
    }
    val latLngResult =
        textFieldState.text
        .let { query -> parseLatLng(query) }
        ?.let { latLng -> LatLngResult(latLng) }

    ExpandedSearch<LocationResult>(
        results = buildList {
            addNotNull(latLngResult)
            addAll(addressResults)
        },
        resultToText = { "" },
        placeholderText = LOCATION_SEARCH_PLACEHOLDER,
        textFieldState = textFieldState,
        searchBarState = searchBarState,
        modifier = modifier,
        onClose = onClose,
        onSelectResult = onSelectResult,
        onClearedText = onClearedText,
    ) { location, onClick ->
        SearchResult(location, onClick)
    }
}