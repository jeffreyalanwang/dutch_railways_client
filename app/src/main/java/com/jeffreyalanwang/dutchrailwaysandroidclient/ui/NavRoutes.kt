package com.jeffreyalanwang.dutchrailwaysandroidclient.ui

import androidx.compose.animation.AnimatedContentScope
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.jeffreyalanwang.dutchrailwaysandroidclient.BackendApi
import com.jeffreyalanwang.dutchrailwaysandroidclient.ui.detailScreens.AreaDetailScreen
import com.jeffreyalanwang.dutchrailwaysandroidclient.ui.detailScreens.RouteDetailScreen
import com.jeffreyalanwang.dutchrailwaysandroidclient.ui.detailScreens.StationDetailScreen
import com.jeffreyalanwang.dutchrailwaysandroidclient.ui.detailScreens.TrainServiceDetailScreen
import com.jeffreyalanwang.dutchrailwaysandroidclient.ui.screens.top.RoutePlannerScreen
import com.jeffreyalanwang.dutchrailwaysandroidclient.ui.screens.top.StationSearchScreen
import com.jeffreyalanwang.dutchrailwaysandroidclient.ui.viewmodel.RoutePlannerViewModel
import kotlinx.serialization.Serializable


interface NavRoute
interface TrainQueryGraphRoute: NavRoute
interface CommonChildRoute: TrainQueryGraphRoute

@Serializable object TrainQueryRoute : NavRoute, TrainQueryGraphRoute
@Serializable object StationSearchRoute : NavRoute

@Serializable data class AreaDetailRoute(val id: Int) : NavRoute, TrainQueryGraphRoute, CommonChildRoute
@Serializable data class StationDetailRoute(val id: Int) : NavRoute, TrainQueryGraphRoute, CommonChildRoute
@Serializable data class TrainServiceDetailRoute(val id: Int) : NavRoute, TrainQueryGraphRoute, CommonChildRoute
@Serializable data class RouteDetailRoute(val index: Int) : NavRoute, TrainQueryGraphRoute, CommonChildRoute

/**
 * Adds navigation routes for pages in the main screen's bottom navbar.
 */
fun NavGraphBuilder.topNavGraph() {
    singleComposableGraphTopRoute<TrainQueryRoute, TrainQueryGraphRoute> { routeArgs, topLevelBackStackEntry, onNavigate, onNavigateBack ->
        val tabViewModel = viewModel<RoutePlannerViewModel>(topLevelBackStackEntry())
        RoutePlannerScreen(routeArgs, tabViewModel, onNavigate, onNavigateBack)
    }
    composableTopRoute<StationSearchRoute>(
        childNavGraph = NavGraphBuilder::tabNavGraph
    ) { backStackEntry, onNavigate ->
        StationSearchScreen(onNavigate = onNavigate)
    }
}

/**
 * Adds navigation routes for pages which may be opened by
 * (thus pushed on top of) top-level pages in the back stack.
 */
fun NavGraphBuilder.tabNavGraph(
    topLevelBackStackEntry: () -> NavBackStackEntry,
    onNavigate: (CommonChildRoute) -> Unit,
    onNavigateBack: () -> Unit,
) {
    composableChildRoute<AreaDetailRoute> { backStackEntry ->
        val routeArgs: AreaDetailRoute = backStackEntry.toRoute()
        AreaDetailScreen (
            BackendApi.get_area_info(routeArgs.id),
            onNavigate = onNavigate,
            onNavigateBack = onNavigateBack,
        )
    }
    composableChildRoute<StationDetailRoute> { backStackEntry ->
        val routeArgs: StationDetailRoute = backStackEntry.toRoute()
        StationDetailScreen(
            BackendApi.get_station_info(routeArgs.id),
            onNavigate = onNavigate,
            onNavigateBack = onNavigateBack,
        )
    }
    composableChildRoute<TrainServiceDetailRoute> { backStackEntry ->
        val routeArgs: TrainServiceDetailRoute = backStackEntry.toRoute()
        TrainServiceDetailScreen(
            BackendApi.get_pass_service(routeArgs.id),
            onNavigate = onNavigate,
            onNavigateBack = onNavigateBack,
        )
    }

    /**
     * Can only be called from Trip Query tab (where the parent route provides a viewModel).
     */
    composableChildRoute<RouteDetailRoute> { backStackEntry ->
        val routeArgs: RouteDetailRoute = backStackEntry.toRoute()
        val tabViewModel = viewModel<RoutePlannerViewModel>(topLevelBackStackEntry())
        val liveVMState by tabViewModel.uiState.collectAsState()

        val originalVMState = remember { liveVMState }

        LaunchedEffect(liveVMState) {
            if (liveVMState != originalVMState) {
                onNavigateBack()
            }
        }

        RouteDetailScreen(
            originalVMState.routes!![routeArgs.index],
            onNavigate = onNavigate,
            onNavigateBack = onNavigateBack,
        )
    }
}

