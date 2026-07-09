package com.jeffreyalanwang.dutchrailwaysandroidclient.ui.util

import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.retain.retain
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
fun retainRefreshKeyState() = retain { RefreshKeyState() }
// TODO if we need [retain], that implies that we need to
// change the key even after the item leaves composition
// (e.g. when it is covered up on the back stack),
// which means that we are trying to refresh state which
// is also beging retained in the back stack.
// But we are not trying to refresh any ViewModels, and I
// don't think I've called retain() anywhere else yet.

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
    retainRefreshKeyState()
    .also {
        RefreshResultEffect(it, resultEventBus, additionalKey = additionalKey)
    }

@Composable
fun RefreshesOnResult(
    resultEventBus: ResultEventBus = LocalResultEventBus.current,
    additionalKey: RefreshKey? = null,
    content: @Composable () -> Unit,
) {
    val key by refreshKeyByResultEffect(resultEventBus, additionalKey = additionalKey)
    key(key) { content() }
}