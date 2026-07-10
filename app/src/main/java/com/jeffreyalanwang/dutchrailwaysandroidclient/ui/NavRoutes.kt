package com.jeffreyalanwang.dutchrailwaysandroidclient.ui

import androidx.compose.foundation.layout.RowScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.rememberViewModelStoreOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.runtime.result.LocalResultEventBus
import androidx.navigation3.runtime.result.ResultEffect
import androidx.navigation3.runtime.result.rememberResultEventBusNavEntryDecorator
import androidx.navigation3.scene.SinglePaneSceneStrategy
import androidx.navigation3.ui.NavDisplay
import backend.BackendApi
import com.jeffreyalanwang.dutchrailwaysandroidclient.ui.components.PredictiveBackDialogSceneStrategy
import com.jeffreyalanwang.dutchrailwaysandroidclient.ui.components.PredictiveBackDialogSceneStrategy.Companion.predictiveBackDialog
import com.jeffreyalanwang.dutchrailwaysandroidclient.ui.components.TimePicker
import com.jeffreyalanwang.dutchrailwaysandroidclient.ui.screens.child.detail.AreaDetailScreen
import com.jeffreyalanwang.dutchrailwaysandroidclient.ui.screens.child.edit.ConfirmDeletePassService
import com.jeffreyalanwang.dutchrailwaysandroidclient.ui.screens.child.edit.EditAreaScreen
import com.jeffreyalanwang.dutchrailwaysandroidclient.ui.screens.child.edit.EditPassServiceScreen
import com.jeffreyalanwang.dutchrailwaysandroidclient.ui.screens.child.edit.EditStationScreen
import com.jeffreyalanwang.dutchrailwaysandroidclient.ui.screens.child.edit.NewPassServiceScreen
import com.jeffreyalanwang.dutchrailwaysandroidclient.ui.screens.child.detail.PassServiceDetailScreen
import com.jeffreyalanwang.dutchrailwaysandroidclient.ui.screens.child.detail.StationDetailScreen
import com.jeffreyalanwang.dutchrailwaysandroidclient.ui.screens.top.EditActions
import com.jeffreyalanwang.dutchrailwaysandroidclient.ui.screens.top.EditScreen
import com.jeffreyalanwang.dutchrailwaysandroidclient.ui.screens.top.StationSearchScreen
import com.jeffreyalanwang.dutchrailwaysandroidclient.ui.screens.top.TripFinderScreen
import com.jeffreyalanwang.dutchrailwaysandroidclient.ui.util.RefreshKey
import com.jeffreyalanwang.dutchrailwaysandroidclient.ui.util.RefreshKeyState
import com.jeffreyalanwang.dutchrailwaysandroidclient.ui.util.RefreshResult
import com.jeffreyalanwang.dutchrailwaysandroidclient.ui.util.RefreshesOnResult
import com.jeffreyalanwang.dutchrailwaysandroidclient.ui.util.rememberNavBackStack
import com.jeffreyalanwang.dutchrailwaysandroidclient.ui.viewmodel.TripFinderViewModel
import kotlinx.datetime.LocalTime
import kotlinx.serialization.Serializable

/** All [NavKey] instances in this app should inherit from this type. */
interface AppNavArgs: NavKey

/**
 * Represents a UI configuration in the [AppDestinations.TRIP] tab,
 * i.e. rendered with [TripFinderScreen].
 */
interface TripFinderGraphNavArgs: AppNavArgs
/**
 * A [TripFinderGraphNavArgs] that cannot be added or removed from the
 * back stack without making modifications to data provided by the
 * ViewModel in [TripFinderDataModel]
 * [com.jeffreyalanwang.dutchrailwaysandroidclient.ui.viewmodel.TripFinderDataModel].
 *
 * @see TripFinderViewModel
 * **/
interface TripFinderGraphMajorNavArgs: TripFinderGraphNavArgs

/**
 * Corresponds to "Edit" tab ([EditScreen]
 * [com.jeffreyalanwang.dutchrailwaysandroidclient.ui.screens.top.EditScreen]).
 */
interface EditGraphNavArgs: AppNavArgs

/**
 * A page which all tabs' nested [NavDisplay]s should be able to navigate to.
 * Generally, these can all be added at once using [EntryProviderScope.commonChildEntries],
 * but can be delegated differently for specific tabs.
 */
interface CommonChildNavArgs: AppNavArgs, TripFinderGraphNavArgs, EditGraphNavArgs
interface PlaceDetailNavArgs: CommonChildNavArgs { val id: Int }

// Tabs' NavKey types (used in both top-level [MainActivity] and nested [NavDisplay] back stacks)
@Serializable data object TripFinderStartNavArgs : AppNavArgs, TripFinderGraphNavArgs, TripFinderGraphMajorNavArgs
@Serializable data object StationSearchStartNavArgs : AppNavArgs
@Serializable data object EditStartNavArgs : AppNavArgs, EditGraphNavArgs

// Child navigation routes' NavKey types

