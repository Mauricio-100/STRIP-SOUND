package com.example.data.remote

import android.util.Log
import com.example.domain.model.ProjectEvent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import okhttp3.*
import org.json.JSONObject
import java.util.concurrent.TimeUnit

class CollaborationWebSocketManager(private val tokenProvider: () -> String?) {

    // Using the wss endpoint matching the correct backend server
    private val WEBSOCKET_URL = "wss://hoosthubs-g.onrender.com/ws/"
    private var webSocket: WebSocket? = null
    private val client = OkHttpClient.Builder()
        .pingInterval(30, TimeUnit.SECONDS)
        .build()

    private val _events = MutableSharedFlow<ProjectEvent>(replay = 1)
    val events: SharedFlow<ProjectEvent> = _events.asSharedFlow()
    
    private val scope = CoroutineScope(Dispatchers.IO + Job())
    private var isConnected = false
    private var currentUserId: String? = null
    
    // Exponential backoff parameters
    private var reconnectAttempt = 0
    private val MAX_RECONNECT_DELAY = 60000L // 60 seconds
    private val INITIAL_RECONNECT_DELAY = 1000L // 1 second

    fun connect(userId: String) {
        if (isConnected) return
        currentUserId = userId
        val url = "$WEBSOCKET_URL$userId"

        val request = Request.Builder()
            .url(url)
            .build()

        webSocket = client.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                isConnected = true
                reconnectAttempt = 0 // Reset attempt counter on successful connection
                Log.d("WebSocket", "Connected")
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                Log.d("WebSocket", "Message received: $text")
                try {
                    val json = JSONObject(text)
                    val type = json.optString("type")
                    when (type) {
                        "project_updated" -> {
                            val by = json.optString("by")
                            val changes = json.optJSONObject("changes")?.toString() ?: ""
                            scope.launch {
                                _events.emit(ProjectEvent.ProjectUpdated(by, changes))
                            }
                        }
                        "playlist_updated" -> {
                            val addedBy = json.optString("added_by")
                            val soundId = json.optString("sound_id")
                            scope.launch {
                                _events.emit(ProjectEvent.PlaylistUpdated(addedBy, soundId))
                            }
                        }
                        "voice_message" -> {
                            val senderId = json.optString("sender_id", json.optString("user_id", "anonymous"))
                            val senderName = json.optString("sender_name", json.optString("username", "Collaborateur"))
                            val audioUrl = json.optString("audio_url", "")
                            if (audioUrl.isNotEmpty()) {
                                scope.launch {
                                    _events.emit(ProjectEvent.VoiceMessageReceived(senderId, senderName, audioUrl))
                                }
                            }
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }

            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                isConnected = false
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                isConnected = false
                Log.e("WebSocket", "Failure: ${t.message}")
                // Auto reconnect with exponential backoff
                scope.launch {
                    val backoffDelay = (INITIAL_RECONNECT_DELAY * Math.pow(2.0, reconnectAttempt.toDouble())).toLong()
                    val nextDelay = minOf(backoffDelay, MAX_RECONNECT_DELAY)
                    reconnectAttempt++
                    Log.d("WebSocket", "Reconnecting in ${nextDelay}ms (Attempt $reconnectAttempt)")
                    delay(nextDelay)
                    if (currentUserId != null) {
                        connect(currentUserId!!)
                    }
                }
            }
        })
    }

    fun sendVoiceMessage(receiverId: String, audioDataBase64: String) {
        val payload = JSONObject().apply {
            put("type", "voice_message")
            put("receiver_id", receiverId)
            put("audio_data", audioDataBase64)
        }
        webSocket?.send(payload.toString())
    }

    fun joinProject(projectId: String) {
        val payload = JSONObject().apply {
            put("type", "join_project")
            put("project_id", projectId)
        }
        webSocket?.send(payload.toString())
    }

    fun leaveProject(projectId: String) {
        val payload = JSONObject().apply {
            put("type", "leave_project")
            put("project_id", projectId)
        }
        webSocket?.send(payload.toString())
    }

    fun sendProjectUpdate(projectId: String, changes: String) {
        val payload = JSONObject().apply {
            put("type", "project_update")
            put("project_id", projectId)
            put("changes", JSONObject(changes))
        }
        webSocket?.send(payload.toString())
    }

    fun disconnect() {
        webSocket?.close(1000, "User disconnected")
        webSocket = null
        isConnected = false
        currentUserId = null
    }
}
