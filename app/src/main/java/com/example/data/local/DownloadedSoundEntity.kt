package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "downloaded_sounds")
data class DownloadedSoundEntity(
    @PrimaryKey val id: String,
    val title: String,
    val category: String,
    val coverUrl: String?,
    val authorName: String?,
    val localFilePath: String, // The path to the downloaded audio file
    val downloadedAt: Long = System.currentTimeMillis()
)