@Serializable data object JourneyListNavArgs : AppNavArgs, TripFinderGraphMajorNavArgs
@Serializable data class JourneyDetailNavArgs(val index: Int) : TripFinderGraphNavArgs
@Serializable data class EditStationNavArgs(val id: Int) : EditGraphNavArgs
@Serializable data class EditAreaNavArgs(val id: Int) : EditGraphNavArgs
@Serializable data class NewPassServiceNavArgs(val basedOnId: Int? = null) : EditGraphNavArgs
@Serializable data class EditPassServiceNavArgs(val id: Int) : EditGraphNavArgs
@Serializable data class ConfirmDeletePassServiceNavArgs(val id: Int) : EditGraphNavArgs

@Serializable data class AreaDetailNavArgs(override val id: Int) : PlaceDetailNavArgs
@Serializable data class StationDetailNavArgs(override val id: Int) : PlaceDetailNavArgs
@Serializable data class PassServiceDetailNavArgs(val id: Int) : CommonChildNavArgs
@Serializable data class TimePickerNavArgs<T>(
    val tag: T,
    val title: String,
    val initialTime: LocalTime? = null,
    val clearable: Boolean = false,
    val enableKeyboard: Boolean = true,
) : CommonChildNavArgs


/**
 * Returns navigation entries for pages in the main screen's bottom navbar.
 */
@Composable
fun appEntries(
    // Specific values are meaningless; they simply need to change
    // in order to reset the corresponding top-level tab's state
    resetKeyState: Map<AppNavArgs, RefreshKey>,
) = entryProvider {

    entry<TripFinderStartNavArgs> { initialNavArgs ->
        key(resetKeyState[initialNavArgs]) {
            // Dispose + regenerate view model when key changes
            val viewModelStoreOwner = rememberViewModelStoreOwner()

            val viewModel = viewModel<TripFinderViewModel>(viewModelStoreOwner)
            val backStack by viewModel.backStack.collectAsStateWithLifecycle()

            NavDisplay(
                backStack = backStack,
                onBack = { viewModel.popBack() },
                entryDecorators = listOf(
                    rememberSaveableStateHolderNavEntryDecorator(),
                    rememberResultEventBusNavEntryDecorator(),
                ),
                sceneStrategies = remember {
                    listOf(
                        PredictiveBackDialogSceneStrategy(),
                        SinglePaneSceneStrategy(),
                    )
                },
            ) { key ->
                // Do not use [entryProvider { }]; this allows us to use a
                // single composable as a default for most [NavArgs] types
                when (key) {
                    is TimePickerNavArgs<*> -> NavEntry(
                        key = key,
                        metadata = predictiveBackDialog(),
                    ) {
                        TimePicker(key) { viewModel.popBack() }
                    }

                    else -> NavEntry(
                        key = key,

                        // Same value for all routes within this NavDisplay,
                        // means we use the same SinglePaneScene, thus same
                        // TripFinderScreen
                        contentKey = true,
                    ) {
                        TripFinderScreen(
                            key,
                            viewModel,
                            onNavigateMinor = { viewModel.pushUserRequested(it) }
                        )
                    }
                }
            }
        }
    }

    entry<StationSearchStartNavArgs> { initialNavArgs ->
        key(resetKeyState[initialNavArgs]) {
            val backstack = rememberNavBackStack<AppNavArgs>(initialNavArgs)

            NavDisplay(
                backStack = backstack,
                entryProvider = entryProvider {
                    entry<StationSearchStartNavArgs> { navArgs ->
                        StationSearchScreen(
                            onNavigate = { newNavArgs -> backstack.add(newNavArgs) }
                        )
                    }
                    commonChildEntries(
                        { backstack.add(it) },
                        { backstack.removeAt(backstack.lastIndex) }
                    )
                },
            )
        }
    }

    entry<EditStartNavArgs> { initialNavArgs ->
        key(resetKeyState[initialNavArgs]) {
            val backstack = rememberNavBackStack<EditGraphNavArgs>(initialNavArgs)

            NavDisplay(
                backStack = backstack,
                sceneStrategies = remember {
                    listOf(
                        PredictiveBackDialogSceneStrategy(),
                        SinglePaneSceneStrategy(),
                    )
                },
                entryDecorators = listOf(
                    rememberSaveableStateHolderNavEntryDecorator(),
                    rememberViewModelStoreNavEntryDecorator(), // ensure separate ViewModelStore for each edit screen
                    rememberResultEventBusNavEntryDecorator(), // allow edit screens to trigger refresh on the previous info screen
                ),
                entryProvider = entryProvider {
                    entry<EditStartNavArgs> { navArgs ->
                        EditScreen(
                            onNavigate = { newNavArgs -> backstack.add(newNavArgs) }
                        )
                    }

                    entry<EditStationNavArgs> { navArgs ->
                        val resultEventBus = LocalResultEventBus.current
                        EditStationScreen(
                            id = navArgs.id,
                            onCancelRequest = { backstack.removeLast() },
                            onSaveFinished = {
                                resultEventBus.sendResult(RefreshResult)
                                backstack.removeLast()
                            },
                        )
                    }
                    entry<EditAreaNavArgs> { navArgs ->
                        val resultEventBus = LocalResultEventBus.current
                        EditAreaScreen(
                            id = navArgs.id,
                            onCancelRequest = { backstack.removeLast() },
                            onSaveFinished = {
                                resultEventBus.sendResult(RefreshResult)
                                backstack.removeLast()
                            },
                        )
                    }

                    entry<NewPassServiceNavArgs> { navArgs ->
                        val resultEventBus = LocalResultEventBus.current
                        NewPassServiceScreen(
                            navArgs.basedOnId
                                ?.let { BackendApi.get_pass_service(it) },
                            onCancelRequest = { backstack.removeLast() },
                            onSaveFinished = {
                                resultEventBus.sendResult(RefreshResult)
                                backstack.removeLast()
                            },
                        )
                    }
                    entry<EditPassServiceNavArgs> { navArgs ->
                        val resultEventBus = LocalResultEventBus.current
                        EditPassServiceScreen(
                            navArgs.id,
                            onCancelRequest = { backstack.removeLast() },
                            onSaveFinished = {
                                resultEventBus.sendResult(RefreshResult)
                                backstack.removeLast()
                            },
                        )
                    }
                    entry<ConfirmDeletePassServiceNavArgs>(
                        metadata = predictiveBackDialog()
                    ) { navArgs ->
                        val resultEventBus = LocalResultEventBus.current
                        val service = BackendApi.get_pass_service(navArgs.id)
                        ConfirmDeletePassService(
                            service.id,
                            service.title,
                            onCancelRequest = { backstack.removeLast() },
                            onDeleteFinished = {
                                backstack.removeLast()
                                resultEventBus.sendResult(DeletedResult)
                            },
                        )
                    }

                    commonChildEntries(
                        onNavigate = { backstack.add(it) },
                        onNavigateBack = { backstack.removeAt(backstack.lastIndex) },
                    ) { navArgs ->
                        // Actions bar content
                        EditActions(
                            navArgs,
                            onNavigate = { backstack.add(it) },
                        )
                    }
                }
            )
        }
    }

}

