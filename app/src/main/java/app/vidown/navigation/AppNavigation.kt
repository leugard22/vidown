package app.vidown.navigation

import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import app.vidown.ui.downloads.DownloadsScreen
import app.vidown.ui.home.HomeScreen
import app.vidown.ui.settings.SettingsScreen
import kotlinx.serialization.Serializable

sealed interface Screen {
    @Serializable data object Home : Screen
    @Serializable data object Downloads : Screen
    @Serializable data object Settings : Screen
}

@Composable
fun AppNavigation(
    navController: NavHostController,
    paddingValues: PaddingValues,
    modifier: Modifier = Modifier
) {
    NavHost(
        navController = navController,
        startDestination = Screen.Home,
        modifier = modifier.padding(paddingValues),
        enterTransition = { EnterTransition.None },
        exitTransition = { ExitTransition.None },
        popEnterTransition = { EnterTransition.None },
        popExitTransition = { ExitTransition.None }
    ) {
        composable<Screen.Home> {
            HomeScreen()
        }

        composable<Screen.Downloads> {
            DownloadsScreen(
                onNavigateHome = {
                    navController.navigate(Screen.Home) {
                        popUpTo(navController.graph.startDestinationId) {
                            saveState = true
                        }
                        launchSingleTop = true
                        restoreState = true
                    }
                }
            )
        }

        composable<Screen.Settings> {
            SettingsScreen()
        }
    }
}
