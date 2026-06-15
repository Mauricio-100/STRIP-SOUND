package com.example.domain.model

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class Sound(
    val id: String,
    val title: String,
    val cover_url: String? = null,
    val category: String,
    val plays: Int = 0,
    val plays_count: Int = 0,
    val likes_count: Int = 0,
    val user_id: String? = null,
    val author_id: String? = null,
    val username: String? = null,
    val author_username: String? = null,
    val avatar_url: String? = null,
    val is_verified: Boolean = false,
    val author_is_verified: Boolean = false,
    val audio_url: String? = null // if available from recommendation vs details
)

@JsonClass(generateAdapter = true)
data class VideoResponse(
    val id: String,
    val video_url: String,
    val thumbnail_url: String,
    val description: String,
    val likes: Int = 0,
    val views: Int = 0,
    val duration: Float? = null,
    val created_at: String? = null,
    val user_id: String,
    val username: String,
    val avatar_url: String? = null,
    val is_verified: Boolean = false,
    val liked: Boolean = false
)

@JsonClass(generateAdapter = true)
data class UserResponse(
    val id: String,
    val username: String,
    val avatar_url: String? = null,
    val bio: String? = null,
    val is_verified: Boolean = false,
    val followers_count: Int = 0,
    val following_count: Int = 0,
    val is_following: Boolean = false,
    val total_wings: Int = 0,
    val total_sounds: Int = 0,
    val total_audio_plays: Int = 0,
    val total_audio_likes: Int = 0
)

@JsonClass(generateAdapter = true)
data class Wing(
    val wing_id: String,
    val thumbnail_url: String,
    val video_url: String,
    val description: String,
    val views_count: Int = 0,
    val likes_count: Int = 0,
    val created_at: String? = null,
    val sound_id: String? = null,
    val sound_title: String? = null,
    val sound_cover: String? = null,
    val sound_category: String? = null
)

@JsonClass(generateAdapter = true)
data class UserWingsResponse(
    val user_id: String,
    val page: Int,
    val wings: List<Wing>
)

@JsonClass(generateAdapter = true)
data class Comment(
    val id: String,
    val user_id: String,
    val sound_id: String? = null,
    @com.squareup.moshi.Json(name = "content") val text: String,
    val created_at: String? = null,
    val username: String? = null,
    val user: UserResponse? = null
)

@JsonClass(generateAdapter = true)
data class CommentRequest(
    @com.squareup.moshi.Json(name = "content") val text: String
)

@JsonClass(generateAdapter = true)
data class UploadResponse(
    val status: String,
    val sound_id: String
)

@JsonClass(generateAdapter = true)
data class StoryResponse(
    val id: String,
    val media_url: String,
    val media_type: String,
    val created_at: String,
    val user: UserResponse
)
