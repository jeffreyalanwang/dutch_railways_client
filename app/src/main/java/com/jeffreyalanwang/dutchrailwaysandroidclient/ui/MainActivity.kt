package com.jeffreyalanwang.dutchrailwaysandroidclient.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.jeffreyalanwang.dutchrailwaysandroidclient.R
import com.jeffreyalanwang.dutchrailwaysandroidclient.ui.theme.DutchRailwaysAndroidClientTheme
import com.jeffreyalanwang.dutchrailwaysandroidclient.ui.util.bottomOnly
import com.jeffreyalanwang.dutchrailwaysandroidclient.ui.util.hasRoute

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
    val topNavController = rememberNavController()
    val navBackStackEntry by topNavController.currentBackStackEntryAsState()

    Scaffold(
        bottomBar = { NavigationBar {
            AppDestinations.entries.forEach { appTab ->
                NavigationBarItem(
                    icon = { Icon(
                        painterResource(appTab.icon),
                        contentDescription = appTab.label,
                    ) },
                    label = { Text(appTab.label) },
                    selected = navBackStackEntry
                        ?.hasRoute(appTab.route::class)
                        ?: false,
                    onClick = {
                        val alreadySelected = navBackStackEntry
                            ?.hasRoute(appTab.route::class)
                            ?: false
                        topNavController.navigate(appTab.route) {
                            popUpTo(
                                topNavController.graph.findStartDestination().id
                            ) { saveState = !alreadySelected }
                            restoreState = !alreadySelected
                            launchSingleTop = true
                        }
                    },
                )
            }
        } }
    ) { innerPadding ->
        NavHost(
            topNavController,
            modifier = Modifier.padding(innerPadding.bottomOnly()),
            startDestination = AppDestinations.TRIP.route
        ) {
            topNavGraph()
        }
    }
}

private enum class AppDestinations(
    val label: String,
    val icon: Int,
    val route: NavRoute,
) {
//    HOME("Home", R.drawable.ic_home, ),
    TRIP("Trip", R.drawable.ic_directions, TrainQueryRoute),
    STATIONS("Stations", R.drawable.ic_dr_station, StationSearchRoute),
//    EDIT("Edit", R.drawable.ic_edit, ),
}