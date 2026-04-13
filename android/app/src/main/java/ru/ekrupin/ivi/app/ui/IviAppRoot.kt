package ru.ekrupin.ivi.app.ui

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.MonitorWeight
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Pets
import androidx.compose.material.icons.outlined.Today
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import ru.ekrupin.ivi.R
import ru.ekrupin.ivi.app.navigation.IviDestination
import ru.ekrupin.ivi.app.navigation.IviNavGraph

@Composable
fun IviAppRoot() {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = backStackEntry?.destination

    val destinations = listOf(
        BottomDestination(IviDestination.Home, R.string.nav_home, Icons.Outlined.Home),
        BottomDestination(IviDestination.Events, R.string.nav_events, Icons.Outlined.Today),
        BottomDestination(IviDestination.Weight, R.string.nav_weight, Icons.Outlined.MonitorWeight),
        BottomDestination(IviDestination.EventTypes, R.string.nav_event_types, Icons.Outlined.Pets),
        BottomDestination(IviDestination.Settings, R.string.nav_settings, Icons.Outlined.Notifications),
    )

    Scaffold(
        bottomBar = {
            NavigationBar {
                destinations.forEach { destination ->
                    val selected = currentDestination?.hierarchy?.any { it.route == destination.route.route } == true
                    NavigationBarItem(
                        selected = selected,
                        onClick = {
                            navController.navigate(destination.route.route) {
                                popUpTo(navController.graph.startDestinationId) {
                                    inclusive = false
                                }
                                launchSingleTop = true
                            }
                        },
                        icon = { Icon(destination.icon, contentDescription = null) },
                        label = { Text(stringResource(destination.labelRes)) },
                    )
                }
            }
        },
    ) { innerPadding ->
        IviNavGraph(
            navController = navController,
            modifier = Modifier.padding(innerPadding),
        )
    }
}

private data class BottomDestination(
    val route: IviDestination,
    val labelRes: Int,
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
)
