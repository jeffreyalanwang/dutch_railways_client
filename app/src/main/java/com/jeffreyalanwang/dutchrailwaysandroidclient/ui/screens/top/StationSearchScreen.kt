package com.jeffreyalanwang.dutchrailwaysandroidclient.ui.screens.top

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.input.clearText
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.foundation.text.input.setTextAndPlaceCursorAtEnd
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AppBarWithSearch
import androidx.compose.material3.Card
import androidx.compose.material3.ExpandedFullScreenSearchBar
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SearchBarDefaults
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.rememberSearchBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.jeffreyalanwang.dutchrailwaysandroidclient.BackendApi
import com.jeffreyalanwang.dutchrailwaysandroidclient.R
import com.jeffreyalanwang.dutchrailwaysandroidclient.Station
import com.jeffreyalanwang.dutchrailwaysandroidclient.ui.TrainServiceDetailRoute
import com.jeffreyalanwang.dutchrailwaysandroidclient.ui.components.PlaceSearchResults
import com.jeffreyalanwang.dutchrailwaysandroidclient.ui.detailScreens.StationDetail
import kotlinx.coroutines.launch

@Preview
@Composable
private fun StationSearchScreenPreview() {
    val snackbarHostState = remember { SnackbarHostState() }
    val snackbarEffectScope = rememberCoroutineScope()

    StationSearchScreen { newRoute ->
        snackbarEffectScope.launch {
            snackbarHostState.showSnackbar(
                newRoute.toString(),
                withDismissAction = true
            )
        }
    }

    SnackbarHost(hostState = snackbarHostState)
}


@Composable
fun StationSearchScreen(onNavigate: (TrainServiceDetailRoute)->Unit) {
    val searchBarState = rememberSearchBarState()
    val textFieldState = rememberTextFieldState()
    var stationState by rememberSaveable { mutableStateOf<Station?>(null) }
    val scope = rememberCoroutineScope()

    @Composable
    fun inputField(
        leadingIcon: @Composable (() -> Unit)? = null,
        trailingIcon: @Composable (() -> Unit)? = null,
    ) {
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
            leadingIcon = leadingIcon,
            trailingIcon = trailingIcon,
        )
    }

    Scaffold(
        topBar = {
            AppBarWithSearch(
                state = searchBarState,
                inputField = { inputField(
                    trailingIcon = {
                        Icon(
                            painterResource(R.drawable.ic_search),
                            contentDescription="Search",
                        )
                    }
                ) }
            )
            ExpandedFullScreenSearchBar(
                state = searchBarState,
                inputField = { inputField(
                    leadingIcon = {
                        IconButton(
                            onClick = {
                                scope.launch { searchBarState.animateToCollapsed() }
                            }
                        ) {
                            Icon(
                                painterResource(R.drawable.ic_back),
                                contentDescription = "Close search",
                            )
                        }
                    },
                    trailingIcon = {
                        IconButton(
                            onClick = {
                                textFieldState.clearText()
                                stationState = null
                            }
                        ) {
                            Icon(
                                painterResource(R.drawable.ic_close),
                                contentDescription = "Clear",
                            )
                        }
                    }
                ) }
            ) {
                PlaceSearchResults(
                    Station::class,
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
        if (stationState == null) NoStationDetail(
            Modifier
                .padding(innerPadding)
        )
        else Box(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
        ) {
            Card(
                Modifier
                    .padding(innerPadding)
                    .padding(10.dp)
            ) {
                StationDetail(
                    stationState!!,
                    onNavigate = onNavigate,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 20.dp),
                )
            }
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