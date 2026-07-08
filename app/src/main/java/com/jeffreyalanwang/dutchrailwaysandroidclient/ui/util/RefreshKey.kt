package com.jeffreyalanwang.dutchrailwaysandroidclient.ui.util

import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.navigation3.runtime.result.LocalResultEventBus
import androidx.navigation3.runtime.result.ResultEffect
import androidx.navigation3.runtime.result.ResultEventBus

@JvmInline
value class RefreshKey
private constructor(val value: Int) {
    constructor() : this(0)
    fun refreshed() = RefreshKey(value + 1)
}

@JvmInline
value class RefreshKeyState
private constructor(
    val state: MutableState<RefreshKey>
) : State<RefreshKey> by state {

    constructor(value: RefreshKey) : this(mutableStateOf(value))
    constructor() : this(RefreshKey())

    fun refresh() {
        state.value = state.value.refreshed()
    }

}

@Composable
fun rememberRefreshKeyState() = rememberSaveable { RefreshKeyState() }

/**
 * To refresh an entry in the back stack, one can modify the content key of any [entry]. TODO confirm this fact
 * Alternatively, [refreshableEntry] allows a dialog or other screen to refresh its direct
 * parent in the back stack, by sharing a [ResultEventBus] and sending a [RefreshResult].
 */
data object RefreshResult;

@Composable
fun RefreshResultEffect(
    refreshKeyState: RefreshKeyState,
    resultEventBus: ResultEventBus = LocalResultEventBus.current,
    additionalKey: RefreshKey? = null,
) {
    OnChangeEffect(additionalKey) {
        refreshKeyState.refresh()
    }

    ResultEffect<RefreshResult>(
        resultEventBus = resultEventBus,
    ) {
        refreshKeyState.refresh()
    }
}

/**
 * Refresh can be triggered by calling [RefreshKeyState.refresh]
 * or by changing the argument passed to the [key] parameter.
 *
 * If you only needed to update via a change to [key],
 *      you could just directly listen to that.
 * Thus, use of this composable with a [key] parameter
 *      only makes sense when you need to combine it
 *      with a [refreshKeyByResultEffect].
 * As a result, we always include the [refreshKeyByResultEffect].
 */
@Composable
fun refreshKeyByResultEffect(
    resultEventBus: ResultEventBus = LocalResultEventBus.current,
    additionalKey: RefreshKey? = null,
): RefreshKeyState =
    rememberRefreshKeyState()
    .also {
        RefreshResultEffect(it, resultEventBus, additionalKey = additionalKey)
    }