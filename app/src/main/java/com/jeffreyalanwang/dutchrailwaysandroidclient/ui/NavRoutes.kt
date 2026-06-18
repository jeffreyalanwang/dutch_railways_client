package com.jeffreyalanwang.dutchrailwaysandroidclient.ui

import androidx.compose.foundation.layout.RowScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.rememberViewModelStoreOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.scene.SinglePaneSceneStrategy
import androidx.navigation3.ui.NavDisplay
import com.jeffreyalanwang.dutchrailwaysandroidclient.BackendApi
import com.jeffreyalanwang.dutchrailwaysandroidclient.ui.components.PredictiveBackDialogSceneStrategy
import com.jeffreyalanwang.dutchrailwaysandroidclient.ui.components.PredictiveBackDialogSceneStrategy.Companion.predictiveBackDialog
import com.jeffreyalanwang.dutchrailwaysandroidclient.ui.detailScreens.AreaDetailScreen
import com.jeffreyalanwang.dutchrailwaysandroidclient.ui.detailScreens.PassServiceDetailScreen
import com.jeffreyalanwang.dutchrailwaysandroidclient.ui.detailScreens.StationDetailScreen
import com.jeffreyalanwang.dutchrailwaysandroidclient.ui.screens.child.ConfirmDeletePassServiceDialog
import com.jeffreyalanwang.dutchrailwaysandroidclient.ui.screens.child.EditAreaScreen
import com.jeffreyalanwang.dutchrailwaysandroidclient.ui.screens.child.EditPassServiceScreen
import com.jeffreyalanwang.dutchrailwaysandroidclient.ui.screens.child.EditStationScreen
import com.jeffreyalanwang.dutchrailwaysandroidclient.ui.screens.child.NewPassServiceScreen
import com.jeffreyalanwang.dutchrailwaysandroidclient.ui.screens.top.EditActions
import com.jeffreyalanwang.dutchrailwaysandroidclient.ui.screens.top.EditScreen
import com.jeffreyalanwang.dutchrailwaysandroidclient.ui.screens.top.EndpointTimePicker
import com.jeffreyalanwang.dutchrailwaysandroidclient.ui.screens.top.StationSearchScreen
import com.jeffreyalanwang.dutchrailwaysandroidclient.ui.screens.top.TripFinderScreen
import com.jeffreyalanwang.dutchrailwaysandroidclient.ui.util.rememberNavBackStack
import com.jeffreyalanwang.dutchrailwaysandroidclient.ui.viewmodel.Endpoint
import com.jeffreyalanwang.dutchrailwaysandroidclient.ui.viewmodel.TripFinderViewModel
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
 * A page represented by [TripFinderGraphNavArgs] that is not
 * the top-level route (i.e. [TripFinderStartNavArgs])
 */
interface TripFinderGraphChildNavArgs: TripFinderGraphNavArgs

/**
 * Corresponds to "Edit" tab ([EditScreen]
 * [com.jeffreyalanwang.dutchrailwaysandroidclient.ui.screens.top.EditScreen]).
 */
interface EditGraphNavArgs: AppNavArgs

/**
 * A page which all tabs' nested [NavDisplay]s should be able to navigate to.
 * Generally, these can all be added at once using [EntryProviderScope.tabEntries],
 * but can be delegated differently for specific tabs.
 */
interface CommonChildNavArgs: AppNavArgs, TripFinderGraphChildNavArgs, EditGraphNavArgs
interface PlaceDetailNavArgs: CommonChildNavArgs { val id: Int }

// Tabs' NavKey types (used in both top-level [MainActivity] and nested [NavDisplay] back stacks)
@Serializable data object TripFinderStartNavArgs : AppNavArgs, TripFinderGraphNavArgs, TripFinderGraphMajorNavArgs
@Serializable data object StationSearchStartNavArgs : AppNavArgs
@Serializable data object EditStartNavArgs : AppNavArgs, EditGraphNavArgs

// Child navigation routes' NavKey types
@Serializable data class AreaDetailNavArgs(override val id: Int) : PlaceDetailNavArgs
@Serializable data class StationDetailNavArgs(override val id: Int) : PlaceDetailNavArgs
@Serializable data class PassServiceDetailNavArgs(val id: Int) : CommonChildNavArgs
@Serializable data object JourneyListNavArgs : AppNavArgs, TripFinderGraphChildNavArgs, TripFinderGraphMajorNavArgs
@Serializable data class JourneyDetailNavArgs(val index: Int) : TripFinderGraphChildNavArgs
@Serializable data class EndpointTimePickerNavArgs(val forEndpoint: Endpoint) : TripFinderGraphChildNavArgs
@Serializable data class EditStationNavArgs(val id: Int) : EditGraphNavArgs
@Serializable data class EditAreaNavArgs(val id: Int) : EditGraphNavArgs
@Serializable data class NewPassServiceNavArgs(val basedOnId: Int? = null) : EditGraphNavArgs
@Serializable data class EditPassServiceNavArgs(val id: Int) : EditGraphNavArgs
@Serializable data class ConfirmDeletePassServiceNavArgs(val id: Int) : EditGraphNavArgs

