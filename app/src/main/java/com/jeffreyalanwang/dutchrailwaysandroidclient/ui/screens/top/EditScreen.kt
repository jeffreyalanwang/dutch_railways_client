package com.jeffreyalanwang.dutchrailwaysandroidclient.ui.screens.top

import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PlainTooltip
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SearchBar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TooltipAnchorPosition
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.TooltipDefaults
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberSearchBarState
import androidx.compose.material3.rememberTooltipState
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.jeffreyalanwang.dutchrailwaysandroidclient.backend.BackendApi
import com.jeffreyalanwang.dutchrailwaysandroidclient.Area
import com.jeffreyalanwang.dutchrailwaysandroidclient.PassService
import com.jeffreyalanwang.dutchrailwaysandroidclient.Place
import com.jeffreyalanwang.dutchrailwaysandroidclient.R
import com.jeffreyalanwang.dutchrailwaysandroidclient.Station
import com.jeffreyalanwang.dutchrailwaysandroidclient.backend.LocalAppSettings
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
import com.jeffreyalanwang.dutchrailwaysandroidclient.ui.components.MarginButtonsBox
import com.jeffreyalanwang.dutchrailwaysandroidclient.ui.search.BaseSearchInputField
import com.jeffreyalanwang.dutchrailwaysandroidclient.ui.search.ExpandedSearch
import com.jeffreyalanwang.dutchrailwaysandroidclient.ui.search.SearchResult
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
    val settings = LocalAppSettings.current
    val locked by settings.stateOf { it.isEditAccessLocked }
    fun setLocked(value: Boolean) =
        settings.update { it.copy(isEditAccessLocked = value) }

    AnimatedContent (locked) { locked ->
        if (locked) {
            RequireUnlockScreen { setLocked(false) }
        } else {
            Scaffold(
                topBar = {
                    TopBar(onNavigate, { setLocked(true) })
                },
            ) { innerPadding ->
                StartContent(
                    Modifier.padding(innerPadding),
                    onNavigate = onNavigate,
                )
            }
        }
    }

}

@Composable
fun RequireUnlockScreen(
    onUnlockRequest: () -> Unit,
) = Box(
        Modifier
            .clickable(
                interactionSource = null,
                indication = ripple(color = Color.White, enableHoverIndication = false),
                onClick = onUnlockRequest,
            )
            .fillMaxSize()
            .alpha(.7f),
    Alignment.Center
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Icon(
                painterResource(R.drawable.ic_lock),
                "Lock icon",
                Modifier.size(96.dp)
            )
            Text(
                "Edit access locked",
                style = MaterialTheme.typography.displaySmall,
            )
            Text(
                "Tap to unlock",
            )
        }
    }

const val ALL_SEARCH_PLACEHOLDER_TEXT = "Search places or trains"

@Composable
private fun TopBar(
    onNavigate: (CommonChildNavArgs) -> Unit,
    onLockRequest: () -> Unit,
) {
    val searchBarState = rememberSearchBarState()
    val textFieldState = rememberTextFieldState()
    val scope = rememberCoroutineScope()

    MarginButtonsBox(
        Modifier
            .windowInsetsPadding(TopAppBarDefaults.windowInsets),
        right = {
            IconButton(onLockRequest) {
                Icon(
                    painterResource(R.drawable.ic_lock_open),
                    contentDescription = "Click to lock edit access",
                )
            }
        },
    ) {
        SearchBar(
            state = searchBarState,
            inputField = {
                BaseSearchInputField(
                    ALL_SEARCH_PLACEHOLDER_TEXT,
                    textFieldState,
                    searchBarState,
                )
            },
        )
    }

    // Search all, but prioritize places
    ExpandedSearch(
        results = textFieldState.text.toString()
            .let { BackendApi.autocomplete_place_or_pass_service(it) },
        resultToText = {
            when (it) {
                is Place -> it.name
                is PassService -> it.title
                else -> throw IllegalArgumentException()
            }
        },
        placeholderText = ALL_SEARCH_PLACEHOLDER_TEXT,
        textFieldState = textFieldState,
        searchBarState = searchBarState,
        onClose = { scope.launch { searchBarState.animateToCollapsed() } },
        onSelectResult = {
            onNavigate(
                when (it) {
                    is Station -> StationDetailNavArgs(it.id)
                    is Area -> AreaDetailNavArgs(it.id)
                    is PassService -> PassServiceDetailNavArgs(it.id)
                    else -> throw IllegalArgumentException()
                }
            )
        },
    ) { item, onClick ->
        when (item) {
            is Place -> SearchResult(item, onClick)
            is PassService -> SearchResult(item, onClick)
            else -> throw IllegalArgumentException()
        }
    }
}

@Composable
private fun StartContent(
    modifier: Modifier = Modifier,
    onNavigate: (EditGraphNavArgs) -> Unit,
) = Box(modifier.fillMaxSize()) {
        Column(
            Modifier.align(Alignment.Center),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Icon(
                painterResource(R.drawable.ic_edit),
                "Hero icon",
                Modifier
                    .alpha(.7f)
                    .size(96.dp)
            )
            Text(
                "No place or service selected",
                Modifier.alpha(.7f),
            )
            TextButton(
                { onNavigate(NewPassServiceNavArgs()) },
                shape = MaterialTheme.shapes.extraSmall,
            ) {
                Text("Create new train service")
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