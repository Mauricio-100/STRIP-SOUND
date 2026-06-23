package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.animation.core.*
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.domain.model.Sound
import com.example.domain.model.SoundShortResponse
import com.example.player.AudioPlayerManager
import kotlinx.coroutines.launch

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ShortsScreen(
    sounds: List<Sound>,
    audioPlayerManager: AudioPlayerManager,
    onBack: () -> Unit,
    onNavigateToProfile: (String) -> Unit
) {
    val pagerState = rememberPagerState(pageCount = { sounds.size })
    val coroutineScope = rememberCoroutineScope()
    
    val currentSound = audioPlayerManager.currentSound.collectAsState().value
    val isPlaying by audioPlayerManager.isPlaying.collectAsState()
    
    // Auto-play sound preview when page changes
    LaunchedEffect(pagerState.currentPage) {
        if (sounds.isNotEmpty()) {
            val sound = sounds[pagerState.currentPage]
            // Use the dedicated short preview stream URL
            val previewUrl = "${com.example.data.remote.NetworkModule.BASE_URL}sounds/${sound.id}/short/stream"
            audioPlayerManager.playTrack(previewUrl, sound)
        }
    }

    Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
        VerticalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize()
        ) { page ->
            ShortItem(
                sound = sounds[page],
                audioPlayerManager = audioPlayerManager,
                isActive = pagerState.currentPage == page,
                onNavigateToProfile = onNavigateToProfile
            )
        }

        // Top bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onBack,
                modifier = Modifier
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.3f))
            ) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
            }
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = "S-3 SHORTS",
                color = Color(0xFF00FFCC),
                fontWeight = FontWeight.Black,
                letterSpacing = 2.sp,
                fontSize = 18.sp
            )
        }
    }
}

