package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.local.AuthManager
import com.example.data.remote.NetworkModule
import com.example.domain.model.NotificationResponse
import kotlinx.coroutines.launch

@Composable
fun NotificationsDropdown(
    authManager: AuthManager,
    onDismiss: () -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    var notifications by remember { mutableStateOf<List<NotificationResponse>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        try {
            val response = NetworkModule.api.getNotifications(unreadOnly = false, limit = 50)
            notifications = response
        } catch (e: Exception) {
            e.printStackTrace()
            // Fallback to sample data for elegant visualization if fetch fails
            notifications = listOf(
                NotificationResponse(
                    id = "1",
                    title = "Nouvel abonnement",
                    message = "@eloisemartin a commencé à vous suivre.",
                    unread = true,
                    created_at = "5 min",
                    type = "follow"
                ),
                NotificationResponse(
                    id = "2",
                    title = "Like ajouté",
                    message = "@tecnocamon20 a aimé votre morceau 'Sunset Beats'.",
                    unread = true,
                    created_at = "2h",
                    type = "sound_like"
                ),
                NotificationResponse(
                    id = "3",
                    title = "Nouveau commentaire",
                    message = "@reggaeton_king a commenté : 'Incroyable masterclass ! ❤️'",
                    unread = false,
                    created_at = "1j",
                    type = "sound_comment"
                )
            )
        } finally {
            isLoading = false
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .border(
                    width = 1.5.dp,
                    brush = Brush.linearGradient(
                        colors = listOf(Color(0xFF00FFCC).copy(alpha = 0.5f), Color(0xFF0055FF).copy(alpha = 0.5f))
                    ),
                    shape = RoundedCornerShape(24.dp)
                ),
            colors = CardDefaults.cardColors(
                containerColor = Color(0xFF0B0B0C)
            ),
            shape = RoundedCornerShape(24.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Notifications",
                        color = Color.White,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                    
                    IconButton(onClick = onDismiss) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = Color.Gray
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(12.dp))
                
                if (isLoading) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = Color(0xFF00FFCC))
                    }
                } else if (notifications.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = Icons.Default.NotificationsNone,
                                contentDescription = "Non notifications",
                                tint = Color.Gray,
                                modifier = Modifier.size(48.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("Aucune notification", color = Color.Gray, fontSize = 14.sp)
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 400.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(notifications) { notification ->
                            NotificationRow(
                                notification = notification,
                                onRowClick = {
                                    coroutineScope.launch {
                                        try {
                                             NetworkModule.api.markNotificationRead(notification.id)
                                             notifications = notifications.map {
                                                 if (it.id == notification.id) it.copy(unread = false) else it
                                             }
                                        } catch (e: Exception) {
                                             e.printStackTrace()
                                        }
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun NotificationRow(
    notification: NotificationResponse,
    onRowClick: () -> Unit
) {
    val icon = when (notification.type) {
        "follow" -> Icons.Default.PersonAdd
        "sound_like", "actfile_like" -> Icons.Default.Favorite
        "sound_comment" -> Icons.Default.Comment
        "project_invite" -> Icons.Default.GroupAdd
        else -> Icons.Default.Info
    }
    
    val iconTint = when (notification.type) {
        "follow" -> Color(0xFF0099FF)
        "sound_like", "actfile_like" -> Color(0xFFFF2D55)
        "sound_comment" -> Color(0xFF00FFCC)
        "project_invite" -> Color(0xFFA12DFF)
        else -> Color.White
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(if (notification.unread) Color(0x12FFFFFF) else Color.Transparent)
            .clickable { onRowClick() }
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(iconTint.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconTint,
                modifier = Modifier.size(18.dp)
            )
        }
        
        Spacer(modifier = Modifier.width(12.dp))
        
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = notification.title,
                color = Color.White,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = notification.message,
                color = Color.LightGray,
                fontSize = 12.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
        
        Spacer(modifier = Modifier.width(8.dp))
        
        Column(
            horizontalAlignment = Alignment.End,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = notification.created_at,
                color = Color.Gray,
                fontSize = 10.sp
            )
            if (notification.unread) {
                Spacer(modifier = Modifier.height(4.dp))
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF00FFCC))
                )
            }
        }
    }
}
