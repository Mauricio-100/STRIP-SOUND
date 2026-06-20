package com.example.ui.screens

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Shadow
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.VolumeUp
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
import androidx.compose.material.icons.filled.Star

fun calculateZodiacFromDob(dob: String): String {
    if (dob.isBlank()) return "Entrez votre date de naissance YY/MM/JJ"
    val parts = dob.trim().split(Regex("[/\\-. ]+"))
    if (parts.size < 3) return "Incomplet (Ex: 01/05/25)"
    val p1 = parts[0].toIntOrNull() ?: return "Format invalide (Ex: YY/MM/JJ)"
    val p2 = parts[1].toIntOrNull() ?: return "Format invalide"
    val p3 = parts[2].toIntOrNull() ?: return "Format invalide"
    var month = p2
    var day = p3
    if (p1 > 31) {
        month = p2
        day = p3
    } else if (p3 > 31) {
        month = p2
        day = p1
    } else {
        month = p2
        day = p3
    }
    if (month !in 1..12) return "Mois invalide"
    if (day !in 1..31) return "Jour invalide"
    return when (month) {
        1 -> if (day < 20) "Capricorne ♑" else "Verseau ♒"
        2 -> if (day < 19) "Verseau ♒" else "Poissons ♓"
        3 -> if (day < 21) "Poissons ♓" else "Bélier ♈"
        4 -> if (day < 20) "Bélier ♈" else "Taureau ♉"
        5 -> if (day < 21) "Taureau ♉" else "Gémeaux ♊"
        6 -> if (day < 21) "Gémeaux ♊" else "Cancer ♋"
        7 -> if (day < 23) "Cancer ♋" else "Lion ♌"
        8 -> if (day < 23) "Lion ♌" else "Vierge ♍"
        9 -> if (day < 23) "Vierge ♍" else "Balance ♎"
        10 -> if (day < 23) "Balance ♎" else "Scorpion ♏"
        11 -> if (day < 22) "Scorpion ♏" else "Sagittaire ♐"
        12 -> if (day < 22) "Sagittaire ♐" else "Capricorne ♑"
        else -> "Inconnu 🌌"
    }
}

