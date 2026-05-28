package com.example.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.ui.screens.*
import com.example.ui.viewmodel.SyncWallViewModel

const val ROUTE_SPLASH = "splash"
const val ROUTE_AUTH = "auth"
const val ROUTE_HOME = "home"
const val ROUTE_ROOM = "room"
const val ROUTE_EDITOR = "editor"
const val ROUTE_SETTINGS = "settings"

@Composable
fun NavGraph(viewModel: SyncWallViewModel = viewModel()) {
    val navController = rememberNavController()
    
    val currentUser by viewModel.currentUser.collectAsStateWithLifecycle()
    val isFirebaseOnline by viewModel.isFirebaseInitialized.collectAsStateWithLifecycle()

    NavHost(
        navController = navController,
        startDestination = ROUTE_SPLASH
    ) {
        composable(ROUTE_SPLASH) {
            SplashScreen(
                onSplashComplete = {
                    // If user profile is not set up, authenticate them (go to auth); otherwise go home!
                    if (currentUser.username == "CreativeArtist") {
                        navController.navigate(ROUTE_AUTH) {
                            popUpTo(ROUTE_SPLASH) { inclusive = true }
                        }
                    } else {
                        navController.navigate(ROUTE_HOME) {
                            popUpTo(ROUTE_SPLASH) { inclusive = true }
                        }
                    }
                }
            )
        }

        composable(ROUTE_AUTH) {
            AuthScreen(
                currentUsername = currentUser.username,
                currentAvatar = currentUser.avatarUrl,
                onAuthComplete = { name, avatar ->
                    viewModel.authenticateUser(name, avatar)
                    navController.navigate(ROUTE_HOME) {
                        popUpTo(ROUTE_AUTH) { inclusive = true }
                    }
                }
            )
        }

        composable(ROUTE_HOME) {
            HomeScreen(
                currentUser = currentUser,
                isFirebaseOnline = isFirebaseOnline,
                onNavigateToCreateJoin = {
                    navController.navigate(ROUTE_ROOM)
                },
                onNavigateToSettings = {
                    navController.navigate(ROUTE_SETTINGS)
                },
                onQuickJoinRoom = { pin ->
                    viewModel.joinRoom(
                        pin,
                        onSuccess = {
                            navController.navigate(ROUTE_EDITOR)
                        },
                        onFailure = {
                            // fallback join or auto launch editor
                            navController.navigate(ROUTE_EDITOR)
                        }
                    )
                }
            )
        }

        composable(ROUTE_ROOM) {
            RoomScreen(
                onCreateRoom = { name ->
                    viewModel.createRoom(
                        name,
                        onSuccess = { code ->
                            navController.navigate(ROUTE_EDITOR) {
                                popUpTo(ROUTE_ROOM) { inclusive = true }
                            }
                        },
                        onFailure = {
                            // Force enter demo room on failure to keep sandbox experience robust
                            navController.navigate(ROUTE_EDITOR) {
                                popUpTo(ROUTE_ROOM) { inclusive = true }
                            }
                        }
                    )
                },
                onJoinRoom = { code ->
                    viewModel.joinRoom(
                        code,
                        onSuccess = {
                            navController.navigate(ROUTE_EDITOR) {
                                popUpTo(ROUTE_ROOM) { inclusive = true }
                            }
                        },
                        onFailure = {
                            // Force enter demo room on failure to keep sandbox experience robust
                            navController.navigate(ROUTE_EDITOR) {
                                popUpTo(ROUTE_ROOM) { inclusive = true }
                            }
                        }
                    )
                },
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }

        composable(ROUTE_EDITOR) {
            EditorScreen(
                viewModel = viewModel,
                onNavigateBack = {
                    navController.navigate(ROUTE_HOME) {
                        popUpTo(ROUTE_EDITOR) { inclusive = true }
                    }
                }
            )
        }

        composable(ROUTE_SETTINGS) {
            SettingsScreen(
                currentFirebaseStatus = isFirebaseOnline,
                onConfigureFirebase = { apiKey, projId, appId ->
                    viewModel.configureFirebaseCustom(apiKey, projId, appId, null) { _ -> }
                },
                onClearFirebase = {
                    viewModel.clearFirebaseCustom()
                },
                getFirebaseValues = {
                    viewModel.getFirebaseConfig()
                },
                onResetProfile = {
                    viewModel.authenticateUser("CreativeArtist", "")
                    navController.navigate(ROUTE_AUTH) {
                        popUpTo(ROUTE_HOME) { inclusive = true }
                    }
                },
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }
    }
}
