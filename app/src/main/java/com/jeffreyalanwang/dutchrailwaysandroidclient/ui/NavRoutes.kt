package com.jeffreyalanwang.dutchrailwaysandroidclient.ui

import android.annotation.SuppressLint
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContentScope
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.jeffreyalanwang.dutchrailwaysandroidclient.BackendApi
import com.jeffreyalanwang.dutchrailwaysandroidclient.ui.detailScreens.AreaDetailScreen
import com.jeffreyalanwang.dutchrailwaysandroidclient.ui.detailScreens.StationDetailScreen
import com.jeffreyalanwang.dutchrailwaysandroidclient.ui.detailScreens.TrainServiceDetailScreen
import com.jeffreyalanwang.dutchrailwaysandroidclient.ui.screens.top.RoutePlannerScreen
import com.jeffreyalanwang.dutchrailwaysandroidclient.ui.screens.top.StationSearchScreen
import com.jeffreyalanwang.dutchrailwaysandroidclient.ui.viewmodel.RoutePlannerViewModel
import kotlinx.serialization.Serializable


interface NavRoute
interface TrainQueryGraphRoute: NavRoute
interface TrainQueryGraphMajorRoute: TrainQueryGraphRoute
interface TrainQueryGraphChildRoute: TrainQueryGraphRoute
interface CommonChildRoute: TrainQueryGraphChildRoute
interface PlaceDetailRoute: CommonChildRoute { val id: Int }

@Serializable object TrainQuerySelectionRoute : NavRoute, TrainQueryGraphRoute, TrainQueryGraphMajorRoute
@Serializable object StationSearchRoute : NavRoute

@Serializable data class AreaDetailRoute(override val id: Int) : NavRoute, TrainQueryGraphChildRoute, CommonChildRoute, PlaceDetailRoute
@Serializable data class StationDetailRoute(override val id: Int) : NavRoute, TrainQueryGraphChildRoute, CommonChildRoute, PlaceDetailRoute
@Serializable data class TrainServiceDetailRoute(val id: Int) : NavRoute, TrainQueryGraphChildRoute, CommonChildRoute
@Serializable object RouteOptionsRoute : NavRoute, TrainQueryGraphChildRoute, TrainQueryGraphMajorRoute
@Serializable data class RouteDetailRoute(val index: Int) : NavRoute, TrainQueryGraphChildRoute

/**
 * Adds navigation routes for pages in the main screen's bottom navbar.
 */
fun NavGraphBuilder.topNavGraph() {
    singleComposableGraphTopRoute<TrainQuerySelectionRoute, TrainQueryGraphRoute> { navController, backStackEntry ->

        val route = backStackEntry.toRoute<TrainQueryGraphRoute>() // TODO does this create a subclass of TrainQueryGraphRoute?
        val destination = backStackEntry.destination
        val topLevelBackStackEntry = remember(backStackEntry) {
            navController.getBackStackEntry(TrainQuerySelectionRoute::class)
        }

        val tabViewModel = viewModel<RoutePlannerViewModel>(topLevelBackStackEntry)
        val stateRequestedMajorRoute by tabViewModel
                .navMajorRouteState
                .collectAsStateWithLifecycle()
        val stateRequestedMinorRoute by tabViewModel
                .navMinorRouteState
                .collectAsStateWithLifecycle()

        // Major routes are controlled by the ViewModel and outline linear stages.
        // Minor routes are all others in the graph; they are temporary user excursions within major routes.
        // (Note: Minor routes are always child routes but are not identified by [TrainQueryGraphChildRoute].)

        // The ViewModel can directly trigger navigation to/back from major routes.
        //      It can directly trigger navigation to a minor route, but only directly above the major route.
        // The composable itself can directly trigger navigation to minor routes.
        //      It can directly trigger navigation back from a major/minor route. (In practice we do not use this.)
        // The back key can trigger navigation back from major and minor routes.

        Pair(stateRequestedMajorRoute, stateRequestedMinorRoute).let { (major, minor) ->
            LaunchedEffect(major, minor) {
                if (destination.hasRoute(major::class)) {
                    navController.popBackStack(
                        major::class,
                        inclusive = false
                    )
                } else {
                    navController.navigate(major) {
                        popUpTo(TrainQueryGraphMajorRoute::class) // TODO does this pop up to any subtype of [TrainQueryGraphMajorRoute]?
                    }
                }

                minor?.let {
                    navController.navigate(it)
                }
            }
        }

        BackHandler(true) {
            when (route) {
                is TrainQueryGraphMajorRoute ->
                    if (!tabViewModel.onPopMajorRoute()) {
                        navController.popBackStack()
                    }
                is TrainQueryGraphChildRoute ->
                    navController.popBackStack()
                else -> throw IllegalStateException()
            }
        }
        RoutePlannerScreen(route, tabViewModel, { navController.navigate(it) })
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
}

/**
 * [tabNavGraph] that uses the provided composable to render all routes,
 * allowing a seamless transition between them.
 */
@SuppressLint("RestrictedApi")
private inline fun <
    reified StartRoute: SubgraphRoute,
    reified SubgraphRoute: NavRoute,
> NavGraphBuilder.singleComposableGraphTopRoute(
    crossinline delegateComposable:
        @Composable AnimatedContentScope.(
            NavHostController,
            NavBackStackEntry,
        ) -> Unit,
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
            delegateComposable(tabNavController, backStackEntry)
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
