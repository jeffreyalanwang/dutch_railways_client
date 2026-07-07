package com.jeffreyalanwang.dutchrailwaysandroidclient.ui.components

import android.annotation.SuppressLint
import androidx.compose.animation.core.AnimationSpec
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.setTextAndPlaceCursorAtEnd
import androidx.compose.material3.AppBarWithSearchColors
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExpandedFullScreenSearchBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SearchBarColors
import androidx.compose.material3.SearchBarDefaults
import androidx.compose.material3.SearchBarState
import androidx.compose.material3.SearchBarValue
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.contentColorFor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.mapSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.isTraversalGroup
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.jeffreyalanwang.dutchrailwaysandroidclient.R
import kotlinx.coroutines.launch

@Preview(widthDp = 350, heightDp = 750)
@Composable
private fun DualSearchBarPreview() {
    val snackbarHostState = remember { SnackbarHostState() }
    val dualSearchBarState = rememberDualSearchBarState()
    var appBarDividerPos by remember { mutableStateOf(0f to 0f) }
    val scope = rememberCoroutineScope()

    val inputField = @Composable { textFieldState: TextFieldState, searchBarState: SearchBarState ->
        SearchBarDefaults.InputField(
            textFieldState,
            searchBarState,
            modifier =  Modifier
                .fillMaxWidth()
                .onGloballyPositioned {
                    appBarDividerPos = appBarDividerPos
                        .copy(second = it.positionInWindow().x)
                },
            placeholder = { Text(
                "Placeholder text",
                Modifier.onGloballyPositioned { appBarDividerPos = appBarDividerPos
                    .copy(first = it.positionInWindow().x) },
            ) },
            onSearch = {
                scope.launch { searchBarState.animateToCollapsed() }
                scope.launch { snackbarHostState.showSnackbar("onSearch: $it") }
            },
        )
    }

    Scaffold(
        topBar = {
            AppBarWithDualSearch(
                dualSearchBarState,
                leadingIcon = {
                    NavBackButton({
                        scope.launch {
                            snackbarHostState.showSnackbar("Leading icon pushed")
                        }
                    })
                },
                actionIcon = {
                    IconButton(
                        colors = IconButtonDefaults.filledIconButtonColors(),
                        onClick = { scope.launch {
                            snackbarHostState.showSnackbar("Action icon pushed")
                        } }
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.ic_search),
                            contentDescription = "Search",
                        )
                    }
                },
                inputFieldBuilder = inputField,
                expandedSearchBuilder = { textFieldState, searchBarState ->
                    ExpandedFullScreenSearchBar(
                        searchBarState,
                        inputField = { inputField(textFieldState, searchBarState) },
                    ) {
                        for (i in 0..<3) {
                            ListItem(
                                headlineContent = { Text(i.toString()) },
                                leadingContent = {
                                    Icon(painterResource(R.drawable.ic_dr_area), null)
                                },
                                colors = ListItemDefaults
                                    .colors(containerColor = Color.Transparent),
                                modifier = Modifier
                                    .clickable {
                                        textFieldState.setTextAndPlaceCursorAtEnd(
                                            i.toString()
                                        )
                                        scope.launch { dualSearchBarState.animateToCollapsed() }
                                        scope.launch {
                                            snackbarHostState.showSnackbar(
                                                "onSearch: $i"
                                            )
                                        }
                                    }
                                    .fillMaxWidth()
                                    .padding(
                                        horizontal = 16.dp,
                                        vertical = 4.dp
                                    ),
                            )
                        }
                    }
                },
                divider = {
                    HorizontalDivider( Modifier.padding(horizontal = 16.dp) )
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { innerPadding ->
        Box(Modifier.padding(innerPadding))
    }
}

@SuppressLint("ModifierParameter")
@Composable
fun AppBarWithDualSearch(
    state: DualSearchBarState,
    leadingIcon: @Composable (() -> Unit)? = null,
    actionIcon: @Composable (() -> Unit)? = null,
    inputFieldBuilder: @Composable (TextFieldState, SearchBarState) -> Unit,
    expandedSearchBuilder: @Composable (TextFieldState, SearchBarState) -> Unit,
    divider: @Composable ColumnScope.() -> Unit = {},
    modifier: Modifier = Modifier,
    shape: Shape = CardDefaults.elevatedShape,
    colors: AppBarWithSearchColors = SearchBarDefaults.appBarWithSearchColors(),
    tonalElevation: Dp = SearchBarDefaults.TonalElevation,
    shadowElevation: Dp = SearchBarDefaults.ShadowElevation,
    contentPadding: PaddingValues = SearchBarDefaults.AppBarContentPadding,
    windowInsets: WindowInsets = SearchBarDefaults.windowInsets,
) = AppBarWithDualSearch(
    state,
    leadingIcon = leadingIcon,
    actionIcon = actionIcon,
    inputField1 = inputFieldBuilder,
    inputField2 = inputFieldBuilder,
    expandedSearch1 = expandedSearchBuilder,
    expandedSearch2 = expandedSearchBuilder,
    divider = divider,
    modifier,
    shape,
    colors,
    tonalElevation,
    shadowElevation,
    contentPadding,
    windowInsets,
)


@SuppressLint("ModifierParameter")
@Composable
fun AppBarWithDualSearch(
    state: DualSearchBarState,
    leadingIcon: @Composable (() -> Unit)? = null,
    actionIcon: @Composable (() -> Unit)? = null,
    inputField1: @Composable (TextFieldState, SearchBarState) -> Unit,
    inputField2: @Composable (TextFieldState, SearchBarState) -> Unit,
    expandedSearch1: @Composable (TextFieldState, SearchBarState) -> Unit,
    expandedSearch2: @Composable (TextFieldState, SearchBarState) -> Unit,
    divider: @Composable ColumnScope.() -> Unit = {},
    modifier: Modifier = Modifier,
    shape: Shape = CardDefaults.elevatedShape,
    colors: AppBarWithSearchColors = SearchBarDefaults.appBarWithSearchColors(),
    tonalElevation: Dp = SearchBarDefaults.TonalElevation,
    shadowElevation: Dp = SearchBarDefaults.ShadowElevation,
    contentPadding: PaddingValues = SearchBarDefaults.AppBarContentPadding,
    windowInsets: WindowInsets = SearchBarDefaults.windowInsets,
) {
    val isVisible = state.currentExpanded.value == SearchBarId.None
                 || state.targetExpanded.value == SearchBarId.None

    WrapNestedSurface(
        color = colors.appBarContainerColor,
        modifier = modifier
            .alpha(if (isVisible) 1f else 0f)
            .padding(contentPadding)
            .windowInsetsPadding(windowInsets)
            .fillMaxWidth()
            .semantics { isTraversalGroup = true },
        tonalElevation = tonalElevation,
        shadowElevation = shadowElevation,
    ) { modifier, tonalElevation, shadowElevation ->

        MarginButtonsBox(
            modifier = modifier,
            left = leadingIcon?.let {
                {
                    Column(Modifier.fillMaxHeight(), Arrangement.Top) {
                        Box(Modifier.fillMaxHeight(.5f), Alignment.Center) {
                            it()
                        }
                    }
                }
            },
            right = actionIcon?.let {
                {
                    Column(Modifier.fillMaxHeight(), Arrangement.Bottom) {
                        Box(Modifier.fillMaxHeight(.5f), Alignment.Center) {
                            it()
                        }
                    }
                }
            },
        ) {
            DualSearchBar(
                state = state,
                inputField1 = inputField1,
                inputField2 = inputField2,
                expandedSearch1 = expandedSearch1,
                expandedSearch2 = expandedSearch2,
                divider = divider,
                modifier = Modifier
                    .padding(vertical = 4.dp)
                    .widthIn(min = 360.dp, max = 720.dp),
                shape = shape,
                colors = colors.searchBarColors,
                tonalElevation = tonalElevation,
                shadowElevation = shadowElevation,
            )
        }
    }
}

/**
 * Wraps content in a surface with the provided arguments,
 * unless the surface is requested to be transparent,
 * in which case [content] is called directly with the provided
 * surface arguments, to be rendered in its own internal surface.
 */
@Composable
private fun WrapNestedSurface(
    color: Color,
    modifier: Modifier = Modifier,
    tonalElevation: Dp = 0.dp,
    shadowElevation: Dp = 0.dp,
    content: @Composable (Modifier, Dp, Dp) -> Unit,
) {
    if (color != Color.Transparent) {
        Surface(
            color = color,
            modifier = modifier,
            tonalElevation = tonalElevation,
            shadowElevation = shadowElevation,
        ) {
            content(Modifier, 0.dp, 0.dp)
        }
    } else {
        content(modifier, tonalElevation, shadowElevation)
    }
}

@SuppressLint("ModifierParameter")
@Composable
fun DualSearchBar(
    state: DualSearchBarState,
    inputFieldBuilder: @Composable (TextFieldState, SearchBarState) -> Unit,
    expandedSearchBuilder: @Composable (TextFieldState, SearchBarState) -> Unit,
    divider: @Composable ColumnScope.() -> Unit = {},
    modifier: Modifier = Modifier,
    shape: Shape = CardDefaults.elevatedShape,
    colors: SearchBarColors = SearchBarDefaults.colors(),
    tonalElevation: Dp = SearchBarDefaults.TonalElevation,
    shadowElevation: Dp = SearchBarDefaults.ShadowElevation,
) = DualSearchBar(
    state,
    inputField1 = inputFieldBuilder,
    inputField2 = inputFieldBuilder,
    expandedSearch1 = expandedSearchBuilder,
    expandedSearch2 = expandedSearchBuilder,
    divider = divider,
    modifier,
    shape,
    colors,
    tonalElevation,
    shadowElevation
)

@SuppressLint("ModifierParameter")
@Composable
fun DualSearchBar(
    state: DualSearchBarState,
    inputField1: @Composable (TextFieldState, SearchBarState) -> Unit,
    inputField2: @Composable (TextFieldState, SearchBarState) -> Unit,
    expandedSearch1: @Composable (TextFieldState, SearchBarState) -> Unit,
    expandedSearch2: @Composable (TextFieldState, SearchBarState) -> Unit,
    divider: @Composable ColumnScope.() -> Unit,
    modifier: Modifier = Modifier,
    shape: Shape = CardDefaults.elevatedShape,
    colors: SearchBarColors = SearchBarDefaults.colors(),
    tonalElevation: Dp = SearchBarDefaults.TonalElevation,
    shadowElevation: Dp = SearchBarDefaults.ShadowElevation,
) {
    Surface(
        shape = shape,
        color = colors.containerColor,
        contentColor = contentColorFor(colors.containerColor),
        tonalElevation = tonalElevation,
        shadowElevation = shadowElevation,
        modifier = modifier,
    ) {
        Column {
            with(state.first) {
                Box(
                    Modifier
                        .onGloballyPositioned {
                            searchBarState.collapsedCoords = it
                        },
                ) {
                    inputField1()
                }
            }

            divider()

            with(state.second) {
                Box(
                    Modifier
                        .onGloballyPositioned {
                            searchBarState.collapsedCoords = it
                        },
                ) {
                    inputField2()
                }
            }
        }
    }

    with (state.first) { expandedSearch1() }
    with (state.second) { expandedSearch2() }
}

@Composable
context(state: SingleSearchState)
operator fun (@Composable (TextFieldState, SearchBarState) -> Unit)
    .invoke()
        = this(state.textFieldState, state.searchBarState)

class SingleSearchState private constructor(
    val textFieldState: TextFieldState,

    // Ideally, we hide the ability to directly expand/collapse the SearchBarState
    internal val searchBarState: SearchBarState,
){
    // Required for [DualSearchBarState.Saver]
    internal constructor(
        textField: TextFieldState,
        initialExpanded: Boolean,
        animationSpecForExpand: AnimationSpec<Float>,
        animationSpecForCollapse: AnimationSpec<Float>,
    ): this(
        textFieldState = textField,
        searchBarState = SearchBarState(
            if (initialExpanded) SearchBarValue.Expanded
                else SearchBarValue.Collapsed,
            animationSpecForExpand,
            animationSpecForCollapse,
        ),
    )

    constructor(
        initialText: String,
        initialExpanded: Boolean,
        animationSpecForExpand: AnimationSpec<Float>,
        animationSpecForCollapse: AnimationSpec<Float>,
    ): this(
        textField = TextFieldState(initialText),
        initialExpanded,
        animationSpecForExpand,
        animationSpecForCollapse,
    )
}

enum class SearchBarId {
    None,
    First,
    Second,
}

class DualSearchBarState private constructor(
    val first: SingleSearchState,
    val second: SingleSearchState,
) {
    private constructor (
        firstTextField: TextFieldState,
        secondTextField: TextFieldState,

        expanded: SearchBarId,
        animationSpecForExpand: AnimationSpec<Float>,
        animationSpecForCollapse: AnimationSpec<Float>,
    ) : this(
        first = SingleSearchState(
            firstTextField,
            (expanded == SearchBarId.First),
            animationSpecForExpand,
            animationSpecForCollapse,
        ),
        second = SingleSearchState(
            secondTextField,
            (expanded == SearchBarId.Second),
            animationSpecForExpand,
            animationSpecForCollapse,
        ),
    )

    constructor (
        initialText1: String,
        initialText2: String,

        initialExpanded: SearchBarId = SearchBarId.None,
        animationSpecForExpand: AnimationSpec<Float>,
        animationSpecForCollapse: AnimationSpec<Float>,
    ) : this(
        TextFieldState(initialText1),
        TextFieldState(initialText2),
        initialExpanded,
        animationSpecForExpand,
        animationSpecForCollapse,
    )

    val currentExpanded = derivedStateOf {
        when (SearchBarValue.Expanded) {
            first.searchBarState.currentValue -> SearchBarId.First
            second.searchBarState.currentValue -> SearchBarId.Second
            else -> SearchBarId.None
        }
    }

    val targetExpanded = derivedStateOf {
        if (first.searchBarState.targetValue == SearchBarValue.Expanded) {
            SearchBarId.First
        } else if (second.searchBarState.targetValue == SearchBarValue.Expanded) {
            SearchBarId.Second
        } else {
            SearchBarId.None
        }
    }

    suspend fun animateTo(searchBar: SearchBarId) {
        if (searchBar != SearchBarId.First) first.searchBarState.animateToCollapsed()
        if (searchBar != SearchBarId.Second) second.searchBarState.animateToCollapsed()
    }

    suspend fun animateToCollapsed()
        = animateTo(SearchBarId.None)

    companion object {
        fun Saver(
            animationSpecForExpand: AnimationSpec<Float>,
            animationSpecForCollapse: AnimationSpec<Float>,
        ) = mapSaver(
            save = { mapOf(
                "expanded" to it.currentExpanded.value.ordinal,
                "textFields" to listOf(it.first, it.second)
                    .map { singleSearchState ->
                        with (TextFieldState.Saver) { save(singleSearchState.textFieldState) }
                    }
            ) },
            restore = {
                val expanded = SearchBarId.entries[it["expanded"] as Int]
                val textFields = (it["textFields"] as List<*>)
                    .map{ savedTextFieldState ->
                        with (TextFieldState.Saver) { restore(savedTextFieldState!!)!! }
                    }
                DualSearchBarState(
                    firstTextField = textFields[0],
                    secondTextField = textFields[1],
                    expanded,
                    animationSpecForExpand,
                    animationSpecForCollapse,
                )
            },
        )
    }
}

@Composable
fun rememberDualSearchBarState(
    initialText1: String = "",
    initialText2: String = "",
    initialExpanded: SearchBarId = SearchBarId.None,
    animationSpecForExpand: AnimationSpec<Float> = MaterialTheme.motionScheme.slowSpatialSpec(),
    animationSpecForCollapse: AnimationSpec<Float> = MaterialTheme.motionScheme.defaultSpatialSpec(),
): DualSearchBarState {
    return rememberSaveable(
        initialText1,
        initialText2,
        initialExpanded,
        animationSpecForExpand,
        animationSpecForCollapse,
        saver =
            DualSearchBarState.Saver(
                animationSpecForExpand = animationSpecForExpand,
                animationSpecForCollapse = animationSpecForCollapse,
            ),
    ) {
        DualSearchBarState(
            initialText1,
            initialText2,
            initialExpanded,
            animationSpecForExpand,
            animationSpecForCollapse,
        )
    }
}