/**
 * Returns navigation entries for pages in the main screen's bottom navbar.
 */
@Composable
fun appEntries(
    // Specific values are meaningless; they simply need to change
    // in order to reset the corresponding top-level tab's state
    resetKeyState: Map<AppNavArgs, Int>,
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
                sceneStrategy = remember {
                    PredictiveBackDialogSceneStrategy<TripFinderGraphNavArgs>()
                        .then(SinglePaneSceneStrategy())
                },
            ) { key ->
                // Do not use [entryProvider { }]; this allows us to use a
                // single composable as a default for most [NavArgs] types
                when (key) {
                    is EndpointTimePickerNavArgs -> NavEntry(
                        key = key,
                        metadata = predictiveBackDialog(),
                    ) { key ->
                        key as EndpointTimePickerNavArgs
                        EndpointTimePicker(
                            key.forEndpoint,
                            viewModel = viewModel,
                            onDismiss = { viewModel.popBack() },
                        )
                    }

                    else -> NavEntry(
                        key = key,

                        // Same value for all routes within this NavDisplay,
                        // means we use the same SinglePaneScene, thus same
                        // TripFinderScreen
                        contentKey = true,
                    ) { key ->
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
                    tabEntries(
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
                sceneStrategy = remember {
                    PredictiveBackDialogSceneStrategy<EditGraphNavArgs>()
                        .then(SinglePaneSceneStrategy())
                },
                entryProvider = entryProvider {
                    entry<EditStartNavArgs> { navArgs ->
                        EditScreen(
                            onNavigate = { newNavArgs -> backstack.add(newNavArgs) }
                        )
                    }
                    tabEntries(
                        onNavigate = { backstack.add(it) },
                        onNavigateBack = { backstack.removeAt(backstack.lastIndex) },
                    ) { navArgs ->
                        EditActions(
                            navArgs,
                            onNavigate = { backstack.add(it) }
                        )
                    }
                    entry<EditStationNavArgs> { navArgs ->
                        EditStationScreen(
                            id = navArgs.id
                        )
                    }
                    entry<EditAreaNavArgs> { navArgs ->
                        EditAreaScreen(
                            id = navArgs.id
                        )
                    }

                    entry<NewPassServiceNavArgs> { navArgs ->
                        val basedOnService = navArgs.basedOnId
                            ?.let { BackendApi.get_pass_service(it) }
                        NewPassServiceScreen(
                            basedOnService,
                            onNavigate = { newNavArgs -> backstack.add(newNavArgs) }
                        )
                    }
                    entry<EditPassServiceNavArgs> { navArgs ->
                        EditPassServiceScreen(
                            navArgs.id
                        )
                    }
                    entry<ConfirmDeletePassServiceNavArgs> { navArgs ->
                        // Also has the responsibility of executing the deletion.
                        ConfirmDeletePassServiceDialog(
                            navArgs.id
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
 * [T] must be a superclass of [CommonChildNavArgs].
 *
 * @param onNavigate:
 *      Used for the base detail Composable,
 *      but not for caller-provided [actions] slot.
 */
@Suppress("UNCHECKED_CAST")
fun <T: AppNavArgs> EntryProviderScope<T>.tabEntries(
    onNavigate: (CommonChildNavArgs) -> Unit,
    onNavigateBack: () -> Unit,
    actions: @Composable (RowScope.(CommonChildNavArgs) -> Unit)? = null,
) {
    // Enables smart cast for rest of method body
    this as EntryProviderScope<CommonChildNavArgs>

    entry<AreaDetailNavArgs> { navArgs ->
        AreaDetailScreen(
            rememberSaveable(navArgs.id) { BackendApi.get_area_info(navArgs.id) },
            onNavigate = onNavigate,
            onNavigateBack = onNavigateBack,
            actionsSlot = actions?.let{ { it(navArgs) } },
        )
    }
    entry<StationDetailNavArgs> { navArgs ->
        StationDetailScreen(
            rememberSaveable(navArgs.id) { BackendApi.get_station_info(navArgs.id) },
            onNavigate = onNavigate,
            onNavigateBack = onNavigateBack,
            actionsSlot = actions?.let{ { it(navArgs) } },
        )
    }
    entry<PassServiceDetailNavArgs> { navArgs ->
        PassServiceDetailScreen(
            rememberSaveable(navArgs.id) { BackendApi.get_pass_service(navArgs.id) },
            onNavigate = onNavigate,
            onNavigateBack = onNavigateBack,
            actionsSlot = actions?.let{ { it(navArgs) } },
        )
    }
}
