package com.jeffreyalanwang.dutchrailwaysandroidclient

import androidx.compose.animation.AnimatedContentScope
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.runtime.Composable
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.toRoute
import kotlinx.serialization.Serializable

private inline fun <reified T: Any> NavGraphBuilder.composableChild(
    noinline content: @Composable AnimatedContentScope.(NavBackStackEntry)->Unit
) = composable<T>(
    enterTransition = {
        slideIntoContainer(
            AnimatedContentTransitionScope.SlideDirection.Left, tween(700)
        ) + fadeIn()
    },
    popExitTransition = {
        slideOutOfContainer(
            AnimatedContentTransitionScope.SlideDirection.Right, tween(700)
        ) + fadeOut()
    },
    popEnterTransition = { null },
    content =  content,
)

@Serializable
object StationSearchRoute

fun NavGraphBuilder.addStationSearchRoute(
    onNavigate: (Any)->Unit
) = composable<StationSearchRoute> {
        StationSearchScreen(onNavigate = onNavigate)
    }

@Serializable
data class StationDetailRoute(val id: Int)

fun NavGraphBuilder.addStationDetailRoute(
    onNavigate: (Any) -> Unit,
    onNavigateBack: () -> Unit,
) = composableChild<StationDetailRoute> { backStackEntry ->
        val routeArgs: StationDetailRoute = backStackEntry.toRoute()
        StationDetailScreen(
            BackendApi.get_station_info(routeArgs.id),
            onNavigate = onNavigate,
            onNavigateBack = onNavigateBack,
        )
    }

@Serializable
data class TrainServiceDetailRoute(val id: Int)

fun NavGraphBuilder.addTrainServiceDetailRoute(
    onNavigate: (Any) -> Unit,
    onNavigateBack: () -> Unit,
) = composableChild<TrainServiceDetailRoute> { backStackEntry ->
        val routeArgs: TrainServiceDetailRoute = backStackEntry.toRoute()
        TrainServiceDetailScreen(
            BackendApi.get_pass_service(routeArgs.id),
            onNavigate = onNavigate,
            onNavigateBack = onNavigateBack,
        )
    }