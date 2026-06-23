package com.example.player

import androidx.annotation.OptIn
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.util.UnstableApi
import androidx.media3.database.StandaloneDatabaseProvider
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.datasource.cache.CacheDataSource
import androidx.media3.datasource.cache.LeastRecentlyUsedCacheEvictor
import androidx.media3.datasource.cache.SimpleCache
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import java.io.File

@OptIn(UnstableApi::class)
class AudioService : MediaSessionService() {

    private var mediaSession: MediaSession? = null

    companion object {
        private var simpleCache: SimpleCache? = null

        fun getCache(context: android.content.Context): SimpleCache {
            if (simpleCache == null) {
                val cacheDir = File(context.cacheDir, "media")
                val evictor = LeastRecentlyUsedCacheEvictor(100 * 1024 * 1024) // 100MB
                simpleCache = SimpleCache(
                    cacheDir,
                    evictor,
                    StandaloneDatabaseProvider(context)
                )
            }
            return simpleCache!!
        }
        
        fun releaseCache() {
            simpleCache?.release()
            simpleCache = null
        }
    }

    override fun onCreate() {
        super.onCreate()
        
        val attributionContext = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
            createAttributionContext("audio")
        } else {
            this
        }
        
        val dataSourceFactory = DefaultDataSource.Factory(attributionContext)
        val cacheDataSourceFactory = CacheDataSource.Factory()
            .setCache(getCache(attributionContext))
            .setUpstreamDataSourceFactory(dataSourceFactory)
            .setFlags(CacheDataSource.FLAG_IGNORE_CACHE_ON_ERROR)

        val player = ExoPlayer.Builder(attributionContext)
            .setMediaSourceFactory(DefaultMediaSourceFactory(attributionContext).setDataSourceFactory(cacheDataSourceFactory))
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
                    .setUsage(C.USAGE_MEDIA)
                    .build(),
                true // True handles audio focus automatically
            )
            .build()
            
        mediaSession = MediaSession.Builder(this, player).build()
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? {
        return mediaSession
    }

    override fun onDestroy() {
        mediaSession?.player?.release()
        mediaSession?.release()
        mediaSession = null
        releaseCache()
        super.onDestroy()
    }
}
