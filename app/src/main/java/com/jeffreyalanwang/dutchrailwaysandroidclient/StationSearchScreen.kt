package com.jeffreyalanwang.dutchrailwaysandroidclient

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.foundation.text.input.setTextAndPlaceCursorAtEnd
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AppBarWithSearch
import androidx.compose.material3.ExpandedFullScreenSearchBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SearchBarDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberSearchBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import java.util.EnumSet

@Preview
@Composable
fun StationSearchScreenTest() {
    StationSearchScreen({})
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StationSearchScreen(onNavigate: (Any)->Unit) {
    val searchBarState = rememberSearchBarState()
    val textFieldState = rememberTextFieldState()
    var stationState by remember { mutableStateOf<Station?>(null) }
    val scope = rememberCoroutineScope()

    val inputField = @Composable {
        SearchBarDefaults.InputField(
            textFieldState = textFieldState,
            searchBarState = searchBarState,
            onSearch = { scope.launch { searchBarState.animateToCollapsed() } },
            placeholder = {
                Text(
                    modifier = Modifier.clearAndSetSemantics {},
                    text = "Search stations"
                )
            },
            trailingIcon = {
                Icon(painterResource(R.drawable.ic_search),
                    contentDescription="Search",
                )
            },
        )
    }

    Scaffold(
        topBar = @Composable {
            AppBarWithSearch(state = searchBarState, inputField = inputField)
            ExpandedFullScreenSearchBar(state = searchBarState, inputField = inputField) {
                StationSearchResults(
                    textFieldState.text.toString(),
                    onResultClick = { id, name ->
                        textFieldState.setTextAndPlaceCursorAtEnd(name)
                        scope.launch { searchBarState.animateToCollapsed() }
                        stationState = BackendApi.get_station_info(id)
                    }
                )
            }
        },
        modifier = Modifier.fillMaxSize()
    ) { innerPadding ->
        if (stationState == null) NoStationDetail(Modifier.padding(innerPadding))
        else Box(Modifier
            .fillMaxSize()
            .padding(innerPadding)
            .padding(all = 10.dp)){
            StationDetail(
                stationState!!,
                modifier = Modifier.fillMaxWidth(),
                onNavigate = onNavigate,
            )
        }
    }
}

@Composable
private fun StationSearchResults(query: String, onResultClick: (UInt, String) -> Unit, modifier: Modifier = Modifier) {
    Column(modifier.verticalScroll(rememberScrollState())) {
        (BackendApi.autocomplete_place(query, EnumSet.of(PlaceSubclass.Station)) as List<Station>).forEach {
            ListItem(
                headlineContent = { Text(it.name) },
                supportingContent = { Text(it.address) },
                leadingContent = { Icon(painterResource(R.drawable.ic_dr_station), contentDescription = "Station") },
                colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                modifier =
                    Modifier
                        .clickable { onResultClick(it.id, it.name) }
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp),
            )
        }
    }
}

@Composable
private fun NoStationDetail(modifier: Modifier = Modifier) {
    Column(
        modifier
            .alpha(.7f)
            .fillMaxSize(),
        Arrangement.Center,
        Alignment.CenterHorizontally,
    ) {
        Icon(
            painterResource(R.drawable.ic_dr_station),
            "Station icon",
            Modifier
                .size(96.dp)
                .padding(bottom = 12.dp)
        )
        Text(
            "No station selected",
            textAlign = TextAlign.Center,
        )
    }
}