/**
 * Returns navigation entries for pages which may be opened by
 * (thus pushed on top of) top-level pages in the back stack.
 *
 * Entries which are more recent on the back stack may return a
 * result of type [RefreshResult] to reload one of these
 * parent entries.
 *
 * [T] must be a superclass of [CommonChildNavArgs].
 *
 * @param onNavigate:
 *      Used for the base detail Composable,
 *      but not for caller-provided [actions] slot.
 */
@Suppress("UNCHECKED_CAST")
fun <T: AppNavArgs> EntryProviderScope<T>.commonChildEntries(
    onNavigate: (CommonChildNavArgs) -> Unit,
    onNavigateBack: () -> Unit,
    actions: @Composable (RowScope.(CommonChildNavArgs) -> Unit)? = null,
) {
    // Enables smart cast for rest of method body
    this as EntryProviderScope<CommonChildNavArgs>

    entry<AreaDetailNavArgs> { navArgs ->
        RefreshesOnResult {
            val area =
                rememberSaveable { BackendApi.get_area_info(navArgs.id) }
            AreaDetailScreen(
                area,
                onNavigate = onNavigate,
                onNavigateBack = onNavigateBack,
                actionsSlot = actions?.let { { it(navArgs) } },
            )
        }
    }
    entry<StationDetailNavArgs> { navArgs ->
        RefreshesOnResult {
            val station =
                rememberSaveable { BackendApi.get_station_info(navArgs.id) }
            StationDetailScreen(
                station,
                onNavigate = onNavigate,
                onNavigateBack = onNavigateBack,
                actionsSlot = actions?.let { { it(navArgs) } },
            )
        }
    }
    entry<PassServiceDetailNavArgs> { navArgs ->
        with (LocalResultEventBus.current) {
            var isDeleted by remember { mutableStateOf(false) }
            ResultEffect<DeletedResult>(this) {
                isDeleted = true
                sendResult(RefreshResult)
                onNavigateBack()
            }
            if (isDeleted) return@entry
        }

        // We need to refresh the entire composable because
        // recompose would be triggered by changes in the [passService]
        // but not by any stops.
        RefreshesOnResult {
            val passService =
                rememberSaveable { BackendApi.get_pass_service(navArgs.id) }
            PassServiceDetailScreen(
                passService,
                onNavigate = onNavigate,
                onNavigateBack = onNavigateBack,
                actionsSlot = actions?.let { { it(navArgs) } },
            )
        }
    }

    entry<TimePickerNavArgs<*>>(
        metadata = predictiveBackDialog(),
    ) { navArgs ->
        TimePicker(navArgs, onNavigateBack)
    }
}

/**
 * Received by a detail screen to indicate that
 * the content it is intended to display has been deleted.
 */
@Serializable data object DeletedResult;