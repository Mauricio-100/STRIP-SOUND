package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
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
import androidx.compose.material.icons.filled.History
import androidx.compose.foundation.border
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.ui.draw.clip
import coil.compose.AsyncImage
import com.example.data.local.SearchHistoryManager

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onSoundClick: (Sound) -> Unit,
    onProfileClick: () -> Unit,
    onArtistClick: (String) -> Unit,
    onUploadClick: () -> Unit,
    onStoryClick: (List<com.example.domain.model.VideoResponse>, Int) -> Unit,
    onCreateStoryClick: () -> Unit,
    searchHistoryManager: SearchHistoryManager
) {
    val coroutineScope = rememberCoroutineScope()
    var sounds by remember { mutableStateOf<List<Sound>>(emptyList()) }
    var storyVideos by remember { mutableStateOf<List<com.example.domain.model.VideoResponse>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    
    var searchQuery by remember { mutableStateOf("") }
    var isSearchActive by remember { mutableStateOf(false) }
    val searchHistory by searchHistoryManager.history.collectAsState()
    
    var searchResults by remember { mutableStateOf<List<Sound>?>(null) }
    var searchCreatorsResults by remember { mutableStateOf<List<com.example.domain.model.UserResponse>?>(null) }
    var searchMetadata by remember { mutableStateOf<com.example.domain.model.SearchMetadata?>(null) }

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
        launch {
            try {
                sounds = NetworkModule.api.getRecommendedSounds(20)
            } catch (e: Exception) {
                e.printStackTrace()
                // Stub data in case of error
                sounds = listOf(
                    Sound("1", "Afro Beat Vibes", category = "Afro", plays_count = 1200, username = "DJ Oumar"),
                    Sound("2", "Gospel Sunday", category = "Gospel", plays_count = 850, username = "Choir"),
                    Sound("3", "Amapiano Groove", category = "Amapiano", plays_count = 3400, username = "DJ Flex")
                )
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

    Scaffold(
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
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(androidx.compose.ui.graphics.Brush.linearGradient(listOf(Color(0xFF06B6D4), Color(0xFF2563EB)))),
                            contentAlignment = Alignment.Center
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(16.dp)
                                    .clip(CircleShape)
                                    .background(Color.White),
                                contentAlignment = Alignment.Center
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .clip(RoundedCornerShape(2.dp))
                                        .background(Color.Black)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            "STRIP SOUND",
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleMedium,
                            letterSpacing = 1.sp
                        )
                    }
                },
                actions = {
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

                if (storyVideos.isNotEmpty() || true) {
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        item {
                            Box(
                                modifier = Modifier
                                    .width(100.dp)
                                    .height(140.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(MaterialTheme.colorScheme.surfaceVariant)
                                    .clickable { onCreateStoryClick() },
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(Icons.Default.Add, contentDescription = "Créer une story", modifier = Modifier.size(32.dp), tint = MaterialTheme.colorScheme.primary)
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text("Créer", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
                                }
                            }
                        }
                        
                        items(storyVideos.size) { index ->
                            val video = storyVideos[index]
                            Box(
                                modifier = Modifier
                                    .width(100.dp)
                                    .height(140.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(Color.DarkGray)
                                    .clickable { onStoryClick(storyVideos, index) }
                            ) {
                                // Story background image
                                AsyncImage(
                                    model = video.thumbnail_url,
                                    contentDescription = null,
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop
                                )
                                // Gradient overlay
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(
                                            androidx.compose.ui.graphics.Brush.verticalGradient(
                                                colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.8f)),
                                                startY = 50f
                                            )
                                        )
                                )
                                // Avatar and Username
                                Column(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(8.dp),
                                    verticalArrangement = Arrangement.SpaceBetween,
                                    horizontalAlignment = Alignment.Start
                                ) {
                                    // Circular avatar
                                    if (video.avatar_url != null) {
                                        AsyncImage(
                                            model = video.avatar_url,
                                            contentDescription = null,
                                            modifier = Modifier
                                                .size(32.dp)
                                                .clip(CircleShape)
                                                .border(2.dp, Color(0xFF06B6D4), CircleShape),
                                            contentScale = ContentScale.Crop
                                        )
                                    } else {
                                        Box(
                                            modifier = Modifier
                                                .size(32.dp)
                                                .clip(CircleShape)
                                                .border(2.dp, Color(0xFF06B6D4), CircleShape)
                                                .background(MaterialTheme.colorScheme.primary),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                video.username.take(1).uppercase(),
                                                style = MaterialTheme.typography.labelSmall,
                                                color = Color.White
                                            )
                                        }
                                    }
                                    // Username at the bottom
                                    Text(
                                        video.username,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Color.White,
                                        fontWeight = FontWeight.Medium,
                                        maxLines = 1
                                    )
                                }
                            }
                        }
                    }
                }

                val filteredSounds = searchResults ?: sounds

                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                ) {
                    item {
                        Text(
                            text = if (searchQuery.isNotEmpty() && !isSearchActive) "Résultats de recherche" else "Tendances Audio \uD83D\uDD25",
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

                    items(filteredSounds) { sound ->
                        SoundItem(sound = sound, onClick = { onSoundClick(sound) })
                    }
                }
            }
        }
    }
}

