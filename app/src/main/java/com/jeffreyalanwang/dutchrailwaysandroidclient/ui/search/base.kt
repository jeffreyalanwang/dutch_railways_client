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
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import com.jeffreyalanwang.dutchrailwaysandroidclient.R

@Composable
fun BaseSearchInputField(
    placeholderText: String,
    textFieldState: TextFieldState,
    searchBarState: SearchBarState,
    onEnterKeyPressed: () -> Unit = {},
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
    )
}

@Composable
fun ExpandedSearchInputField(
    placeholderText: String,
    textFieldState: TextFieldState,
    searchBarState: SearchBarState,
    onClose: () -> Unit,
    onEnterKeyPressed: () -> Unit,
    onClearedText: () -> Unit,
) {
    BaseSearchInputField(
        placeholderText = placeholderText,
        onEnterKeyPressed = onEnterKeyPressed,
        textFieldState = textFieldState,
        searchBarState = searchBarState,
        leadingIcon = {
            IconButton(
                onClick = onClose
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
        modifier = modifier,
    ) {
        Column( Modifier.verticalScroll(rememberScrollState()) ) {
            results.forEach {
                searchItem(it) {
                    textFieldState.setTextAndPlaceCursorAtEnd(resultToText(it))
                    onSelectResult(it)
                    onClose()
                }
            }
        }
    }
}