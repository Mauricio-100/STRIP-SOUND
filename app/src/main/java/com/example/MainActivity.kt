package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Pause
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import coil.compose.AsyncImage
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

import com.example.ui.screens.AudioMetadataScreen

class MainActivity : ComponentActivity() {

    private lateinit var audioPlayerManager: AudioPlayerManager
    private lateinit var authManager: AuthManager
    private lateinit var appDatabase: AppDatabase
    private lateinit var audioDownloader: AudioDownloader
    private lateinit var searchHistoryManager: SearchHistoryManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Request runtime notification & bluetooth connect permissions dynamically to support AirPods, Speakers and Media Notifications elegantly
        val permissionsNeeded = mutableListOf<String>()
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            permissionsNeeded.add(android.Manifest.permission.POST_NOTIFICATIONS)
        }
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
            permissionsNeeded.add(android.Manifest.permission.BLUETOOTH_CONNECT)
        }
        if (permissionsNeeded.isNotEmpty()) {
            requestPermissions(permissionsNeeded.toTypedArray(), 101)
        }

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

                    val currentPlayActiveSound by audioPlayerManager.currentSound.collectAsState()
                    val isPlaying by audioPlayerManager.isPlaying.collectAsState()
                    val currentPosition by audioPlayerManager.currentPosition.collectAsState()
                    val duration by audioPlayerManager.duration.collectAsState()
                    val volume by audioPlayerManager.volume.collectAsState()

                    LaunchedEffect(currentPlayActiveSound) {
                        currentPlayActiveSound?.let { currentSound = it }
                    }

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

                    val context = androidx.compose.ui.platform.LocalContext.current
                    LaunchedEffect(isLoggedIn) {
                        if (isLoggedIn) {
                            val notificationManager = com.example.util.CustomNotificationManager(context)
                            val creators = listOf("Emma", "Julien", "Lucas", "Sophie", "Lucas", "Alexandre", "Marc", "Julie", "Thomas", "Manon")
                            val sounds = listOf("Deep Bass Drop", "Electro Chill", "Acoustic Sunset", "Summer Wave", "Strip Beat #3", "Podcast Chill")
                            val commentsText = listOf("Wow, super son !", "Ce rythme est incroyable 🔥", "J'adore l'ambiance, propre !", "Vraiment lourd !", "Add a ma playlist direct !")
                            
                            while (true) {
                                kotlinx.coroutines.delay(kotlin.random.Random.nextLong(15000, 30000))
                                when (kotlin.random.Random.nextInt(6)) {
                                    0 -> {
                                        val creator = creators.random()
                                        val title = sounds.random()
                                        notificationManager.notifyNewStory(creator, title)
                                    }
                                    1 -> {
                                        val follower = creators.random()
                                        notificationManager.notifyNewSubscription(follower)
                                    }
                                    2 -> {
                                        val liker = creators.random()
                                        val item = sounds.random()
                                        notificationManager.notifyNewLike(liker, item)
                                    }
                                    3 -> {
                                        val commenter = creators.random()
                                        val comment = commentsText.random()
                                        val item = sounds.random()
                                        notificationManager.notifyNewComment(commenter, comment, item)
                                    }
                                    4 -> {
                                        val sharer = creators.random()
                                        val item = sounds.random()
                                        notificationManager.notifyNewShare(sharer, item)
                                    }
                                    5 -> {
                                        val title = sounds.random()
                                        notificationManager.notifyTenNewPlays(title)
                                    }
                                }
                            }
                        }
                    }

                    Box(modifier = Modifier.fillMaxSize()) {
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
                                    onArtistClick = { userId ->
                                        navController.navigate("profile/$userId")
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
                                    searchHistoryManager = searchHistoryManager,
                                    authManager = authManager
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
                                    onBack = { navController.popBackStack() },
                                    onNavigateToAudioMetadata = { navController.navigate("audio_metadata") }
                                )
                            }
                            composable("profile") {
                                ProfileScreen(
                                    userId = null,
                                    authManager = authManager,
                                    appDatabase = appDatabase,
                                    onBack = { navController.popBackStack() },
                                    onLogout = { },
                                    onAnalyticsClick = { navController.navigate("analytics") },
                                    onSoundClick = { sound ->
                                        currentSound = sound
                                        showPlayer = true
                                    }
                                )
                            }
                            composable("profile/{userId}") { backStackEntry ->
                                ProfileScreen(
                                    userId = backStackEntry.arguments?.getString("userId"),
                                    authManager = authManager,
                                    appDatabase = appDatabase,
                                    onBack = { navController.popBackStack() },
                                    onLogout = { },
                                    onAnalyticsClick = { navController.navigate("analytics") },
                                    onSoundClick = { sound ->
                                        currentSound = sound
                                        showPlayer = true
                                    }
                                )
                            }
                            composable("analytics") {
                                AnalyticsScreen(
                                    onBack = { navController.popBackStack() }
                                )
                            }
                            composable("audio_metadata") {
                                AudioMetadataScreen(
                                    onBack = { navController.popBackStack() }
                                )
                            }
                        }

                        // Persistent bottom audio player component removed as requested
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
                                authManager = authManager,
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

@Composable
fun PersistentPlayerBar(
    sound: Sound,
    isPlaying: Boolean,
    currentPosition: Long,
    duration: Long,
    volume: Float,
    onPlayPauseClick: () -> Unit,
    onVolumeChange: (Float) -> Unit,
    onBarClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val progressFract = if (duration > 0) currentPosition.toFloat() / duration else 0f
    
    Card(
        modifier = modifier
            .fillMaxWidth()
            .height(72.dp)
            .clickable(onClick = onBarClick),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.95f)
        ),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            LinearProgressIndicator(
                progress = { progressFract },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(3.dp),
                color = Color(0xFF06B6D4),
                trackColor = Color.DarkGray
            )
            
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                AsyncImage(
                    model = sound.cover_url ?: "https://picsum.photos/200",
                    contentDescription = "Cover",
                    modifier = Modifier
                        .size(42.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .border(1.dp, Color.White.copy(alpha = 0.15f), RoundedCornerShape(6.dp)),
                    contentScale = ContentScale.Crop
                )
                
                Spacer(modifier = Modifier.width(10.dp))
                
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = sound.title ?: "Untitled",
                        color = Color.White,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = sound.username ?: sound.author_username ?: "Unknown Artist",
                        color = Color.Gray,
                        fontSize = 11.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                
                IconButton(onClick = onPlayPauseClick, modifier = Modifier.size(36.dp)) {
                    Icon(
                        imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = if (isPlaying) "Pause" else "Play",
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                }
                
                Spacer(modifier = Modifier.width(8.dp))
                
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(end = 4.dp)
                ) {
                    Text(
                        text = " VOL ",
                        color = Color(0xFF06B6D4),
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier
                            .border(1.dp, Color(0xFF06B6D4).copy(alpha = 0.4f), RoundedCornerShape(4.dp))
                            .padding(horizontal = 4.dp, vertical = 2.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Slider(
                        value = volume,
                        onValueChange = onVolumeChange,
                        modifier = Modifier.width(60.dp),
                        colors = SliderDefaults.colors(
                            thumbColor = Color(0xFF06B6D4),
                            activeTrackColor = Color(0xFF06B6D4),
                            inactiveTrackColor = Color.DarkGray
                        )
                    )
                }
            }
        }
    }
}
