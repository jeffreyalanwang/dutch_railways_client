package com.jeffreyalanwang.dutchrailwaysandroidclient.ui.screens.top

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.setTextAndPlaceCursorAtEnd
import androidx.compose.material3.BottomSheet
import androidx.compose.material3.ExpandedFullScreenSearchBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SearchBarDefaults
import androidx.compose.material3.SearchBarState
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.rememberStandardBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
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
import com.jeffreyalanwang.dutchrailwaysandroidclient.ui.components.SearchBarId
import com.jeffreyalanwang.dutchrailwaysandroidclient.ui.components.rememberDualSearchBarState
import com.jeffreyalanwang.dutchrailwaysandroidclient.ui.detailScreens.AreaDetailWithoutMap
import com.jeffreyalanwang.dutchrailwaysandroidclient.ui.detailScreens.StationDetailWithoutMap
import com.jeffreyalanwang.dutchrailwaysandroidclient.ui.util.bottomOnly
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
): @Composable (TextFieldState, SearchBarState, @Composable (() -> Unit)?) -> Unit {
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
    val bottomSheetState = rememberStandardBottomSheetState()
    var departEndpointState by rememberSaveable { mutableStateOf<Place?>(null) }
    var arriveEndpointState by rememberSaveable { mutableStateOf<Place?>(null) }

    // Will be [SearchBarId.None] only if no selection has been made yet
    var lastChanged by rememberSaveable { mutableStateOf(SearchBarId.None) }

    fun closeSearch() { scope.launch { dualSearchBarState.animateToCollapsed() } }
    fun openSheet() { scope.launch { bottomSheetState.show() } }

    Scaffold (
        modifier = Modifier.fillMaxSize(),
        topBar = {
            AppBarWithDualSearch(
                dualSearchBarState,
                colors = SearchBarDefaults.appBarWithSearchColors(
                    appBarContainerColor = Color.Transparent,
                    scrolledAppBarContainerColor = Color.White.copy(alpha=0.5f),
                ),
                inputField1 = inputFieldFactory(
                    placeholderText = "Search departure",
                    onFinishSearch = { closeSearch() },
                ),
                inputField2 = inputFieldFactory(
                    placeholderText = "Search arrival",
                    onFinishSearch = { closeSearch() },
                ),
                expandedSearch1 = expandedSearchFactory(
                    onNewSelection = { place ->
                        departEndpointState = place
                        lastChanged = SearchBarId.First
                        openSheet()
                    },
                    onFinishSearch = { closeSearch() },
                ),
                expandedSearch2 = expandedSearchFactory(
                    onNewSelection = { place ->
                        arriveEndpointState = place
                        lastChanged = SearchBarId.Second
                        openSheet()
                    },
                    onFinishSearch = { closeSearch() },
                ),
            )
        },
    ) { innerPadding ->

        if (lastChanged != SearchBarId.None) {
            val place = when(lastChanged) {
                SearchBarId.First -> departEndpointState
                SearchBarId.Second -> arriveEndpointState
                else -> throw NotImplementedError()
            }
            BottomSheet(
                state = bottomSheetState,
                modifier = Modifier.padding(innerPadding.topOnly()),
                shadowElevation = 12.dp,
            ) {
                Box(Modifier.padding(innerPadding.bottomOnly())) {
                    when (place) {
                        is Station -> StationDetailWithoutMap(
                            station = place,
                            onNavigate = onNavigate,
                            Modifier.padding(horizontal = 10.dp),
                        )

                        is Area -> AreaDetailWithoutMap(
                            area = place,
                            onNavigate = onNavigate,
                            Modifier.padding(horizontal = 10.dp),
                        )

                        else -> throw NotImplementedError()
                    }
                }
            }
        }
    }
}
