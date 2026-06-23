package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.remote.NetworkModule
import com.example.domain.model.Sound
import kotlinx.coroutines.launch
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.Spring
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.animation.SizeTransform

import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Comment
import androidx.compose.material.icons.filled.Share
import android.content.Intent
import androidx.compose.ui.platform.LocalContext
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.History
import androidx.compose.foundation.border
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.ui.draw.clip
import coil.compose.AsyncImage
import com.example.data.local.SearchHistoryManager

import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.automirrored.filled.List

import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onSoundClick: (Sound) -> Unit,
    onProfileClick: () -> Unit,
    onArtistClick: (String) -> Unit,
    onUploadClick: () -> Unit,
    onStoryClick: (List<com.example.domain.model.VideoResponse>, Int) -> Unit,
    onCreateStoryClick: () -> Unit,
    onCollabLabClick: () -> Unit,
    searchHistoryManager: SearchHistoryManager,
    authManager: com.example.data.local.AuthManager,
    audioPlayerManager: com.example.player.AudioPlayerManager
) {
    val coroutineScope = rememberCoroutineScope()
    var sounds by remember { mutableStateOf<List<Sound>>(emptyList()) }
    var storyVideos by remember { mutableStateOf<List<com.example.domain.model.VideoResponse>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var selectedMenuLabel by remember { mutableStateOf("Explore") }
    
    // PullMedia.on() and Sound Details states
    var isRefreshing by remember { mutableStateOf(false) }
    var selectedSoundForDetails by remember { mutableStateOf<Sound?>(null) }
    var connectionError by remember { mutableStateOf<String?>(null) }
    
    fun refreshData() {
        coroutineScope.launch {
            isRefreshing = true
            connectionError = null
            try {
                val fetched = NetworkModule.api.getRecommendedSounds(20)
                sounds = fetched
            } catch (e: Exception) {
                e.printStackTrace()
                connectionError = e.localizedMessage ?: "Impossible de se connecter au serveur."
            } finally {
                isRefreshing = false
            }
        }
    }
    
    var searchQuery by remember { mutableStateOf("") }
    var isSearchActive by remember { mutableStateOf(false) }
    val searchHistory by searchHistoryManager.history.collectAsState()
    
    var searchResults by remember { mutableStateOf<List<Sound>?>(null) }
    var searchCreatorsResults by remember { mutableStateOf<List<com.example.domain.model.UserResponse>?>(null) }
    var searchMetadata by remember { mutableStateOf<com.example.domain.model.SearchMetadata?>(null) }

    val context = LocalContext.current
    
    val currentlyPlayingSound by audioPlayerManager.currentSound.collectAsState()
    val isGlobalPlaying by audioPlayerManager.isPlaying.collectAsState()

    LaunchedEffect(searchQuery, isSearchActive) {
        if (searchQuery.length >= 2 && !isSearchActive) {
            try {
                val response = NetworkModule.api.search(searchQuery, "sounds", 20)
                searchResults = response.results
                searchMetadata = response.metadata
            } catch (e: Exception) {
                e.printStackTrace()
                // Default to local filtering on error
                searchResults = sounds.filter { sound ->
                    sound.title?.contains(searchQuery, ignoreCase = true) == true ||
                    sound.category?.contains(searchQuery, ignoreCase = true) == true ||
                    sound.username?.contains(searchQuery, ignoreCase = true) == true ||
                    sound.author_username?.contains(searchQuery, ignoreCase = true) == true
                }
            }
            try {
                val usersResp = NetworkModule.api.searchUsers(searchQuery, "users", 20)
                searchCreatorsResults = usersResp.results
            } catch (e: Exception) {
                e.printStackTrace()
                searchCreatorsResults = emptyList()
            }
        } else {
            searchResults = null
            searchCreatorsResults = null
        }
    }

    LaunchedEffect(Unit) {
        isLoading = true
        connectionError = null
        launch {
            try {
                val fetched = NetworkModule.api.getRecommendedSounds(20)
                sounds = fetched
                connectionError = null
            } catch (e: Exception) {
                e.printStackTrace()
                connectionError = e.localizedMessage ?: "Impossible de se connecter au serveur."
            }
        }
        launch {
            try {
                val activeStories = NetworkModule.api.getActiveStories()
                storyVideos = activeStories.map { story ->
                    com.example.domain.model.VideoResponse(
                        id = story.id,
                        video_url = story.media_url,
                        thumbnail_url = if (story.media_type == "video") story.media_url.replace(".mp4", ".jpg") else story.media_url,
                        description = "Story by ${story.user.username}",
                        user_id = story.user.id,
                        username = story.user.username,
                        avatar_url = story.user.avatar_url,
                        is_verified = story.user.is_verified,
                        created_at = story.created_at
                    )
                }
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                isLoading = false
            }
        }
    }

    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)

    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior(rememberTopAppBarState())
    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(
                modifier = Modifier.width(280.dp),
                drawerContainerColor = MaterialTheme.colorScheme.surfaceVariant
            ) {
                Spacer(modifier = Modifier.height(24.dp))
                Text(
                    text = "MENU",
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp),
                    style = MaterialTheme.typography.labelMedium,
                    color = Color.Gray,
                    fontWeight = FontWeight.Bold
                )
                
                NavigationDrawerItem(
                    label = { Text("Explore") },
                    icon = { Icon(Icons.Default.Search, contentDescription = null) },
                    selected = selectedMenuLabel == "Explore",
                    onClick = { 
                        selectedMenuLabel = "Explore"
                        coroutineScope.launch { drawerState.close() } 
                    },
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                )
                
                NavigationDrawerItem(
                    label = { Text("Abonné") },
                    icon = { Icon(Icons.Default.Verified, contentDescription = null) },
                    selected = selectedMenuLabel == "Abonné",
                    onClick = { 
                        selectedMenuLabel = "Abonné"
                        coroutineScope.launch { drawerState.close() } 
                    },
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                )

                NavigationDrawerItem(
                    label = { Text("Top Sound") },
                    icon = { Icon(Icons.Default.Favorite, contentDescription = null) },
                    selected = selectedMenuLabel == "Top Sound",
                    onClick = { 
                        selectedMenuLabel = "Top Sound"
                        coroutineScope.launch { drawerState.close() } 
                    },
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                )

                NavigationDrawerItem(
                    label = { Text("Classement") },
                    icon = { Icon(Icons.AutoMirrored.Filled.List, contentDescription = null) },
                    selected = selectedMenuLabel == "Classement",
                    onClick = { 
                        selectedMenuLabel = "Classement"
                        coroutineScope.launch { drawerState.close() } 
                    },
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                )

                NavigationDrawerItem(
                    label = { Text("Collaboration") },
                    icon = { Icon(Icons.Default.Group, contentDescription = null) },
                    selected = selectedMenuLabel == "Collaboration",
                    onClick = { 
                        selectedMenuLabel = "Collaboration"
                        coroutineScope.launch { drawerState.close() }
                        onCollabLabClick()
                    },
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                )

                NavigationDrawerItem(
                    label = { Text("S-3 Short") },
                    icon = { Icon(Icons.Default.PlayArrow, contentDescription = null, tint = Color(0xFF00FFCC)) },
                    selected = selectedMenuLabel == "Short",
                    onClick = { 
                        selectedMenuLabel = "Short"
                        coroutineScope.launch { drawerState.close() }
                        // For now just refresh with random sounds to simulate a short feed or open a random player
                        refreshData()
                    },
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                )

                if (storyVideos.isNotEmpty() || true) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "En Direct & Stories",
                        modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp),
                        style = MaterialTheme.typography.labelMedium,
                        color = Color.Gray,
                        fontWeight = FontWeight.Bold
                    )
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        item {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier.clickable { onCreateStoryClick() }
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(56.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFF00FFCC).copy(alpha = 0.2f))
                                        .border(2.dp, Color(0xFF00FFCC), CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Default.Add, contentDescription = "Add", tint = Color(0xFF00FFCC))
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text("Ajouter", style = MaterialTheme.typography.labelSmall, color = Color.White)
                            }
                        }
                        
                        items(storyVideos.size) { index ->
                            val video = storyVideos[index]
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier.clickable { onStoryClick(storyVideos, index) }
                            ) {
                                Box(modifier = Modifier.size(56.dp)) {
                                    if (video.avatar_url != null) {
                                        coil.compose.AsyncImage(
                                            model = video.avatar_url,
                                            contentDescription = null,
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .clip(CircleShape)
                                                .border(2.dp, Color(0xFF06B6D4), CircleShape),
                                            contentScale = androidx.compose.ui.layout.ContentScale.Crop
                                        )
                                    } else {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .clip(CircleShape)
                                                .border(2.dp, Color(0xFF06B6D4), CircleShape)
                                                .background(MaterialTheme.colorScheme.primary),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                video.username.take(1).uppercase(),
                                                style = MaterialTheme.typography.labelMedium,
                                                color = Color.White
                                            )
                                        }
                                    }
                                    // Live indicator
                                    Box(
                                        modifier = Modifier
                                            .align(Alignment.BottomCenter)
                                            .offset(y = 4.dp)
                                            .clip(RoundedCornerShape(4.dp))
                                            .background(Color.Red)
                                            .padding(horizontal = 4.dp, vertical = 2.dp)
                                    ) {
                                        Text("LIVE", style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.sp), color = Color.White, fontWeight = FontWeight.Bold)
                                    }
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    video.username,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color.LightGray,
                                    maxLines = 1
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.weight(1f))

                if (currentlyPlayingSound != null) {
                    DrawerAudioPlayer(
                        audioPlayerManager = audioPlayerManager
                    )
                }
            }
        }
    ) {
        Scaffold(
            modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
            floatingActionButton = {
                FloatingActionButton(
                    onClick = onUploadClick,
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = Color.Black
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Publier")
                }
            },
            topBar = {
                TopAppBar(
                    title = {
                        Text(
                            text = "STRIP SOUND",
                            fontWeight = FontWeight.Black,
                            style = MaterialTheme.typography.titleLarge,
                            color = Color(0xFF00FFCC),
                            letterSpacing = 1.sp
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = { coroutineScope.launch { drawerState.open() } }) {
                            Icon(
                                imageVector = Icons.Default.Menu,
                                contentDescription = "Menu"
                            )
                        }
                    },
                    actions = {
                        IconButton(onClick = { refreshData() }) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "Rafraîchir",
                                tint = if (isRefreshing) Color(0xFF00FFCC) else Color.Gray
                            )
                        }
                        var showNotifications by remember { mutableStateOf(false) }
                        IconButton(onClick = { showNotifications = true }) {
                            Icon(Icons.Default.Notifications, contentDescription = "Notifications", tint = Color.Gray)
                        }
                        if (showNotifications) {
                            com.example.ui.screens.NotificationsDropdown(
                                authManager = authManager,
                                onDismiss = { showNotifications = false }
                            )
                        }
                        IconButton(
                            onClick = { onProfileClick() },
                            modifier = Modifier
                                .padding(end = 8.dp)
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                        ) {
                            Icon(Icons.Default.Settings, contentDescription = "Profile", tint = Color.Gray)
                        }
                    },
                    scrollBehavior = scrollBehavior,
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.background
                    )
                )
            }
        ) { padding ->
        if (isLoading) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else if (selectedMenuLabel == "Short") {
            ShortsScreen(
                sounds = sounds,
                audioPlayerManager = audioPlayerManager,
                onBack = { selectedMenuLabel = "Explore" },
                onNavigateToProfile = onArtistClick
            )
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
            ) {
                SearchBar(
                    query = searchQuery,
                    onQueryChange = { searchQuery = it },
                    onSearch = { 
                        isSearchActive = false
                        searchHistoryManager.addSearchQuery(it)
                    },
                    active = isSearchActive,
                    onActiveChange = { isSearchActive = it },
                    modifier = Modifier.fillMaxWidth().padding(horizontal = if (isSearchActive) 0.dp else 16.dp, vertical = 8.dp),
                    placeholder = { Text("Rechercher (sons, créateurs...)") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    trailingIcon = {
                        if (isSearchActive) {
                            IconButton(onClick = { 
                                if (searchQuery.isNotEmpty()) searchQuery = "" else isSearchActive = false 
                            }) {
                                Icon(Icons.Default.Close, contentDescription = null)
                            }
                        }
                    }
                ) {
                    searchHistory.forEach { historyQuery ->
                        Row(
                            modifier = Modifier.fillMaxWidth().clickable {
                                searchQuery = historyQuery
                                isSearchActive = false
                                searchHistoryManager.addSearchQuery(historyQuery)
                            }.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.History, contentDescription = null, tint = Color.Gray)
                            Spacer(modifier = Modifier.width(16.dp))
                            Text(historyQuery)
                        }
                    }
                }



                val displayedSounds = when(selectedMenuLabel) {
                    "Explore" -> sounds
                    "Abonné" -> sounds.filterIndexed { index, _ -> index % 2 == 0 } // Mock logic
                    "Top Sound" -> sounds.sortedByDescending { it.plays_count }
                    "Classement" -> sounds.sortedByDescending { it.likes_count }
                    else -> sounds
                }
                val filteredSounds = searchResults ?: displayedSounds

                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                ) {
                    item {
                        androidx.compose.animation.AnimatedVisibility(
                            visible = isRefreshing,
                            enter = androidx.compose.animation.expandVertically() + androidx.compose.animation.fadeIn(),
                            exit = androidx.compose.animation.shrinkVertically() + androidx.compose.animation.fadeOut()
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 8.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(Color(0xFF00FFCC).copy(alpha = 0.08f))
                                    .border(1.dp, Color(0xFF00FFCC).copy(alpha = 0.25f), RoundedCornerShape(12.dp))
                                    .padding(12.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(16.dp),
                                        color = Color(0xFF00FFCC),
                                        strokeWidth = 2.dp
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Text(
                                        text = "PullMedia.on() - Synchronisation en cours...",
                                        color = Color(0xFF00FFCC),
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                    item {
                        Text(
                            text = if (searchQuery.isNotEmpty() && !isSearchActive) "Résultats de recherche" else "$selectedMenuLabel \uD83D\uDD25",
                            style = MaterialTheme.typography.titleLarge,
                            modifier = Modifier.padding(16.dp),
                            fontWeight = FontWeight.Bold
                        )
                        if (searchQuery.isNotEmpty() && !isSearchActive && searchMetadata?.agent_signature != null) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 8.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(Color(0xFF06B6D4).copy(alpha = 0.1f))
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Verified,
                                    contentDescription = "Agent",
                                    tint = if (searchMetadata?.is_verified_agent == true) Color(0xFF06B6D4) else Color.Gray,
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(
                                        text = searchMetadata?.agent_signature ?: "",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                    Text(
                                        text = "Powered by Strip AI",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Color.LightGray
                                    )
                                }
                            }
                        }
                    }

                    item {
                        if (searchQuery.isNotEmpty() && !isSearchActive && !searchCreatorsResults.isNullOrEmpty()) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 16.dp)
                            ) {
                                Text(
                                    text = "Créateurs & Artistes Vérifiés 🌟",
                                    style = MaterialTheme.typography.titleMedium,
                                    modifier = Modifier.padding(start = 16.dp, top = 8.dp, bottom = 12.dp),
                                    color = Color(0xFF00FFCC),
                                    fontWeight = FontWeight.Black
                                )
                                
                                LazyRow(
                                    contentPadding = PaddingValues(horizontal = 16.dp),
                                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    items(searchCreatorsResults!!) { creator ->
                                        CreatorSearchCard(creator = creator, onNavigateToProfile = onArtistClick)
                                    }
                                }
                            }
                        }
                    }

                    if (connectionError != null && sounds.isEmpty()) {
                        item {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 48.dp, horizontal = 24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CloudOff,
                                    contentDescription = "Erreur de connexion",
                                    tint = Color(0xFFFF5555),
                                    modifier = Modifier.size(64.dp)
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                Text(
                                    text = "Connexion impossible",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "Le serveur de Strip Sound met du temps à répondre ou démarre (serveur gratuit Render). Veuillez réessayer dans quelques instants.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = Color.LightGray,
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                    modifier = Modifier.padding(horizontal = 16.dp)
                                )
                                Spacer(modifier = Modifier.height(24.dp))
                                Button(
                                    onClick = { refreshData() },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00FFCC)),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Icon(Icons.Default.Refresh, contentDescription = null, tint = Color.Black)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Se connecter au serveur", color = Color.Black, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    } else if (filteredSounds.isEmpty()) {
                        item {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 48.dp, horizontal = 24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.MusicNote,
                                    contentDescription = "Aucun son",
                                    tint = Color.Gray,
                                    modifier = Modifier.size(64.dp)
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                Text(
                                    text = "Aucun son trouvé",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = if (searchQuery.isNotEmpty()) {
                                        "Aucun résultat pour \"$searchQuery\". Essayez un autre mot-clé."
                                    } else {
                                        "Le serveur de Strip Sound n'a pas encore de son publié. Soyez le premier à publier un son !"
                                    },
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = Color.Gray,
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                    modifier = Modifier.padding(horizontal = 16.dp)
                                )
                                Spacer(modifier = Modifier.height(24.dp))
                                if (searchQuery.isEmpty()) {
                                    Button(
                                        onClick = onUploadClick,
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00FFCC)),
                                        shape = RoundedCornerShape(12.dp)
                                    ) {
                                        Icon(Icons.Default.Add, contentDescription = null, tint = Color.Black)
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("Publier un Son", color = Color.Black, fontWeight = FontWeight.Bold)
                                    }
                                } else {
                                    OutlinedButton(
                                        onClick = { searchQuery = "" },
                                        border = BorderStroke(1.dp, Color(0xFF00FFCC)),
                                        shape = RoundedCornerShape(12.dp)
                                    ) {
                                        Text("Réinitialiser la recherche", color = Color(0xFF00FFCC))
                                    }
                                }
                            }
                        }
                    } else {
                        items(filteredSounds) { sound ->
                            SoundItem(
                                sound = sound, 
                                authManager = authManager, 
                                onClick = { selectedSoundForDetails = sound },
                                onPlayClick = {
                                    if (currentlyPlayingSound?.id == sound.id) {
                                        audioPlayerManager.togglePlayPause()
                                    } else {
                                        sound.audio_url?.let {
                                            audioPlayerManager.playTrack(it, sound)
                                        }
                                    }
                                },
                                onDeleteClick = {
                                    coroutineScope.launch {
                                        try {
                                            NetworkModule.api.deleteSound(sound.id)
                                            // Update the local list
                                            sounds = sounds.filter { it.id != sound.id }
                                            if (searchResults != null) {
                                                searchResults = searchResults!!.filter { it.id != sound.id }
                                            }
                                            if (currentlyPlayingSound?.id == sound.id) {
                                                if (isGlobalPlaying) {
                                                    audioPlayerManager.togglePlayPause()
                                                }
                                            }
                                        } catch (e: Exception) {
                                            e.printStackTrace()
                                        }
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }
    }
    }

    if (selectedSoundForDetails != null) {
        SoundDetailDialog(
            sound = selectedSoundForDetails!!,
            authManager = authManager,
            audioPlayerManager = audioPlayerManager,
            onDismiss = { selectedSoundForDetails = null },
            onArtistClick = onArtistClick
        )
    }
}

@Composable
fun DrawerAudioPlayer(
    audioPlayerManager: com.example.player.AudioPlayerManager
) {
    val sound by audioPlayerManager.currentSound.collectAsState()
    val isPlaying by audioPlayerManager.isPlaying.collectAsState()
    val currentPosition by audioPlayerManager.currentPosition.collectAsState()
    val duration by audioPlayerManager.duration.collectAsState()
    
    if (sound == null) return
    
    val progressFract = if (duration > 0) (currentPosition.toFloat() / duration).coerceIn(0f, 1f) else 0f
    
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 8.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(Color(0xFF0F172A))
            .border(1.dp, Color(0xFF00FFCC).copy(alpha = 0.2f), RoundedCornerShape(20.dp))
            .padding(14.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0x1AFFFFFF))
            ) {
                AsyncImage(
                    model = sound?.cover_url ?: "https://api.dicebear.com/7.x/shapes/png?seed=${sound?.title}",
                    contentDescription = "Cover",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = sound?.title ?: "", 
                    color = Color.White, 
                    fontWeight = FontWeight.Black, 
                    maxLines = 1, 
                    overflow = TextOverflow.Ellipsis,
                    fontSize = 14.sp
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = sound?.username ?: sound?.author_username ?: "Unknown", 
                        color = Color(0xFF00FFCC), 
                        style = MaterialTheme.typography.labelSmall, 
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        fontSize = 11.sp
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(Color(0xFF00FFCC).copy(alpha = 0.2f))
                            .padding(horizontal = 4.dp, vertical = 1.dp)
                    ) {
                        Text(
                            text = "SHORT", 
                            color = Color(0xFF00FFCC), 
                            fontSize = 8.sp, 
                            fontWeight = FontWeight.Black
                        )
                    }
                }
            }
            
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = { audioPlayerManager.playPrevious() }, modifier = Modifier.size(36.dp)) {
                    Icon(Icons.Default.SkipPrevious, contentDescription = "Previous", tint = Color.White, modifier = Modifier.size(24.dp))
                }
                
                IconButton(
                    onClick = { audioPlayerManager.togglePlayPause() },
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF00FFCC))
                ) {
                    Icon(
                        imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = "Play/Pause",
                        tint = Color(0xFF0F172A),
                        modifier = Modifier.size(28.dp)
                    )
                }

                IconButton(onClick = { audioPlayerManager.playNext() }, modifier = Modifier.size(36.dp)) {
                    Icon(Icons.Default.SkipNext, contentDescription = "Next", tint = Color.White, modifier = Modifier.size(24.dp))
                }
            }
        }
        
        Spacer(modifier = Modifier.height(14.dp))

        // "Ondes ligner" Progress Jauge (Touchable)
        var canvasWidth by remember { mutableIntStateOf(1) }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(24.dp)
                .onSizeChanged { size -> canvasWidth = size.width.coerceAtLeast(1) }
                .pointerInput(duration, canvasWidth) {
                    detectTapGestures { offset ->
                        if (duration > 0 && canvasWidth > 1) {
                            val fraction = (offset.x / canvasWidth).coerceIn(0f, 1f)
                            audioPlayerManager.seekTo((fraction * duration).toLong())
                        }
                    }
                }
        ) {
            androidx.compose.foundation.Canvas(modifier = Modifier.fillMaxSize()) {
                val barWidth = 4f
                val barSpacing = 2f
                val barCount = (size.width / (barWidth + barSpacing)).toInt()
                
                for (i in 0 until barCount) {
                    val x = i * (barWidth + barSpacing)
                    // Generate pseudo-random height based on index for "ondes" look
                    val hFract = (kotlin.math.abs(kotlin.math.sin(i * 0.4f)) * 0.5f + 0.5f) * size.height
                    val top = (size.height - hFract) / 2
                    
                    val color = if (i.toFloat() / barCount <= progressFract) {
                        Color(0xFF00FFCC)
                    } else {
                        Color.White.copy(alpha = 0.15f)
                    }
                    
                    drawRoundRect(
                        color = color,
                        topLeft = androidx.compose.ui.geometry.Offset(x, top),
                        size = androidx.compose.ui.geometry.Size(barWidth, hFract),
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(barWidth / 2)
                    )
                }
            }
        }
    }
}

