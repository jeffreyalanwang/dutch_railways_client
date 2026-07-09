package com.jeffreyalanwang.dutchrailwaysandroidclient

import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.waitUntilExactlyOneExists

fun ComposeUiTest.onNodeAfterExactlyOneExists(
    matcher: SemanticsMatcher,
    timeoutMillis: Long = 1_000L,
): SemanticsNodeInteraction {
    waitUntilExactlyOneExists(matcher, timeoutMillis)
    return onNode(matcher)
}