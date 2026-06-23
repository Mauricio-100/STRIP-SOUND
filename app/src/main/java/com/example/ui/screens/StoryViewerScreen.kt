package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
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
import com.example.domain.model.VideoResponse
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun StoryViewerScreen(
    stories: List<VideoResponse>,
    initialIndex: Int,
    onClose: () -> Unit
) {
    var currentIndex by remember { mutableIntStateOf(if (stories.isNotEmpty()) initialIndex else 0) }
    var progress by remember { mutableFloatStateOf(0f) }
    var isLiked by remember { mutableStateOf(false) }
    var likesCount by remember { mutableIntStateOf(45) }
    
    val currentStory = if (stories.isNotEmpty() && currentIndex < stories.size) stories[currentIndex] else null

    // Story progresses forward over time
    LaunchedEffect(currentIndex) {
        progress = 0f
        isLiked = false
        likesCount = (24..134).random()
        while (progress < 1f) {
            delay(50)
            progress += 0.01f
        }
        if (currentIndex < stories.size - 1) {
            currentIndex++
        } else {
            onClose()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        if (currentStory != null) {
            // Unrestricted immersive media background
            AsyncImage(
                model = currentStory.thumbnail_url.ifBlank { "https://picsum.photos/1080/1920?random=$currentIndex" },
                contentDescription = "Story Media",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )

            // Dynamic Overlay overlay gradients for typography scanning
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        androidx.compose.ui.graphics.Brush.verticalGradient(
                            listOf(
                                Color.Black.copy(alpha = 0.6f),
                                Color.Transparent,
                                Color.Black.copy(alpha = 0.7f)
                            )
                        )
                    )
            )

            // Clickable navigation quadrants
            Row(modifier = Modifier.fillMaxSize()) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clickable {
                            if (currentIndex > 0) currentIndex-- else onClose()
                        }
                )
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clickable {
                            if (currentIndex < stories.size - 1) currentIndex++ else onClose()
                        }
                )
            }

            // Top segment loaders indicator
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp)
                    .align(Alignment.TopCenter),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                stories.forEachIndexed { idx, _ ->
                    val segmentProgress = when {
                        idx < currentIndex -> 1f
                        idx == currentIndex -> progress
                        else -> 0f
                    }
                    LinearProgressIndicator(
                        progress = { segmentProgress },
                        modifier = Modifier
                            .weight(1f)
                            .height(3.dp)
                            .clip(RoundedCornerShape(2.dp)),
                        color = Color.White,
                        trackColor = Color.White.copy(alpha = 0.3f)
                    )
                }
            }

            // Header profile details
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 28.dp, start = 16.dp, end = 16.dp)
                    .align(Alignment.TopCenter),
                verticalAlignment = Alignment.CenterVertically
            ) {
                AsyncImage(
                    model = currentStory.avatar_url ?: "https://picsum.photos/100?random=${currentStory.username}",
                    contentDescription = "Avatar",
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = currentStory.username,
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                        if (currentStory.is_verified) {
                            Spacer(modifier = Modifier.width(4.dp))
                            Icon(Icons.Default.Verified, contentDescription = "Certifié", tint = Color(0xFF3B82F6), modifier = Modifier.size(14.dp))
                        }
                    }
                    Text(
                        text = currentStory.created_at.ifBlank { "il y a 2h" },
                        color = Color.LightGray,
                        fontSize = 11.sp
                    )
                }
                IconButton(onClick = onClose) {
                    Icon(Icons.Default.Close, contentDescription = "Fermer", tint = Color.White)
                }
            }

            // Sidebar interactive features (Like, Vol)
            Column(
                modifier = Modifier
                    .padding(16.dp)
                    .align(Alignment.CenterEnd),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    IconButton(
                        onClick = {
                            isLiked = !isLiked
                            if (isLiked) likesCount++ else likesCount--
                        },
                        colors = IconButtonDefaults.iconButtonColors(containerColor = Color.Black.copy(alpha = 0.4f))
                    ) {
                        Icon(
                            imageVector = Icons.Default.Favorite,
                            contentDescription = "Like",
                            tint = if (isLiked) Color.Red else Color.White
                        )
                    }
                    Text("$likesCount", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }

                IconButton(
                    onClick = { /* Share clip */ },
                    colors = IconButtonDefaults.iconButtonColors(containerColor = Color.Black.copy(alpha = 0.4f))
                ) {
                    Icon(Icons.Default.Share, contentDescription = "Partager", tint = Color.White)
                }
            }

            // Bottom description details
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .align(Alignment.BottomStart),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = currentStory.description.ifBlank { "Nouveau rythme de beat-making direct du studio" },
                    color = Color.White,
                    fontWeight = FontWeight.Medium,
                    fontSize = 14.sp,
                    maxLines = 3
                )
            }
        } else {
            // Null state handler
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = Color.White)
            }
        }
    }
}
