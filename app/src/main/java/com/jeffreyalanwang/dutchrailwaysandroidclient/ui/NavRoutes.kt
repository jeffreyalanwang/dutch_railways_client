package com.jeffreyalanwang.dutchrailwaysandroidclient.ui

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
 * A page which all tabs' nested [NavDisplay]s should be able to navigate to.
 * Generally, these can all be added at once using [EntryProviderScope.tabEntries],
 * but can be delegated differently for specific tabs.
 */
interface CommonChildNavArgs: AppNavArgs, TripFinderGraphChildNavArgs
interface PlaceDetailNavArgs: CommonChildNavArgs { val id: Int }

// Tabs' NavKey types (used in both top-level [MainActivity] and nested [NavDisplay] back stacks)
@Serializable data object TripFinderStartNavArgs : AppNavArgs, TripFinderGraphNavArgs, TripFinderGraphMajorNavArgs
@Serializable data object StationSearchNavArgs : AppNavArgs

// Child navigation routes' NavKey types
@Serializable data class AreaDetailNavArgs(override val id: Int) : PlaceDetailNavArgs, TripFinderGraphChildNavArgs
@Serializable data class StationDetailNavArgs(override val id: Int) : PlaceDetailNavArgs, TripFinderGraphChildNavArgs
@Serializable data class PassServiceDetailNavArgs(val id: Int) : CommonChildNavArgs, TripFinderGraphChildNavArgs
@Serializable data object JourneyListNavArgs : AppNavArgs, TripFinderGraphChildNavArgs, TripFinderGraphMajorNavArgs
@Serializable data class JourneyDetailNavArgs(val index: Int) : TripFinderGraphChildNavArgs
@Serializable data class TimePickerNavArgs(val forEndpoint: Endpoint) : TripFinderGraphChildNavArgs

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
        key(resetKeyState.get(initialNavArgs)) {
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
                when (key) {
                    is TimePickerNavArgs -> NavEntry(
                        key = key,
                        metadata = predictiveBackDialog(),
                    ) { key ->
                        key as TimePickerNavArgs
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

    entry<StationSearchNavArgs> { initialNavArgs ->
        key(resetKeyState.get(initialNavArgs)) {
            val backstack = rememberNavBackStack<AppNavArgs>(initialNavArgs)

            NavDisplay(
                backStack = backstack,
                entryProvider = entryProvider {
                    entry<StationSearchNavArgs> { navArgs ->
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
}
/**
 * Returns navigation entries for pages which may be opened by
 * (thus pushed on top of) top-level pages in the back stack.
 *
 * [T] must be a superclass of [CommonChildNavArgs].
 */
@Suppress("UNCHECKED_CAST")
fun <T: AppNavArgs> EntryProviderScope<T>.tabEntries(
    onNavigate: (CommonChildNavArgs) -> Unit,
    onNavigateBack: () -> Unit,
) {
    // Enables smart cast for rest of method body
    this as EntryProviderScope<CommonChildNavArgs>

    entry<AreaDetailNavArgs> { navArgs ->
        AreaDetailScreen(
            rememberSaveable(navArgs.id) { BackendApi.get_area_info(navArgs.id) },
            onNavigate = onNavigate,
            onNavigateBack = onNavigateBack,
        )
    }
    entry<StationDetailNavArgs> { navArgs ->
        StationDetailScreen(
            rememberSaveable(navArgs.id) { BackendApi.get_station_info(navArgs.id) },
            onNavigate = onNavigate,
            onNavigateBack = onNavigateBack,
        )
    }
    entry<PassServiceDetailNavArgs> { navArgs ->
        PassServiceDetailScreen(
            rememberSaveable(navArgs.id) { BackendApi.get_pass_service(navArgs.id) },
            onNavigate = onNavigate,
            onNavigateBack = onNavigateBack,
        )
    }
}
