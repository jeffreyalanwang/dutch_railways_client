package com.jeffreyalanwang.dutchrailwaysandroidclient.ui

import androidx.compose.animation.AnimatedContentScope
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.runtime.Composable
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.jeffreyalanwang.dutchrailwaysandroidclient.BackendApi
import com.jeffreyalanwang.dutchrailwaysandroidclient.ui.detailScreens.StationDetailScreen
import com.jeffreyalanwang.dutchrailwaysandroidclient.ui.detailScreens.TrainServiceDetailScreen
import com.jeffreyalanwang.dutchrailwaysandroidclient.ui.screens.top.StationSearchScreen
import com.jeffreyalanwang.dutchrailwaysandroidclient.ui.screens.top.RoutePlannerScreen
import kotlinx.serialization.Serializable


interface NavRoute

@Serializable
object StationSearchRoute : NavRoute

@Serializable
object TrainQueryRoute : NavRoute

@Serializable
data class StationDetailRoute(val id: Int) : NavRoute

@Serializable
data class TrainServiceDetailRoute(val id: Int) : NavRoute


/**
 * Adds navigation routes for pages in the main screen's bottom navbar.
 */
fun NavGraphBuilder.topNavGraph() {
    composableTopRoute<TrainQueryRoute> { backStackEntry, onNavigate ->
        RoutePlannerScreen(onNavigate = onNavigate)
    }
    composableTopRoute<StationSearchRoute> { backStackEntry, onNavigate ->
        StationSearchScreen(onNavigate = onNavigate)
    }
}

/**
 * Adds navigation routes for pages which may be opened by
 * (thus pushed on top of) top-level pages in the back stack.
 */
fun NavGraphBuilder.tabNavGraph(
    onNavigate: (NavRoute) -> Unit,
    onNavigateBack: () -> Unit,
) {
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
 * Encloses [topContent] in its own nested NavHost,
 * and allows it to navigate to routes in [tabNavGraph].
 *
 * Also provides with appropriate tab-change animations
 * (i.e. none).
 */
private inline fun <reified T: Any> NavGraphBuilder.composableTopRoute(
    noinline topContent: @Composable AnimatedContentScope.(NavBackStackEntry, (NavRoute)->Unit)->Unit
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
        tabNavGraph(
            onNavigate = { tabNavController.navigate(it) },
            onNavigateBack = { tabNavController.popBackStack() },
        )
    }
}

/**
 * Provides child routes with enter/exit animations.
 */
private inline fun <reified T: Any> NavGraphBuilder.composableChildRoute(
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