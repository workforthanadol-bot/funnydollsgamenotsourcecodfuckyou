package com.roadforge.launcher.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.*
import androidx.navigation.compose.*
import com.roadforge.launcher.ui.screens.*

sealed class Screen(val route: String) {
    object Library      : Screen("library")
    object Setup        : Screen("setup/{gameId}") {
        fun withId(id: String) = "setup/$id"
    }
    object Settings     : Screen("settings")
    object OverlayEditor: Screen("overlay_editor/{layoutId}") {
        fun withId(id: String) = "overlay_editor/$id"
    }
    object Diagnostics  : Screen("diagnostics/{gameId}") {
        fun withId(id: String) = "diagnostics/$id"
    }
    object Permissions  : Screen("permissions")
}

@Composable
fun RoadForgeNavGraph(navController: NavHostController) {
    NavHost(navController = navController, startDestination = Screen.Library.route) {

        composable(Screen.Library.route) {
            LibraryScreen(
                onAddGame = { navController.navigate(Screen.Permissions.route) },
                onConfigure = { id -> navController.navigate(Screen.Setup.withId(id)) },
                onDiagnostics = { id -> navController.navigate(Screen.Diagnostics.withId(id)) },
                onSettings = { navController.navigate(Screen.Settings.route) },
                onEditOverlay = { id -> navController.navigate(Screen.OverlayEditor.withId(id)) },
            )
        }

        composable(
            Screen.Setup.route,
            arguments = listOf(navArgument("gameId") { type = NavType.StringType })
        ) { backStack ->
            val gameId = backStack.arguments?.getString("gameId") ?: return@composable
            SetupScreen(
                gameId = gameId,
                onDone = { navController.popBackStack(Screen.Library.route, false) },
                onBack = { navController.popBackStack() },
            )
        }

        composable(Screen.Settings.route) {
            SettingsScreen(onBack = { navController.popBackStack() })
        }

        composable(
            Screen.OverlayEditor.route,
            arguments = listOf(navArgument("layoutId") { type = NavType.StringType })
        ) { backStack ->
            val layoutId = backStack.arguments?.getString("layoutId") ?: "default_beamng"
            OverlayEditorScreen(
                layoutId = layoutId,
                onDone = { navController.popBackStack() },
            )
        }

        composable(
            Screen.Diagnostics.route,
            arguments = listOf(navArgument("gameId") { type = NavType.StringType })
        ) { backStack ->
            val gameId = backStack.arguments?.getString("gameId") ?: return@composable
            DiagnosticsScreen(
                gameId = gameId,
                onBack = { navController.popBackStack() },
            )
        }

        composable(Screen.Permissions.route) {
            PermissionsScreen(
                onGranted = { navController.popBackStack() },
                onBack = { navController.popBackStack() },
            )
        }
    }
}
