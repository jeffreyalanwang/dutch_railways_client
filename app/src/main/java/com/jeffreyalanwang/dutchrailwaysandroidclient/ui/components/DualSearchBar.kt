@file:OptIn(ExperimentalMaterial3Api::class)

package com.jeffreyalanwang.dutchrailwaysandroidclient.ui.components

import android.annotation.SuppressLint
import androidx.compose.animation.core.AnimationSpec
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.ui.platform.LocalDensity
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

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            AppBarWithDualSearch(
                dualSearchBarState,
                leadingIcon = {
                    Icon(
                        painter = painterResource(R.drawable.ic_back),
                        contentDescription = null,
                    )
                },
                inputFieldBuilder = { textFieldState, searchBarState, leadingIcon ->
                    SearchBarDefaults.InputField(
                        textFieldState,
                        searchBarState,
                        modifier =  Modifier
                            .fillMaxWidth()
                            .onGloballyPositioned {
                                appBarDividerPos = appBarDividerPos
                                    .copy(second = it.positionInWindow().x)
                            },
                        leadingIcon = leadingIcon,
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
                },
                expandedSearchBuilder = { textFieldState, searchBarState, inputField ->
                    ExpandedFullScreenSearchBar(
                        searchBarState,
                        inputField = inputField,
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
                    HorizontalDivider(
                        Modifier.padding(
                            start = with(LocalDensity.current) {
                                appBarDividerPos
                                    .run { first - second }
                                    .toDp()
                            },
                            end = 16.dp,
                        )
                    )
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
    inputFieldBuilder: @Composable (TextFieldState, SearchBarState, @Composable (() -> Unit)?) -> Unit,
    expandedSearchBuilder: @Composable (TextFieldState, SearchBarState, @Composable () -> Unit) -> Unit,
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
    inputField1: @Composable (TextFieldState, SearchBarState, @Composable (() -> Unit)?) -> Unit,
    inputField2: @Composable (TextFieldState, SearchBarState, @Composable (() -> Unit)?) -> Unit,
    expandedSearch1: @Composable (TextFieldState, SearchBarState, @Composable () -> Unit) -> Unit,
    expandedSearch2: @Composable (TextFieldState, SearchBarState, @Composable () -> Unit) -> Unit,
    divider: @Composable ColumnScope.() -> Unit = {},
    modifier: Modifier = Modifier,
    shape: Shape = CardDefaults.elevatedShape,
    colors: AppBarWithSearchColors = SearchBarDefaults.appBarWithSearchColors(),
    tonalElevation: Dp = SearchBarDefaults.TonalElevation,
    shadowElevation: Dp = SearchBarDefaults.ShadowElevation,
    contentPadding: PaddingValues = SearchBarDefaults.AppBarContentPadding,
    windowInsets: WindowInsets = SearchBarDefaults.windowInsets,
) {
    @Composable
    fun content(
        modifier: Modifier,
        tonalElevation: Dp,
        shadowElevation: Dp,
    ) {
        Row(
            modifier,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            val isVisible = state.currentExpanded.value == SearchBarId.None ||
                    state.targetExpanded.value == SearchBarId.None
            Box(
                modifier = Modifier
                    .weight(1f)
                    .alpha(if (isVisible) 1f else 0f)
            ) {
                DualSearchBar(
                    state = state,
                    leadingIcon = leadingIcon,
                    inputField1 = inputField1,
                    inputField2 = inputField2,
                    expandedSearch1 = expandedSearch1,
                    expandedSearch2 = expandedSearch2,
                    divider = divider,
                    modifier =
                        Modifier
                            .padding(
                                horizontal = 8.dp,
                                vertical = 4.dp,
                            )
                            .widthIn(min = 360.dp, max = 720.dp)
                            .align(Alignment.Center),
                    shape = shape,
                    colors = colors.searchBarColors,
                    tonalElevation = tonalElevation,
                    shadowElevation = shadowElevation,
                )
            }
        }
    }

    if (colors.appBarContainerColor != Color.Transparent) {
        Surface(
            color = colors.appBarContainerColor,
            modifier = modifier.padding(contentPadding)
                .windowInsetsPadding(windowInsets)
                .fillMaxWidth()
                .semantics { isTraversalGroup = true },
            tonalElevation = tonalElevation,
            shadowElevation = shadowElevation,
        ) {
            content(
                Modifier,
                tonalElevation = 0.dp,
                shadowElevation = 0.dp,
            )
        }
    } else {
        content(
            modifier.padding(contentPadding)
                .windowInsetsPadding(windowInsets)
                .fillMaxWidth()
                .semantics { isTraversalGroup = true },
            tonalElevation = tonalElevation,
            shadowElevation = shadowElevation,
        )
    }
}

@SuppressLint("ModifierParameter")
@Composable
fun DualSearchBar(
    state: DualSearchBarState,
    leadingIcon: @Composable (() -> Unit)? = null,
    inputFieldBuilder: @Composable (TextFieldState, SearchBarState, @Composable (()->Unit)?) -> Unit,
    expandedSearchBuilder: @Composable (TextFieldState, SearchBarState, @Composable ()->Unit) -> Unit,
    divider: @Composable ColumnScope.() -> Unit = {},
    modifier: Modifier = Modifier,
    shape: Shape = CardDefaults.elevatedShape,
    colors: SearchBarColors = SearchBarDefaults.colors(),
    tonalElevation: Dp = SearchBarDefaults.TonalElevation,
    shadowElevation: Dp = SearchBarDefaults.ShadowElevation,
) = DualSearchBar(
    state,
    leadingIcon = leadingIcon,
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
    leadingIcon: @Composable (() -> Unit)?,
    inputField1: @Composable (TextFieldState, SearchBarState, @Composable (()->Unit)?) -> Unit,
    inputField2: @Composable (TextFieldState, SearchBarState, @Composable (()->Unit)?) -> Unit,
    expandedSearch1: @Composable (TextFieldState, SearchBarState, @Composable ()->Unit) -> Unit,
    expandedSearch2: @Composable (TextFieldState, SearchBarState, @Composable ()->Unit) -> Unit,
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
                    inputField1(leadingIcon)
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
                    inputField2(leadingIcon?.let { {} })
                }
            }
        }
    }

    with (state.first) { expandedSearch1 { inputField1(null) } }
    with (state.second) { expandedSearch2 { inputField2(null) } }
}

@Composable
context(state: SingleSearchState)
operator fun (@Composable (TextFieldState, SearchBarState) -> Unit)
    .invoke()
        = this(state.textFieldState, state.searchBarState)

@Composable
context(state: SingleSearchState)
operator fun <T: @Composable (()->Unit)?> (@Composable (TextFieldState, SearchBarState, T) -> Unit)
    .invoke(content: T)
        = this(state.textFieldState, state.searchBarState, content)

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
                val expanded = SearchBarId.entries.get(it["expanded"] as Int)
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