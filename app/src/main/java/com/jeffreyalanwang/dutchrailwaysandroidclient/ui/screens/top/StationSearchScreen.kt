package com.jeffreyalanwang.dutchrailwaysandroidclient.ui.screens.top

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.material3.AppBarWithSearch
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.rememberSearchBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.jeffreyalanwang.dutchrailwaysandroidclient.R
import com.jeffreyalanwang.dutchrailwaysandroidclient.Station
import com.jeffreyalanwang.dutchrailwaysandroidclient.ui.PassServiceDetailNavArgs
import com.jeffreyalanwang.dutchrailwaysandroidclient.ui.components.CardContentScaffold
import com.jeffreyalanwang.dutchrailwaysandroidclient.ui.screens.child.detail.StationDetail
import com.jeffreyalanwang.dutchrailwaysandroidclient.ui.search.BaseSearchInputField
import com.jeffreyalanwang.dutchrailwaysandroidclient.ui.search.ExpandedSearch
import com.jeffreyalanwang.dutchrailwaysandroidclient.ui.search.STATION_SEARCH_PLACEHOLDER
import kotlinx.coroutines.launch

@Preview(heightDp = 500)
@Composable
private fun StationSearchScreenPreview() {
    val snackbarHostState = remember { SnackbarHostState() }
    val snackbarEffectScope = rememberCoroutineScope()

    StationSearchScreen { newNavArgs ->
        snackbarEffectScope.launch {
            snackbarHostState.showSnackbar(
                newNavArgs.toString(),
                withDismissAction = true
            )
        }
    }

    SnackbarHost(hostState = snackbarHostState)
}


@Composable
fun StationSearchScreen(onNavigate: (PassServiceDetailNavArgs)->Unit) {
    var station by rememberSaveable { mutableStateOf<Station?>(null) }
    val contentScrollState = key(station?.id) { rememberScrollState () }
    val searchBarState = rememberSearchBarState()
    val textFieldState = rememberTextFieldState()
    val scope = rememberCoroutineScope()

    val topBar = @Composable {
        AppBarWithSearch(
            state = searchBarState,
            inputField = {
                BaseSearchInputField(
                    STATION_SEARCH_PLACEHOLDER,
                    textFieldState,
                    searchBarState
                )
            }
        )
    }

    if (station == null) {
        Scaffold(
            topBar = topBar,
        ) { innerPadding ->
            NoStationDetail(Modifier.padding(innerPadding))
        }
    } else {
        CardContentScaffold(
            topBar = topBar,
            scrollState = contentScrollState,
        ) {
            StationDetail(
                station!!,
                onNavigate = onNavigate,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 20.dp),
            )
        }
    }

    ExpandedSearch<Station>(
        textFieldState,
        searchBarState,
        onClose = { scope.launch { searchBarState.animateToCollapsed() } },
        onSelectResult = { station = it },
    )
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