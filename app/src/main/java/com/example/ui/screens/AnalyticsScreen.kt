package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.Hearing
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalContext
import com.example.data.local.AuthManager
import com.example.data.remote.NetworkModule
import com.example.domain.model.UserProfileFull

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnalyticsScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val authManager = remember { AuthManager(context) }
    var profile by remember { mutableStateOf<UserProfileFull?>(null) }
    
    LaunchedEffect(Unit) {
        try {
            val userId = authManager.getUserId()
            if (userId != null) {
                profile = NetworkModule.api.getFullUserProfile(userId)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF0C1424), // deep navy slate
                        Color(0xFF030712)  // jet black
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
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Retour", tint = Color.White)
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Statistiques d'Audience",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp
                )
            }

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Overview Banner Card
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B).copy(alpha = 0.8f))
                    ) {
                        Column(modifier = Modifier.padding(20.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Assessment, contentDescription = "Overview", tint = Color(0xFF3B82F6))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Audience Générale (Globale)", color = Color.LightGray, fontSize = 14.sp)
                            }
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(profile?.total_audio_plays?.toString() ?: "0", color = Color.White, fontSize = 42.sp, fontWeight = FontWeight.Black)
                            Text("Total des écoutes générées", color = Color(0xFF10B981), fontSize = 12.sp, fontWeight = FontWeight.Medium)
                        }
                    }
                }

                // Grid stats
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        StatMetricCard(
                            label = "Abonnés",
                            value = profile?.followers_count?.toString() ?: "0",
                            delta = "+0%",
                            deltaColor = Color(0xFF10B981),
                            icon = Icons.Default.Hearing,
                            modifier = Modifier.weight(1f)
                        )
                        StatMetricCard(
                            label = "Likes Reçus",
                            value = profile?.total_audio_likes?.toString() ?: "0",
                            delta = "+0%",
                            deltaColor = Color(0xFF10B981),
                            icon = Icons.Default.Favorite,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                // Graphic simulation placeholder
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B).copy(alpha = 0.5f))
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text("Fréquence d'Écoute Globale", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            Spacer(modifier = Modifier.height(16.dp))
                            
                            // Visual bar simulation
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(120.dp),
                                verticalAlignment = Alignment.Bottom,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                val barHeights = listOf(0.4f, 0.6f, 0.3f, 0.8f, 0.5f, 0.9f, 0.7f, 0.6f, 0.85f, 0.35f, 0.55f, 0.95f)
                                barHeights.forEachIndexed { idx, height ->
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .padding(horizontal = 2.dp)
                                            .fillMaxHeight(height)
                                            .background(
                                                if (idx == 11) Color(0xFF3B82F6) else Color(0xFF3B82F6).copy(alpha = 0.4f),
                                                RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp)
                                            )
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Lun", color = Color.Gray, fontSize = 10.sp)
                                Text("Mer", color = Color.Gray, fontSize = 10.sp)
                                Text("Ven", color = Color.Gray, fontSize = 10.sp)
                                Text("Dim", color = Color.Gray, fontSize = 10.sp)
                            }
                        }
                    }
                }

                // Best Performing sound
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B).copy(alpha = 0.5f))
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text("Top Titre cette semaine", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            Spacer(modifier = Modifier.height(12.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Star, contentDescription = "Star", tint = Color(0xFFFBBF24))
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text("Midnight Synthwaves Kick", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                    Text("3,200 écoutes réelles • Durée d'écoute moyenne 84%", color = Color.Gray, fontSize = 11.sp)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun StatMetricCard(
    label: String,
    value: String,
    delta: String,
    deltaColor: Color,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B).copy(alpha = 0.5f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Icon(icon, contentDescription = label, tint = Color(0xFF3B82F6), modifier = Modifier.size(24.dp))
            Spacer(modifier = Modifier.height(12.dp))
            Text(value, color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(4.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(label, color = Color.Gray, fontSize = 11.sp, modifier = Modifier.weight(1f))
                Text(delta, color = deltaColor, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}
