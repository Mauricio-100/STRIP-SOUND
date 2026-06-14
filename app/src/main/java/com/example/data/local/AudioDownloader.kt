package com.example.data.local

import android.content.Context
import com.example.domain.model.Sound
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream

class AudioDownloader(
    private val context: Context,
    private val appDatabase: AppDatabase
) {
    private val okHttpClient = OkHttpClient()

    suspend fun downloadSound(sound: Sound) {
        if (sound.audio_url == null) return
        
        withContext(Dispatchers.IO) {
            try {
                val request = Request.Builder().url(sound.audio_url).build()
                val response = okHttpClient.newCall(request).execute()
                
                if (response.isSuccessful) {
                    val input: InputStream? = response.body?.byteStream()
                    val fileName = "sound_${sound.id}.mp3" // Simplify extension for now
                    val file = File(context.filesDir, fileName)
                    
                    val output = FileOutputStream(file)
                    input?.use { inp ->
                        output.use { out ->
                            inp.copyTo(out)
                        }
                    }
                    
                    // Save to database
                    val entity = DownloadedSoundEntity(
                        id = sound.id,
                        title = sound.title,
                        category = sound.category,
                        coverUrl = sound.cover_url,
                        authorName = sound.username,
                        localFilePath = file.absolutePath
                    )
                    appDatabase.soundDao().insert(entity)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
    
    suspend fun isDownloaded(soundId: String): Boolean {
        return withContext(Dispatchers.IO) {
            appDatabase.soundDao().getDownloadedSound(soundId) != null
        }
    }
    
    suspend fun deleteSound(soundId: String) {
        withContext(Dispatchers.IO) {
            val entity = appDatabase.soundDao().getDownloadedSound(soundId)
            if (entity != null) {
                val file = File(entity.localFilePath)
                if (file.exists()) {
                    file.delete()
                }
                appDatabase.soundDao().deleteById(soundId)
            }
        }
    }
}
