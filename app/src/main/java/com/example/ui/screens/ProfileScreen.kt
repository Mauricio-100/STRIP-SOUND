package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.List
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
import com.example.data.local.AuthManager
import com.example.data.local.AudioDownloader
import com.example.data.local.AppDatabase
import com.example.data.local.PlaylistEntity
import com.example.data.remote.NetworkModule
import com.example.domain.model.UserResponse
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.first

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.ui.platform.LocalContext

import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Share

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    userId: String? = null,
    authManager: AuthManager,
    appDatabase: AppDatabase,
    onBack: () -> Unit,
    onLogout: () -> Unit,
    onAnalyticsClick: () -> Unit,
    onSoundClick: (com.example.domain.model.Sound) -> Unit = {}
) {
    val context = LocalContext.current
    var userProfile by remember { mutableStateOf<UserResponse?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var downloadedCount by remember { mutableStateOf(0) }
    val coroutineScope = rememberCoroutineScope()

    var selectedTab by remember { mutableStateOf(0) }
    var wingsList by remember { mutableStateOf<List<com.example.domain.model.Wing>>(emptyList()) }
    var userSounds by remember { mutableStateOf<List<com.example.domain.model.Sound>>(emptyList()) }
    var isWingsLoading by remember { mutableStateOf(false) }

    val playlists by appDatabase.playlistDao().getAllPlaylists().collectAsState(initial = emptyList())
    val expandedPlaylists = remember { mutableStateMapOf<String, Boolean>() }
    var showCreatePlaylistDialog by remember { mutableStateOf(false) }
    var newPlaylistName by remember { mutableStateOf("") }

    LaunchedEffect(userId) {
        try {
            if (userId != null) {
                userProfile = NetworkModule.api.getFullUserProfile(userId)
            } else {
                userProfile = NetworkModule.api.getMyProfile()
            }
            val targetId = userId ?: userProfile?.id
            if (targetId != null) {
                userSounds = NetworkModule.api.getUserSounds(targetId)
            }
            val dlSounds = appDatabase.soundDao().getAllDownloadedSounds().first()
            downloadedCount = dlSounds.size
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            isLoading = false
        }
    }

    LaunchedEffect(userId, userProfile) {
        val targetId = userId ?: userProfile?.id
        if (targetId != null) {
            isWingsLoading = true
            try {
                val response = NetworkModule.api.getUserWings(targetId)
                wingsList = response.wings
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                isWingsLoading = false
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Profile & Settings") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = {
                        authManager.clear()
                        onLogout()
                    }) {
                        Icon(Icons.Default.ExitToApp, contentDescription = "Logout", tint = MaterialTheme.colorScheme.error)
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
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item {
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (userProfile?.avatar_url != null) {
                            AsyncImage(
                                model = userProfile?.avatar_url,
                                contentDescription = null,
                                modifier = Modifier
                                    .size(80.dp)
                                    .clip(CircleShape),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            Box(
                                modifier = Modifier
                                    .size(80.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primary),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = (userProfile?.username?.take(1) ?: "U").uppercase(),
                                    style = MaterialTheme.typography.headlineMedium,
                                    color = Color.Black
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        Column {
                            Text(
                                text = userProfile?.username ?: "Guest",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            if (userProfile?.is_verified == true) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.Verified,
                                        contentDescription = "Verified",
                                        tint = Color(0xFF06B6D4),
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Verified Artist", color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.bodySmall)
                                }
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(modifier = Modifier.fillMaxWidth()) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(end = 16.dp)) {
                                    Text("${userProfile?.followers_count ?: 0}", color = Color.White, fontWeight = FontWeight.Bold)
                                    Text("Followers", color = Color.Gray, style = MaterialTheme.typography.bodySmall)
                                }
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text("${userProfile?.following_count ?: 0}", color = Color.White, fontWeight = FontWeight.Bold)
                                    Text("Following", color = Color.Gray, style = MaterialTheme.typography.bodySmall)
                                }
                            }
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceAround
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("${userProfile?.total_wings ?: 0}", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                                Text("Wings", color = Color.Gray, style = MaterialTheme.typography.labelSmall)
                            }
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("${userProfile?.total_sounds ?: 0}", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                                Text("Sounds", color = Color.Gray, style = MaterialTheme.typography.labelSmall)
                            }
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("${userProfile?.total_audio_plays ?: 0}", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                                Text("Plays", color = Color.Gray, style = MaterialTheme.typography.labelSmall)
                            }
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("${userProfile?.total_audio_likes ?: 0}", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                                Text("Likes", color = Color.Gray, style = MaterialTheme.typography.labelSmall)
                            }
                        }
                    }
                }

            item {
                Divider(color = Color.Gray.copy(alpha=0.3f))
            }

            item {
                ScrollableTabRow(
                    selectedTabIndex = selectedTab,
                    containerColor = Color.Transparent,
                    edgePadding = 0.dp
                ) {
                    Tab(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        text = { Text("Wings (${wingsList.size})", fontWeight = FontWeight.Bold) }
                    )
                    Tab(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        text = { Text("Sons (${userSounds.size})", fontWeight = FontWeight.Bold) }
                    )
                    Tab(
                        selected = selectedTab == 2,
                        onClick = { selectedTab = 2 },
                        text = { Text("Playlists", fontWeight = FontWeight.Bold) }
                    )
                    Tab(
                        selected = selectedTab == 3,
                        onClick = { selectedTab = 3 },
                        text = { Text("Téléchargements ($downloadedCount)", fontWeight = FontWeight.Bold) }
                    )
                    Tab(
                        selected = selectedTab == 4,
                        onClick = { selectedTab = 4 },
                        text = { Text("Settings", fontWeight = FontWeight.Bold) }
                    )
                }
            }

            if (selectedTab == 0) {
                if (isWingsLoading) {
                    item {
                        Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator()
                        }
                    }
                } else if (wingsList.isEmpty()) {
                    item {
                        Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                            Text("Aucune publication vidéo (Wings)", color = Color.Gray)
                        }
                    }
                } else {
                    items(wingsList.size) { index ->
                        val wing = wingsList[index]
                        WingItem(wing)
                    }
                }
            } else if (selectedTab == 1) {
                if (userSounds.isEmpty()) {
                    item {
                        Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                            Text("Aucun son", color = Color.Gray)
                        }
                    }
                } else {
                    items(userSounds.size) { index ->
                        val sound = userSounds[index]
                        Card(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp).clickable { onSoundClick(sound) },
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                        ) {
                            Row(modifier = Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                AsyncImage(
                                    model = sound.cover_url ?: "https://picsum.photos/200",
                                    contentDescription = null,
                                    modifier = Modifier.size(50.dp).clip(RoundedCornerShape(8.dp)),
                                    contentScale = ContentScale.Crop
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(sound.title ?: "Untitled", color = Color.White, fontWeight = FontWeight.SemiBold)
                                    Text(sound.category ?: "Music", color = Color.Gray, style = MaterialTheme.typography.bodySmall)
                                }
                                Text("${sound.plays_count} plays", color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelSmall)
                            }
                        }
                    }
                }
            } else if (selectedTab == 2) {
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Mes Playlists", style = MaterialTheme.typography.titleMedium, color = Color.White, fontWeight = FontWeight.Bold)
                        IconButton(onClick = { showCreatePlaylistDialog = true }) {
                            Icon(Icons.Default.Add, contentDescription = "Créer une playlist", tint = MaterialTheme.colorScheme.primary)
                        }
                    }
                }
                
                if (showCreatePlaylistDialog) {
                    item {
                        AlertDialog(
                            onDismissRequest = { showCreatePlaylistDialog = false },
                            title = { Text("Nouvelle Playlist", color = Color.White) },
                            text = {
                                OutlinedTextField(
                                    value = newPlaylistName,
                                    onValueChange = { newPlaylistName = it },
                                    label = { Text("Nom de la playlist") },
                                    singleLine = true,
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedTextColor = Color.White,
                                        unfocusedTextColor = Color.White,
                                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                                        unfocusedBorderColor = Color.Gray
                                    )
                                )
                            },
                            confirmButton = {
                                TextButton(
                                    onClick = {
                                        if (newPlaylistName.isNotBlank()) {
                                            coroutineScope.launch {
                                                appDatabase.playlistDao().createPlaylist(
                                                    com.example.data.local.PlaylistEntity(
                                                        id = java.util.UUID.randomUUID().toString(),
                                                        name = newPlaylistName
                                                    )
                                                )
                                                newPlaylistName = ""
                                                showCreatePlaylistDialog = false
                                            }
                                        }
                                    }
                                ) {
                                    Text("Créer", color = MaterialTheme.colorScheme.primary)
                                }
                            },
                            dismissButton = {
                                TextButton(onClick = { showCreatePlaylistDialog = false }) {
                                    Text("Annuler", color = Color.Gray)
                                }
                            },
                            containerColor = Color(0xFF1E1E24)
                        )
                    }
                }
                
                if (playlists.isEmpty()) {
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha=0.3f))
                        ) {
                            Column(
                                modifier = Modifier.fillMaxWidth().padding(24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(Icons.Default.List, contentDescription = null, modifier = Modifier.size(48.dp), tint = Color.Gray)
                                Spacer(modifier = Modifier.height(12.dp))
                                Text("Aucune playlist disponible", color = Color.White, fontWeight = FontWeight.SemiBold)
                                Spacer(modifier = Modifier.height(4.dp))
                                Text("Créez votre première playlist pour organiser vos pistes sonore préférées de Strip Sound.", color = Color.Gray, style = MaterialTheme.typography.bodySmall, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                            }
                        }
                    }
                } else {
                    items(playlists.size) { index ->
                        val playlist = playlists[index]
                        val isExpanded = expandedPlaylists[playlist.id] ?: false
                        Card(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                        ) {
                            Column(modifier = Modifier.fillMaxWidth().padding(12.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth().clickable {
                                        expandedPlaylists[playlist.id] = !isExpanded
                                    },
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.List, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Text(playlist.name, color = Color.White, fontWeight = FontWeight.SemiBold)
                                    }
                                    IconButton(
                                        onClick = {
                                            coroutineScope.launch {
                                                appDatabase.playlistDao().deletePlaylist(playlist.id)
                                            }
                                        }
                                    ) {
                                        Icon(Icons.Default.Delete, contentDescription = "Supprimer la playlist", tint = Color.Gray.copy(alpha=0.7f), modifier = Modifier.size(20.dp))
                                    }
                                }
                                
                                if (isExpanded) {
                                    Spacer(modifier = Modifier.height(12.dp))
                                    Divider(color = Color.Gray.copy(alpha=0.2f))
                                    Spacer(modifier = Modifier.height(8.dp))
                                    PlaylistTracksList(
                                        playlistId = playlist.id,
                                        appDatabase = appDatabase,
                                        onSoundClick = onSoundClick
                                    )
                                }
                            }
                        }
                    }
                }
            } else if (selectedTab == 3) {
                item {
                    DownloadedSoundsList(appDatabase, onSoundClick)
                }
            } else {
                item {
                    Text("App Settings", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Surface(
                        color = MaterialTheme.colorScheme.surface,
                        shape = MaterialTheme.shapes.medium,
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        onClick = onAnalyticsClick
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.BarChart, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.width(16.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Dashboard Analytique", color = Color.White, fontWeight = FontWeight.SemiBold)
                                Text("Statistiques Créateur", color = Color.Gray, style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }

                    Surface(
                        color = MaterialTheme.colorScheme.surface,
                        shape = MaterialTheme.shapes.medium,
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        onClick = {
                            val username = userProfile?.username ?: "guest"
                            val link = "https://stripsound.com/creator/$username"
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            val clip = ClipData.newPlainText("Portfolio Link", link)
                            clipboard.setPrimaryClip(clip)
                            Toast.makeText(context, "Lien du portfolio copié !", Toast.LENGTH_SHORT).show()
                        }
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Share, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.width(16.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Partager mon Portfolio", color = Color.White, fontWeight = FontWeight.SemiBold)
                                Text("Copier le lien public de mes sons", color = Color.Gray, style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }

                    SettingItem("High Quality Audio (24-bit/192kHz)", "Stream in Hi-Res format when available", true) {}
                    SettingItem("Background Playback", "Keep playing audio when app is minimized", true) {}
                    SettingItem("Smart Cache Offline", "Manage downloaded offline tracks ($downloadedCount items)", false) {}
                }
            }
        }
    }
}
}

@Composable
fun DownloadedSoundsList(appDatabase: AppDatabase, onSoundClick: (com.example.domain.model.Sound) -> Unit) {
    val downloadedSounds by appDatabase.soundDao().getAllDownloadedSounds().collectAsState(initial = emptyList())
    if (downloadedSounds.isEmpty()) {
        Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
            Text("Aucun son téléchargé", color = Color.Gray)
        }
    } else {
        Column {
            downloadedSounds.forEach { entity ->
                Card(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp).clickable {
                        onSoundClick(com.example.domain.model.Sound(
                            id = entity.id,
                            title = entity.title,
                            audio_url = entity.localFilePath,
                            cover_url = null,
                            category = "Offline",
                            plays = 0,
                            plays_count = 0,
                            likes_count = 0,
                            author_id = "",
                            username = "Offline",
                            author_username = "Offline",
                            author_is_verified = false
                        ))
                    },
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Row(modifier = Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.List, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(entity.title, color = Color.White, fontWeight = FontWeight.SemiBold)
                            Text("Sauvegardé hors-ligne", color = Color.Gray, style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun WingItem(wing: com.example.domain.model.Wing) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AsyncImage(
                model = wing.thumbnail_url,
                contentDescription = wing.description,
                modifier = Modifier
                    .size(64.dp)
                    .clip(androidx.compose.foundation.shape.RoundedCornerShape(8.dp)),
                contentScale = ContentScale.Crop
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = wing.description.ifBlank { "Publication Wing" },
                    color = Color.White,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    style = MaterialTheme.typography.bodyMedium
                )
                if (!wing.sound_title.isNullOrBlank()) {
                    Text(
                        text = "🎵 ${wing.sound_title}",
                        color = MaterialTheme.colorScheme.primary,
                        style = MaterialTheme.typography.labelSmall
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    Text("👁 ${wing.views_count} views", color = Color.Gray, style = MaterialTheme.typography.labelSmall)
                    Text("♥ ${wing.likes_count} likes", color = Color.Gray, style = MaterialTheme.typography.labelSmall)
                }
            }
        }
    }
}

@Composable
fun SettingItem(title: String, subtitle: String, isSwitch: Boolean, onClick: () -> Unit) {
    var checked by remember { mutableStateOf(true) }
    Surface(
        color = MaterialTheme.colorScheme.surface,
        shape = MaterialTheme.shapes.medium,
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        onClick = onClick
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(title, color = Color.White, fontWeight = FontWeight.SemiBold)
                Text(subtitle, color = Color.Gray, style = MaterialTheme.typography.bodySmall)
            }
            if (isSwitch) {
                Switch(
                    checked = checked,
                    onCheckedChange = { checked = it },
                    colors = SwitchDefaults.colors(checkedThumbColor = MaterialTheme.colorScheme.primary)
                )
            }
        }
    }
}

@Composable
fun PlaylistTracksList(
    playlistId: String,
    appDatabase: AppDatabase,
    onSoundClick: (com.example.domain.model.Sound) -> Unit
) {
    val tracks by appDatabase.playlistDao().getTracksForPlaylist(playlistId).collectAsState(initial = emptyList())
    val coroutineScope = rememberCoroutineScope()

    if (tracks.isEmpty()) {
        Text(
            text = "Cette playlist est vide",
            color = Color.Gray,
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.padding(16.dp)
        )
    } else {
        Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp)) {
            tracks.forEach { track ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                        .clickable {
                            val sound = com.example.domain.model.Sound(
                                id = track.soundId,
                                title = track.title,
                                category = track.category,
                                cover_url = track.coverUrl,
                                username = track.username ?: "Unknown",
                                plays_count = track.playsCount,
                                audio_url = track.audioUrl ?: ""
                            )
                            onSoundClick(sound)
                        },
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    AsyncImage(
                        model = track.coverUrl ?: "https://picsum.photos/100",
                        contentDescription = "Cover",
                        modifier = Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(4.dp)),
                        contentScale = ContentScale.Crop
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(track.title, color = Color.White, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                        Text(track.username ?: "Unknown", color = Color.Gray, style = MaterialTheme.typography.labelSmall)
                    }
                    IconButton(
                        onClick = {
                            coroutineScope.launch {
                                appDatabase.playlistDao().removeTrackFromPlaylist(playlistId, track.soundId)
                            }
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Retirer",
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun AddToPlaylistDialog(
    sound: com.example.domain.model.Sound,
    appDatabase: com.example.data.local.AppDatabase,
    onDismiss: () -> Unit
) {
    val playlists by appDatabase.playlistDao().getAllPlaylists().collectAsState(initial = emptyList())
    var newPlaylistName by remember { mutableStateOf("") }
    var showCreateForm by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = "Ajouter à la playlist", color = Color.White, style = MaterialTheme.typography.titleMedium) },
        text = {
            Column(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
                if (!showCreateForm) {
                    androidx.compose.material3.Button(
                        onClick = { showCreateForm = true },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                    ) {
                        Text("+ Créer une playlist", color = Color.Black)
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    if (playlists.isEmpty()) {
                        Text("Aucune playlist disponible.", color = Color.Gray, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(vertical = 8.dp))
                    } else {
                        LazyColumn(modifier = Modifier.heightIn(max = 200.dp)) {
                            items(playlists.size) { index ->
                                val playlist = playlists[index]
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            coroutineScope.launch {
                                                val entity = com.example.data.local.PlaylistTrackEntity(
                                                    playlistId = playlist.id,
                                                    soundId = sound.id,
                                                    title = sound.title ?: "Untitled",
                                                    category = sound.category ?: "Music",
                                                    coverUrl = sound.cover_url,
                                                    username = sound.username ?: sound.author_username ?: "Unknown",
                                                    playsCount = sound.plays_count,
                                                    audioUrl = sound.audio_url
                                                )
                                                appDatabase.playlistDao().addTrackToPlaylist(entity)
                                                Toast.makeText(context, "Ajouté à la playlist ${playlist.name}!", Toast.LENGTH_SHORT).show()
                                                onDismiss()
                                            }
                                        }
                                        .padding(vertical = 12.dp, horizontal = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.List, contentDescription = null, tint = Color.Gray)
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Text(playlist.name, color = Color.White, style = MaterialTheme.typography.bodyMedium)
                                }
                                Divider(color = Color.Gray.copy(alpha = 0.2f))
                            }
                        }
                    }
                } else {
                    OutlinedTextField(
                        value = newPlaylistName,
                        onValueChange = { newPlaylistName = it },
                        label = { Text("Nom de la playlist") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = Color.Gray
                        )
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                        TextButton(onClick = { showCreateForm = false }) {
                            Text("Annuler")
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        androidx.compose.material3.Button(
                            onClick = {
                                if (newPlaylistName.isNotBlank()) {
                                    coroutineScope.launch {
                                        val newId = java.util.UUID.randomUUID().toString()
                                        appDatabase.playlistDao().createPlaylist(
                                            com.example.data.local.PlaylistEntity(id = newId, name = newPlaylistName)
                                        )
                                        newPlaylistName = ""
                                        showCreateForm = false
                                    }
                                }
                            }
                        ) {
                            Text("Créer", color = Color.Black)
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Fermer", color = Color.White)
            }
        },
        containerColor = Color(0xFF1E1E24)
    )
}
