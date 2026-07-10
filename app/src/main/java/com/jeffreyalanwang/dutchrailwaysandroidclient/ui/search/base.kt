package com.jeffreyalanwang.dutchrailwaysandroidclient.ui.search

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.clearText
import androidx.compose.foundation.text.input.setTextAndPlaceCursorAtEnd
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExpandedFullScreenSearchBar
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.SearchBarDefaults
import androidx.compose.material3.SearchBarState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import com.jeffreyalanwang.dutchrailwaysandroidclient.R
import com.jeffreyalanwang.dutchrailwaysandroidclient.ui.components.NavBackButton

/**
 * @see ExpandedSearchInputField
 */
@Composable
fun BaseSearchInputField(
    placeholderText: String,
    textFieldState: TextFieldState,
    searchBarState: SearchBarState,
    modifier: Modifier = Modifier,
    onEnterKeyPressed: () -> Unit = {},
    onFocusChanged: (Boolean) -> Unit = {},
    leadingIcon: @Composable (() -> Unit)? = null,
    trailingIcon: @Composable (() -> Unit)? = {
        Icon(
            painterResource(R.drawable.ic_search),
            contentDescription = "Search",
        )
    },
) {
    SearchBarDefaults.InputField(
        textFieldState = textFieldState,
        searchBarState = searchBarState,
        onSearch = { onEnterKeyPressed() },
        placeholder = {
            Text(
                modifier = Modifier.clearAndSetSemantics {},
                text = placeholderText,
            )
        },
        leadingIcon = leadingIcon,
        trailingIcon = trailingIcon,
        modifier = modifier
            .onFocusChanged{ onFocusChanged(it.hasFocus) }
            .testTag("search_input")
    )
}

/**
 * @see BaseSearchInputField
 * @see ExpandedSearch
 */
@Composable
fun ExpandedSearchInputField(
    placeholderText: String,
    textFieldState: TextFieldState,
    searchBarState: SearchBarState,
    onClose: () -> Unit,
    onEnterKeyPressed: () -> Unit,
    onClearedText: () -> Unit,
    onFocusChanged: (Boolean) -> Unit = {},
) {
    BaseSearchInputField(
        placeholderText = placeholderText,
        onEnterKeyPressed = onEnterKeyPressed,
        textFieldState = textFieldState,
        searchBarState = searchBarState,
        onFocusChanged = onFocusChanged,
        leadingIcon = {
            NavBackButton(
                onClose,
                contentDescription = "Close search",
            )
        },
        trailingIcon = {
            IconButton(
                onClick = {
                    textFieldState.clearText()
                    onClearedText()
                }
            ) {
                Icon(
                    painterResource(R.drawable.ic_close),
                    contentDescription = "Clear",
                )
            }
        },
    )
}

/**
 * Convenience function that composes a [ExpandedFullScreenSearchBar].
 * @param results           The list of results, as arbitrary data objects.
 * @param resultToText      Transforms the user selection from [results]
 *                          into a string which populates the text field.
 * @param placeholderText
 * @param textFieldState
 * @param searchBarState
 * @param modifier
 * @param onClose           Called when the user requests to close the search
 *                          dialog. Most likely a call to `CoroutineScope.launch
 *                          { searchBarState.animateToCollapsed() }`.
 * @param onSelectResult    Handles the user-selected search result.
 *                          Should not handle populating the text field.
 *                          Should not handle collapsing the search dialog.
 * @param onClearedText     Handles all state change when the user clears
 *                          the text field.
 * @param inputField
 * @param searchItem        Transforms data objects from [results] into
 *                          composables in the UI. Responsible for calling
 *                          the provided [onClick] lambda when clicked,
 *                          which references the other parameters of this
 *                          function.
 */
@Composable
fun <T> ExpandedSearch(
    results: List<T>,
    resultToText: (T) -> String,
    placeholderText: String,
    textFieldState: TextFieldState,
    searchBarState: SearchBarState,
    modifier: Modifier = Modifier,
    onClose: () -> Unit,
    onSelectResult: (T?) -> Unit,
    onClearedText: () -> Unit = {
        textFieldState.clearText()
        onSelectResult(null)
    },
    onFocusChanged: (Boolean) -> Unit = {},
    inputField: @Composable () -> Unit = {
        ExpandedSearchInputField(
            placeholderText,
            onEnterKeyPressed = { onSelectResult(results.getOrNull(0)) },
            onClose = onClose,
            onClearedText = onClearedText,
            textFieldState = textFieldState,
            searchBarState = searchBarState,
        )
    },
    searchItem: @Composable (T, onClick: () -> Unit) -> Unit,
) {
    ExpandedFullScreenSearchBar(
        state = searchBarState,
        inputField = inputField,
        modifier = modifier.onFocusChanged { onFocusChanged(it.hasFocus) },
    ) {
        Column( Modifier.verticalScroll(rememberScrollState()) ) {
            results.forEach {
                key(it) {
                    searchItem(it, { // onClick
                        textFieldState.setTextAndPlaceCursorAtEnd(resultToText(it))
                        onSelectResult(it)
                        onClose()
                    })
                }
            }
        }
    }
}