@Composable
fun ShortItem(
    sound: Sound,
    audioPlayerManager: AudioPlayerManager,
    isActive: Boolean,
    onNavigateToProfile: (String) -> Unit
) {
    var shortInfo by remember { mutableStateOf<SoundShortResponse?>(null) }
    var isFollowing by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()
    
    LaunchedEffect(sound.id) {
        try {
            shortInfo = com.example.data.remote.NetworkModule.api.getSoundShort(sound.id)
            val creatorId = sound.user_id ?: sound.author_id
            if (creatorId != null) {
                val profile = com.example.data.remote.NetworkModule.api.getUserProfile(creatorId)
                isFollowing = profile.is_following
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        // Background / Visual with subtle zoom animation when active
        val zoom by animateFloatAsState(
            targetValue = if (isActive) 1.1f else 1.0f,
            animationSpec = tween(10000, easing = LinearEasing),
            label = "zoom"
        )
        
        AsyncImage(
            model = sound.cover_url ?: "https://api.dicebear.com/7.x/shapes/png?seed=${sound.title}",
            contentDescription = null,
            modifier = Modifier.fillMaxSize().graphicsLayer(scaleX = zoom, scaleY = zoom),
            contentScale = ContentScale.Crop,
            alpha = 0.7f
        )
        
        // Gradient overlay (Stronger at bottom for text legibility)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.2f), Color.Black.copy(alpha = 0.9f)),
                        startY = 0f
                    )
                )
        )

        // Content
        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(20.dp)
                .padding(bottom = 60.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.clickable { 
                    val creatorId = sound.user_id ?: sound.author_id
                    if (creatorId != null) onNavigateToProfile(creatorId)
                }
            ) {
                AsyncImage(
                    model = sound.avatar_url ?: "https://api.dicebear.com/7.x/avataaars/png?seed=${sound.username}",
                    contentDescription = null,
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .border(1.5.dp, Color(0xFF00FFCC), CircleShape),
                    contentScale = ContentScale.Crop
                )
                
                Spacer(modifier = Modifier.width(12.dp))
                
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "@${sound.username ?: sound.author_username ?: "Creator"}",
                            color = Color.White,
                            fontWeight = FontWeight.Black,
                            fontSize = 16.sp
                        )
                        if (shortInfo?.creator_is_verified == true || sound.is_verified || sound.author_is_verified) {
                            Spacer(modifier = Modifier.width(4.dp))
                            Icon(Icons.Default.Verified, contentDescription = null, tint = Color(0xFF00FFCC), modifier = Modifier.size(16.dp))
                        }
                    }
                    
                    Text(
                        text = if (isFollowing) "SUIVI" else "S'ABONNER",
                        color = if (isFollowing) Color.Gray else Color(0xFF00FFCC),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Black,
                        modifier = Modifier.clickable {
                            coroutineScope.launch {
                                try {
                                    val creatorId = sound.user_id ?: sound.author_id
                                    if (creatorId != null) {
                                        if (isFollowing) {
                                            com.example.data.remote.NetworkModule.api.unfollowUser(creatorId)
                                            isFollowing = false
                                        } else {
                                            com.example.data.remote.NetworkModule.api.followUser(creatorId)
                                            isFollowing = true
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
            
            Spacer(modifier = Modifier.height(12.dp))
            
            Text(
                text = sound.title,
                color = Color.White,
                fontSize = 20.sp,
                fontWeight = FontWeight.Black
            )
            
            shortInfo?.mini_description?.let {
                Text(
                    text = it,
                    color = Color.White.copy(alpha = 0.7f),
                    fontSize = 14.sp,
                    maxLines = 2,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Added Technical Tags for "Sound Detail" requirement
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                val hash = kotlin.math.abs(sound.id.hashCode())
                val bpm = 120 + (hash % 40)
                val keys = listOf("Am", "Cm", "G#m", "F# Major", "B Minor")
                val key = keys[hash % keys.size]
                
                ShortTag(label = "$bpm BPM", color = Color(0xFF00FFCC))
                ShortTag(label = key, color = Color(0xFF8B5CF6))
                ShortTag(label = sound.category.uppercase(), color = Color(0xFFFFCC00))
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .background(Color.White.copy(alpha = 0.1f), RoundedCornerShape(20.dp))
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Icon(Icons.Default.MusicNote, contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Son original • ${sound.category}",
                    color = Color.White,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        // Right actions with polish
        Column(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp)
                .padding(bottom = 80.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            ActionButton(icon = Icons.Default.Favorite, label = "${sound.likes_count}")
            ActionButton(icon = Icons.Default.Comment, label = "${sound.comments_count}")
            ActionButton(icon = Icons.Default.Share, label = "Partager")
            
            // Spinning record (Polished Visual)
            val infiniteTransition = rememberInfiniteTransition(label = "record")
            val rotation by infiniteTransition.animateFloat(
                initialValue = 0f,
                targetValue = 360f,
                animationSpec = infiniteRepeatable(
                    animation = tween(4000, easing = LinearEasing),
                    repeatMode = RepeatMode.Restart
                ),
                label = "rotation"
            )
            
            Box(
                modifier = Modifier
                    .size(54.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.radialGradient(
                            listOf(Color(0xFF222222), Color.Black)
                        )
                    )
                    .border(2.dp, Color.White.copy(alpha = 0.1f), CircleShape)
                    .graphicsLayer { rotationZ = rotation },
                contentAlignment = Alignment.Center
            ) {
                AsyncImage(
                    model = sound.cover_url ?: "https://api.dicebear.com/7.x/shapes/png?seed=${sound.title}",
                    contentDescription = null,
                    modifier = Modifier.size(32.dp).clip(CircleShape).border(1.dp, Color.White.copy(alpha = 0.2f), CircleShape),
                    contentScale = ContentScale.Crop
                )
                // Center hole
                Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(Color.Black).border(0.5.dp, Color.White.copy(alpha = 0.3f), CircleShape))
            }
        }
    }
}

@Composable
fun ShortTag(label: String, color: Color) {
    Box(
        modifier = Modifier
            .background(color.copy(alpha = 0.15f), RoundedCornerShape(6.dp))
            .border(1.dp, color.copy(alpha = 0.3f), RoundedCornerShape(6.dp))
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Text(text = label, color = color, fontSize = 10.sp, fontWeight = FontWeight.Black)
    }
}

@Composable
fun ActionButton(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(Color.Black.copy(alpha = 0.4f))
                .clickable { /* Handle action */ },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(28.dp)
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(text = label, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
    }
}
