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
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.domain.model.Sound
import com.example.player.AudioPlayerManager
import androidx.media3.common.Player

import kotlinx.coroutines.launch
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Add

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlayerScreen(
    sound: Sound,
    audioPlayerManager: AudioPlayerManager,
    audioDownloader: com.example.data.local.AudioDownloader,
    appDatabase: com.example.data.local.AppDatabase,
    onClose: () -> Unit
) {
    val isPlaying by audioPlayerManager.isPlaying.collectAsState()
    var isDownloaded by remember { mutableStateOf(false) }
    var isDownloading by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()
    
    LaunchedEffect(sound) {
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
            
        audioPlayerManager.playTrack(url = url, itemMetadata = mediaMetadata)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(24.dp)
            .windowInsetsPadding(WindowInsets.statusBars)
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
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

                IconButton(
                    onClick = onClose,
                    modifier = Modifier.background(MaterialTheme.colorScheme.surfaceVariant, CircleShape)
                ) {
                    Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.Gray)
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
            Row(verticalAlignment = Alignment.CenterVertically) {
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

            Spacer(modifier = Modifier.weight(1f))
            
            // "Visualizer" / progress bar placeholder
            Row(
                modifier = Modifier.fillMaxWidth().height(48.dp),
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                val bars = listOf(0.2f, 0.4f, 0.7f, 0.9f, 0.6f, 1.0f, 0.7f, 0.4f, 0.3f, 0.5f, 0.8f, 0.6f, 0.4f, 0.2f)
                bars.forEachIndexed { index, heightFract ->
                    val color = if (index in 2..7) Color(0xFF06B6D4) else Color.DarkGray
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight(heightFract)
                            .padding(horizontal = 1.dp)
                            .clip(RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp))
                            .background(color)
                    )
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("00:00", fontSize = 10.sp, color = Color.Gray)
                Text("-00:00", fontSize = 10.sp, color = Color.Gray)
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
                IconButton(onClick = { /* Previous */ }) {
                    Icon(Icons.Default.SkipPrevious, contentDescription = "Previous", tint = Color.White, modifier = Modifier.size(32.dp))
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

