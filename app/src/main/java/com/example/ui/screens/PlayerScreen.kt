package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material.icons.filled.Report
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.Spring
import androidx.compose.ui.graphics.graphicsLayer
import com.example.domain.model.Sound
import com.example.player.AudioPlayerManager
import androidx.media3.common.Player

import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Comment
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.List
import androidx.compose.foundation.clickable
import android.content.Intent
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.gestures.detectTapGestures

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.VolumeUp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlayerScreen(
    sound: Sound,
    audioPlayerManager: AudioPlayerManager,
    audioDownloader: com.example.data.local.AudioDownloader,
    appDatabase: com.example.data.local.AppDatabase,
    authManager: com.example.data.local.AuthManager,
    onClose: () -> Unit,
    onNavigateToProfile: (String) -> Unit
) {
    val isPlaying by audioPlayerManager.isPlaying.collectAsState()
    val activeDeviceState by audioPlayerManager.deviceDetector.activeDevice.collectAsState()
    val webBluetoothStatus by audioPlayerManager.webBluetoothManager.webBluetoothStatus.collectAsState()
    var isDownloaded by remember { mutableStateOf(false) }
    var isDownloading by remember { mutableStateOf(false) }
    val currentPosition by audioPlayerManager.currentPosition.collectAsState()
    val duration by audioPlayerManager.duration.collectAsState()
    var isLiked by remember { mutableStateOf(authManager.isSoundLiked(sound.id)) }
    var likesCount by remember { mutableIntStateOf(sound.likes_count) }
    var sharesCount by remember { mutableIntStateOf(35 + (sound.plays_count / 150)) }
    var showComments by remember { mutableStateOf(false) }
    var showAddToPlaylistDialog by remember { mutableStateOf(false) }
    var commentsList by remember { mutableStateOf<List<com.example.domain.model.Comment>>(emptyList()) }
    var isCommentsLoading by remember { mutableStateOf(false) }
    var newCommentText by remember { mutableStateOf("") }
    var soundDetails by remember { mutableStateOf<com.example.domain.model.SoundDetailsResponse?>(null) }
    
    // Real-time counter stream activity update - directly connected to the server
    LaunchedEffect(sound) {
        while (true) {
            try {
                val details = com.example.data.remote.NetworkModule.api.getSoundDetails(sound.id)
                soundDetails = details
                likesCount = details.sound.likes_count
            } catch (e: Exception) {
                e.printStackTrace()
            }
            delay(5000) // Poll real-time values every 5 seconds
        }
    }
    
    // Real-time comments loader and updater from backend server
    LaunchedEffect(showComments) {
        if (showComments) {
            while (true) {
                try {
                    commentsList = com.example.data.remote.NetworkModule.api.getComments(sound.id)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
                delay(4000) // Poll real-time comments from server every 4 seconds
            }
        }
    }
    
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current
    
    LaunchedEffect(sound) {
        com.example.util.UniqueViewsTracker.trackSoundPlay(context, sound.id)
        isDownloaded = audioDownloader.isDownloaded(sound.id)
        val localUrl = if (isDownloaded) {
            val entity = appDatabase.soundDao().getDownloadedSound(sound.id)
            val file = java.io.File(entity?.localFilePath ?: "")
            if (file.exists()) file.toURI().toString() else null
        } else null
        
        // Use an actual fallback test file since getRecommendedSounds doesn't return audio_url unfortunately
        val url = localUrl ?: sound.audio_url ?: "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-1.mp3"
        
        val mediaMetadata = androidx.media3.common.MediaMetadata.Builder()
            .setTitle(sound.title)
            .setArtist(sound.username ?: sound.author_username ?: "Unknown Artist")
            .setArtworkUri(if (sound.cover_url != null) android.net.Uri.parse(sound.cover_url) else null)
            .build()
            
        audioPlayerManager.playTrack(url = url, sound = sound, itemMetadata = mediaMetadata)

        try {
            soundDetails = com.example.data.remote.NetworkModule.api.getSoundDetails(sound.id)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                androidx.compose.ui.graphics.Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF0F1B22),
                        Color(0xFF080D11),
                        Color(0xFF020305)
                    )
                )
            )
    ) {
        // Cyan-purple ambient background glow
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(350.dp)
                .background(
                    androidx.compose.ui.graphics.Brush.radialGradient(
                        colors = listOf(Color(0x3306B6D4), Color(0x0C7C3AED), Color.Transparent),
                        radius = 1100f
                    )
                )
        )

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp)
                .windowInsetsPadding(WindowInsets.statusBars)
        ) {
            item {
                Column(
                    modifier = Modifier
                        .fillParentMaxHeight()
                        .padding(vertical = 16.dp)
                ) {
                    // Header Bar
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = {
                                if (!isDownloaded && !isDownloading) {
                                    isDownloading = true
                                    coroutineScope.launch {
                                        audioDownloader.downloadSound(sound)
                                        isDownloaded = true
                                        isDownloading = false
                                    }
                                } else if (isDownloaded) {
                                    coroutineScope.launch {
                                        audioDownloader.deleteSound(sound.id)
                                        isDownloaded = false
                                    }
                                }
                            },
                            modifier = Modifier
                                .size(46.dp)
                                .background(Color(0x14FFFFFF), CircleShape)
                                .border(1.dp, Color(0x17FFFFFF), CircleShape)
                        ) {
                            if (isDownloading) {
                                CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp, color = Color(0xFF06B6D4))
                            } else {
                                Icon(
                                    imageVector = if (isDownloaded) Icons.Default.Check else Icons.Default.Add,
                                    contentDescription = "Download",
                                    tint = if (isDownloaded) Color(0xFF06B6D4) else Color.White.copy(alpha = 0.85f),
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                        
                        val context = LocalContext.current
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            IconButton(
                                onClick = {
                                    val notificationManager = com.example.util.CustomNotificationManager(context)
                                    notificationManager.showReportNotification(sound.username ?: sound.author_username ?: "Unknown")
                                },
                                modifier = Modifier
                                    .size(46.dp)
                                    .background(Color(0x14FFFFFF), CircleShape)
                                    .border(1.dp, Color(0x17FFFFFF), CircleShape)
                            ) {
                                Icon(Icons.Default.Report, contentDescription = "Signaler", tint = Color.White.copy(alpha = 0.7f), modifier = Modifier.size(20.dp))
                            }
                            IconButton(
                                onClick = onClose,
                                modifier = Modifier
                                    .size(46.dp)
                                    .background(Color(0x14FFFFFF), CircleShape)
                                    .border(1.dp, Color(0x17FFFFFF), CircleShape)
                            ) {
                                Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White, modifier = Modifier.size(20.dp))
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Premium Art Frame with shadow & backdrop glow
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(1f)
                            .padding(8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        // Blurred shadow under artwork
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(14.dp)
                                .clip(RoundedCornerShape(32.dp))
                                .background(
                                    androidx.compose.ui.graphics.Brush.linearGradient(
                                        colors = listOf(Color(0x4006B6D4), Color(0x1A7C3AED))
                                    )
                                )
                        )

                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(RoundedCornerShape(24.dp))
                                .border(1.5.dp, Color(0x33FFFFFF), RoundedCornerShape(24.dp))
                                .background(Color(0xFF101418))
                        ) {
                            if (sound.cover_url != null) {
                                AsyncImage(
                                    model = sound.cover_url,
                                    contentDescription = "Cover",
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop
                                )
                            } else {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(
                                            androidx.compose.ui.graphics.Brush.verticalGradient(
                                                listOf(Color(0xFF141D24), Color(0xFF0F151B))
                                            )
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        Icons.Default.PlayArrow,
                                        contentDescription = null,
                                        modifier = Modifier.size(90.dp),
                                        tint = Color(0xFF06B6D4).copy(alpha = 0.25f)
                                    )
                                }
                            }
                            
                            // Glossy Satin overlay on artwork
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(
                                        androidx.compose.ui.graphics.Brush.verticalGradient(
                                            listOf(Color(0x11FFFFFF), Color.Transparent, Color(0x11000000))
                                        )
                                    )
                            )

                            // Hi-Res Audio tag bottom left
                            Box(
                                modifier = Modifier
                                    .align(Alignment.BottomStart)
                                    .padding(16.dp)
                                    .background(Color(0xCC000000), RoundedCornerShape(12.dp))
                                    .border(1.dp, Color(0x1AFFFFFF), RoundedCornerShape(12.dp))
                                    .padding(horizontal = 10.dp, vertical = 6.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(6.dp)
                                            .clip(CircleShape)
                                            .background(Color(0xFF00FFCC))
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        "HI-RES STUDIO 24-BIT",
                                        color = Color(0xFF00FFCC),
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        letterSpacing = 1.sp
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // Title with modern Display style and Verified Indicator
                    Text(
                        text = sound.title,
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Black,
                            fontSize = 24.sp,
                            letterSpacing = (-0.5).sp
                        ),
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clickable {
                                val uid = sound.user_id ?: sound.author_id ?: ""
                                if (uid.isNotEmpty()) onNavigateToProfile(uid)
                            }
                            .padding(vertical = 4.dp)
                    ) {
                        Text(
                            text = "${sound.username ?: sound.author_username ?: "Unknown Artist"} • ${sound.category}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.LightGray.copy(alpha = 0.7f),
                            fontWeight = FontWeight.Medium
                        )
                        if (sound.is_verified || sound.author_is_verified) {
                            Spacer(modifier = Modifier.width(6.dp))
                            Icon(
                                imageVector = Icons.Default.Verified,
                                contentDescription = "Verified",
                                tint = Color(0xFF06B6D4),
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }

                    // Directly beneath the audio metadata display section, embed creator profile
                    val creatorId = sound.user_id ?: sound.author_id ?: ""
                    if (creatorId.isNotBlank()) {
                        UserProfile(userId = creatorId, onNavigateToProfile = onNavigateToProfile)
                    }

                    Spacer(modifier = Modifier.height(18.dp))

                    // Premium Cyber Stats grid
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        StatBox(label = "BPM", value = "128", modifier = Modifier.weight(1f))
                        StatBox(label = "KEY", value = "G#m", modifier = Modifier.weight(1f))
                        StatBox(label = "BASS", value = "High", modifier = Modifier.weight(1f))
                        StatBox(label = "FORMAT", value = "FLAC", modifier = Modifier.weight(1f))
                    }

                    // Satin Glassy Social Action Sheet
                    Spacer(modifier = Modifier.height(14.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0x0CFFFFFF), RoundedCornerShape(16.dp))
                            .border(1.dp, Color(0x0FFFFFFF), RoundedCornerShape(16.dp))
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val likeScale by animateFloatAsState(
                            targetValue = if (isLiked) 1.3f else 1.0f,
                            animationSpec = spring(dampingRatio = Spring.DampingRatioHighBouncy, stiffness = Spring.StiffnessMedium),
                            label = "likeScale"
                        )
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .clickable {
                                    coroutineScope.launch {
                                        try {
                                            val response = com.example.data.remote.NetworkModule.api.likeSound(sound.id)
                                            isLiked = response.liked
                                            authManager.setSoundLiked(sound.id, response.liked)
                                            // Sync likesCount immediately of the server info
                                            val details = com.example.data.remote.NetworkModule.api.getSoundDetails(sound.id)
                                            soundDetails = details
                                            likesCount = details.sound.likes_count
                                        } catch (e: Exception) {
                                            e.printStackTrace()
                                        }
                                    }
                                }
                                .padding(4.dp)
                        ) {
                            Icon(
                                imageVector = if (isLiked) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                                contentDescription = "Like",
                                tint = if (isLiked) Color(0xFFEF4444) else Color.White.copy(alpha = 0.6f),
                                modifier = Modifier
                                    .size(20.dp)
                                    .graphicsLayer(scaleX = likeScale, scaleY = likeScale)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("$likesCount", color = Color.White.copy(alpha = 0.85f), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .clickable {
                                    showComments = true
                                    isCommentsLoading = true
                                    coroutineScope.launch {
                                        try {
                                            commentsList = com.example.data.remote.NetworkModule.api.getComments(sound.id)
                                        } catch (e: Exception) {
                                            e.printStackTrace()
                                        } finally {
                                            isCommentsLoading = false
                                        }
                                    }
                                }
                                .padding(4.dp)
                        ) {
                            Icon(Icons.Default.Comment, contentDescription = "Comment", tint = Color.White.copy(alpha = 0.6f), modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("${sound.plays_count / 100}", color = Color.White.copy(alpha = 0.85f), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .clickable {
                                    sharesCount++
                                    val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                        type = "text/plain"
                                        putExtra(Intent.EXTRA_TEXT, "Écoute ${sound.title} par ${sound.username} sur StripSound!")
                                    }
                                    context.startActivity(Intent.createChooser(shareIntent, "Partager via"))
                                }
                                .padding(4.dp)
                        ) {
                            Icon(Icons.Default.Share, contentDescription = "Share", tint = Color.White.copy(alpha = 0.6f), modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("$sharesCount", color = Color.White.copy(alpha = 0.85f), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .clickable {
                                    showAddToPlaylistDialog = true
                                }
                                .padding(4.dp)
                        ) {
                            Icon(Icons.Default.List, contentDescription = "Playlist", tint = Color.White.copy(alpha = 0.6f), modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Playlist", color = Color.White.copy(alpha = 0.85f), fontSize = 12.sp)
                        }
                    }

                    Spacer(modifier = Modifier.weight(1f))
                    
                    // High-fidelity active waveform seek bar
                    WaveformVisualizer(
                        soundId = sound.id,
                        currentPosition = currentPosition,
                        duration = duration,
                        onSeek = { audioPlayerManager.seekTo(it) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp)
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    
                    val currSec = (currentPosition / 1000) % 60
                    val currMin = (currentPosition / 1000) / 60
                    val durSec = (duration / 1000) % 60
                    val durMin = (duration / 1000) / 60
                    val remainingSec = ((duration - currentPosition) / 1000).coerceAtLeast(0) % 60
                    val remainingMin = ((duration - currentPosition) / 1000).coerceAtLeast(0) / 60
                    
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 2.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(String.format("%02d:%02d", currMin, currSec), fontSize = 11.sp, color = Color.White.copy(alpha = 0.5f), fontWeight = FontWeight.Bold)
                        Text(String.format("-%02d:%02d", remainingMin, remainingSec), fontSize = 11.sp, color = Color.White.copy(alpha = 0.5f), fontWeight = FontWeight.Bold)
                    }
                    
                    Spacer(modifier = Modifier.weight(1f))

                    // Player circular interactive control deck
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = { /* Shuffle logic placeholder */ }) {
                            Icon(Icons.Default.Sync, contentDescription = "Shuffle", tint = Color.White.copy(alpha = 0.4f), modifier = Modifier.size(22.dp))
                        }
                        IconButton(onClick = { audioPlayerManager.seekTo(0) }) {
                            Icon(Icons.Default.SkipPrevious, contentDescription = "Previous/Restart", tint = Color.White, modifier = Modifier.size(34.dp))
                        }

                        // Radiant Outer neon play ring
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier.size(80.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .border(2.dp, Color(0x3B06B6D4), CircleShape)
                                    .background(Color(0x0D06B6D4), CircleShape)
                                    .padding(4.dp)
                            )
                            FloatingActionButton(
                                onClick = { audioPlayerManager.togglePlayPause() },
                                shape = CircleShape,
                                containerColor = Color(0xFF06B6D4),
                                elevation = FloatingActionButtonDefaults.elevation(0.dp, 0.dp),
                                modifier = Modifier.fillMaxSize()
                            ) {
                                Icon(
                                    imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                    contentDescription = if (isPlaying) "Pause" else "Play",
                                    tint = Color.Black,
                                    modifier = Modifier.size(36.dp)
                                )
                            }
                        }

                        IconButton(onClick = { /* Next audio queue placeholder */ }) {
                            Icon(Icons.Default.SkipNext, contentDescription = "Next", tint = Color.White, modifier = Modifier.size(34.dp))
                        }
                        IconButton(onClick = { /* Repeat queue placeholder */ }) {
                            Icon(Icons.Default.Sync, contentDescription = "Repeat", tint = Color.White.copy(alpha = 0.4f), modifier = Modifier.size(22.dp))
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // Volume Control
                    val volume by audioPlayerManager.volume.collectAsState()
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(androidx.compose.material.icons.Icons.Default.VolumeUp, contentDescription = "Volume", tint = Color.White.copy(alpha = 0.5f), modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(12.dp))
                        Slider(
                            value = volume,
                            onValueChange = { audioPlayerManager.setVolume(it) },
                            modifier = Modifier.weight(1f),
                            colors = SliderDefaults.colors(
                                thumbColor = Color.White,
                                activeTrackColor = Color(0xFF06B6D4),
                                inactiveTrackColor = Color.White.copy(alpha = 0.15f)
                            )
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Bluetooth/AirPods/JBL device output detector display
                    activeDeviceState?.let { activeDevice ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp, horizontal = 4.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .background(Color(0x1A06B6D4))
                                .border(1.dp, Color(0x3B06B6D4), RoundedCornerShape(14.dp))
                                .padding(horizontal = 14.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                modifier = Modifier.weight(1f),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                val icon = if (activeDevice.isBluetooth) {
                                    Icons.Default.Bluetooth
                                } else if (activeDevice.type == com.example.player.AudioDeviceDetector.DeviceType.WIRED_HEADPHONE ||
                                           activeDevice.type == com.example.player.AudioDeviceDetector.DeviceType.WIRED_HEADSET) {
                                    Icons.Default.Headphones
                                } else {
                                    Icons.Default.VolumeUp
                                }
                                
                                Icon(
                                    imageVector = icon,
                                    contentDescription = "Routing Output",
                                    tint = if (activeDevice.isBluetooth) Color(0xFF00FFCC) else Color.White,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = "Audio diffusé sur :",
                                        color = Color.White.copy(alpha = 0.5f),
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Normal
                                    )
                                    Text(
                                        text = activeDevice.name,
                                        color = Color.White,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                            
                            // High contrast status indicator pill
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(if (activeDevice.isBluetooth) Color(0x3300FFCC) else Color(0x22FFFFFF))
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = if (activeDevice.isBluetooth) "Bluetooth" else "Haut-parleur",
                                    color = if (activeDevice.isBluetooth) Color(0xFF00FFCC) else Color.White,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    // Web Bluetooth Connection Compatibility status
                    webBluetoothStatus?.let { status ->
                        Spacer(modifier = Modifier.height(12.dp))
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 4.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .background(Color(0x0CFFFFFF))
                                .border(1.dp, Color(0x16FFFFFF), RoundedCornerShape(14.dp))
                                .padding(12.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.Bluetooth,
                                        contentDescription = "Web Bluetooth",
                                        tint = if (status.isGattConnected) Color(0xFF00FFCC) else Color.Gray,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "Web Bluetooth API Standard",
                                        color = Color.White,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(if (status.isGattConnected) Color(0x2200FFCC) else Color(0x1AFFFFFF))
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = if (status.isGattConnected) "GATT CONNECTÉ" else "PRÊT",
                                        color = if (status.isGattConnected) Color(0xFF00FFCC) else Color.White.copy(alpha = 0.6f),
                                        fontSize = 8.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        text = "Périphérique : ${status.deviceName}",
                                        color = Color.LightGray,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Text(
                                        text = "UUID Service : ${status.serviceUuid}",
                                        color = Color.Gray,
                                        fontSize = 10.sp,
                                        fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                                    )
                                }
                                Column(horizontalAlignment = Alignment.End) {
                                    Text(
                                        text = "Audio: ${status.audioCodec}",
                                        color = Color.White,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "Latence : ~${status.estimatedLatencyMs}ms",
                                        color = if (status.estimatedLatencyMs <= 150) Color(0xFF00FFCC) else Color(0xFFEF4444),
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = "Compatible",
                                    tint = Color(0xFF00FFCC),
                                    modifier = Modifier.size(12.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "Compatibilité Flux Android : ${status.streamQuality}",
                                    color = Color.LightGray.copy(alpha = 0.8f),
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Normal
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Creator profile information непосредственно beneath the audio interface
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 4.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(Color(0x0CFFFFFF))
                            .border(1.dp, Color(0x12FFFFFF), RoundedCornerShape(16.dp))
                            .clickable {
                                val uid = sound.user_id ?: sound.author_id ?: ""
                                if (uid.isNotEmpty()) onNavigateToProfile(uid)
                            }
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        coil.compose.AsyncImage(
                            model = sound.avatar_url ?: "https://api.dicebear.com/7.x/avataaars/png?seed=${sound.username ?: "artist"}",
                            contentDescription = "Avatar",
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(Color(0x16FFFFFF), CircleShape)
                                .border(1.dp, Color(0x1AFFFFFF), CircleShape),
                            contentScale = androidx.compose.ui.layout.ContentScale.Crop
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = sound.username ?: sound.author_username ?: "Unknown Artist",
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp
                                )
                                if (sound.is_verified || sound.author_is_verified) {
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Icon(
                                        imageVector = Icons.Default.Verified,
                                        contentDescription = "Verified",
                                        tint = Color(0xFF06B6D4),
                                        modifier = Modifier.size(14.dp)
                                    )
                                }
                            }
                            Text(
                                text = "Créateur original • Tapez pour voir le profil",
                                color = Color.White.copy(alpha = 0.5f),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Normal
                            )
                        }
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = "Details",
                            tint = Color.White.copy(alpha = 0.4f),
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
            
            val resolvedAuthorId = soundDetails?.sound?.user_id 
                ?: soundDetails?.sound?.author_id 
                ?: sound.user_id 
                ?: sound.author_id 
                ?: ""
                
            val resolvedAuthorUsername = soundDetails?.sound?.author_username 
                ?: soundDetails?.sound?.username 
                ?: sound.author_username 
                ?: sound.username 
                ?: "Unknown"
                
            val resolvedAuthorAvatar = soundDetails?.sound?.avatar_url 
                ?: sound.avatar_url

            item {
                if (resolvedAuthorId.isNotBlank()) {
                    AuthorMiniProfile(
                        authorId = resolvedAuthorId,
                        authorUsername = resolvedAuthorUsername,
                        authorAvatar = resolvedAuthorAvatar,
                        onNavigateToProfile = onNavigateToProfile
                    )
                }
                Spacer(modifier = Modifier.height(32.dp))
            }

            if (soundDetails?.videos?.isNotEmpty() == true) {
                item {
                    Text(
                        text = "Vidéos avec ce son (${soundDetails?.videos?.size ?: 0})",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )
                }
                item {
                    androidx.compose.foundation.lazy.LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth().padding(bottom = 32.dp)
                    ) {
                        items(soundDetails?.videos ?: emptyList()) { video ->
                            Box(
                                modifier = Modifier
                                    .width(120.dp)
                                    .aspectRatio(9f / 16f)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color.DarkGray)
                            ) {
                                AsyncImage(
                                    model = video.thumbnail_url,
                                    contentDescription = "Video thumbnail",
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop
                                )
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(
                                            androidx.compose.ui.graphics.Brush.verticalGradient(
                                                colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.7f)),
                                                startY = 100f
                                            )
                                        )
                                )
                                Row(
                                    modifier = Modifier.align(Alignment.BottomStart).padding(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.PlayArrow,
                                        contentDescription = "Plays",
                                        tint = Color.White,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "${video.views}",
                                        color = Color.White,
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showComments) {
        ModalBottomSheet(
            onDismissRequest = { showComments = false },
            containerColor = MaterialTheme.colorScheme.surface,
            modifier = Modifier.fillMaxHeight(0.7f)
        ) {
            Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
                Text("Comments", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(16.dp))
                if (isCommentsLoading) {
                    Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                } else {
                    LazyColumn(modifier = Modifier.weight(1f)) {
                        items(commentsList) { c ->
                            Column(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
                                Text(c.username ?: "User", style = MaterialTheme.typography.labelMedium, color = Color.Gray)
                                Text(c.text, color = Color.White)
                                Divider(modifier = Modifier.padding(top = 8.dp), color = Color.Gray.copy(alpha=0.2f))
                            }
                        }
                        if (commentsList.isEmpty()) {
                            item {
                                Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                                    Text("No comments yet", color = Color.Gray)
                                }
                            }
                        }
                    }
                }
                
                // Add comment field
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) {
                    OutlinedTextField(
                        value = newCommentText,
                        onValueChange = { newCommentText = it },
                        modifier = Modifier.weight(1f),
                        placeholder = { Text("Write a comment...") },
                        shape = CircleShape
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    IconButton(
                        onClick = {
                            if (newCommentText.isNotBlank()) {
                                coroutineScope.launch {
                                    try {
                                        val c = com.example.data.remote.NetworkModule.api.postComment(
                                            sound.id,
                                            com.example.domain.model.CommentRequest(newCommentText)
                                        )
                                        commentsList = commentsList + c
                                        newCommentText = ""
                                    } catch (e: Exception) {
                                        e.printStackTrace()
                                    }
                                }
                            }
                        },
                        modifier = Modifier.background(MaterialTheme.colorScheme.primary, CircleShape)
                    ) {
                        Icon(Icons.Default.Send, contentDescription = "Send", tint = Color.Black)
                    }
                }
                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }

    if (showAddToPlaylistDialog) {
        AddToPlaylistDialog(
            sound = sound,
            appDatabase = appDatabase,
            onDismiss = { showAddToPlaylistDialog = false }
        )
    }
}

@Composable
fun StatBox(label: String, value: String, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .background(Color(0x0CFFFFFF), RoundedCornerShape(16.dp))
            .border(1.dp, Color(0x0FFFFFFF), RoundedCornerShape(16.dp))
            .padding(vertical = 10.dp, horizontal = 4.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = label, 
                fontSize = 9.sp, 
                color = Color(0xFF06B6D4), 
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = 1.sp
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = value, 
                fontSize = 13.sp, 
                color = Color.White, 
                fontWeight = FontWeight.Black
            )
        }
    }
}

@Composable
fun AuthorMiniProfile(
    authorId: String,
    authorUsername: String,
    authorAvatar: String?,
    onNavigateToProfile: (String) -> Unit
) {
    var userProfile by remember { mutableStateOf<com.example.domain.model.UserResponse?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var isFollowing by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(authorId) {
        try {
            if (authorId.isNotBlank()) {
                val profile = com.example.data.remote.NetworkModule.api.getUserProfile(authorId)
                userProfile = profile
                isFollowing = profile.is_following
            }
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            isLoading = false
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0x0CFFFFFF), RoundedCornerShape(20.dp))
            .border(1.dp, Color(0x0FFFFFFF), RoundedCornerShape(20.dp))
            .padding(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            coil.compose.AsyncImage(
                model = userProfile?.avatar_url ?: authorAvatar ?: "https://api.dicebear.com/7.x/avataaars/png?seed=$authorUsername",
                contentDescription = "Avatar",
                modifier = Modifier
                    .size(54.dp)
                    .clip(CircleShape)
                    .background(Color(0x16FFFFFF), CircleShape)
                    .border(1.5.dp, Color(0x1AFFFFFF), CircleShape),
                contentScale = androidx.compose.ui.layout.ContentScale.Crop
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = authorUsername, 
                    color = Color.White, 
                    fontWeight = FontWeight.Black, 
                    style = MaterialTheme.typography.titleMedium
                )
                if (!isLoading && userProfile != null) {
                    Text(
                        text = "${userProfile!!.followers_count} abonnés", 
                        color = Color.LightGray.copy(alpha = 0.6f), 
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
                if (!isLoading && userProfile != null && userProfile?.bio?.isNotBlank() == true) {
                    val url = "https://hoosthubs-g.onrender.com"
                    val context = LocalContext.current
                    Text(
                        text = "Portfolio: $url",
                        color = Color(0xFF06B6D4),
                        style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier
                            .padding(top = 4.dp)
                            .clickable {
                                val intent = Intent(Intent.ACTION_VIEW, android.net.Uri.parse(url))
                                context.startActivity(intent)
                            }
                    )
                }
            }
            if (userProfile?.is_verified == true) {
                Icon(
                    imageVector = Icons.Default.Verified, 
                    contentDescription = "Verified", 
                    tint = Color(0xFF06B6D4), 
                    modifier = Modifier.size(22.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
            }
        }
        
        if (!isLoading && userProfile != null && !userProfile!!.bio.isNullOrBlank()) {
            Spacer(modifier = Modifier.height(12.dp))
            val bioText = userProfile!!.bio!!
            Text(
                text = if (bioText.length > 100) "${bioText.take(100)}..." else bioText,
                color = Color.LightGray.copy(alpha = 0.8f),
                style = MaterialTheme.typography.bodySmall,
                maxLines = 2
            )
        }

        Spacer(modifier = Modifier.height(16.dp))
        Row(
            modifier = Modifier.fillMaxWidth(), 
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            androidx.compose.material3.OutlinedButton(
                onClick = { onNavigateToProfile(authorId) },
                shape = CircleShape,
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.35f)),
                modifier = Modifier.weight(1f)
            ) {
                Text("Voir plus", color = Color.White, fontWeight = FontWeight.Bold)
            }
            Spacer(modifier = Modifier.width(16.dp))
            
            val followScale by animateFloatAsState(
                targetValue = if (isFollowing) 1.05f else 1.0f,
                animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
                label = "followScale"
            )
            
            androidx.compose.material3.Button(
                onClick = {
                    coroutineScope.launch {
                        try {
                            if (isFollowing) {
                                com.example.data.remote.NetworkModule.api.unfollowUser(authorId)
                            } else {
                                com.example.data.remote.NetworkModule.api.followUser(authorId)
                            }
                            // Fetch updated user profile from server in real-time
                            val updated = com.example.data.remote.NetworkModule.api.getUserProfile(authorId)
                            userProfile = updated
                            isFollowing = updated.is_following
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }
                },
                shape = CircleShape,
                modifier = Modifier.weight(1f).graphicsLayer(scaleX = followScale, scaleY = followScale),
                colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                    containerColor = if (isFollowing) Color.White.copy(alpha = 0.2f) else Color(0xFF06B6D4),
                    contentColor = if (isFollowing) Color.White else Color.Black
                )
            ) {
                Text(
                    text = if (isFollowing) "Abonné" else "S'abonner", 
                    fontWeight = FontWeight.ExtraBold
                )
            }
        }
    }
}

@Composable
fun WaveformVisualizer(
    soundId: String,
    currentPosition: Long,
    duration: Long,
    onSeek: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val heights = remember(soundId) {
        val seed = soundId.hashCode().toLong()
        val random = java.util.Random(seed)
        List(60) { 0.15f + 0.85f * random.nextFloat() }
    }

    var width by remember { mutableIntStateOf(1) }

    Box(
        modifier = modifier
            .onSizeChanged { width = it.width.coerceAtLeast(1) }
            .pointerInput(duration, width) {
                detectTapGestures { offset ->
                    if (duration > 0 && width > 1) {
                        val fraction = (offset.x / width).coerceIn(0f, 1f)
                        onSeek((fraction * duration).toLong())
                    }
                }
            }
    ) {
        androidx.compose.foundation.Canvas(modifier = Modifier.fillMaxSize()) {
            val canvasWidth = size.width
            val canvasHeight = size.height
            val barCount = heights.size
            val barSpacing = 4f
            val totalSpacing = barSpacing * (barCount - 1)
            val barWidth = (canvasWidth - totalSpacing) / barCount

            val progressFract = if (duration > 0) currentPosition.toFloat() / duration else 0f
            val activeBarsCount = (barCount * progressFract).toInt()

            for (i in 0 until barCount) {
                val baseHeight = heights[i] * canvasHeight
                val left = i * (barWidth + barSpacing)
                val top = (canvasHeight - baseHeight) / 2f
                val barSize = androidx.compose.ui.geometry.Size(barWidth, baseHeight)
                
                val brush = if (i <= activeBarsCount) {
                    androidx.compose.ui.graphics.Brush.verticalGradient(
                        colors = listOf(Color(0xFF00FFCC), Color(0xFF00D2FF))
                    )
                } else {
                    androidx.compose.ui.graphics.Brush.verticalGradient(
                        colors = listOf(Color(0x26FFFFFF), Color(0x13FFFFFF))
                    )
                }
                
                drawRoundRect(
                    brush = brush,
                    topLeft = androidx.compose.ui.geometry.Offset(left, top),
                    size = barSize,
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(barWidth / 2f, barWidth / 2f)
                )
            }
        }
    }
}

@Composable
fun UserProfile(
    userId: String,
    onNavigateToProfile: (String) -> Unit
) {
    var profile by remember { mutableStateOf<com.example.domain.model.UserResponse?>(null) }
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(userId) {
        if (userId.isNotBlank()) {
            try {
                profile = com.example.data.remote.NetworkModule.api.getUserProfile(userId)
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                isLoading = false
            }
        }
    }

    if (!isLoading && profile != null) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(Color(0x0CFFFFFF))
                .border(1.dp, Color(0x3B06B6D4), RoundedCornerShape(16.dp))
                .clickable { onNavigateToProfile(userId) }
                .padding(16.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                coil.compose.AsyncImage(
                    model = profile?.avatar_url ?: "https://api.dicebear.com/7.x/avataaars/png?seed=${profile?.username}",
                    contentDescription = "Avatar",
                    modifier = Modifier
                        .size(50.dp)
                        .clip(CircleShape)
                        .background(Color(0x16FFFFFF), CircleShape)
                        .border(1.5.dp, Color(0x1AFFFFFF), CircleShape),
                    contentScale = androidx.compose.ui.layout.ContentScale.Crop
                )
                Spacer(modifier = Modifier.width(14.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = profile?.username ?: "Artiste",
                            color = Color.White,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Black
                        )
                        if (profile?.is_verified == true) {
                            Spacer(modifier = Modifier.width(6.dp))
                            Icon(
                                imageVector = Icons.Default.Verified,
                                contentDescription = "Verified Profile",
                                tint = Color(0xFF06B6D4),
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                    Text(
                        text = "${profile?.followers_count ?: 0} followers • ${profile?.total_sounds ?: 0} sons",
                        color = Color.White.copy(alpha = 0.5f),
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
            if (!profile?.bio.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = profile!!.bio!!,
                    color = Color.LightGray.copy(alpha = 0.8f),
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 2,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }
        }
    }
}


