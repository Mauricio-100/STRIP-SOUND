package com.example.data.local

import android.content.Context
import androidx.room.*

@Entity(tableName = "downloaded_sounds")
data class DownloadedSoundEntity(
    @PrimaryKey val id: String = "",
    val title: String = "",
    val category: String = "",
    val coverUrl: String? = null,
    val authorName: String = "",
    val localFilePath: String = ""
)

@Dao
interface SoundDao {
    @Query("SELECT * FROM downloaded_sounds")
    suspend fun getAllDownloadedSounds(): List<DownloadedSoundEntity>

    @Query("SELECT * FROM downloaded_sounds WHERE id = :id LIMIT 1")
    suspend fun getDownloadedSound(id: String): DownloadedSoundEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(sound: DownloadedSoundEntity)

    @Query("DELETE FROM downloaded_sounds WHERE id = :id")
    suspend fun deleteById(id: String)
}

@Database(entities = [DownloadedSoundEntity::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun soundDao(): SoundDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "strip_sound_database"
                ).fallbackToDestructiveMigration().build()
                INSTANCE = instance
                instance
            }
        }
    }
}
