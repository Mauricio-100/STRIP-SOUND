package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.PersonRemove
import androidx.compose.material3.*
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
import com.example.data.remote.NetworkModule
import com.example.domain.model.Sound
import com.example.domain.model.UserProfileFull
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreatorProfileScreen(
    creatorId: String,
    onBack: () -> Unit,
    onPlaySound: (Sound) -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    var profile by remember { mutableStateOf<UserProfileFull?>(null) }
    var publishedSounds by remember { mutableStateOf<List<Sound>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var isFollowing by remember { mutableStateOf(false) }

    LaunchedEffect(creatorId) {
        try {
            isLoading = true
            profile = NetworkModule.api.getFullUserProfile(creatorId)
            publishedSounds = NetworkModule.api.getUserSounds(creatorId)
            isFollowing = profile?.is_following ?: false
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            isLoading = false
        }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { 
                    Text(
                        profile?.username?.uppercase() ?: "PROFILE",
                        color = Color.White,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 2.sp,
                        fontSize = 16.sp
                    ) 
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = Color(0xFF0F172A))
            )
        },
        containerColor = Color(0xFF020617)
    ) { padding ->
        if (isLoading) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = Color(0xFF00FFCC))
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(bottom = 32.dp)
            ) {
                item {
                    ProfileHeader(
                        profile = profile,
                        isFollowing = isFollowing,
                        onFollowClick = {
                            coroutineScope.launch {
                                try {
                                    if (isFollowing) {
                                        NetworkModule.api.unfollowUser(creatorId)
                                        isFollowing = false
                                    } else {
                                        NetworkModule.api.followUser(creatorId)
                                        isFollowing = true
                                    }
                                } catch (e: Exception) {
                                    e.printStackTrace()
                                }
                            }
                        }
                    )
                }

                item {
                    Text(
                        "SOUNDS PUBLIÉS",
                        color = Color.Gray,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Black,
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp),
                        letterSpacing = 1.sp
                    )
                }

                items(publishedSounds) { sound ->
                    PublishedSoundItem(sound = sound, onClick = { onPlaySound(sound) })
                }
                
                if (publishedSounds.isEmpty()) {
                    item {
                        Box(Modifier.fillMaxWidth().padding(40.dp), contentAlignment = Alignment.Center) {
                            Text("Aucun son publié pour le moment", color = Color.DarkGray, fontSize = 14.sp)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ProfileHeader(
    profile: UserProfileFull?,
    isFollowing: Boolean,
    onFollowClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF0F172A))
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(contentAlignment = Alignment.BottomEnd) {
            AsyncImage(
                model = profile?.avatar_url ?: "https://api.dicebear.com/7.x/avataaars/png?seed=${profile?.username}",
                contentDescription = null,
                modifier = Modifier
                    .size(100.dp)
                    .clip(CircleShape)
                    .border(2.dp, Color(0xFF00FFCC), CircleShape),
                contentScale = ContentScale.Crop
            )
            if (profile?.is_verified == true) {
                Icon(
                    Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = Color(0xFF00FFCC),
                    modifier = Modifier.size(28.dp).background(Color.Black, CircleShape).padding(2.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "@${profile?.username ?: "unknown"}",
            color = Color.White,
            fontWeight = FontWeight.ExtraBold,
            fontSize = 20.sp
        )

        profile?.bio?.let {
            Text(
                text = it,
                color = Color.Gray,
                fontSize = 14.sp,
                modifier = Modifier.padding(top = 8.dp),
                maxLines = 2
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            StatItem(label = "Auditions", value = "${profile?.total_audio_plays ?: 0}")
            StatItem(label = "Likes", value = "${profile?.total_audio_likes ?: 0}")
            StatItem(label = "Followers", value = "${profile?.followers_count ?: 0}")
        }

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = onFollowClick,
            modifier = Modifier.fillMaxWidth().height(48.dp),
            shape = RoundedCornerShape(24.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = if (isFollowing) Color.Transparent else Color(0xFF00FFCC),
                contentColor = if (isFollowing) Color.White else Color.Black
            ),
            border = if (isFollowing) BorderStroke(1.dp, Color.Gray) else null
        ) {
            Icon(
                if (isFollowing) Icons.Default.PersonRemove else Icons.Default.PersonAdd,
                contentDescription = null
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                if (isFollowing) "DÉSABONNER" else "S'ABONNER",
                fontWeight = FontWeight.Black,
                letterSpacing = 1.sp
            )
        }
    }
}

@Composable
fun StatItem(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, color = Color.White, fontWeight = FontWeight.Black, fontSize = 18.sp)
        Text(label, color = Color.Gray, fontSize = 11.sp)
    }
}

@Composable
fun PublishedSoundItem(sound: Sound, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 20.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AsyncImage(
            model = sound.cover_url ?: "https://api.dicebear.com/7.x/shapes/png?seed=${sound.title}",
            contentDescription = null,
            modifier = Modifier.size(50.dp).clip(RoundedCornerShape(8.dp)),
            contentScale = ContentScale.Crop
        )
        
        Spacer(modifier = Modifier.width(16.dp))
        
        Column(modifier = Modifier.weight(1f)) {
            Text(sound.title, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
            Text("${sound.category} • ${sound.plays_count} auditions", color = Color.Gray, fontSize = 12.sp)
        }
        
        Icon(Icons.Default.MusicNote, contentDescription = null, tint = Color(0xFF00FFCC).copy(alpha = 0.5f))
    }
}
