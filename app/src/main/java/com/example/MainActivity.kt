package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.data.local.AppDatabase
import com.example.data.local.AudioDownloader
import com.example.data.local.AuthManager
import com.example.data.remote.NetworkModule
import com.example.domain.model.Sound
import com.example.player.AudioPlayerManager
import com.example.ui.screens.AuthScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.PlayerScreen
import com.example.ui.screens.ProfileScreen
import com.example.ui.screens.AnalyticsScreen
import com.example.ui.screens.UploadSoundScreen
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {

    private lateinit var audioPlayerManager: AudioPlayerManager
    private lateinit var authManager: AuthManager
    private lateinit var appDatabase: AppDatabase
    private lateinit var audioDownloader: AudioDownloader

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        audioPlayerManager = AudioPlayerManager(this)
        authManager = AuthManager(this)
        appDatabase = AppDatabase.getDatabase(this)
        audioDownloader = AudioDownloader(this, appDatabase)

        NetworkModule.tokenProvider = { authManager.getToken() }

        setContent {
            MyApplicationTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val navController = rememberNavController()
                    val isLoggedIn by authManager.isLoggedIn.collectAsState()
                    var currentSound by remember { mutableStateOf<Sound?>(null) }
                    var showPlayer by remember { mutableStateOf(false) }

                    LaunchedEffect(isLoggedIn) {
                        if (isLoggedIn) {
                            navController.navigate("home") {
                                popUpTo("auth") { inclusive = true }
                            }
                        } else {
                            navController.navigate("auth") {
                                popUpTo("home") { inclusive = true }
                            }
                        }
                    }

                    NavHost(
                        navController = navController, 
                        startDestination = if (isLoggedIn) "home" else "auth"
                    ) {
                        composable("auth") {
                            AuthScreen(
                                authManager = authManager,
                                onLoginSuccess = {
                                    navController.navigate("home") {
                                        popUpTo("auth") { inclusive = true }
                                    }
                                }
                            )
                        }
                        composable("home") {
                            HomeScreen(
                                onSoundClick = { sound ->
                                    currentSound = sound
                                    showPlayer = true
                                },
                                onProfileClick = {
                                    navController.navigate("profile")
                                },
                                onUploadClick = {
                                    navController.navigate("upload")
                                }
                            )
                        }
                        composable("upload") {
                            UploadSoundScreen(
                                onBack = { navController.popBackStack() }
                            )
                        }
                        composable("profile") {
                            ProfileScreen(
                                authManager = authManager,
                                appDatabase = appDatabase,
                                onBack = { navController.popBackStack() },
                                onLogout = { },
                                onAnalyticsClick = { navController.navigate("analytics") }
                            )
                        }
                        composable("analytics") {
                            AnalyticsScreen(
                                onBack = { navController.popBackStack() }
                            )
                        }
                    }

                    AnimatedVisibility(
                        visible = showPlayer,
                        enter = slideInVertically(initialOffsetY = { it }),
                        exit = slideOutVertically(targetOffsetY = { it })
                    ) {
                        currentSound?.let { sound ->
                            PlayerScreen(
                                sound = sound,
                                audioPlayerManager = audioPlayerManager,
                                audioDownloader = audioDownloader,
                                appDatabase = appDatabase,
                                onClose = { showPlayer = false }
                            )
                        }
                    }
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        audioPlayerManager.release()
    }
}
