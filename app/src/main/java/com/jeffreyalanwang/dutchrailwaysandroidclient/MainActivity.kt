package com.jeffreyalanwang.dutchrailwaysandroidclient

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.rememberNavController
import com.jeffreyalanwang.dutchrailwaysandroidclient.ui.theme.DutchRailwaysAndroidClientTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            DutchRailwaysAndroidClientTheme {
                DutchRailwaysAndroidClientApp()
            }
        }
    }
}

@Preview
@Composable
fun DutchRailwaysAndroidClientApp() {
    var currentDestination by rememberSaveable { mutableStateOf(AppDestinations.HOME) }

    NavigationSuiteScaffold(
        navigationSuiteItems = {
            AppDestinations.entries.forEach {
                item(
                    icon = {
                        Icon(
                            painterResource(it.icon),
                            contentDescription = it.label,
                        )
                    },
                    label = { Text(it.label) },
                    selected = (it == currentDestination),
                    onClick = { currentDestination = it },
                )
            }
        }
    ) {
        val tabNavController = rememberNavController()
        NavHost(
            tabNavController,
            startDestination = when (currentDestination) {
                AppDestinations.HOME -> StationSearchRoute
                AppDestinations.TRIP -> TrainServiceDetailRoute(119)
                AppDestinations.STATIONS -> StationSearchRoute
                AppDestinations.EDIT -> StationDetailRoute(358)
            }
        ) {
            // Top-level pages
            addStationSearchRoute { newPage ->
                tabNavController.navigate(newPage) {
                    popUpTo(tabNavController.graph.startDestinationRoute!!) {
                        saveState = true //TODO cannot get state to restore
                    }
                    restoreState = true
                    launchSingleTop = true
                }
            }

            // Below: Child pages (do not pop up when navigating to them)
            addStationDetailRoute (
                onNavigate = { newPage ->
                    tabNavController.navigate(newPage) {
                        restoreState = true
                    }
                },
                onNavigateBack = { tabNavController.popBackStack() },
            )
            addTrainServiceDetailRoute (
                onNavigate = { newPage ->
                    tabNavController.navigate(newPage) {
                        restoreState = true
                    }
                },
                onNavigateBack = { tabNavController.popBackStack() },
            )
        }
    }
}

enum class AppDestinations(
    val label: String,
    val icon: Int,
) {
    HOME("Home", R.drawable.ic_home),
    TRIP("Trip", R.drawable.ic_directions),
    STATIONS("Stations", R.drawable.ic_dr_station),
    EDIT("Edit", R.drawable.ic_edit),
}