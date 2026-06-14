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

    init {
        val sessionToken = SessionToken(context, ComponentName(context, AudioService::class.java))
        controllerFuture = MediaController.Builder(context, sessionToken).buildAsync()
        controllerFuture?.addListener(
            {
                player = controllerFuture?.get()
                player?.addListener(object : Player.Listener {
                    override fun onPlaybackStateChanged(playbackState: Int) {
                        _playbackState.value = playbackState
                    }

                    override fun onIsPlayingChanged(isPlaying: Boolean) {
                        _isPlaying.value = isPlaying
                    }
                })
            },
            ContextCompat.getMainExecutor(context)
        )
    }

    fun playTrack(url: String, metadata: MediaItem.RequestMetadata? = null, itemMetadata: androidx.media3.common.MediaMetadata? = null) {
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

    fun release() {
        controllerFuture?.let { MediaController.releaseFuture(it) }
    }
}