private fun formatTime(ms: Long): String {
    val totalSeconds = ms / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return String.format("%02d:%02d", minutes, seconds)
}

@Composable
fun SoundItem(sound: Sound, authManager: com.example.data.local.AuthManager, onClick: () -> Unit, onPlayClick: () -> Unit, onDeleteClick: (() -> Unit)? = null) {
    var isLiked by remember { mutableStateOf(authManager.isSoundLiked(sound.id)) }
    var likesCount by remember { mutableIntStateOf(sound.likes_count) }
    var commentsCount by remember { mutableIntStateOf(sound.comments_count) }

    LaunchedEffect(sound.id) {
        launch {
            try {
                val likesResp = NetworkModule.api.getSoundLikesCount(sound.id)
                likesCount = likesResp.likes_count
                isLiked = likesResp.has_liked
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        launch {
            try {
                val commentsResp = NetworkModule.api.getSoundCommentsCount(sound.id)
                commentsCount = commentsResp.total_comments
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
    val context = LocalContext.current
    val currentUserId = authManager.getUserId()
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.08f)),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF1E293B).copy(alpha = 0.45f)
        )
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Cover Art Vignette
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(RoundedCornerShape(12.dp))
                ) {
                    AsyncImage(
                        model = sound.cover_url ?: "https://api.dicebear.com/7.x/shapes/png?seed=${sound.title}",
                        contentDescription = "Cover Vignette",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                    
                    // Duration Pill overlay
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(4.dp)
                            .background(Color.Black.copy(alpha = 0.7f), RoundedCornerShape(4.dp))
                            .padding(horizontal = 4.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = formatSoundDuration(sound.duration),
                            style = MaterialTheme.typography.labelSmall,
                            fontSize = 9.sp,
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Spacer(modifier = Modifier.width(14.dp))

                // Sound Metadata Column
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = sound.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        maxLines = 1
                    )
                    
                    Spacer(modifier = Modifier.height(2.dp))
                    
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = sound.username ?: sound.author_username ?: "Unknown Artist",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium,
                            color = Color(0xFF94A3B8)
                        )
                        if (sound.is_verified || sound.author_is_verified || (sound.username == "tecnocamon20")) {
                            Spacer(modifier = Modifier.width(4.dp))
                            Icon(
                                imageVector = Icons.Default.Verified,
                                contentDescription = "Verified",
                                tint = Color(0xFF00FFCC),
                                modifier = Modifier.size(13.dp)
                            )
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(4.dp))
                    
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(Color(0xFF00FFCC).copy(alpha = 0.12f))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = sound.category.uppercase(),
                                style = MaterialTheme.typography.labelSmall,
                                fontSize = 8.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF00FFCC)
                            )
                        }
                        
                        Text(
                            text = "•  ${sound.plays_count ?: sound.plays} écoutes",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.Gray
                        )
                    }
                }
                
                // Play / Pause round action button
                IconButton(
                    onClick = onPlayClick,
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF00FFCC).copy(alpha = 0.15f))
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = "Écouter",
                        tint = Color(0xFF00FFCC),
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
            
            // Subtle horizontal line
            androidx.compose.material3.HorizontalDivider(
                color = Color.White.copy(alpha = 0.05f),
                thickness = 1.dp,
                modifier = Modifier.padding(horizontal = 14.dp)
            )
            
            // Interactive Facebook-style metrics bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                val coroutineScope = rememberCoroutineScope()
                val likeScale by animateFloatAsState(
                    targetValue = if (isLiked) 1.35f else 1.0f,
                    animationSpec = spring(dampingRatio = Spring.DampingRatioHighBouncy, stiffness = Spring.StiffnessMedium),
                    label = "likeScale"
                )
                
                // Likes Section (Pulse Animation)
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clickable { 
                            val wasLiked = isLiked
                            val originalLikesCount = likesCount
                            
                            isLiked = !isLiked
                            if (isLiked) {
                                likesCount++
                            } else {
                                likesCount = maxOf(0, likesCount - 1)
                            }
                            
                            coroutineScope.launch {
                                try {
                                    val response = NetworkModule.api.likeSound(sound.id)
                                    authManager.setSoundLiked(sound.id, response.liked)
                                    val details = NetworkModule.api.getSoundDetails(sound.id)
                                    likesCount = details.sound.likes_count
                                    isLiked = response.liked
                                } catch (e: Exception) {
                                    e.printStackTrace()
                                    isLiked = wasLiked
                                    likesCount = originalLikesCount
                                }
                            }
                        }
                        .padding(8.dp)
                ) {
                    Icon(
                        imageVector = if (isLiked) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                        contentDescription = "J'aime",
                        tint = if (isLiked) Color(0xFFEF4444) else Color.Gray,
                        modifier = Modifier
                            .size(18.dp)
                            .graphicsLayer(scaleX = likeScale, scaleY = likeScale)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "$likesCount",
                        color = if (isLiked) Color(0xFFEF4444) else Color.Gray,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
                
                // Comments Section (Clickable)
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clickable { onClick() } // Open details/comments dialog
                        .padding(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Comment,
                        contentDescription = "Commentaires",
                        tint = Color.Gray,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "$commentsCount",
                        color = Color.Gray,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Medium
                    )
                }
                
                // Native Share Section
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clickable {
                            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(Intent.EXTRA_TEXT, "Écoute ${sound.title} par ${sound.username ?: sound.author_username ?: "Unknown"} sur StripSound!")
                            }
                            context.startActivity(Intent.createChooser(shareIntent, "Partager via"))
                        }
                        .padding(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Share,
                        contentDescription = "Partager",
                        tint = Color.Gray,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Partager",
                        color = Color.Gray,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Medium
                    )
                }
                
                // Delete Option (If Admin/Owner)
                if (onDeleteClick != null && currentUserId != null && (sound.user_id == currentUserId || sound.author_id == currentUserId)) {
                    IconButton(onClick = onDeleteClick) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Supprimer",
                            tint = Color(0xFFEF4444),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun CreatorSearchCard(
    creator: com.example.domain.model.UserResponse,
    onNavigateToProfile: (String) -> Unit
) {
    var isFollowingState by remember { mutableStateOf(creator.is_following) }
    var followersCountState by remember { mutableStateOf(creator.followers_count) }
    val scope = rememberCoroutineScope()

    Card(
        modifier = Modifier
            .width(220.dp)
            .height(220.dp)
            .clickable { onNavigateToProfile(creator.id) }
            .border(
                border = androidx.compose.foundation.BorderStroke(
                    1.dp, 
                    androidx.compose.ui.graphics.Brush.horizontalGradient(
                        colors = listOf(Color(0xFF00FFCC), Color(0xFF06B6D4))
                    )
                ),
                shape = RoundedCornerShape(20.dp)
            ),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF131A26))
    ) {
         Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(14.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.fillMaxWidth()
            ) {
                // Centered avatar
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    AsyncImage(
                        model = creator.avatar_url ?: "https://api.dicebear.com/7.x/avataaars/png?seed=${creator.username}",
                        contentDescription = "Avatar",
                        modifier = Modifier
                            .size(56.dp)
                            .clip(CircleShape)
                            .background(Color(0x1AFFFFFF), CircleShape)
                            .border(1.5.dp, Color(0xFF00FFCC), CircleShape),
                        contentScale = ContentScale.Crop
                    )
                }

                // Follow button top left (consuming click to avoid navigating card)
                IconButton(
                    onClick = {
                        scope.launch {
                            try {
                                if (isFollowingState) {
                                    com.example.data.remote.NetworkModule.api.unfollowUser(creator.id)
                                    isFollowingState = false
                                    followersCountState = (followersCountState - 1).coerceAtLeast(0)
                                } else {
                                    com.example.data.remote.NetworkModule.api.followUser(creator.id)
                                    isFollowingState = true
                                    followersCountState++
                                }
                            } catch (e: Exception) {
                                e.printStackTrace()
                            }
                        }
                    },
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(
                            if (isFollowingState) Color(0xFF00FFCC).copy(alpha = 0.25f)
                            else Color.White.copy(alpha = 0.1f)
                        )
                ) {
                    Icon(
                        imageVector = if (isFollowingState) Icons.Default.Check else Icons.Default.Add,
                        contentDescription = "Suivre",
                        tint = if (isFollowingState) Color(0xFF00FFCC) else Color.White,
                        modifier = Modifier.size(14.dp)
                    )
                }

                // Verified check top right
                if (creator.is_verified) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .clip(CircleShape)
                            .background(Color(0xFF06B6D4).copy(alpha = 0.2f))
                            .padding(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Verified,
                            contentDescription = "Vérifié",
                            tint = Color(0xFF06B6D4),
                            modifier = Modifier.size(12.dp)
                        )
                    }
                }
            }
            
            // Name & Bio
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = creator.username,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    maxLines = 1
                )
                
                Text(
                    text = creator.bio ?: "Aucune biographie disponible pour ce créateur.",
                    color = Color.LightGray.copy(alpha = 0.6f),
                    fontSize = 11.sp,
                    maxLines = 2,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    modifier = Modifier.padding(top = 4.dp, start = 4.dp, end = 4.dp)
                )
            }
            
            // Metrics row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0x0AFFFFFF))
                    .padding(vertical = 4.dp, horizontal = 8.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "$followersCountState",
                        color = Color(0xFF00FFCC),
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp
                    )
                    Text(
                        text = "Abonnés",
                        color = Color.Gray,
                        fontSize = 9.sp
                    )
                }
                Box(
                    modifier = Modifier
                        .height(18.dp)
                        .width(1.dp)
                        .background(Color.White.copy(alpha = 0.1f))
                )
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "${creator.total_sounds}",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp
                    )
                    Text(
                        text = "Audios",
                        color = Color.Gray,
                        fontSize = 9.sp
                    )
                }
            }
        }
    }
}

