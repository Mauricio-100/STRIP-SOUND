package com.example.player

import android.content.ComponentName
import android.content.Context
import androidx.core.content.ContextCompat
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.google.common.util.concurrent.ListenableFuture
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.isActive
import kotlinx.coroutines.delay

class AudioPlayerManager(private val context: Context) {

    private var controllerFuture: ListenableFuture<MediaController>? = null
    var player: Player? = null
        private set

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private val _playbackState = MutableStateFlow(Player.STATE_IDLE)
    val playbackState: StateFlow<Int> = _playbackState.asStateFlow()

    private val _currentTrack = MutableStateFlow<MediaItem?>(null)
    val currentTrack: StateFlow<MediaItem?> = _currentTrack.asStateFlow()

    private val _currentSound = MutableStateFlow<com.example.domain.model.Sound?>(null)
    val currentSound: StateFlow<com.example.domain.model.Sound?> = _currentSound.asStateFlow()

    private val _currentPosition = MutableStateFlow(0L)
    val currentPosition: StateFlow<Long> = _currentPosition.asStateFlow()

    private val _duration = MutableStateFlow(0L)
    val duration: StateFlow<Long> = _duration.asStateFlow()

    private val _volume = MutableStateFlow(1f)
    val volume: StateFlow<Float> = _volume.asStateFlow()

    private val playerScope = kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.Main + kotlinx.coroutines.SupervisorJob())
    private var progressJob: kotlinx.coroutines.Job? = null

    private fun startProgressUpdate() {
        progressJob?.cancel()
        progressJob = playerScope.launch {
            while (isActive) {
                _currentPosition.value = player?.currentPosition ?: 0L
                _duration.value = player?.duration?.coerceAtLeast(0L) ?: 0L
                delay(250)
            }
        }
    }

    private fun stopProgressUpdate() {
        progressJob?.cancel()
    }

    init {
        val attributionContext = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
            context.createAttributionContext("AudioService")
        } else {
            context
        }
        val sessionToken = SessionToken(attributionContext, ComponentName(attributionContext, AudioService::class.java))
        controllerFuture = MediaController.Builder(attributionContext, sessionToken).buildAsync()
        controllerFuture?.addListener(
            {
                player = controllerFuture?.get()
                _volume.value = player?.volume ?: 1f
                player?.addListener(object : Player.Listener {
                    override fun onPlaybackStateChanged(playbackState: Int) {
                        _playbackState.value = playbackState
                        if (playbackState == Player.STATE_READY) {
                            _duration.value = player?.duration?.coerceAtLeast(0L) ?: 0L
                        }
                    }

                    override fun onIsPlayingChanged(isPlaying: Boolean) {
                        _isPlaying.value = isPlaying
                        if (isPlaying) {
                            startProgressUpdate()
                        } else {
                            stopProgressUpdate()
                        }
                    }
                })
            },
            ContextCompat.getMainExecutor(context)
        )
    }

    fun playTrack(url: String, sound: com.example.domain.model.Sound? = null, metadata: MediaItem.RequestMetadata? = null, itemMetadata: androidx.media3.common.MediaMetadata? = null) {
        sound?.let { _currentSound.value = it }
        val mediaItem = MediaItem.Builder()
            .setUri(url)
            .setRequestMetadata(metadata ?: MediaItem.RequestMetadata.EMPTY)
            .setMediaMetadata(itemMetadata ?: androidx.media3.common.MediaMetadata.EMPTY)
            .build()
        _currentTrack.value = mediaItem
        
        player?.setMediaItem(mediaItem)
        player?.prepare()
        player?.play()
    }
    
    fun togglePlayPause() {
        if (player?.isPlaying == true) {
            player?.pause()
        } else {
            player?.play()
        }
    }
    
    fun seekTo(positionMs: Long) {
        player?.seekTo(positionMs)
        _currentPosition.value = positionMs
    }

    fun setVolume(vol: Float) {
        val clamped = vol.coerceIn(0f, 1f)
        player?.volume = clamped
        _volume.value = clamped
    }

    fun release() {
        progressJob?.cancel()
        controllerFuture?.let { MediaController.releaseFuture(it) }
    }
}
