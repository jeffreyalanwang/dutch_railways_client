package com.jeffreyalanwang.dutchrailwaysandroidclient.ui

import android.content.Context
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
import androidx.compose.runtime.remember
import androidx.compose.runtime.tooling.ComposeStackTraceMode
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import com.jeffreyalanwang.dutchrailwaysandroidclient.R
import com.jeffreyalanwang.dutchrailwaysandroidclient.backend.Geocoding
import com.jeffreyalanwang.dutchrailwaysandroidclient.replaceAt
import com.jeffreyalanwang.dutchrailwaysandroidclient.ui.theme.DutchRailwaysAndroidClientTheme
import com.jeffreyalanwang.dutchrailwaysandroidclient.ui.util.RefreshKeyState
import com.jeffreyalanwang.dutchrailwaysandroidclient.ui.util.bottomOnly
import com.jeffreyalanwang.dutchrailwaysandroidclient.ui.util.rememberNavBackStack
import com.jeffreyalanwang.dutchrailwaysandroidclient.ui.util.retainRefreshKeyState
import com.jeffreyalanwang.dutchrailwaysandroidclient.ui.util.toMutableStateMap

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Composer.setDiagnosticStackTraceMode(ComposeStackTraceMode.SourceInformation)
        Geocoding.initialize(this.baseContext)
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
    val topBackStack = rememberNavBackStack<AppNavArgs>(AppDestinations.TRIP.navKey)
    val resetKeys = remember {
        AppDestinations.entries
            .map { it.navKey }
            .associateWith { RefreshKeyState() }
    }

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
                        if (!isSelected) {
                            topBackStack.add(appTab.navKey)
                        } else {
                            resetKeys[appTab.navKey]!!.refresh()
                        }
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
            entryProvider = appEntries(resetKeys.mapValues { (k, v) -> v.value }),
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
    val navKey: AppNavArgs,
) {
    // TODO HOME
    TRIP("Trip", R.drawable.ic_directions, TripFinderStartNavArgs),
    STATIONS("Stations", R.drawable.ic_dr_station, StationSearchStartNavArgs),
    EDIT("Edit", R.drawable.ic_edit, EditStartNavArgs),
}
