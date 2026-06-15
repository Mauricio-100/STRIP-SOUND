package com.example.util

import android.content.Context
import android.util.Log
import com.example.data.remote.NetworkModule
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

object UniqueViewsTracker {
    private val scope = CoroutineScope(Dispatchers.IO)

    fun trackSoundPlay(context: Context, soundId: String) {
        val prefs = context.applicationContext.getSharedPreferences("unique_views_prefs", Context.MODE_PRIVATE)
        val key = "sound_$soundId"
        if (!prefs.getBoolean(key, false)) {
            prefs.edit().putBoolean(key, true).apply()
            scope.launch {
                try {
                    NetworkModule.api.incrementSoundPlay(soundId)
                    Log.d("UniqueViewsTracker", "Successfully sent unique play for sound $soundId")
                } catch (e: Exception) {
                    Log.e("UniqueViewsTracker", "Error sending play for sound $soundId: ${e.message}")
                }
            }
        }
    }

    fun trackVideoView(context: Context, videoId: String) {
        val prefs = context.applicationContext.getSharedPreferences("unique_views_prefs", Context.MODE_PRIVATE)
        val key = "video_$videoId"
        if (!prefs.getBoolean(key, false)) {
            prefs.edit().putBoolean(key, true).apply()
            scope.launch {
                try {
                    NetworkModule.api.incrementVideoView(videoId)
                    Log.d("UniqueViewsTracker", "Successfully sent unique view for video $videoId")
                } catch (e: Exception) {
                    Log.e("UniqueViewsTracker", "Error sending view for video $videoId: ${e.message}")
                }
            }
        }
    }
}
