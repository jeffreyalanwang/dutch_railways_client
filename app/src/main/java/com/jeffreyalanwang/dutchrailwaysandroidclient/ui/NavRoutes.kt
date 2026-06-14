package com.jeffreyalanwang.dutchrailwaysandroidclient.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.ui.NavDisplay
import com.jeffreyalanwang.dutchrailwaysandroidclient.BackendApi
import com.jeffreyalanwang.dutchrailwaysandroidclient.ui.detailScreens.AreaDetailScreen
import com.jeffreyalanwang.dutchrailwaysandroidclient.ui.detailScreens.StationDetailScreen
import com.jeffreyalanwang.dutchrailwaysandroidclient.ui.detailScreens.TrainServiceDetailScreen
import com.jeffreyalanwang.dutchrailwaysandroidclient.ui.screens.top.RoutePlannerScreen
import com.jeffreyalanwang.dutchrailwaysandroidclient.ui.screens.top.StationSearchScreen
import com.jeffreyalanwang.dutchrailwaysandroidclient.ui.util.rememberNavBackStack
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

/**
 * Returns navigation entries for pages in the main screen's bottom navbar.
 */
fun appEntries(): (NavRoute) -> NavEntry<NavRoute> = entryProvider {
    trainQueryEntryTopGraph()

    entryTopGraph<NavRoute, StationSearchRoute, CommonChildRoute>(
        childEntries = { onNavigate, onBack -> tabEntries(onNavigate, onBack) }
    ) { route, onNavigate ->
        StationSearchScreen(onNavigate = onNavigate)
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

/**
 * [tabNavGraph] that uses the provided composable to render all routes,
 * allowing a seamless transition between them.
 */
private fun EntryProviderScope<NavRoute>.trainQueryEntryTopGraph() {

    entry<TrainQuerySelectionRoute> { topKey ->
        val viewModel = viewModel<RoutePlannerViewModel>()
        val backStack by viewModel.backStack.collectAsStateWithLifecycle()

        NavDisplay(
            backStack = backStack,
            onBack = { viewModel.popBack() },
        ) { key ->
            NavEntry(
                key = key,
                contentKey = true, // Same for all routes within this NavDisplay, means we use the same SinglePaneScene, thus same RoutePlannerScreen
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
/**
 * Encloses [topContent] in its own nested NavDisplay,
 * and allows it to navigate to routes in [childEntries].
 *
 * [TopKey] should be a concrete class.
 * [GraphKey] and [ChildKey] should be superclasses.
 */
private inline fun <
    reified GraphKey: NavRoute,
    reified TopKey: GraphKey,
    reified ChildKey: GraphKey,
> EntryProviderScope<NavRoute>.entryTopGraph(
    crossinline childEntries:
        EntryProviderScope<GraphKey>.(
            onNavigate: (ChildKey) -> Unit,
            onBack: () -> Unit,
        ) -> Unit,

    crossinline topContent:
        @Composable (
            TopKey,
            (ChildKey) -> Unit,
        ) -> Unit,
) {
    entry<TopKey> { initialRoute ->
        val backstack = rememberNavBackStack<GraphKey>(initialRoute)

        NavDisplay(
            backStack = backstack,
            entryProvider = entryProvider {
                entry<TopKey> { route ->
                    topContent(
                        route,
                        { newRoute -> backstack.add(newRoute) }
                    )
                }
                childEntries(
                    { backstack.add(it) },
                    { backstack.removeAt(backstack.lastIndex) }
                )
            },
        )
    }
}