@Composable
fun ZodiacBadge(sign: String, modifier: Modifier = Modifier) {
    val (symbolLetter, color1, color2) = when (sign.lowercase()) {
        "bélier" -> Triple("AR", Color(0xFFFF512F), Color(0xFFDD2476))
        "taureau" -> Triple("TA", Color(0xFF56AB2F), Color(0xFFA8E063))
        "gémeaux" -> Triple("GE", Color(0xFFFFD89B), Color(0xFF19547B))
        "cancer" -> Triple("CA", Color(0xFFE0EAFC), Color(0xFFCFDEF3))
        "lion" -> Triple("LE", Color(0xFFFDC830), Color(0xFFF37335))
        "vierge" -> Triple("VI", Color(0xFF4CA1AF), Color(0xFFC4E0E5))
        "balance" -> Triple("LI", Color(0xFF8A2387), Color(0xFFF27121))
        "scorpion" -> Triple("SC", Color(0xFF141E30), Color(0xFF243B55))
        "sagittaire" -> Triple("SA", Color(0xFF8E2DE2), Color(0xFF4A00E0))
        "capricorne" -> Triple("CP", Color(0xFF3E5151), Color(0xFFDECBA4))
        "verseau" -> Triple("AQ", Color(0xFF1D976C), Color(0xFF93F9B9))
        "poissons" -> Triple("PI", Color(0xFF2BC0E4), Color(0xFFEAECC6))
        else -> Triple("??", Color.Gray, Color.White)
    }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(Brush.linearGradient(listOf(color1.copy(alpha = 0.2f), color2.copy(alpha = 0.2f))))
            .border(1.dp, Brush.linearGradient(listOf(color1, color2)), RoundedCornerShape(12.dp))
            .padding(horizontal = 16.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .background(Brush.linearGradient(listOf(color1, color2)), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = symbolLetter,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Black,
                    color = Color.White
                )
            }
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = sign.uppercase(),
                fontSize = 13.sp,
                fontWeight = FontWeight.ExtraBold,
                color = Color.White,
                letterSpacing = 2.sp
            )
        }
    }
}

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

    var phoneState by remember { mutableStateOf("") }
    var emailState by remember { mutableStateOf("") }
    var dobState by remember { mutableStateOf("") }
    var isFollowing by remember { mutableStateOf(false) }

    var selectedTab by remember { mutableStateOf(0) }
    var wingsList by remember { mutableStateOf<List<com.example.domain.model.Wing>>(emptyList()) }
    var userSounds by remember { mutableStateOf<List<com.example.domain.model.Sound>>(emptyList()) }
    var isWingsLoading by remember { mutableStateOf(false) }

    val playlists by appDatabase.playlistDao().getAllPlaylists().collectAsState(initial = emptyList())
    val expandedPlaylists = remember { mutableStateMapOf<String, Boolean>() }
    var showCreatePlaylistDialog by remember { mutableStateOf(false) }
    var newPlaylistName by remember { mutableStateOf("") }

    // Audio Player State
    val exoPlayer = remember { 
        val attr = androidx.media3.common.AudioAttributes.Builder()
            .setUsage(androidx.media3.common.C.USAGE_MEDIA)
            .setContentType(androidx.media3.common.C.AUDIO_CONTENT_TYPE_MUSIC)
            .build()
        androidx.media3.exoplayer.ExoPlayer.Builder(context)
            .setAudioAttributes(attr, false)
            .build() 
    }
    var currentlyPlayingId by remember { mutableStateOf<String?>(null) }
    var isPlaying by remember { mutableStateOf(false) }
    var currentPosition by remember { mutableLongStateOf(0L) }
    var trackDuration by remember { mutableLongStateOf(0L) }

    DisposableEffect(Unit) {
        val listener = object : androidx.media3.common.Player.Listener {
            override fun onIsPlayingChanged(isPlayingChange: Boolean) {
                isPlaying = isPlayingChange
            }
            override fun onPlaybackStateChanged(playbackState: Int) {
                if (playbackState == androidx.media3.common.Player.STATE_READY) {
                    trackDuration = exoPlayer.duration.coerceAtLeast(0L)
                } else if (playbackState == androidx.media3.common.Player.STATE_ENDED) {
                    isPlaying = false
                    currentPosition = 0L
                    exoPlayer.seekTo(0L)
                    exoPlayer.pause()
                }
            }
        }
        exoPlayer.addListener(listener)
        onDispose {
            exoPlayer.removeListener(listener)
            exoPlayer.release()
        }
    }

    LaunchedEffect(isPlaying) {
        while(isPlaying) {
            currentPosition = exoPlayer.currentPosition
            kotlinx.coroutines.delay(200)
        }
    }

    LaunchedEffect(userId) {
        try {
            if (userId != null) {
                userProfile = NetworkModule.api.getFullUserProfile(userId)
            } else {
                userProfile = NetworkModule.api.getMyProfile()
            }
            userProfile?.let {
                isFollowing = it.is_following
                phoneState = authManager.getPhone(it.id)
                emailState = authManager.getEmail(it.id)
                dobState = authManager.getDob(it.id)
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

                            if (userId != null && userId != authManager.getUserId()) {
                                Spacer(modifier = Modifier.height(10.dp))
                                Button(
                                    onClick = {
                                        coroutineScope.launch {
                                            try {
                                                if (isFollowing) {
                                                    NetworkModule.api.unfollowUser(userId)
                                                    isFollowing = false
                                                    userProfile = userProfile?.copy(followers_count = (userProfile?.followers_count ?: 1) - 1)
                                                } else {
                                                    NetworkModule.api.followUser(userId)
                                                    com.example.util.CustomNotificationManager(context).notifyNewSubscription(userProfile?.username ?: "un créateur")
                                                    isFollowing = true
                                                    userProfile = userProfile?.copy(followers_count = (userProfile?.followers_count ?: 0) + 1)
                                                }
                                            } catch (e: Exception) {
                                                e.printStackTrace()
                                            }
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = if (isFollowing) Color(0xFF1F2937) else MaterialTheme.colorScheme.primary,
                                        contentColor = if (isFollowing) Color.White else Color.Black
                                    ),
                                    shape = RoundedCornerShape(20.dp),
                                    modifier = Modifier.height(34.dp)
                                ) {
                                    Text(
                                        text = if (isFollowing) "Abonné ✓" else "S'abonner",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp
                                    )
                                }
                            }
                        }
                    }
                    
                    val currentProfileId = userProfile?.id
                    val currentPhone = remember(currentProfileId, phoneState) { currentProfileId?.let { authManager.getPhone(it) } ?: "" }
                    val currentEmail = remember(currentProfileId, emailState) { currentProfileId?.let { authManager.getEmail(it) } ?: "" }
                    val currentDob = remember(currentProfileId, dobState) { currentProfileId?.let { authManager.getDob(it) } ?: "" }
                    val currentZodiac = remember(currentDob) { if (currentDob.isNotBlank()) calculateZodiacFromDob(currentDob) else "" }

                    if (currentDob.isNotBlank() || currentEmail.isNotBlank() || currentPhone.isNotBlank()) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF181528)),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF8B5CF6).copy(alpha = 0.4f)),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Star,
                                        contentDescription = "Zodiac",
                                        tint = Color(0xFFA78BFA),
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "Profil Astrologique & Contact",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                }
                                
                                if (currentDob.isNotBlank()) {
                                    Spacer(modifier = Modifier.height(10.dp))
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text("📅 Date de Naissance : ", color = Color.Gray, style = MaterialTheme.typography.bodyMedium)
                                        Text(currentDob, color = Color.White, fontWeight = FontWeight.Medium, style = MaterialTheme.typography.bodyMedium)
                                    }
                                    Spacer(modifier = Modifier.height(8.dp))
                                    ZodiacBadge(sign = currentZodiac, modifier = Modifier.fillMaxWidth())
                                }
                                
                                if (currentEmail.isNotBlank()) {
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text("📧 Email : ", color = Color.Gray, style = MaterialTheme.typography.bodyMedium)
                                        Text(currentEmail, color = Color.White, style = MaterialTheme.typography.bodyMedium)
                                    }
                                }
                                
                                if (currentPhone.isNotBlank()) {
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text("📞 Téléphone : ", color = Color.Gray, style = MaterialTheme.typography.bodyMedium)
                                        Text(currentPhone, color = Color.White, style = MaterialTheme.typography.bodyMedium)
                                    }
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
                        text = { Text("Wings", fontWeight = FontWeight.Bold) }
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
                            Text("Aucune publication (Wing)", color = Color.Gray)
                        }
                    }
                } else {
                    items(wingsList.size) { index ->
                        WingItem(wing = wingsList[index])
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
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    AsyncImage(
                                        model = sound.cover_url,
                                        contentDescription = "Cover",
                                        modifier = Modifier.size(48.dp).clip(RoundedCornerShape(8.dp)),
                                        contentScale = ContentScale.Crop
                                    )
                                    Spacer(modifier = Modifier.width(16.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(sound.title, fontWeight = FontWeight.Bold, color = Color.White)
                                        Text("Durée: ${String.format("%.1f", sound.duration ?: 0f)} s", color = Color.Gray, style = MaterialTheme.typography.bodySmall)
                                    }
                                    IconButton(
                                        onClick = {
                                            if (currentlyPlayingId == sound.id && isPlaying) {
                                                exoPlayer.pause()
                                            } else {
                                                if (currentlyPlayingId != sound.id) {
                                                    sound.audio_url?.let {
                                                        exoPlayer.setMediaItem(androidx.media3.common.MediaItem.fromUri(it))
                                                        exoPlayer.prepare()
                                                    }
                                                    currentlyPlayingId = sound.id
                                                }
                                                exoPlayer.play()
                                            }
                                        }
                                    ) {
                                        Icon(
                                            imageVector = if (currentlyPlayingId == sound.id && isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                            contentDescription = "Play/Pause",
                                            tint = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                    if (authManager.getUserId() == sound.user_id || authManager.getUserId() == sound.author_id) {
                                        IconButton(onClick = {
                                            coroutineScope.launch {
                                                try {
                                                    NetworkModule.api.deleteSound(sound.id)
                                                    userSounds = userSounds.filter { it.id != sound.id }
                                                    if (currentlyPlayingId == sound.id) {
                                                        exoPlayer.stop()
                                                        currentlyPlayingId = null
                                                    }
                                                } catch(e: Exception) {}
                                            }
                                        }) {
                                            Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color.Red)
                                        }
                                    }
                                }
                                
                                // Player controls exactly like an HTML5 audio player when active
                                if (currentlyPlayingId == sound.id) {
                                    Spacer(modifier = Modifier.height(12.dp))
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            "${String.format("%02d:%02d", (currentPosition / 1000) / 60, (currentPosition / 1000) % 60)}",
                                            color = Color.LightGray, 
                                            style = MaterialTheme.typography.labelSmall
                                        )
                                        Slider(
                                            value = if (trackDuration > 0) currentPosition.toFloat() / trackDuration else 0f,
                                            onValueChange = { frac ->
                                                val target = (frac * trackDuration).toLong()
                                                exoPlayer.seekTo(target)
                                                currentPosition = target
                                            },
                                            modifier = Modifier.weight(1f).padding(horizontal = 8.dp),
                                            colors = SliderDefaults.colors(
                                                thumbColor = MaterialTheme.colorScheme.primary,
                                                activeTrackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f)
                                            )
                                        )
                                        Text(
                                            "${String.format("%02d:%02d", (trackDuration / 1000) / 60, (trackDuration / 1000) % 60)}",
                                            color = Color.LightGray, 
                                            style = MaterialTheme.typography.labelSmall
                                        )
                                    }
                                    // Volume control
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        var vol by remember { mutableFloatStateOf(exoPlayer.volume) }
                                        Icon(
                                            imageVector = Icons.Default.VolumeUp,
                                            contentDescription = "Volume",
                                            tint = Color.Gray,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Slider(
                                            value = vol,
                                            onValueChange = { exoPlayer.volume = it; vol = it },
                                            modifier = Modifier.fillMaxWidth(0.4f).padding(horizontal = 8.dp),
                                            colors = SliderDefaults.colors(
                                                thumbColor = Color.Gray,
                                                activeTrackColor = Color.LightGray
                                            )
                                        )
                                    }
                                }
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
            } else if (selectedTab == 4) {
                item {
                    val isMyProfile = (userId == null || userId == authManager.getUserId())
                    if (isMyProfile) {
                        Card(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF151821)),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF00FFCC).copy(alpha = 0.3f)),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(
                                    text = "Éditer mes coordonnées & Zodiaque",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF00FFCC)
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                
                                // Email Input
                                OutlinedTextField(
                                    value = emailState,
                                    onValueChange = { emailState = it },
                                    label = { Text("Adresse Email") },
                                    leadingIcon = { Text("📧") },
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedTextColor = Color.White,
                                        unfocusedTextColor = Color.White,
                                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                                        unfocusedBorderColor = Color.Gray
                                    )
                                )
                                
                                Spacer(modifier = Modifier.height(8.dp))
                                
                                // Phone Input
                                OutlinedTextField(
                                    value = phoneState,
                                    onValueChange = { phoneState = it },
                                    label = { Text("Numéro de Téléphone") },
                                    leadingIcon = { Text("📞") },
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedTextColor = Color.White,
                                        unfocusedTextColor = Color.White,
                                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                                        unfocusedBorderColor = Color.Gray
                                    )
                                )
                                
                                Spacer(modifier = Modifier.height(8.dp))
                                
                                // DOB Input
                                OutlinedTextField(
                                    value = dobState,
                                    onValueChange = { dobState = it },
                                    label = { Text("Date de Naissance (YY/MM/JJ)") },
                                    leadingIcon = { Text("📅") },
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedTextColor = Color.White,
                                        unfocusedTextColor = Color.White,
                                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                                        unfocusedBorderColor = Color.Gray
                                    )
                                )
                                
                                Spacer(modifier = Modifier.height(10.dp))
                                
                                // Zodiac Sign live detection
                                val liveZodiac = calculateZodiacFromDob(dobState)
                                val isError = liveZodiac.startsWith("Format") || liveZodiac.startsWith("Mois") || liveZodiac.startsWith("Jour") || liveZodiac.startsWith("Inconnu") || liveZodiac.startsWith("Entrez") || liveZodiac.startsWith("Incomplet")
                                if (isError) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(Color(0x0DFFFFFF))
                                            .padding(8.dp)
                                    ) {
                                        Text("🔮 Signe Détecté : ", color = Color.Gray, style = MaterialTheme.typography.bodyMedium)
                                        Text(
                                            text = liveZodiac,
                                            color = Color.LightGray.copy(alpha = 0.5f),
                                            fontWeight = FontWeight.Bold,
                                            style = MaterialTheme.typography.bodyMedium
                                        )
                                    }
                                } else {
                                    ZodiacBadge(sign = liveZodiac, modifier = Modifier.fillMaxWidth())
                                }
                                
                                Spacer(modifier = Modifier.height(12.dp))
                                
                                Button(
                                    onClick = {
                                        val myId = userProfile?.id
                                        if (myId != null) {
                                            authManager.savePersonalDetails(
                                                userId = myId,
                                                phone = phoneState,
                                                email = emailState,
                                                dob = dobState
                                            )
                                            // Synchronize to cloud if dobState has valid date Format YYYY-MM-DD
                                            // dobState might be like YYYY/MM/DD, so replace / with -
                                            val formattedDob = dobState.replace("/", "-").replace(".", "-")
                                            if (formattedDob.length >= 10) {
                                                coroutineScope.launch {
                                                    try {
                                                        val req = com.example.domain.model.VerificationCriteria(birth_date = formattedDob)
                                                        val result = NetworkModule.api.requestVerification(req)
                                                        if (result.verified) {
                                                            Toast.makeText(context, "Profil enregistré et vérifié! Signe: ${result.zodiac_sign}", Toast.LENGTH_SHORT).show()
                                                        } else {
                                                            Toast.makeText(context, "Profil enregistré ! ${result.reason ?: ""}", Toast.LENGTH_SHORT).show()
                                                        }
                                                    } catch (e: Exception) {
                                                        Toast.makeText(context, "Profil enregistré localement !", Toast.LENGTH_SHORT).show()
                                                    }
                                                }
                                            } else {
                                                Toast.makeText(context, "Profil enregistré ! Signe: $liveZodiac", Toast.LENGTH_SHORT).show()
                                            }
                                        } else {
                                            Toast.makeText(context, "Erreur : Profil introuvable", Toast.LENGTH_SHORT).show()
                                        }
                                    },
                                    shape = RoundedCornerShape(24.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                                    modifier = Modifier.align(Alignment.End)
                                ) {
                                    Text("Sauvegarder", color = Color.Black, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                    }

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
                                Text("Dashboard Projets & Analytique", color = Color.White, fontWeight = FontWeight.SemiBold)
                                Text("Gérez vos sons et vos statistiques", color = Color.Gray, style = MaterialTheme.typography.bodySmall)
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
