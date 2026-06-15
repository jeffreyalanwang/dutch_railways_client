package com.jeffreyalanwang.dutchrailwaysandroidclient.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Composer
import androidx.compose.runtime.ExperimentalComposeApi
import androidx.compose.runtime.tooling.ComposeStackTraceMode
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import com.jeffreyalanwang.dutchrailwaysandroidclient.R
import com.jeffreyalanwang.dutchrailwaysandroidclient.ui.theme.DutchRailwaysAndroidClientTheme
import com.jeffreyalanwang.dutchrailwaysandroidclient.ui.util.bottomOnly
import com.jeffreyalanwang.dutchrailwaysandroidclient.ui.util.rememberNavBackStack

class MainActivity : ComponentActivity() {
    @OptIn(ExperimentalComposeApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Composer.setDiagnosticStackTraceMode(ComposeStackTraceMode.SourceInformation)
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
    val topBackStack = rememberNavBackStack<NavRoute>(AppDestinations.TRIP.navKey)

    Scaffold(
        bottomBar = { NavigationBar {
            AppDestinations.entries.forEach { appTab ->
                val isSelected = (topBackStack.lastOrNull() == appTab.navKey)
                NavigationBarItem(
                    icon = { Icon(
                        painterResource(appTab.icon),
                        contentDescription = appTab.label,
                    ) },
                    label = { Text(appTab.label) },
                    selected = isSelected,
                    onClick = {
                        if (!isSelected) topBackStack.add(appTab.navKey)
                    },
                )
            }
        } }
    ) { innerPadding ->
        NavDisplay(
            topBackStack,
            onBack = { topBackStack.removeLast() },
            entryDecorators = listOf(
                rememberSaveableStateHolderNavEntryDecorator(),
                rememberViewModelStoreNavEntryDecorator(),
            ),
            entryProvider = appEntries(),
            modifier = Modifier
                .padding(innerPadding.bottomOnly())
                .consumeWindowInsets(innerPadding.bottomOnly()),
            predictivePopTransitionSpec = {
                ContentTransform(
                    targetContentEnter = EnterTransition.None,
                    initialContentExit = ExitTransition.None,
                    sizeTransform = null
                )
            }
        )
    }
}

private enum class AppDestinations(
    val label: String,
    val icon: Int,
    val navKey: NavRoute,
) {
    // TODO HOME
    TRIP("Trip", R.drawable.ic_directions, TrainQuerySelectionRoute),
    STATIONS("Stations", R.drawable.ic_dr_station, StationSearchRoute),
    // TODO EDIT
}
