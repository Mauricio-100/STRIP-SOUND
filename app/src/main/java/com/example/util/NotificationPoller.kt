package com.example.util

import android.content.Context
import android.util.Log
import com.example.data.local.AuthManager
import com.example.data.remote.NetworkModule
import com.example.domain.model.NotificationResponse
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class NotificationPoller(
    private val context: Context,
    private val authManager: AuthManager
) {
    private val notificationManager = CustomNotificationManager(context)
    private var pollJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.IO + Job())
    
    // Store processed notification IDs to avoid duplicate alerts 
    // when unread_only backend mechanism doesn't immediately mark as read
    private val processedIds = mutableSetOf<String>()

    fun startPolling() {
        if (pollJob?.isActive == true) return
        
        pollJob = scope.launch {
            while (isActive) {
                try {
                    val token = authManager.getToken()
                    if (token != null) {
                        val newNotifications = NetworkModule.api.getNotifications(unreadOnly = true, limit = 10)
                        
                        newNotifications.forEach { notification ->
                            if (!processedIds.contains(notification.id)) {
                                processedIds.add(notification.id)
                                showNativeNotification(notification)
                            }
                        }
                    }
                } catch (e: Exception) {
                    Log.e("NotificationPoller", "Error polling notifications: ${e.message}")
                }
                delay(10_000) // Poll every 10 seconds while the app is active
            }
        }
    }

    fun stopPolling() {
        pollJob?.cancel()
        pollJob = null
    }

    private fun showNativeNotification(notification: NotificationResponse) {
        val title = when (notification.type) {
            "follow" -> "STRIP SOUND - Nouvel Abonné 🎉"
            "message" -> "STRIP SOUND - Nouveau message 💬"
            "project_invite" -> "STRIP SOUND - Invitation 🎤"
            "sound_like" -> "STRIP SOUND - Nouveau Like ❤️"
            "sound_comment" -> "STRIP SOUND - Nouveau Commentaire 💬"
            "actfile_like" -> "STRIP SOUND - Nouveau Like ❤️"
            else -> "STRIP SOUND - Nouvelle Activité"
        }
        
        notificationManager.showActivityNotification(
            title = title,
            message = notification.message
        )
    }
}
