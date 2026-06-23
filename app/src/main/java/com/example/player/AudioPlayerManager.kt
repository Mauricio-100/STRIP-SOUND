package com.example.player

import android.content.Context
import android.os.Handler
import android.os.Looper
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import com.example.domain.model.Sound
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class AudioPlayerManager(private val context: Context) {

    val player: ExoPlayer = ExoPlayer.Builder(context.applicationContext).build()

    private val _currentSound = MutableStateFlow<Sound?>(null)
    val currentSound: StateFlow<Sound?> = _currentSound.asStateFlow()

    private val _playlist = MutableStateFlow<List<Sound>>(emptyList())
    val playlist: StateFlow<List<Sound>> = _playlist.asStateFlow()

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private val _currentPosition = MutableStateFlow(0L)
    val currentPosition: StateFlow<Long> = _currentPosition.asStateFlow()

    private val _duration = MutableStateFlow(0L)
    val duration: StateFlow<Long> = _duration.asStateFlow()

    private val _volume = MutableStateFlow(1.0f)
    val volume: StateFlow<Float> = _volume.asStateFlow()

    private val _currentTrack = MutableStateFlow<MediaItem?>(null)
    val currentTrack: StateFlow<MediaItem?> = _currentTrack.asStateFlow()

    val deviceDetector = AudioDeviceDetector(context)
    val webBluetoothManager = WebBluetoothManager(context)

    private val mainHandler = Handler(Looper.getMainLooper())
    private var progressRunnable: Runnable? = null
    
    init {
        mainHandler.post {
            player.addListener(object : Player.Listener {
                override fun onIsPlayingChanged(playing: Boolean) {
                    _isPlaying.value = playing
                    if (playing) {
                        startProgressUpdates()
                    } else {
                        stopProgressUpdates()
                    }
                }

                override fun onPlaybackStateChanged(state: Int) {
                    _duration.value = if (player.duration > 0) player.duration else 0L
                    if (state == Player.STATE_ENDED) {
                        _currentPosition.value = 0L
                    }
                }
            })
        }
    }

    fun playTrack(url: String, sound: Sound? = null, itemMetadata: Any? = null) {
        mainHandler.post {
            val builder = MediaItem.Builder()
                .setUri(url)
                .setRequestMetadata(
                    MediaItem.RequestMetadata.Builder()
                        .setMediaUri(android.net.Uri.parse(url))
                        .build()
                )
            val mediaItem = builder.build()
            _currentTrack.value = mediaItem
            _currentSound.value = sound ?: Sound(
                id = url.hashCode().toString(),
                title = "Online Audio Track",
                category = "Streaming",
                audio_url = url
            )
            player.setMediaItem(mediaItem)
            player.prepare()
            player.play()
        }
    }

    fun playTrack(url: String, sound: Sound) {
        playTrack(url, sound, null)
    }

    fun playTrack(url: String) {
        playTrack(url, null, null)
    }

    fun seekTo(positionMs: Long) {
        mainHandler.post {
            player.seekTo(positionMs)
            _currentPosition.value = positionMs
        }
    }

    fun setPlaybackSpeed(speed: Float) {
        mainHandler.post {
            player.setPlaybackSpeed(speed)
        }
    }

    fun togglePlayPause() {
        mainHandler.post {
            if (player.isPlaying) {
                player.pause()
            } else {
                player.play()
            }
        }
    }

    fun setVolume(vol: Float) {
        mainHandler.post {
            player.volume = vol
            _volume.value = vol
        }
    }

    private fun startProgressUpdates() {
        stopProgressUpdates()
        progressRunnable = object : Runnable {
            override fun run() {
                _currentPosition.value = player.currentPosition
                _duration.value = if (player.duration > 0) player.duration else 0L
                mainHandler.postDelayed(this, 250)
            }
        }
        mainHandler.post(progressRunnable!!)
    }

    private fun stopProgressUpdates() {
        progressRunnable?.let { mainHandler.removeCallbacks(it) }
        progressRunnable = null
    }

    fun setPlaylist(list: List<Sound>) {
        _playlist.value = list
    }

    fun playNext() {
        val current = _currentSound.value ?: return
        val list = _playlist.value
        if (list.isEmpty()) return
        val currentIndex = list.indexOfFirst { it.id == current.id }
        if (currentIndex != -1 && currentIndex < list.size - 1) {
            val nextSound = list[currentIndex + 1]
            nextSound.audio_url?.let { playTrack(it, nextSound) }
        } else if (list.isNotEmpty()) {
            val nextSound = list.first()
            nextSound.audio_url?.let { playTrack(it, nextSound) }
        }
    }

    fun playPrevious() {
        val current = _currentSound.value ?: return
        val list = _playlist.value
        if (list.isEmpty()) return
        val currentIndex = list.indexOfFirst { it.id == current.id }
        if (currentIndex > 0) {
            val prevSound = list[currentIndex - 1]
            prevSound.audio_url?.let { playTrack(it, prevSound) }
        } else if (list.isNotEmpty()) {
            val prevSound = list.last()
            prevSound.audio_url?.let { playTrack(it, prevSound) }
        }
    }

    fun release() {
        mainHandler.post {
            stopProgressUpdates()
            player.release()
        }
    }
}
