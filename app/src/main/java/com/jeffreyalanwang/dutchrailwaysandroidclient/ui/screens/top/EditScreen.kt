package com.jeffreyalanwang.dutchrailwaysandroidclient.ui.screens.top

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.input.clearText
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.foundation.text.input.setTextAndPlaceCursorAtEnd
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AppBarWithSearch
import androidx.compose.material3.ExpandedFullScreenSearchBar
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PlainTooltip
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SearchBarDefaults
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TooltipAnchorPosition
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.TooltipDefaults
import androidx.compose.material3.rememberSearchBarState
import androidx.compose.material3.rememberTooltipState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.jeffreyalanwang.dutchrailwaysandroidclient.Area
import com.jeffreyalanwang.dutchrailwaysandroidclient.BackendApi
import com.jeffreyalanwang.dutchrailwaysandroidclient.Place
import com.jeffreyalanwang.dutchrailwaysandroidclient.R
import com.jeffreyalanwang.dutchrailwaysandroidclient.Station
import com.jeffreyalanwang.dutchrailwaysandroidclient.ui.AreaDetailNavArgs
import com.jeffreyalanwang.dutchrailwaysandroidclient.ui.CommonChildNavArgs
import com.jeffreyalanwang.dutchrailwaysandroidclient.ui.ConfirmDeletePassServiceNavArgs
import com.jeffreyalanwang.dutchrailwaysandroidclient.ui.EditAreaNavArgs
import com.jeffreyalanwang.dutchrailwaysandroidclient.ui.EditGraphNavArgs
import com.jeffreyalanwang.dutchrailwaysandroidclient.ui.EditPassServiceNavArgs
import com.jeffreyalanwang.dutchrailwaysandroidclient.ui.EditStationNavArgs
import com.jeffreyalanwang.dutchrailwaysandroidclient.ui.NewPassServiceNavArgs
import com.jeffreyalanwang.dutchrailwaysandroidclient.ui.PassServiceDetailNavArgs
import com.jeffreyalanwang.dutchrailwaysandroidclient.ui.StationDetailNavArgs
import com.jeffreyalanwang.dutchrailwaysandroidclient.ui.components.PassServiceSearchResults
import com.jeffreyalanwang.dutchrailwaysandroidclient.ui.components.PlaceSearchResults
import kotlinx.coroutines.launch

@Preview
@Composable
private fun EditScreenPreview() {
    val snackbarHostState = remember { SnackbarHostState() }
    val snackbarEffectScope = rememberCoroutineScope()

    EditScreen { newNavArgs ->
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
fun EditScreen(onNavigate: (EditGraphNavArgs)->Unit) {
    Scaffold(
        topBar = { TopBar(onNavigate) },
        modifier = Modifier.fillMaxSize()
    ) { innerPadding ->
        StartContent(
            Modifier.padding(innerPadding),
            onNavigate = onNavigate,
        )
    }
}

@Composable
private fun TopBar(onNavigate: (CommonChildNavArgs) -> Unit) {
    val searchBarState = rememberSearchBarState()
    val textFieldState = rememberTextFieldState()
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
                    text = "Search places or trains"
                )
            },
            leadingIcon = leadingIcon,
            trailingIcon = trailingIcon,
        )
    }

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
                    onClick = textFieldState::clearText
                ) {
                    Icon(
                        painterResource(R.drawable.ic_close),
                        contentDescription = "Clear",
                    )
                }
            }
        ) }
    ) {
        Column( Modifier.verticalScroll(rememberScrollState()) ) {
            PlaceSearchResults(
                Place::class,
                textFieldState.text.toString(),
                onResultClick = { id, name ->
                    textFieldState.setTextAndPlaceCursorAtEnd(name)
                    scope.launch { searchBarState.animateToCollapsed() }
                    onNavigate(
                        when (BackendApi.get_place_info(id)) {
                            is Station -> StationDetailNavArgs(id)
                            is Area -> AreaDetailNavArgs(id)
                            else -> throw IllegalArgumentException()
                        }
                    )
                }
            )
            PassServiceSearchResults(
                textFieldState.text.toString(),
                onResultClick = { id, name ->
                    textFieldState.setTextAndPlaceCursorAtEnd(name)
                    scope.launch { searchBarState.animateToCollapsed() }
                    onNavigate(PassServiceDetailNavArgs(id))
                }
            )
        }
    }
}

@Composable
private fun StartContent(
    modifier: Modifier = Modifier,
    onNavigate: (EditGraphNavArgs) -> Unit,
) {
    Box(modifier.fillMaxSize()) {
        Column(
            Modifier.align(Alignment.Center),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Icon(
                painterResource(R.drawable.ic_edit),
                "Station icon",
                Modifier
                    .alpha(.7f)
                    .size(96.dp)
            )
            Text(
                "No place or service selected",
                Modifier.alpha(.7f),
                textAlign = TextAlign.Center,
            )
            TextButton(
                { onNavigate(NewPassServiceNavArgs()) },
                shape = MaterialTheme.shapes.extraSmall,
            ) {
                Text("Create new train service")
            }
        }
    }
}

@Composable
fun RowScope.EditActions(
    navArgs: CommonChildNavArgs,
    onNavigate: (EditGraphNavArgs) -> Unit,
) {
    when (navArgs) {
        is StationDetailNavArgs ->
            ActionButton(
                "Edit",
                painterResource(R.drawable.ic_edit),
            ) { onNavigate(
                EditStationNavArgs(navArgs.id)
            ) }
        is AreaDetailNavArgs ->
            ActionButton(
                "Edit",
                painterResource(R.drawable.ic_edit),
            ) { onNavigate(
                EditAreaNavArgs(navArgs.id)
            ) }
        is PassServiceDetailNavArgs -> {
            ActionButton(
                "Duplicate",
                painterResource(R.drawable.ic_copy),
            ) { onNavigate(
                NewPassServiceNavArgs(navArgs.id)
            ) }
            ActionButton(
                "Edit",
                painterResource(R.drawable.ic_edit),
            ) { onNavigate(
                EditPassServiceNavArgs(navArgs.id)
            ) }
            ActionButton(
                "Delete",
                painterResource(R.drawable.ic_trash),
            ) { onNavigate(
                ConfirmDeletePassServiceNavArgs(navArgs.id)
            ) }
        }
        else -> throw IllegalArgumentException()
    }
}

@Composable
private fun ActionButton(
    title: String,
    icon: Painter,
    navigateTo: () -> Unit,
) {
    TooltipBox(
        positionProvider = TooltipDefaults
            .rememberTooltipPositionProvider(
                TooltipAnchorPosition.Below,
            ),
        state = rememberTooltipState(),
        tooltip = {
            PlainTooltip {
                Text(title)
            }
        }
    ) {
        IconButton(onClick = navigateTo) {
            Icon(
                icon,
                contentDescription = title,
            )
        }
    }
}