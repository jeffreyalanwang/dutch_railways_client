package com.jeffreyalanwang.dutchrailwaysandroidclient.ui.screens.top

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.setTextAndPlaceCursorAtEnd
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExpandedFullScreenSearchBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SearchBarDefaults
import androidx.compose.material3.SearchBarState
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.jeffreyalanwang.dutchrailwaysandroidclient.Area
import com.jeffreyalanwang.dutchrailwaysandroidclient.BackendApi
import com.jeffreyalanwang.dutchrailwaysandroidclient.Place
import com.jeffreyalanwang.dutchrailwaysandroidclient.R
import com.jeffreyalanwang.dutchrailwaysandroidclient.Station
import com.jeffreyalanwang.dutchrailwaysandroidclient.ui.NavRoute
import com.jeffreyalanwang.dutchrailwaysandroidclient.ui.components.AppBarWithDualSearch
import com.jeffreyalanwang.dutchrailwaysandroidclient.ui.components.PlaceSearchResults
import com.jeffreyalanwang.dutchrailwaysandroidclient.ui.components.rememberDualSearchBarState
import com.jeffreyalanwang.dutchrailwaysandroidclient.ui.detailScreens.StationDetail
import com.jeffreyalanwang.dutchrailwaysandroidclient.ui.util.topOnly
import com.jeffreyalanwang.dutchrailwaysandroidclient.ui.viewmodel.RoutePlannerViewModel
import kotlinx.coroutines.launch

@Preview
@Composable
private fun RoutePlannerScreenTest() {
    val snackbarHostState = remember { SnackbarHostState() }
    val snackbarEffectScope = rememberCoroutineScope()

    RoutePlannerScreen (
        viewModel = viewModel<RoutePlannerViewModel>(),
        onNavigate = { newRoute ->
            snackbarEffectScope.launch {
                snackbarHostState.showSnackbar(
                    newRoute.toString(),
                    withDismissAction = true
                )
            }
        }
    )

    SnackbarHost(hostState = snackbarHostState)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun inputFieldFactory(
    placeholderText: String,
    onFinishSearch: () -> Unit,
): @Composable (TextFieldState, SearchBarState, (@Composable () -> Unit)?) -> Unit {
    return { textFieldState, searchBarState, leadingIcon ->
        SearchBarDefaults.InputField(
            textFieldState = textFieldState,
            searchBarState = searchBarState,
            leadingIcon = leadingIcon,
            onSearch = { onFinishSearch() },
            placeholder = {
                Text(
                    modifier = Modifier.clearAndSetSemantics {},
                    text = placeholderText,
                )
            },
            trailingIcon = {
                Icon(painterResource(R.drawable.ic_search),
                    contentDescription="Search",
                )
            },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun expandedSearchFactory(
    onNewSelection: (Place) -> Unit,
    onFinishSearch: () -> Unit,
): @Composable (TextFieldState, SearchBarState, @Composable () -> Unit) -> Unit {
    return { textFieldState, searchBarState, inputField ->
        ExpandedFullScreenSearchBar(searchBarState, inputField = inputField) {
            PlaceSearchResults(
                Place::class,
                query = textFieldState.text.toString(),
                onResultClick = { id, name ->
                    textFieldState.setTextAndPlaceCursorAtEnd(name)
                    onNewSelection(BackendApi.get_place_info(id))
                    onFinishSearch()
                },
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RoutePlannerScreen(viewModel: RoutePlannerViewModel, onNavigate: (NavRoute)->Unit) {
    val scope = rememberCoroutineScope()
    val dualSearchBarState = rememberDualSearchBarState()
    var departEndpointState by rememberSaveable { mutableStateOf<Place?>(null) }
    var arriveEndpointState by rememberSaveable { mutableStateOf<Place?>(null) }

    fun close() { scope.launch { dualSearchBarState.animateToCollapsed() } }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            AppBarWithDualSearch(
                dualSearchBarState,
                inputField1 = inputFieldFactory(
                    placeholderText = "Search departure",
                    onFinishSearch = { close() },
                ),
                inputField2 = inputFieldFactory(
                    placeholderText = "Search arrival",
                    onFinishSearch = { close() },
                ),
                expandedSearch1 = expandedSearchFactory(
                    onNewSelection = { place ->
                        departEndpointState = place },
                    onFinishSearch = { close() },
                ),
                expandedSearch2 = expandedSearchFactory(
                    onNewSelection = { place ->
                        arriveEndpointState = place },
                    onFinishSearch = { close() },
                ),
                colors = SearchBarDefaults.appBarWithSearchColors(
                    appBarContainerColor = Color.Transparent,
                    scrolledAppBarContainerColor = Color.White.copy(alpha=0.5f),
                ),
            )
        },
    ) { innerPadding ->
        if (departEndpointState == arriveEndpointState == null) {
            Box( Modifier
                .fillMaxSize()
                .padding(innerPadding.topOnly()),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    painterResource(R.drawable.ic_dr_area),
                    null,
                    Modifier
                        .size(128.dp)
                        .alpha(.7f),
                )
            }
        } else { // TODO show a map, move the below to a bottom-sheet
            Column( Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(innerPadding)
            ) {
                for (endpointState in
                    listOfNotNull(departEndpointState, arriveEndpointState)
                ) { when (endpointState) {
                    is Station -> StationDetail(
                        endpointState,
                        Modifier.padding(all = 10.dp),
                        onNavigate = onNavigate
                    ) //TODO remove google maps element from StationDetail composable
                    is Area -> TODO()//AreaDetail()
                    else -> throw NotImplementedError()
                } }
            }
        }
    }
}