/**
 * [tabNavGraph] that uses the provided composable to render all routes,
 * allowing a seamless transition between them.
 */
private inline fun <
    reified StartRoute: SubgraphRoute,
    reified SubgraphRoute: NavRoute,
> NavGraphBuilder.singleComposableGraphTopRoute(
    crossinline composable: @Composable (SubgraphRoute, () -> NavBackStackEntry, (CommonChildRoute) -> Unit, () -> Unit) -> Unit,
) = composable<SubgraphRoute>(
    enterTransition = { EnterTransition.None },
    exitTransition = { ExitTransition.None },
    popEnterTransition = { EnterTransition.None },
    popExitTransition = { ExitTransition.None },
) {
    val tabNavController = rememberNavController()
    NavHost(
        tabNavController,
        startDestination = StartRoute::class
    ) {
        composable<SubgraphRoute> { backStackEntry ->
            composable(
                backStackEntry.toRoute(),
                { tabNavController.getBackStackEntry<StartRoute>() },
                { tabNavController.navigate(it) },
                { tabNavController.popBackStack() },
            )
        }

    }
}

/**
 * Encloses [topContent] in its own nested NavHost,
 * and allows it to navigate to routes in [tabNavGraph].
 *
 * Also provides with appropriate tab-change animations
 * (i.e. none).
 */
private inline fun <
    reified T: NavRoute
> NavGraphBuilder.composableTopRoute(
    crossinline childNavGraph: NavGraphBuilder.(
        topLevelBackStackEntry: () -> NavBackStackEntry,
        onNavigate: (CommonChildRoute) -> Unit,
        onNavigateBack: () -> Unit,
    ) -> Unit,
    noinline topContent: @Composable AnimatedContentScope.(NavBackStackEntry, (CommonChildRoute)->Unit)->Unit,
) = composable<T>(
    enterTransition = { EnterTransition.None },
    exitTransition = { ExitTransition.None },
    popEnterTransition = { EnterTransition.None },
    popExitTransition = { ExitTransition.None },
) {
    val tabNavController = rememberNavController()
    NavHost(
        tabNavController,
        startDestination = T::class
    ) {
        composable<T> { backStackEntry ->
            topContent(
                backStackEntry,
                { newRoute -> tabNavController.navigate(newRoute) }
            )
        }
        childNavGraph(
            { tabNavController.getBackStackEntry<T>() },
            { tabNavController.navigate(it) },
            { tabNavController.popBackStack() },
        )
    }
}

/**
 * Provides child routes with enter/exit animations.
 */
private inline fun <
    reified T: CommonChildRoute
> NavGraphBuilder.composableChildRoute(
    noinline content: @Composable AnimatedContentScope.(NavBackStackEntry)->Unit
) = composable<T>(
    enterTransition = {
        slideIntoContainer(
            AnimatedContentTransitionScope.SlideDirection.Left, tween(700)
        ) + fadeIn()
    },
    // exitTransition = default
    popEnterTransition = { EnterTransition.None },
    popExitTransition = {
        slideOutOfContainer(
            AnimatedContentTransitionScope.SlideDirection.Right, tween(700)
        ) + fadeOut()
    },
    content = content
)