@Composable
fun SoundItem(sound: Sound, onClick: () -> Unit) {
    var isLiked by remember { mutableStateOf(false) }
    var likesCount by remember { mutableIntStateOf(sound.likes_count) }
    val context = LocalContext.current
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .clickable { onClick() },
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (sound.cover_url != null) {
                    AsyncImage(
                        model = sound.cover_url,
                        contentDescription = null,
                        modifier = Modifier
                            .size(64.dp)
                            .clip(RoundedCornerShape(8.dp)),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .size(64.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.PlayArrow,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                Spacer(modifier = Modifier.width(16.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = sound.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = sound.username ?: sound.author_username ?: "Unknown Artist",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        if (sound.is_verified || sound.author_is_verified || (sound.username == "tecnocamon20")) {
                            Spacer(modifier = Modifier.width(4.dp))
                            Icon(
                                imageVector = Icons.Default.Verified,
                                contentDescription = "Verified",
                                tint = Color(0xFF06B6D4), // Cyan 500
                                modifier = Modifier.size(14.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "• il y a 2h", // dummy timestamp to look like Facebook post
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.Gray
                        )
                    }
                    Text(
                        text = "${sound.plays_count ?: sound.plays} plays \u2022 ${sound.category}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                }
                
                IconButton(onClick = onClick) {
                    Icon(
                        Icons.Default.PlayArrow,
                        contentDescription = "Play",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
            
            // Facebook-style Action Row
            androidx.compose.material3.HorizontalDivider(color = Color.DarkGray, modifier = Modifier.padding(horizontal = 12.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceAround,
                verticalAlignment = Alignment.CenterVertically
            ) {
                val coroutineScope = rememberCoroutineScope()
                val likeScale by animateFloatAsState(
                    targetValue = if (isLiked) 1.3f else 1.0f,
                    animationSpec = spring(dampingRatio = Spring.DampingRatioHighBouncy, stiffness = Spring.StiffnessMedium),
                    label = "likeScale"
                )
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.clickable { 
                    coroutineScope.launch {
                        try {
                            val response = NetworkModule.api.likeSound(sound.id)
                            isLiked = response.liked
                            // Query sound details to sync current actual count
                            val details = NetworkModule.api.getSoundDetails(sound.id)
                            likesCount = details.sound.likes_count
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }
                }.padding(8.dp)) {
                    Icon(
                        if (isLiked) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                        contentDescription = "Like",
                        tint = if (isLiked) Color.Red else Color.Gray,
                        modifier = Modifier
                            .size(18.dp)
                            .graphicsLayer(scaleX = likeScale, scaleY = likeScale)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("$likesCount", color = Color.Gray, style = MaterialTheme.typography.labelMedium)
                }
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.clickable { onClick() }.padding(8.dp)) {
                    Icon(Icons.Default.Comment, contentDescription = "Comment", tint = Color.Gray, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("${sound.plays_count / 100}", color = Color.Gray, style = MaterialTheme.typography.labelMedium)
                }
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.clickable {
                    val shareIntent = Intent(Intent.ACTION_SEND).apply {
                        type = "text/plain"
                        putExtra(Intent.EXTRA_TEXT, "Écoute ${sound.title} par ${sound.username} sur StripSound!")
                    }
                    context.startActivity(Intent.createChooser(shareIntent, "Partager via"))
                }.padding(8.dp)) {
                    Icon(Icons.Default.Share, contentDescription = "Share", tint = Color.Gray, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Partager", color = Color.Gray, style = MaterialTheme.typography.labelMedium)
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
