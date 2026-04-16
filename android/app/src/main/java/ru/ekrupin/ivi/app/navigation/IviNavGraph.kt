package ru.ekrupin.ivi.app.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavType
import androidx.navigation.NavHostController
import androidx.navigation.navArgument
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import ru.ekrupin.ivi.feature.eventedit.EventEditScreen
import ru.ekrupin.ivi.feature.events.EventsScreen
import ru.ekrupin.ivi.feature.eventtypes.EventTypesScreen
import ru.ekrupin.ivi.feature.home.HomeScreen
import ru.ekrupin.ivi.feature.petedit.PetEditScreen
import ru.ekrupin.ivi.feature.settings.SettingsScreen
import ru.ekrupin.ivi.feature.syncconflicts.SyncConflictsScreen
import ru.ekrupin.ivi.feature.weight.WeightScreen

@Composable
fun IviNavGraph(
    navController: NavHostController,
    modifier: Modifier = Modifier,
) {
    NavHost(
        navController = navController,
        startDestination = IviDestination.Home.route,
        modifier = modifier,
    ) {
        composable(IviDestination.Home.route) {
            HomeScreen(
                onAddEvent = { navController.navigate(IviDestination.EventEdit.createRoute()) },
                onOpenWeight = { navController.navigate(IviDestination.Weight.route) },
                onOpenEvents = { navController.navigate(IviDestination.Events.route) },
                onOpenSettings = { navController.navigate(IviDestination.Settings.route) },
                onEditPet = { navController.navigate(IviDestination.PetEdit.route) },
            )
        }
        composable(IviDestination.Events.route) {
            EventsScreen(
                onCreateEvent = { navController.navigate(IviDestination.EventEdit.createRoute()) },
                onEditEvent = { eventId -> navController.navigate(IviDestination.EventEdit.createRoute(eventId)) },
            )
        }
        composable(IviDestination.Weight.route) {
            WeightScreen()
        }
        composable(IviDestination.EventTypes.route) {
            EventTypesScreen()
        }
        composable(IviDestination.Settings.route) {
            SettingsScreen(onOpenConflicts = { navController.navigate(IviDestination.SyncConflicts.route) })
        }
        composable(IviDestination.SyncConflicts.route) {
            SyncConflictsScreen()
        }
        composable(IviDestination.PetEdit.route) {
            PetEditScreen(onSaved = { navController.popBackStack() })
        }
        composable(
            route = IviDestination.EventEdit.route,
            arguments = listOf(
                navArgument("eventId") {
                    type = NavType.LongType
                    defaultValue = -1L
                },
            ),
        ) {
            EventEditScreen(onSaved = { navController.popBackStack() })
        }
    }
}
