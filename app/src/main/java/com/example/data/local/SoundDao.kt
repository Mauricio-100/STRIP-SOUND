package com.example.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface SoundDao {
    @Query("SELECT * FROM downloaded_sounds ORDER BY downloadedAt DESC")
    fun getAllDownloadedSounds(): Flow<List<DownloadedSoundEntity>>

    @Query("SELECT * FROM downloaded_sounds WHERE id = :soundId")
    suspend fun getDownloadedSound(soundId: String): DownloadedSoundEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(sound: DownloadedSoundEntity)

    @Delete
    suspend fun delete(sound: DownloadedSoundEntity)

    @Query("DELETE FROM downloaded_sounds WHERE id = :soundId")
    suspend fun deleteById(soundId: String)
}
