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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlayerScreen(
    sound: Sound,
    audioPlayerManager: AudioPlayerManager,
    audioDownloader: com.example.data.local.AudioDownloader,
    appDatabase: com.example.data.local.AppDatabase,
    onClose: () -> Unit,
    onNavigateToProfile: (String) -> Unit
) {
    val isPlaying by audioPlayerManager.isPlaying.collectAsState()
    var isDownloaded by remember { mutableStateOf(false) }
    var isDownloading by remember { mutableStateOf(false) }
    val currentPosition by audioPlayerManager.currentPosition.collectAsState()
    val duration by audioPlayerManager.duration.collectAsState()
    var isLiked by remember { mutableStateOf(false) }
    var likesCount by remember { mutableIntStateOf(sound.likes_count) }
    
    var showComments by remember { mutableStateOf(false) }
    var showAddToPlaylistDialog by remember { mutableStateOf(false) }
    var commentsList by remember { mutableStateOf<List<com.example.domain.model.Comment>>(emptyList()) }
    var isCommentsLoading by remember { mutableStateOf(false) }
    var newCommentText by remember { mutableStateOf("") }
    
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
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 24.dp)
            .windowInsetsPadding(WindowInsets.statusBars)
    ) {
        item {
            Column(
                modifier = Modifier
                    .fillParentMaxHeight()
                    .padding(vertical = 24.dp)
            ) {
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
                    modifier = Modifier.background(MaterialTheme.colorScheme.surfaceVariant, CircleShape)
                ) {
                    if (isDownloading) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp, color = Color(0xFF06B6D4))
                    } else {
                        Icon(
                            imageVector = if (isDownloaded) Icons.Default.Check else Icons.Default.Add,
                            contentDescription = "Download",
                            tint = if (isDownloaded) Color(0xFF06B6D4) else Color.Gray
                        )
                    }
                }
                
                val context = LocalContext.current
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    IconButton(
                        onClick = {
                            val notificationManager = com.example.util.CustomNotificationManager(context)
                            notificationManager.showReportNotification(sound.username ?: sound.author_username ?: "Unknown")
                        },
                        modifier = Modifier.background(MaterialTheme.colorScheme.surfaceVariant, CircleShape)
                    ) {
                        Icon(Icons.Default.Report, contentDescription = "Signaler", tint = Color.Gray)
                    }
                    IconButton(
                        onClick = onClose,
                        modifier = Modifier.background(MaterialTheme.colorScheme.surfaceVariant, CircleShape)
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.Gray)
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Album Art Area
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
                    .clip(RoundedCornerShape(24.dp))
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
                            .background(MaterialTheme.colorScheme.surface),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.PlayArrow,
                            contentDescription = null,
                            modifier = Modifier.size(80.dp),
                            tint = Color.DarkGray
                        )
                    }
                }
                
                // Hi-Res Audio tag bottom left
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(16.dp)
                        .background(Color(0x99000000), RoundedCornerShape(16.dp))
                        .border(1.dp, Color(0x1AFFFFFF), RoundedCornerShape(16.dp))
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF06B6D4))
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            "HI-RES 24-BIT / 192KHZ",
                            color = Color(0xFF06B6D4),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Title and Subtitle
            Text(
                text = sound.title,
                style = MaterialTheme.typography.titleLarge,
                color = Color.White,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(4.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.clickable {
                    val uid = sound.user_id ?: sound.author_id ?: ""
                    if (uid.isNotEmpty()) onNavigateToProfile(uid)
                }.padding(vertical = 4.dp)
            ) {
                Text(
                    text = "${sound.username ?: sound.author_username ?: "Unknown"} • ${sound.category}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.Gray,
                    fontWeight = FontWeight.Medium
                )
                if (sound.is_verified || sound.author_is_verified) {
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(
                        imageVector = Icons.Default.Verified,
                        contentDescription = "Verified",
                        tint = Color(0xFF06B6D4), // Cyan 500
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Stats grid
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                StatBox(label = "BPM", value = "128", modifier = Modifier.weight(1f))
                StatBox(label = "KEY", value = "G#m", modifier = Modifier.weight(1f))
                StatBox(label = "ENERGY", value = "84%", modifier = Modifier.weight(1f))
                StatBox(label = "CODEC", value = "FLAC", modifier = Modifier.weight(1f))
            }

            // Social Actions (Facebook style post simulation)
            Spacer(modifier = Modifier.height(16.dp))
            Row(
                modifier = Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha=0.5f), RoundedCornerShape(12.dp)).padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.clickable { 
                    val wasLiked = isLiked
                    isLiked = !isLiked 
                    if (isLiked) likesCount++ else likesCount--
                    coroutineScope.launch {
                        try {
                            com.example.data.remote.NetworkModule.api.likeSound(sound.id)
                        } catch (e: Exception) {
                            e.printStackTrace()
                            // Revert on failure
                            isLiked = wasLiked
                            if (isLiked) likesCount++ else likesCount--
                        }
                    }
                }) {
                    Icon(
                        if (isLiked) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                        contentDescription = "Like",
                        tint = if (isLiked) Color.Red else Color.Gray,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("$likesCount", color = Color.LightGray, fontSize = 12.sp)
                }

                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.clickable {
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
                }) {
                    Icon(Icons.Default.Comment, contentDescription = "Comment", tint = Color.Gray, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("${sound.plays_count / 100}", color = Color.LightGray, fontSize = 12.sp)
                }

                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.clickable {
                    val shareIntent = Intent(Intent.ACTION_SEND).apply {
                        type = "text/plain"
                        putExtra(Intent.EXTRA_TEXT, "Écoute ${sound.title} par ${sound.username} sur StripSound!")
                    }
                    context.startActivity(Intent.createChooser(shareIntent, "Partager via"))
                }) {
                    Icon(Icons.Default.Share, contentDescription = "Share", tint = Color.Gray, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Partager", color = Color.LightGray, fontSize = 12.sp)
                }

                Spacer(modifier = Modifier.width(16.dp))

                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.clickable {
                    showAddToPlaylistDialog = true
                }) {
                    Icon(Icons.Default.List, contentDescription = "Playlist", tint = Color.Gray, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Playlist", color = Color.LightGray, fontSize = 12.sp)
                }
            }

            Spacer(modifier = Modifier.weight(1f))
            
            // "Visualizer" / progress bar with deterministic high-res waveform
            WaveformVisualizer(
                soundId = sound.id,
                currentPosition = currentPosition,
                duration = duration,
                onSeek = { audioPlayerManager.seekTo(it) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))
            
            val currSec = (currentPosition / 1000) % 60
            val currMin = (currentPosition / 1000) / 60
            val durSec = (duration / 1000) % 60
            val durMin = (duration / 1000) / 60
            val remainingSec = ((duration - currentPosition) / 1000).coerceAtLeast(0) % 60
            val remainingMin = ((duration - currentPosition) / 1000).coerceAtLeast(0) / 60
            
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(String.format("%02d:%02d", currMin, currSec), fontSize = 10.sp, color = Color.Gray)
                Text(String.format("-%02d:%02d", remainingMin, remainingSec), fontSize = 10.sp, color = Color.Gray)
            }
            
            Spacer(modifier = Modifier.weight(1f))

            // Player controls
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { /* TODO */ }) {
                    Icon(Icons.Default.Sync, contentDescription = "Shuffle", tint = Color.Gray)
                }
                IconButton(onClick = { audioPlayerManager.seekTo(0) }) {
                    Icon(Icons.Default.SkipPrevious, contentDescription = "Previous/Restart", tint = Color.White, modifier = Modifier.size(32.dp))
                }

                FloatingActionButton(
                    onClick = { audioPlayerManager.togglePlayPause() },
                    shape = CircleShape,
                    containerColor = Color.White,
                    modifier = Modifier.size(80.dp)
                ) {
                    Icon(
                        if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = if (isPlaying) "Pause" else "Play",
                        tint = Color.Black,
                        modifier = Modifier.size(40.dp)
                    )
                }

                IconButton(onClick = { /* Next */ }) {
                    Icon(Icons.Default.SkipNext, contentDescription = "Next", tint = Color.White, modifier = Modifier.size(32.dp))
                }
                IconButton(onClick = { /* TODO */ }) {
                    Icon(Icons.Default.Sync, contentDescription = "Repeat", tint = Color.Gray)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            }
        }
        
        item {
            AuthorMiniProfile(
                authorId = sound.user_id ?: "",
                authorUsername = sound.username ?: sound.author_username ?: "Unknown",
                authorAvatar = sound.avatar_url,
                onNavigateToProfile = onNavigateToProfile
            )
            Spacer(modifier = Modifier.height(32.dp))
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
            .background(Color(0xFF141414), RoundedCornerShape(12.dp))
            .border(1.dp, Color(0x0DFFFFFF), RoundedCornerShape(12.dp))
            .padding(8.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(label, fontSize = 9.sp, color = Color.Gray)
            Text(value, fontSize = 12.sp, color = Color.White, fontWeight = FontWeight.Bold)
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
            .background(Color(0xFF1A1A1A), RoundedCornerShape(16.dp))
            .padding(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            coil.compose.AsyncImage(
                model = authorAvatar ?: "https://api.dicebear.com/7.x/avataaars/png?seed=$authorUsername",
                contentDescription = "Avatar",
                modifier = Modifier
                    .size(50.dp)
                    .background(Color.Gray, CircleShape),
                contentScale = androidx.compose.ui.layout.ContentScale.Crop
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(authorUsername, color = Color.White, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                if (!isLoading && userProfile != null) {
                    Text("${userProfile!!.followers_count} Followers", color = Color.Gray, style = MaterialTheme.typography.labelMedium)
                }
            }
            if (userProfile?.is_verified == true) {
                Icon(Icons.Default.Verified, contentDescription = "Verified", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(8.dp))
            }
        }
        
        if (!isLoading && userProfile != null && !userProfile!!.bio.isNullOrBlank()) {
            Spacer(modifier = Modifier.height(12.dp))
            val bioText = userProfile!!.bio!!
            Text(
                text = if (bioText.length > 100) "${bioText.take(100)}..." else bioText,
                color = Color.LightGray,
                style = MaterialTheme.typography.bodySmall,
                maxLines = 2
            )
        }

        Spacer(modifier = Modifier.height(16.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            androidx.compose.material3.OutlinedButton(
                onClick = { onNavigateToProfile(authorId) },
                shape = CircleShape,
                modifier = Modifier.weight(1f)
            ) {
                Text("Voir plus", color = Color.White)
            }
            Spacer(modifier = Modifier.width(16.dp))
            androidx.compose.material3.Button(
                onClick = {
                    isFollowing = !isFollowing
                    coroutineScope.launch {
                        try {
                            if (isFollowing) {
                                com.example.data.remote.NetworkModule.api.followUser(authorId)
                            } else {
                                com.example.data.remote.NetworkModule.api.unfollowUser(authorId)
                            }
                        } catch (e: Exception) {
                            e.printStackTrace()
                            isFollowing = !isFollowing
                        }
                    }
                },
                shape = CircleShape,
                modifier = Modifier.weight(1f),
                colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                    containerColor = if (isFollowing) Color.DarkGray else MaterialTheme.colorScheme.primary
                )
            ) {
                Text(if (isFollowing) "Following" else "Follow", color = if (isFollowing) Color.White else Color.Black)
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
                val color = if (i <= activeBarsCount) {
                    Color(0xFF06B6D4) // Lucid Cyan for played portion
                } else {
                    Color.Gray.copy(alpha = 0.35f) // Warm dark gray for unplayed portion
                }
                
                drawRoundRect(
                    color = color,
                    topLeft = androidx.compose.ui.geometry.Offset(left, top),
                    size = barSize,
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(barWidth / 2f, barWidth / 2f)
                )
            }
        }
    }
}

