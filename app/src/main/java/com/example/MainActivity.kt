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
import com.example.data.local.SearchHistoryManager
import com.example.data.remote.NetworkModule
import com.example.domain.model.Sound
import com.example.player.AudioPlayerManager
import com.example.ui.screens.AuthScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.PlayerScreen
import com.example.ui.screens.ProfileScreen
import com.example.ui.screens.AnalyticsScreen
import com.example.ui.screens.UploadSoundScreen
import com.example.ui.screens.StoryViewerScreen
import com.example.ui.screens.CreateStoryScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.domain.model.VideoResponse

class MainActivity : ComponentActivity() {

    private lateinit var audioPlayerManager: AudioPlayerManager
    private lateinit var authManager: AuthManager
    private lateinit var appDatabase: AppDatabase
    private lateinit var audioDownloader: AudioDownloader
    private lateinit var searchHistoryManager: SearchHistoryManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        audioPlayerManager = AudioPlayerManager(this)
        authManager = AuthManager(this)
        appDatabase = AppDatabase.getDatabase(this)
        audioDownloader = AudioDownloader(this, appDatabase)
        searchHistoryManager = SearchHistoryManager(this)

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
                    
                    var selectedStories by remember { mutableStateOf<List<VideoResponse>>(emptyList()) }
                    var initialStoryIndex by remember { mutableIntStateOf(0) }

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
                                },
                                onStoryClick = { stories, index ->
                                    selectedStories = stories
                                    initialStoryIndex = index
                                    navController.navigate("story_view")
                                },
                                onCreateStoryClick = {
                                    navController.navigate("create_story")
                                },
                                searchHistoryManager = searchHistoryManager
                            )
                        }
                        composable("create_story") {
                            CreateStoryScreen(onBack = { navController.popBackStack() })
                        }
                        composable("story_view") {
                            StoryViewerScreen(
                                stories = selectedStories,
                                initialIndex = initialStoryIndex,
                                onClose = { navController.popBackStack() }
                            )
                        }
                        composable("upload") {
                            UploadSoundScreen(
                                onBack = { navController.popBackStack() }
                            )
                        }
                        composable("profile") {
                            ProfileScreen(
                                userId = null,
                                authManager = authManager,
                                appDatabase = appDatabase,
                                onBack = { navController.popBackStack() },
                                onLogout = { },
                                onAnalyticsClick = { navController.navigate("analytics") }
                            )
                        }
                        composable("profile/{userId}") { backStackEntry ->
                            ProfileScreen(
                                userId = backStackEntry.arguments?.getString("userId"),
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
                                onClose = { showPlayer = false },
                                onNavigateToProfile = { userId ->
                                    showPlayer = false
                                    navController.navigate("profile/$userId")
                                }
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
