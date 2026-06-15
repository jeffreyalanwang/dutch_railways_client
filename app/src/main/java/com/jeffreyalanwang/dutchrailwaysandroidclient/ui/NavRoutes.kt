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
import com.jeffreyalanwang.dutchrailwaysandroidclient.ui.detailScreens.StationDetailScreen
import com.jeffreyalanwang.dutchrailwaysandroidclient.ui.detailScreens.TrainServiceDetailScreen
import com.jeffreyalanwang.dutchrailwaysandroidclient.ui.screens.top.EndpointTimePicker
import com.jeffreyalanwang.dutchrailwaysandroidclient.ui.screens.top.RoutePlannerScreen
import com.jeffreyalanwang.dutchrailwaysandroidclient.ui.screens.top.StationSearchScreen
import com.jeffreyalanwang.dutchrailwaysandroidclient.ui.util.rememberNavBackStack
import com.jeffreyalanwang.dutchrailwaysandroidclient.ui.viewmodel.Endpoint
import com.jeffreyalanwang.dutchrailwaysandroidclient.ui.viewmodel.RoutePlannerViewModel
import kotlinx.serialization.Serializable

interface NavRoute: NavKey

interface TrainQueryGraphRoute: NavRoute
interface TrainQueryGraphMajorRoute: NavRoute, TrainQueryGraphRoute
interface TrainQueryGraphChildRoute: NavRoute, TrainQueryGraphRoute
interface CommonChildRoute: NavRoute, TrainQueryGraphChildRoute
interface PlaceDetailRoute: NavRoute, CommonChildRoute { val id: Int }

@Serializable data object TrainQuerySelectionRoute : NavRoute, TrainQueryGraphRoute, TrainQueryGraphMajorRoute
@Serializable data object StationSearchRoute : NavRoute

@Serializable data class AreaDetailRoute(override val id: Int) : NavRoute, TrainQueryGraphChildRoute, CommonChildRoute, PlaceDetailRoute
@Serializable data class StationDetailRoute(override val id: Int) : NavRoute, TrainQueryGraphChildRoute, CommonChildRoute, PlaceDetailRoute
@Serializable data class TrainServiceDetailRoute(val id: Int) : NavRoute, TrainQueryGraphChildRoute, CommonChildRoute
@Serializable data object RouteOptionsRoute : NavRoute, TrainQueryGraphChildRoute, TrainQueryGraphMajorRoute
@Serializable data class RouteDetailRoute(val index: Int) : NavRoute, TrainQueryGraphChildRoute
@Serializable data class TimePickerRoute(val forEndpoint: Endpoint) : NavRoute, TrainQueryGraphChildRoute

/**
 * Returns navigation entries for pages in the main screen's bottom navbar.
 */
@Composable
fun appEntries(
    // Specific values are meaningless; they simply need to change
    // in order to reset the corresponding top-level route state
    resetKeyState: Map<NavRoute, Int>,
) = entryProvider {
    entry<TrainQuerySelectionRoute> { initialRoute ->
        key(resetKeyState.get(initialRoute)) {
            // Dispose + regenerate view model when key changes
            val viewModelStoreOwner = rememberViewModelStoreOwner()

            val viewModel = viewModel<RoutePlannerViewModel>(viewModelStoreOwner)
            val backStack by viewModel.backStack.collectAsStateWithLifecycle()

            NavDisplay(
                backStack = backStack,
                onBack = { viewModel.popBack() },
                sceneStrategy = remember {
                    PredictiveBackDialogSceneStrategy<TrainQueryGraphRoute>()
                        .then(SinglePaneSceneStrategy())
                },
            ) { key ->
                when (key) {
                    is TimePickerRoute -> NavEntry(
                        key = key,
                        metadata = predictiveBackDialog(),
                    ) { key ->
                        key as TimePickerRoute
                        EndpointTimePicker(
                            key.forEndpoint,
                            viewModel = viewModel,
                            onDismiss = { viewModel.popBack() },
                        )
                    }

                    else -> NavEntry(
                        key = key,

                        // Same for all routes within this NavDisplay,
                        // means we use the same SinglePaneScene,
                        // thus same RoutePlannerScreen
                        contentKey = true,
                    ) { key ->
                        RoutePlannerScreen(
                            key,
                            viewModel,
                            onNavigateMinor = { viewModel.pushUserRequested(it) }
                        )
                    }
                }
            }
        }
    }

    entry<StationSearchRoute> { initialRoute ->
        key(resetKeyState.get(initialRoute)) {
            val backstack = rememberNavBackStack<NavRoute>(initialRoute)

            NavDisplay(
                backStack = backstack,
                entryProvider = entryProvider {
                    entry<StationSearchRoute> { route ->
                        StationSearchScreen(
                            onNavigate = { newRoute -> backstack.add(newRoute) }
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
 * [T] must be a superclass of [CommonChildRoute].
 */
fun <T: NavRoute> EntryProviderScope<T>.tabEntries(
    onNavigate: (CommonChildRoute) -> Unit,
    onNavigateBack: () -> Unit,
) {
    // Enables smart cast for rest of method body
    this as EntryProviderScope<CommonChildRoute>

    entry<AreaDetailRoute> { routeArgs ->
        AreaDetailScreen(
            rememberSaveable(routeArgs.id) { BackendApi.get_area_info(routeArgs.id) },
            onNavigate = onNavigate,
            onNavigateBack = onNavigateBack,
        )
    }
    entry<StationDetailRoute> { routeArgs ->
        StationDetailScreen(
            rememberSaveable(routeArgs.id) { BackendApi.get_station_info(routeArgs.id) },
            onNavigate = onNavigate,
            onNavigateBack = onNavigateBack,
        )
    }
    entry<TrainServiceDetailRoute> { routeArgs ->
        TrainServiceDetailScreen(
            rememberSaveable(routeArgs.id) { BackendApi.get_pass_service(routeArgs.id) },
            onNavigate = onNavigate,
            onNavigateBack = onNavigateBack,
        )
    }
}
