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
import com.example.worshipstudio.ui.screens.LiveSessionScreen
import com.example.worshipstudio.ui.screens.LoginScreen
import com.example.worshipstudio.ui.screens.SetDetailScreen
import com.example.worshipstudio.ui.screens.SongDetailScreen
import com.example.worshipstudio.ui.screens.SongListScreen
import com.example.worshipstudio.viewmodel.AuthViewModel
import com.example.worshipstudio.viewmodel.SessionViewModel
import com.example.worshipstudio.viewmodel.SetViewModel
import com.example.worshipstudio.viewmodel.SongViewModel

@Composable
fun AppNavigation() {
    val navController = rememberNavController()
    val authViewModel: AuthViewModel = viewModel()
    val authState by authViewModel.state.collectAsState()

    // Shared single instance — all screens see the same song list state
    val songViewModel: SongViewModel = viewModel()
    val setViewModel: SetViewModel = viewModel()

    val startDestination = if (authState.isLoggedIn) Screen.SongList.route else Screen.Login.route

    NavHost(navController = navController, startDestination = startDestination) {

        composable(Screen.Login.route) {
            LoginScreen(
                viewModel = authViewModel,
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
                setViewModel = setViewModel,
                onSongClick  = { navController.navigate(Screen.SongDetail.createRoute(it)) },
                onAddSong    = { navController.navigate(Screen.AddSong.createRoute()) },
                onSetClick   = { navController.navigate(Screen.SetDetail.createRoute(it)) },
                onCreateSet  = { navController.navigate(Screen.CreateSet.route) },
                onLogout = {
                    authViewModel.logout()
                    navController.navigate(Screen.Login.route) { popUpTo(0) { inclusive = true } }
                }
            )
        }

        composable(
            route = Screen.AddSong.route,
            arguments = listOf(navArgument("songId") {
                type = NavType.StringType; nullable = true; defaultValue = null
            })
        ) { backStack ->
            val songId = backStack.arguments?.getString("songId")
            AddSongScreen(
                songId = songId,
                authViewModel = authViewModel,
                songViewModel = songViewModel,   // same shared instance
                onSaved = { navController.popBackStack() },
                onBack  = { navController.popBackStack() }
            )
        }

        composable(
            route = Screen.SongDetail.route,
            arguments = listOf(navArgument("songId") { type = NavType.StringType })
        ) { backStack ->
            val songId = backStack.arguments?.getString("songId") ?: return@composable
            SongDetailScreen(
                songId = songId,
                songViewModel = songViewModel,   // same shared instance
                onBack = { navController.popBackStack() },
                onEdit = { navController.navigate(Screen.AddSong.createRoute(songId)) }
            )
        }

        composable(Screen.CreateSet.route) {
            CreateSetScreen(
                authViewModel = authViewModel,
                setViewModel = setViewModel,
                onCreated = { setId ->
                    navController.navigate(Screen.SetDetail.createRoute(setId)) {
                        popUpTo(Screen.CreateSet.route) { inclusive = true }
                    }
                },
                onBack = { navController.popBackStack() }
            )
        }

        composable(
            route = Screen.SetDetail.route,
            arguments = listOf(navArgument("setId") { type = NavType.StringType })
        ) { backStack ->
            val setId = backStack.arguments?.getString("setId") ?: return@composable
            val sessionViewModel: SessionViewModel = viewModel()
            SetDetailScreen(
                setId = setId,
                authViewModel = authViewModel,
                setViewModel = setViewModel,
                songViewModel = songViewModel,   // same shared instance
                sessionViewModel = sessionViewModel,
                onBack = { navController.popBackStack() },
                onSongClick = { navController.navigate(Screen.SongDetail.createRoute(it)) },
                onStartSession = { sessionId, isAdmin ->
                    navController.navigate(Screen.LiveSession.createRoute(sessionId, isAdmin))
                }
            )
        }

        composable(
            route = Screen.LiveSession.route,
            arguments = listOf(
                navArgument("sessionId") { type = NavType.StringType },
                navArgument("isAdmin")   { type = NavType.BoolType }
            )
        ) { backStack ->
            val sessionId = backStack.arguments?.getString("sessionId") ?: return@composable
            val isAdmin   = backStack.arguments?.getBoolean("isAdmin") ?: false
            val sessionViewModel: SessionViewModel = viewModel()
            LiveSessionScreen(
                sessionId = sessionId,
                isAdmin = isAdmin,
                sessionViewModel = sessionViewModel,
                songViewModel = songViewModel,   // same shared instance
                onBack = { navController.popBackStack() }
            )
        }
    }
}