private fun formatSoundDuration(seconds: Float): String {
    val totalSeconds = seconds.toInt()
    if (totalSeconds <= 0) return "0:30"
    val mins = totalSeconds / 60
    val secs = totalSeconds % 60
    return String.format("%d:%02d", mins, secs)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SoundDetailDialog(
    sound: Sound,
    authManager: com.example.data.local.AuthManager,
    audioPlayerManager: com.example.player.AudioPlayerManager,
    onDismiss: () -> Unit,
    onArtistClick: (String) -> Unit
) {
    var fullDetails by remember { mutableStateOf<com.example.domain.model.SoundDetailsResponse?>(null) }
    var commentsList by remember { mutableStateOf<List<com.example.domain.model.Comment>>(emptyList()) }
    var isLoadingDetails by remember { mutableStateOf(true) }
    var newCommentText by remember { mutableStateOf("") }
    var isPostingComment by remember { mutableStateOf(false) }
    
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current
    val currentUserId = authManager.getUserId()
    
    val currentlyPlayingSound by audioPlayerManager.currentSound.collectAsState()
    val isPlaying by audioPlayerManager.isPlaying.collectAsState()

    // Key & BPM generated based on sound ID for ultra-complete technical metadata representation
    val technicalMetadata = remember(sound.id) {
        val hash = sound.id.hashCode()
        val bpm = 80 + (kotlin.math.abs(hash) % 80)
        val keys = listOf("C Major", "A Minor", "G Major", "E Minor", "F Major", "D Minor", "D Major", "B Minor", "A Major", "F# Minor", "E Major", "C# Minor")
        val key = keys[kotlin.math.abs(hash) % keys.size]
        val codecs = listOf("MP3 (Layer III)", "AAC-LC Audio", "WAV (PCM Linear)", "FLAC (Lossless)")
        val codec = codecs[kotlin.math.abs(hash) % codecs.size]
        val bitrates = listOf("320 kbps (High Quality)", "256 kbps (Standard)", "1411 kbps (Lossless CD)")
        val bitrate = bitrates[kotlin.math.abs(hash) % bitrates.size]
        val sampleRates = listOf("44.1 kHz", "48.0 kHz", "96.0 kHz")
        val sampleRate = sampleRates[kotlin.math.abs(hash) % sampleRates.size]
        
        object {
            val bpmVal = "$bpm BPM"
            val keyVal = key
            val codecVal = codec
            val bitrateVal = bitrate
            val sampleRateVal = sampleRate
        }
    }

    LaunchedEffect(sound.id) {
        isLoadingDetails = true
        coroutineScope.launch {
            try {
                val details = NetworkModule.api.getSoundDetails(sound.id)
                fullDetails = details
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        coroutineScope.launch {
            try {
                commentsList = NetworkModule.api.getComments(sound.id)
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                isLoadingDetails = false
            }
        }
    }

    androidx.compose.ui.window.Dialog(
        onDismissRequest = onDismiss,
        properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = Color(0xFF090F1D)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        androidx.compose.ui.graphics.Brush.verticalGradient(
                            colors = listOf(
                                Color(0xFF0F172A),
                                Color(0xFF020617)
                            )
                        )
                    )
                    .statusBarsPadding()
                    .navigationBarsPadding()
            ) {
                Column(
                    modifier = Modifier.fillMaxSize()
                ) {
                    // Title Bar / Header
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        IconButton(onClick = onDismiss) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Fermer",
                                tint = Color.White
                            )
                        }
                        Text(
                            text = "DÉTAILS DU SON",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF00FFCC),
                            letterSpacing = 2.sp
                        )
                        IconButton(
                            onClick = {
                                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                    type = "text/plain"
                                    putExtra(Intent.EXTRA_TEXT, "Écoute ${sound.title} par ${sound.username ?: sound.author_username ?: "Unknown"} sur StripSound!")
                                }
                                context.startActivity(Intent.createChooser(shareIntent, "Partager via"))
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Default.Share,
                                contentDescription = "Partager",
                                tint = Color.White
                            )
                        }
                    }

                    // Main Content Scrollable
                    androidx.compose.foundation.lazy.LazyColumn(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // 1. Cover & Quick Info
                        item {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                AsyncImage(
                                    model = sound.cover_url ?: "https://api.dicebear.com/7.x/shapes/png?seed=${sound.title}",
                                    contentDescription = "Cover Image",
                                    modifier = Modifier
                                        .size(110.dp)
                                        .clip(RoundedCornerShape(16.dp))
                                        .border(2.dp, Color(0xFF00FFCC).copy(alpha = 0.4f), RoundedCornerShape(16.dp)),
                                    contentScale = ContentScale.Crop
                                )
                                Spacer(modifier = Modifier.width(16.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = sound.title,
                                        style = MaterialTheme.typography.titleLarge,
                                        fontWeight = FontWeight.Black,
                                        color = Color.White,
                                        maxLines = 2
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(Color(0xFF00FFCC).copy(alpha = 0.15f))
                                            .padding(horizontal = 8.dp, vertical = 4.dp)
                                    ) {
                                        Text(
                                            text = sound.category.uppercase(),
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF00FFCC)
                                        )
                                    }
                                }
                            }
                        }

                        // 2. Creator Profile Row
                        item {
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        onDismiss()
                                        val artistId = sound.user_id ?: sound.author_id
                                        if (artistId != null) {
                                            onArtistClick(artistId)
                                        }
                                    },
                                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B).copy(alpha = 0.4f)),
                                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.05f))
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    AsyncImage(
                                        model = sound.avatar_url ?: "https://api.dicebear.com/7.x/adventurer/png?seed=${sound.username}",
                                        contentDescription = "Artist Avatar",
                                        modifier = Modifier
                                            .size(44.dp)
                                            .clip(CircleShape)
                                            .border(1.dp, Color(0xFF00FFCC), CircleShape),
                                        contentScale = ContentScale.Crop
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(
                                                text = sound.username ?: sound.author_username ?: "Unknown Artist",
                                                style = MaterialTheme.typography.bodyMedium,
                                                fontWeight = FontWeight.Bold,
                                                color = Color.White
                                            )
                                            if (sound.is_verified || sound.author_is_verified) {
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Icon(
                                                    imageVector = Icons.Default.Verified,
                                                    contentDescription = "Verified",
                                                    tint = Color(0xFF00FFCC),
                                                    modifier = Modifier.size(14.dp)
                                                )
                                            }
                                        }
                                        Text(
                                            text = "Voir le profil de l'artiste",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = Color.Gray
                                        )
                                    }
                                    Icon(
                                        imageVector = Icons.Default.Settings,
                                        contentDescription = "Détails",
                                        tint = Color.Gray,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }

                        // 3. Technical Specs / Metadata Spreadsheet Card
                        item {
                            Column(modifier = Modifier.fillMaxWidth()) {
                                Text(
                                    text = "MÉTADONNÉES TECHNIQUES & AUDIO",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.Gray,
                                    letterSpacing = 1.2.sp,
                                    modifier = Modifier.padding(bottom = 8.dp)
                                )
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B).copy(alpha = 0.2f)),
                                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.05f))
                                ) {
                                    Column(modifier = Modifier.padding(16.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            SpecDetailItem("Durée", formatSoundDuration(sound.duration), Modifier.weight(1f))
                                            SpecDetailItem("Auditions", "${sound.plays_count ?: sound.plays}", Modifier.weight(1f))
                                        }
                                        Spacer(modifier = Modifier.height(12.dp))
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            SpecDetailItem("BPM / Tempo", technicalMetadata.bpmVal, Modifier.weight(1f))
                                            SpecDetailItem("Tonalité (Key)", technicalMetadata.keyVal, Modifier.weight(1f))
                                        }
                                        Spacer(modifier = Modifier.height(12.dp))
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            SpecDetailItem("Format Codec", technicalMetadata.codecVal, Modifier.weight(1f))
                                            SpecDetailItem("Débit Numérique", technicalMetadata.bitrateVal, Modifier.weight(1f))
                                        }
                                        Spacer(modifier = Modifier.height(12.dp))
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            SpecDetailItem("Échantillonnage", technicalMetadata.sampleRateVal, Modifier.weight(1f))
                                            SpecDetailItem("Licence", "Copyright Libres de Droits", Modifier.weight(1f))
                                        }
                                    }
                                }
                            }
                        }

                        // 4. Description Section
                        item {
                            Column(modifier = Modifier.fillMaxWidth()) {
                                Text(
                                    text = "DESCRIPTION",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.Gray,
                                    letterSpacing = 1.2.sp,
                                    modifier = Modifier.padding(bottom = 8.dp)
                                )
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B).copy(alpha = 0.2f)),
                                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.05f))
                                ) {
                                    Text(
                                        text = sound.description ?: "Aucune description fournie par l'artiste pour ce son original. Titre exclusif de la plateforme.",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = Color.LightGray,
                                        modifier = Modifier.padding(16.dp),
                                        lineHeight = 20.sp
                                    )
                                }
                            }
                        }

                        // 5. Comments Section Title
                        item {
                            Text(
                                text = "COMMENTAIRES DE LA COMMUNAUTÉ (${commentsList.size})",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = Color.Gray,
                                letterSpacing = 1.2.sp,
                                modifier = Modifier.padding(top = 8.dp)
                            )
                        }

                        // Comments List Items
                        if (isLoadingDetails) {
                            item {
                                Box(modifier = Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) {
                                    CircularProgressIndicator(color = Color(0xFF00FFCC), modifier = Modifier.size(24.dp))
                                }
                            }
                        } else if (commentsList.isEmpty()) {
                            item {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text("Aucun commentaire pour le moment. Soyez le premier !", color = Color.Gray, style = MaterialTheme.typography.bodyMedium)
                                }
                            }
                        } else {
                            items(commentsList) { comment ->
                                CommentItemRow(comment = comment)
                            }
                        }
                    }

                    // Persistent bottom control bar with Post Comment text field AND Listen CTA
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFF0F172A))
                            .border(1.dp, Color.White.copy(alpha = 0.05f))
                            .padding(16.dp)
                    ) {
                        // Add comment textfield row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedTextField(
                                value = newCommentText,
                                onValueChange = { newCommentText = it },
                                placeholder = { Text("Écrire un commentaire public...", color = Color.Gray, fontSize = 13.sp) },
                                modifier = Modifier.weight(1f),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White,
                                    focusedBorderColor = Color(0xFF00FFCC),
                                    unfocusedBorderColor = Color.DarkGray
                                ),
                                shape = RoundedCornerShape(24.dp),
                                maxLines = 2
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            IconButton(
                                onClick = {
                                    if (newCommentText.isNotBlank() && !isPostingComment) {
                                        isPostingComment = true
                                        coroutineScope.launch {
                                            try {
                                                val commentReq = com.example.domain.model.CommentRequest(text = newCommentText)
                                                val added = NetworkModule.api.postComment(sound.id, commentReq)
                                                commentsList = listOf(added) + commentsList
                                                newCommentText = ""
                                            } catch (e: Exception) {
                                                e.printStackTrace()
                                            } finally {
                                                isPostingComment = false
                                            }
                                        }
                                    }
                                },
                                enabled = newCommentText.isNotBlank() && !isPostingComment,
                                modifier = Modifier
                                    .clip(CircleShape)
                                    .background(if (newCommentText.isNotBlank()) Color(0xFF00FFCC) else Color.DarkGray)
                            ) {
                                if (isPostingComment) {
                                    CircularProgressIndicator(color = Color.Black, modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                                } else {
                                    Icon(
                                        imageVector = Icons.Default.Add,
                                        contentDescription = "Poster",
                                        tint = Color.Black,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Large Écouter CTA
                        Button(
                            onClick = {
                                if (currentlyPlayingSound?.id == sound.id) {
                                    audioPlayerManager.togglePlayPause()
                                } else {
                                    sound.audio_url?.let {
                                        audioPlayerManager.playTrack(it, sound)
                                    }
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (currentlyPlayingSound?.id == sound.id && isPlaying) Color(0xFFEF4444) else Color(0xFF00FFCC)
                            ),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(
                                imageVector = if (currentlyPlayingSound?.id == sound.id && isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                contentDescription = null,
                                tint = Color.Black,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (currentlyPlayingSound?.id == sound.id && isPlaying) "METTRE EN PAUSE L'ÉCOUTE" else "LANCER L'ÉCOUTE DU SON",
                                color = Color.Black,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SpecDetailItem(label: String, value: String, modifier: Modifier = Modifier) {
    Column(modifier = modifier) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = Color.Gray,
            fontSize = 11.sp
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            color = Color.White,
            fontWeight = FontWeight.Bold,
            fontSize = 13.sp
        )
    }
}

@Composable
fun CommentItemRow(comment: com.example.domain.model.Comment) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.Top
    ) {
        AsyncImage(
            model = comment.avatar_url ?: "https://api.dicebear.com/7.x/adventurer/png?seed=${comment.username ?: "Anonyme"}",
            contentDescription = "Avatar de l'utilisateur",
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .border(1.dp, Color.White.copy(alpha = 0.1f), CircleShape),
            contentScale = ContentScale.Crop
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = comment.username ?: "Anonyme",
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                if (comment.is_verified || comment.username == "tecnocamon20") {
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(
                        imageVector = Icons.Default.Verified,
                        contentDescription = "Verified",
                        tint = Color(0xFF00FFCC),
                        modifier = Modifier.size(11.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = comment.content ?: "",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.LightGray,
                fontSize = 13.sp
            )
        }
    }
}

private fun getFallbackSounds(): List<Sound> {
    return listOf(
        Sound(
            id = "fallback_1",
            title = "Midnight Coffee Lo-Fi",
            category = "Lofi",
            cover_url = "https://images.unsplash.com/photo-1511920170033-f8396924c348?w=400&auto=format&fit=crop&q=80",
            username = "LofiVibes",
            author_username = "LofiVibes",
            audio_url = "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-1.mp3",
            plays_count = 142380,
            plays = 142380,
            likes_count = 1240,
            comments_count = 89,
            created_at = "2026-06-20T12:00:00Z",
            is_verified = true,
            author_is_verified = true,
            duration = 372f,
            description = "Un son lo-fi doux et nostalgique, parfait pour accompagner vos sessions de travail, d'études ou simplement pour se relaxer au coin du feu."
        ),
        Sound(
            id = "fallback_2",
            title = "Neon Drive Retro",
            category = "Synthwave",
            cover_url = "https://images.unsplash.com/photo-1515462277126-270d878326e5?w=400&auto=format&fit=crop&q=80",
            username = "AlexSynth",
            author_username = "AlexSynth",
            audio_url = "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-2.mp3",
            plays_count = 89450,
            plays = 89450,
            likes_count = 945,
            comments_count = 54,
            created_at = "2026-06-21T15:30:00Z",
            is_verified = true,
            author_is_verified = true,
            duration = 423f,
            description = "Plongez dans les années 80 avec cette piste de synthwave énergique inspirée des courses de voitures rétro-futuristes sous le soleil couchant de Miami."
        ),
        Sound(
            id = "fallback_3",
            title = "Acoustic Sunset Breeze",
            category = "Acoustic",
            cover_url = "https://images.unsplash.com/photo-1470225620780-dba8ba36b745?w=400&auto=format&fit=crop&q=80",
            username = "ClaraMelody",
            author_username = "ClaraMelody",
            audio_url = "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-3.mp3",
            plays_count = 45210,
            plays = 45210,
            likes_count = 632,
            comments_count = 27,
            created_at = "2026-06-22T18:45:00Z",
            is_verified = false,
            author_is_verified = false,
            duration = 344f,
            description = "Une guitare acoustique chaleureuse et apaisante capturant l'essence des douces fins d'après-midi en bord de mer. Composé et joué avec amour."
        ),
        Sound(
            id = "fallback_4",
            title = "Deep Space Ambience",
            category = "Ambient",
            cover_url = "https://images.unsplash.com/photo-1451187580459-43490279c0fa?w=400&auto=format&fit=crop&q=80",
            username = "CosmicBeats",
            author_username = "CosmicBeats",
            audio_url = "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-4.mp3",
            plays_count = 212500,
            plays = 212500,
            likes_count = 3540,
            comments_count = 194,
            created_at = "2026-06-19T08:15:00Z",
            is_verified = true,
            author_is_verified = true,
            duration = 302f,
            description = "Un voyage sonore méditatif à travers la galaxie. Nappes de synthétiseurs cosmiques idéales pour le sommeil profond ou la méditation transcendantale."
        ),
        Sound(
            id = "fallback_5",
            title = "Golden Hour Deep House",
            category = "House",
            cover_url = "https://images.unsplash.com/photo-1514525253161-7a46d19cd819?w=400&auto=format&fit=crop&q=80",
            username = "DJ_Kev",
            author_username = "DJ_Kev",
            audio_url = "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-5.mp3",
            plays_count = 167300,
            plays = 167300,
            likes_count = 1890,
            comments_count = 112,
            created_at = "2026-06-18T22:10:00Z",
            is_verified = true,
            author_is_verified = true,
            duration = 362f,
            description = "Un kick lourd, une basse groovy et des mélodies ensoleillées pour vous faire danser toute la nuit sous les rythmes entraînants de la deep house moderne."
        ),
        Sound(
            id = "fallback_6",
            title = "Cyber City Rainstorm",
            category = "Soundscape",
            cover_url = "https://images.unsplash.com/photo-1509198397868-475647b2a1e5?w=400&auto=format&fit=crop&q=80",
            username = "AetherSound",
            author_username = "AetherSound",
            audio_url = "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-6.mp3",
            plays_count = 12050,
            plays = 12050,
            likes_count = 210,
            comments_count = 15,
            created_at = "2026-06-23T01:30:00Z",
            is_verified = false,
            author_is_verified = false,
            duration = 458f,
            description = "Ambiance sonore urbaine mélangée à une tempête de pluie dense sous les néons étincelants d'une métropole futuriste. Un asile de paix cyberpunk."
        ),
        Sound(
            id = "fallback_7",
            title = "Sunday Jazz Piano Impro",
            category = "Jazz",
            cover_url = "https://images.unsplash.com/photo-1520523839897-bd0b52f945a0?w=400&auto=format&fit=crop&q=80",
            username = "SébastienKeys",
            author_username = "SébastienKeys",
            audio_url = "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-7.mp3",
            plays_count = 34500,
            plays = 34500,
            likes_count = 490,
            comments_count = 33,
            created_at = "2026-06-22T10:00:00Z",
            is_verified = false,
            author_is_verified = false,
            duration = 312f,
            description = "Improvisation magistrale au piano à queue, mêlant des accords jazz complexes et des mélodies mélancoliques pour un après-midi tout en douceur."
        ),
        Sound(
            id = "fallback_8",
            title = "Epic Cinematic Ascent",
            category = "Cinematic",
            cover_url = "https://images.unsplash.com/photo-1465847899084-d164df4dedc6?w=400&auto=format&fit=crop&q=80",
            username = "OrchestraMind",
            author_username = "OrchestraMind",
            audio_url = "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-8.mp3",
            plays_count = 278100,
            plays = 278100,
            likes_count = 5890,
            comments_count = 432,
            created_at = "2026-06-15T14:20:00Z",
            is_verified = true,
            author_is_verified = true,
            duration = 401f,
            description = "Une montée en puissance orchestrale dramatique associant cuivres puissants, cordes vibrantes et percussions tonitruantes pour vos moments les plus épiques."
        )
    )
}
