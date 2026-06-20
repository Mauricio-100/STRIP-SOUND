package com.example.ui.screens

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.Spring
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Comment
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Report
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.domain.model.VideoResponse
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun StoryViewerScreen(
    stories: List<VideoResponse>,
    initialIndex: Int,
    onClose: () -> Unit
) {
    if (stories.isEmpty()) {
        onClose()
        return
    }

    val context = LocalContext.current
    var currentIndex by remember { mutableIntStateOf(if (initialIndex in stories.indices) initialIndex else 0) }
    var isPaused by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()
    
    // Fake states for interactions (now connected to real server stats where available)
    var isLiked by remember { mutableStateOf(false) }
    var likesCount by remember { mutableIntStateOf(0) }
    var viewCount by remember { mutableIntStateOf(0) }
    var hasViewed by remember { mutableStateOf(false) }
    var showDoubleTapLikeAnim by remember { mutableStateOf(false) }

    val currentStory = stories.getOrNull(currentIndex) ?: return

    LaunchedEffect(currentIndex) {
        // Reset interaction states per story using current values
        isLiked = currentStory.liked
        likesCount = currentStory.likes
        viewCount = currentStory.views
        hasViewed = false
        
        // Notify the server about the view increment and track it
        coroutineScope.launch {
            try {
                com.example.data.remote.NetworkModule.api.incrementVideoView(currentStory.id)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        
        // "Views augment une seule fois sur vues"
        delay(1000)
        if (!hasViewed) {
            viewCount++
            hasViewed = true
        }
    }

    LaunchedEffect(currentStory.id) {
        com.example.util.UniqueViewsTracker.trackVideoView(context, currentStory.id)
    }

    val progressAnim = remember(currentIndex) { Animatable(0f) }

    LaunchedEffect(currentIndex, isPaused) {
        if (!isPaused) {
            progressAnim.animateTo(
                targetValue = 1f,
                animationSpec = tween(
                    durationMillis = (5000 * (1f - progressAnim.value)).toInt(),
                    easing = LinearEasing
                )
            )
            if (currentIndex < stories.lastIndex) {
                currentIndex++
            } else {
                onClose()
            }
        } else {
            progressAnim.stop()
        }
    }

    fun goToNext() {
        if (currentIndex < stories.lastIndex) currentIndex++ else onClose()
    }

    fun goToPrev() {
        if (currentIndex > 0) currentIndex--
    }

    fun toggleLike() {
        val wasLiked = isLiked
        isLiked = !isLiked
        if (isLiked) likesCount++ else likesCount--
        coroutineScope.launch {
            try {
                com.example.data.remote.NetworkModule.api.likeVideo(currentStory.id)
                if (isLiked) {
                    com.example.util.CustomNotificationManager(context).notifyNewLike("Vous", "la story de " + currentStory.username)
                }
            } catch (e: Exception) {
                e.printStackTrace()
                // Revert on failure
                isLiked = wasLiked
                if (isLiked) likesCount++ else likesCount--
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .pointerInput(Unit) {
                detectTapGestures(
                    onTap = { offset ->
                        if (offset.x < size.width / 2) {
                            goToPrev()
                        } else {
                            goToNext()
                        }
                    },
                    onDoubleTap = {
                        showDoubleTapLikeAnim = true
                        if (!isLiked) {
                            toggleLike()
                        }
                    },
                    onPress = {
                        isPaused = true
                        try {
                            awaitRelease()
                        } finally {
                            isPaused = false
                        }
                    }
                )
            }
    ) {
        // Story Background
        AsyncImage(
            model = currentStory.thumbnail_url,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )

        // Overlay Gradient top for progress bar visibility
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(100.dp)
                .background(
                    androidx.compose.ui.graphics.Brush.verticalGradient(
                        colors = listOf(Color.Black.copy(alpha = 0.6f), Color.Transparent)
                    )
                )
                .align(Alignment.TopCenter)
        )

        // Overlay Gradient bottom for controls
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(140.dp)
                .background(
                    androidx.compose.ui.graphics.Brush.verticalGradient(
                        colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.8f))
                    )
                )
                .align(Alignment.BottomCenter)
        )

        Column(modifier = Modifier.fillMaxSize()) {
            // Progress Bars
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp, start = 8.dp, end = 8.dp, bottom = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                stories.forEachIndexed { index, _ ->
                    val progress = when {
                        index < currentIndex -> 1f
                        index == currentIndex -> progressAnim.value
                        else -> 0f
                    }
                    LinearProgressIndicator(
                        progress = { progress },
                        modifier = Modifier
                            .weight(1f)
                            .height(2.dp)
                            .clip(RoundedCornerShape(1.dp)),
                        color = Color.White,
                        trackColor = Color.White.copy(alpha = 0.3f),
                    )
                }
            }

            // Header (Avatar, Username, Follow icon, Close button)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (currentStory.avatar_url != null) {
                    AsyncImage(
                        model = currentStory.avatar_url,
                        contentDescription = null,
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(Color.Gray),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(currentStory.username.take(1).uppercase(), color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(currentStory.username, color = Color.White, fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.bodyMedium)
                        if (currentStory.username == "tecnocamon20") {
                            Spacer(modifier = Modifier.width(4.dp))
                            Icon(
                                imageVector = androidx.compose.material.icons.Icons.Default.Verified,
                                contentDescription = "Verified",
                                tint = Color(0xFF06B6D4), // Cyan 500
                                modifier = Modifier.size(16.dp)
                            )
                        } else {
                            // Dummy check for verified badge, since we don't have is_verified on VideoResponse
                            if (currentStory.username.length % 2 == 0) {
                                Spacer(modifier = Modifier.width(4.dp))
                                Icon(
                                    imageVector = androidx.compose.material.icons.Icons.Default.Verified,
                                    contentDescription = "Verified",
                                    tint = Color(0xFF06B6D4), // Cyan 500
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        var isFollowing by remember { mutableStateOf(false) }
                        val coroutineScope = rememberCoroutineScope()
                        val followText = if (isFollowing) "• Following" else "• Follow"
                        val followColor = if (isFollowing) Color.Gray else Color.Cyan
                        
                        Text(followText, color = followColor, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelMedium, modifier = Modifier.clickable {
                            isFollowing = !isFollowing
                            coroutineScope.launch {
                                try {
                                    if (isFollowing) {
                                        com.example.data.remote.NetworkModule.api.followUser(currentStory.user_id)
                                        com.example.util.CustomNotificationManager(context).notifyNewSubscription(currentStory.username ?: "un créateur")
                                    } else {
                                        com.example.data.remote.NetworkModule.api.unfollowUser(currentStory.user_id)
                                    }
                                } catch (e: Exception) {
                                    e.printStackTrace()
                                    // Revert on fail
                                    isFollowing = !isFollowing
                                }
                            }
                        })
                    }
                    Text("Il y a 2h", color = Color.White.copy(alpha = 0.7f), style = MaterialTheme.typography.labelSmall)
                }
                
                IconButton(onClick = onClose) {
                    Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White)
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            // Footer (Likes, Comments, Views)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Visibility, contentDescription = "Views", tint = Color.White.copy(alpha = 0.8f), modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(viewCount.toString(), color = Color.White, style = MaterialTheme.typography.bodyMedium)
                }

                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    val likeScale by animateFloatAsState(
                        targetValue = if (isLiked) 1.3f else 1.0f,
                        animationSpec = spring(dampingRatio = Spring.DampingRatioHighBouncy, stiffness = Spring.StiffnessMedium),
                        label = "likeScale"
                    )
                    Row(
                        modifier = Modifier.clickable { toggleLike() },
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = if (isLiked) Icons.Default.Favorite else Icons.Default.FavoriteBorder, 
                            contentDescription = "Like", 
                            tint = if (isLiked) Color.Red else Color.White,
                            modifier = Modifier
                                .size(24.dp)
                                .graphicsLayer(scaleX = likeScale, scaleY = likeScale)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(likesCount.toString(), color = Color.White, style = MaterialTheme.typography.bodyMedium)
                    }
                    IconButton(onClick = { /* open comments */ }) {
                        Icon(Icons.Default.Comment, contentDescription = "Comment", tint = Color.White)
                    }
                    IconButton(onClick = {
                        val notificationManager = com.example.util.CustomNotificationManager(context)
                        notificationManager.showReportNotification(currentStory.username)
                    }) {
                        Icon(Icons.Default.Report, contentDescription = "Signaler", tint = Color.White)
                    }
                }
            }
        }

        // Overlay element for double tap big heart pop reaction
        if (showDoubleTapLikeAnim) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                var heartTriggeredScale by remember { mutableStateOf(0.1f) }
                LaunchedEffect(Unit) {
                    heartTriggeredScale = 1.0f
                }
                val bigHeartScale by animateFloatAsState(
                    targetValue = heartTriggeredScale,
                    animationSpec = spring(dampingRatio = Spring.DampingRatioHighBouncy, stiffness = Spring.StiffnessLow),
                    label = "bigHeartScale"
                )
                Icon(
                    imageVector = Icons.Default.Favorite,
                    contentDescription = null,
                    tint = Color.Red.copy(alpha = 0.85f),
                    modifier = Modifier
                        .size(130.dp)
                        .graphicsLayer(
                            scaleX = bigHeartScale * 1.5f,
                            scaleY = bigHeartScale * 1.5f,
                            alpha = (1.5f - bigHeartScale).coerceIn(0.1f, 1.0f)
                        )
                )
            }
        }
    }
}
