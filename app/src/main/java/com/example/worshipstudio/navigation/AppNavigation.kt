package com.example.worshipstudio.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.worshipstudio.ui.screens.AddSongScreen
import com.example.worshipstudio.ui.screens.CreateSetScreen
import com.example.worshipstudio.ui.screens.JoinSessionScreen
import com.example.worshipstudio.ui.screens.LiveSessionScreen
import com.example.worshipstudio.ui.screens.LoginScreen
import com.example.worshipstudio.ui.screens.SetDetailScreen
import com.example.worshipstudio.ui.screens.SettingsScreen
import com.example.worshipstudio.ui.screens.SongDetailScreen
import com.example.worshipstudio.ui.screens.SongListScreen
import com.example.worshipstudio.utils.AppTheme
import com.example.worshipstudio.viewmodel.AuthViewModel
import com.example.worshipstudio.viewmodel.SessionViewModel
import com.example.worshipstudio.viewmodel.SetViewModel
import com.example.worshipstudio.viewmodel.SettingsViewModel
import com.example.worshipstudio.viewmodel.SongViewModel
import com.example.worshipstudio.viewmodel.TagViewModel

@Composable
fun AppNavigation(
    currentTheme:  AppTheme = AppTheme.NIGHTFALL,
    onThemeChange: (AppTheme) -> Unit = {}
) {
    val navController   = rememberNavController()
    val authViewModel: AuthViewModel       = viewModel()
    val authState by authViewModel.state.collectAsState()

    // Shared single instances — all screens see the same list state
    val songViewModel:     SongViewModel     = viewModel()
    val setViewModel:      SetViewModel      = viewModel()
    val settingsViewModel: SettingsViewModel = viewModel()
    val tagViewModel:      TagViewModel      = viewModel()

    val startDestination = if (authState.isLoggedIn) Screen.SongList.route else Screen.Login.route

    NavHost(navController = navController, startDestination = startDestination) {

        composable(Screen.Login.route) {
            LoginScreen(
                viewModel      = authViewModel,
                onLoginSuccess = {
                    navController.navigate(Screen.SongList.route) {
                        popUpTo(Screen.Login.route) { inclusive = true }
                    }
                }
            )
        }

        composable(Screen.SongList.route) {
            SongListScreen(
                authViewModel = authViewModel,
                songViewModel = songViewModel,
                setViewModel  = setViewModel,
                tagViewModel  = tagViewModel,
                currentTheme  = currentTheme,
                onSongClick   = { navController.navigate(Screen.SongDetail.createRoute(it)) },
                onAddSong     = { navController.navigate(Screen.AddSong.createRoute()) },
                onSetClick    = { navController.navigate(Screen.SetDetail.createRoute(it)) },
                onCreateSet   = { navController.navigate(Screen.CreateSet.route) },
                onSettings    = { navController.navigate(Screen.Settings.route) },
                onLogout      = {
                    authViewModel.logout()
                    navController.navigate(Screen.Login.route) { popUpTo(0) { inclusive = true } }
                }
            )
        }

        composable(Screen.Settings.route) {
            SettingsScreen(
                authViewModel     = authViewModel,
                settingsViewModel = settingsViewModel,
                tagViewModel      = tagViewModel,
                currentTheme      = currentTheme,
                onThemeChange     = onThemeChange,
                onBack            = { navController.popBackStack() }
            )
        }

        composable(
            route     = Screen.AddSong.route,
            arguments = listOf(navArgument("songId") {
                type = NavType.StringType; nullable = true; defaultValue = null
            })
        ) { backStack ->
            val songId = backStack.arguments?.getString("songId")
            AddSongScreen(
                songId        = songId,
                authViewModel = authViewModel,
                songViewModel = songViewModel,
                tagViewModel  = tagViewModel,
                onSaved       = { navController.popBackStack() },
                onBack        = { navController.popBackStack() }
            )
        }

        composable(
            route     = Screen.SongDetail.route,
            arguments = listOf(navArgument("songId") { type = NavType.StringType })
        ) { backStack ->
            val songId = backStack.arguments?.getString("songId") ?: return@composable
            SongDetailScreen(
                songId        = songId,
                songViewModel = songViewModel,
                tagViewModel  = tagViewModel,
                isAdmin       = authState.role == "admin",
                onBack        = { navController.popBackStack() },
                onEdit        = { navController.navigate(Screen.AddSong.createRoute(songId)) }
            )
        }

        composable(Screen.CreateSet.route) {
            CreateSetScreen(
                authViewModel = authViewModel,
                setViewModel  = setViewModel,
                onCreated     = { setId ->
                    navController.navigate(Screen.SetDetail.createRoute(setId)) {
                        popUpTo(Screen.CreateSet.route) { inclusive = true }
                    }
                },
                onBack = { navController.popBackStack() }
            )
        }

        composable(
            route     = Screen.SetDetail.route,
            arguments = listOf(navArgument("setId") { type = NavType.StringType })
        ) { backStack ->
            val setId            = backStack.arguments?.getString("setId") ?: return@composable
            val sessionViewModel: SessionViewModel = viewModel()
            SetDetailScreen(
                setId            = setId,
                authViewModel    = authViewModel,
                setViewModel     = setViewModel,
                songViewModel    = songViewModel,
                sessionViewModel = sessionViewModel,
                onBack           = { navController.popBackStack() },
                onSongClick      = { navController.navigate(Screen.SongDetail.createRoute(it)) },
                onStartSession   = { sessionId, isAdmin ->
                    navController.navigate(Screen.LiveSession.createRoute(sessionId, isAdmin))
                },
                onJoinSession    = {
                    navController.navigate(Screen.JoinSession.createRoute(authState.churchId))
                }
            )
        }

        composable(
            route     = Screen.LiveSession.route,
            arguments = listOf(
                navArgument("sessionId") { type = NavType.StringType },
                navArgument("isAdmin")   { type = NavType.BoolType   }
            )
        ) { backStack ->
            val sessionId        = backStack.arguments?.getString("sessionId") ?: return@composable
            val isAdmin          = backStack.arguments?.getBoolean("isAdmin") ?: false
            val sessionViewModel: SessionViewModel = viewModel()
            LiveSessionScreen(
                sessionId        = sessionId,
                isAdmin          = isAdmin,
                sessionViewModel = sessionViewModel,
                songViewModel    = songViewModel,
                onBack           = { navController.popBackStack() }
            )
        }

        composable(
            route     = Screen.JoinSession.route,
            arguments = listOf(navArgument("churchId") { type = NavType.StringType })
        ) { backStack ->
            val churchId         = backStack.arguments?.getString("churchId") ?: ""
            val sessionViewModel: SessionViewModel = viewModel()
            JoinSessionScreen(
                churchId         = churchId,
                sessionViewModel = sessionViewModel,
                onJoined         = { sessionId ->
                    navController.navigate(Screen.LiveSession.createRoute(sessionId, false)) {
                        popUpTo(Screen.JoinSession.route) { inclusive = true }
                    }
                },
                onBack           = { navController.popBackStack() }
            )
        }
    }
}
