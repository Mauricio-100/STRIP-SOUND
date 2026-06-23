package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CameraFront
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun CreateStoryScreen(onBack: () -> Unit) {
    var isRecording by remember { mutableStateOf(false) }
    var countdown by remember { mutableIntStateOf(15) }
    var filterSelected by remember { mutableStateOf("Normal") }
    var postingStep by remember { mutableStateOf(false) }
    
    val coroutineScope = rememberCoroutineScope()

    // Countdown clock during active story shooting
    LaunchedEffect(isRecording) {
        if (isRecording) {
            countdown = 15
            while (countdown > 0 && isRecording) {
                delay(1000)
                countdown--
            }
            if (isRecording) {
                isRecording = false
                postingStep = true
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        if (postingStep) {
            // Posting loading screen
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                CircularProgressIndicator(color = Color(0xFF3B82F6), modifier = Modifier.size(54.dp))
                Spacer(modifier = Modifier.height(16.dp))
                Text("Publication de votre Strip Story...", color = Color.White, fontWeight = FontWeight.Bold)
                
                LaunchedEffect(Unit) {
                    delay(2000)
                    postingStep = false
                    onBack()
                }
            }
        } else {
            // Immersive studio camera simulation
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            listOf(Color(0xFF1E293B), Color(0xFF0F172A))
                        )
                    )
            ) {
                // Top Action controls
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                        .align(Alignment.TopCenter),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Retour", tint = Color.White)
                    }
                    
                    Row(
                        modifier = Modifier
                            .background(Color.Black.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.MusicNote, contentDescription = "Add Sound", tint = Color(0xFF3B82F6), modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Ajouter de la musique", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        IconButton(onClick = { }) {
                            Icon(Icons.Default.FlashOn, contentDescription = "Flash", tint = Color.White)
                        }
                        IconButton(onClick = { }) {
                            Icon(Icons.Default.CameraFront, contentDescription = "Cam", tint = Color.White)
                        }
                    }
                }

                // Sidebar video effects
                Column(
                    modifier = Modifier
                        .padding(16.dp)
                        .align(Alignment.CenterEnd),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    listOf("Normal", "Sépia", "N&B", "Neon").forEach { f ->
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(if (filterSelected == f) Color(0xFF3B82F6) else Color.White.copy(alpha=0.15f))
                                .clickable { filterSelected = f },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(f.take(3), color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                // Bottom capturing controls
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp)
                        .align(Alignment.BottomCenter),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(20.dp)
                ) {
                    // Recording counter tag
                    if (isRecording) {
                        Text(
                            text = "00:${if (countdown < 10) "0$countdown" else countdown}",
                            color = Color.Red,
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            modifier = Modifier
                                .background(Color.Black.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                                .padding(horizontal = 12.dp, vertical = 4.dp)
                        )
                    }

                    // Recording button trigger
                    Box(
                        modifier = Modifier
                            .size(80.dp)
                            .border(4.dp, Color.White, CircleShape)
                            .padding(6.dp)
                            .clip(CircleShape)
                            .background(if (isRecording) Color.Red else Color.White)
                            .clickable { isRecording = !isRecording }
                    )

                    Text(
                        text = if (isRecording) "Appuyez pour terminer" else "Appuyez pour enregistrer",
                        color = Color.LightGray,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}
