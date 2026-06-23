package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
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
import com.example.data.local.AppDatabase
import com.example.data.local.AuthManager
import com.example.data.remote.NetworkModule
import com.example.domain.model.UserProfileFull
import com.example.data.local.DownloadedSoundEntity
import com.example.domain.model.Sound
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    userId: String?,
    authManager: AuthManager,
    appDatabase: AppDatabase,
    onBack: () -> Unit,
    onLogout: () -> Unit,
    onAnalyticsClick: () -> Unit,
    onSoundClick: (Sound) -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    var selectedTab by remember { mutableIntStateOf(0) }
    var offlineSounds by remember { mutableStateOf<List<DownloadedSoundEntity>>(emptyList()) }
    var userSounds by remember { mutableStateOf<List<Sound>>(emptyList()) }
    var userProfileFull by remember { mutableStateOf<UserProfileFull?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    
    val targetUserId = userId ?: authManager.getUserId()
    val isMe = targetUserId == authManager.getUserId()
    val profileName = userProfileFull?.username ?: if (userId != null && !isMe) "Artiste $userId" else authManager.getUsername() ?: "Moi"

    // Retrieve offline sounds from Room and Network Details
    LaunchedEffect(targetUserId) {
        coroutineScope.launch {
            try {
                offlineSounds = appDatabase.soundDao().getAllDownloadedSounds()
            } catch (e: Exception) {
                e.printStackTrace()
            }
            if (targetUserId != null) {
                try {
                    isLoading = true
                    userProfileFull = NetworkModule.api.getFullUserProfile(targetUserId)
                    userSounds = NetworkModule.api.getUserSounds(targetUserId)
                } catch (e: Exception) {
                    e.printStackTrace()
                } finally {
                    isLoading = false
                }
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF0F1E36), // Deep midnight blue
                        Color(0xFF030712)  // Near Pitch Black
                    )
                )
            )
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Header Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Retour", tint = Color.White)
                }
                
                Text(
                    text = "Profil",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )

                if (isMe) {
                    IconButton(onClick = {
                        authManager.clearSession()
                        onLogout()
                    }) {
                        Icon(Icons.Default.ExitToApp, contentDescription = "Déconnexion", tint = Color.Red)
                    }
                } else {
                    Spacer(modifier = Modifier.width(48.dp))
                }
            }

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f),
                contentPadding = PaddingValues(bottom = 32.dp)
            ) {
                // Profile Avatar & Name Header
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        AsyncImage(
                            model = "https://picsum.photos/300?random=$profileName",
                            contentDescription = "Avatar",
                            modifier = Modifier
                                .size(120.dp)
                                .clip(CircleShape)
                                .border(3.dp, Color(0xFF3B82F6), CircleShape),
                            contentScale = ContentScale.Crop
                        )
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = profileName,
                                color = Color.White,
                                fontSize = 24.sp,
                                fontWeight = FontWeight.ExtraBold
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Icon(
                                imageVector = Icons.Default.Verified,
                                contentDescription = "Certifié",
                                tint = Color(0xFF3B82F6),
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        Text(
                            text = if (isMe) "Producteur de sons expert" else "Artiste créateur - Strip Sound",
                            color = Color.LightGray,
                            fontSize = 14.sp,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                        
                        Spacer(modifier = Modifier.height(24.dp))
                        
                        // Action Buttons (Analytics, Follow)
                        Row(
                            modifier = Modifier.padding(horizontal = 24.dp),
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            if (isMe) {
                                Button(
                                    onClick = onAnalyticsClick,
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF3B82F6)),
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(Icons.Default.Assessment, contentDescription = "Stats", modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Statistiques d'écoute")
                                }
                            } else {
                                Button(
                                    onClick = { /* Follow mock */ },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF3B82F6)),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Text("S'abonner")
                                }
                            }
                        }
                    }
                }

                // Stats Dashboard Row
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp, vertical = 8.dp)
                            .background(Color(0xFF1E293B).copy(alpha = 0.6f), RoundedCornerShape(16.dp))
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceAround
                    ) {
                        ProfileStatItem("Créations", userProfileFull?.total_sounds?.toString() ?: "0")
                        ProfileStatItem("Abonnés", userProfileFull?.followers_count?.toString() ?: "0")
                        ProfileStatItem("Mentions", userProfileFull?.total_audio_likes?.toString() ?: "0")
                    }
                    Spacer(modifier = Modifier.height(24.dp))
                }

                // Grid Tabs
                item {
                    TabRow(
                        selectedTabIndex = selectedTab,
                        containerColor = Color.Transparent,
                        contentColor = Color(0xFF3B82F6),
                        indicator = { tabPositions ->
                            TabRowDefaults.SecondaryIndicator(
                                modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                                color = Color(0xFF3B82F6)
                            )
                        }
                    ) {
                        Tab(
                            selected = selectedTab == 0,
                            onClick = { selectedTab = 0 },
                            text = { Text("Créations", color = if (selectedTab == 0) Color.White else Color.Gray) }
                        )
                        Tab(
                            selected = selectedTab == 1,
                            onClick = { selectedTab = 1 },
                            text = { Text("Téléchargements (${offlineSounds.size})", color = if (selectedTab == 1) Color.White else Color.Gray) }
                        )
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                }

                // Tab Content
                if (selectedTab == 0) {
                    if (userSounds.isEmpty() && !isLoading) {
                        item {
                            Text(
                                "Aucune création pour le moment.",
                                color = Color.Gray,
                                modifier = Modifier.padding(32.dp)
                            )
                        }
                    } else {
                        items(userSounds) { sound ->
                            ProfileSoundItem(sound = sound, onPlayClick = { onSoundClick(sound) })
                        }
                    }
                } else {
                    if (offlineSounds.isEmpty()) {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(48.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("Aucun titre téléchargé hors-ligne", color = Color.Gray)
                            }
                        }
                    } else {
                        items(offlineSounds) { entity ->
                            val s = Sound(
                                id = entity.id,
                                title = entity.title,
                                category = entity.category,
                                cover_url = entity.coverUrl,
                                username = entity.authorName,
                                audio_url = entity.localFilePath
                            )
                            OfflineSoundItem(entity = entity, onPlayClick = { onSoundClick(s) })
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ProfileStatItem(label: String, count: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(count, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 20.sp)
        Spacer(modifier = Modifier.height(4.dp))
        Text(label, color = Color.Gray, fontSize = 12.sp)
    }
}

@Composable
fun ProfileSoundItem(sound: Sound, onPlayClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .clickable(onClick = onPlayClick),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B).copy(alpha = 0.4f))
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AsyncImage(
                model = sound.cover_url ?: "https://picsum.photos/150?random=${sound.id}",
                contentDescription = "Cover",
                modifier = Modifier
                    .size(50.dp)
                    .clip(RoundedCornerShape(8.dp)),
                contentScale = ContentScale.Crop
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(sound.title, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Text(sound.category, color = Color.Gray, fontSize = 12.sp)
            }
            IconButton(onClick = onPlayClick) {
                Icon(Icons.Default.PlayArrow, contentDescription = "Play", tint = Color(0xFF3B82F6))
            }
        }
    }
}

@Composable
fun OfflineSoundItem(entity: DownloadedSoundEntity, onPlayClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .clickable(onClick = onPlayClick),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF10192C))
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AsyncImage(
                model = entity.coverUrl ?: "https://picsum.photos/150?random=${entity.id}",
                contentDescription = "Cover",
                modifier = Modifier
                    .size(50.dp)
                    .clip(RoundedCornerShape(8.dp)),
                contentScale = ContentScale.Crop
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(entity.title, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.CloudDone, contentDescription = "Offline", tint = Color(0xFF0F766E), modifier = Modifier.size(12.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(entity.authorName, color = Color(0xFF0F766E), fontSize = 12.sp)
                }
            }
            IconButton(onClick = onPlayClick) {
                Icon(Icons.Default.Headphones, contentDescription = "Play offline", tint = Color(0xFF3B82F6))
            }
        }
    }
}
