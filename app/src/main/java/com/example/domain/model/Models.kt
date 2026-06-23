package com.example.domain.model

data class Sound(
    val id: String = "",
    val title: String = "",
    val category: String = "",
    val cover_url: String? = null,
    val username: String = "Unknown",
    val author_username: String? = null,
    val audio_url: String? = null,
    val plays_count: Int = 0,
    val likes_count: Int = 0,
    val comments_count: Int = 0,
    val created_at: String = "",
    val is_verified_agent: Boolean = false,
    val agent_signature: String? = null,
    val user_id: String? = null,
    val author_id: String? = null,
    val avatar_url: String? = null,
    val is_verified: Boolean = false,
    val author_is_verified: Boolean = false,
    val plays: Int = 0,
    val duration: Float = 0f,
    val description: String? = null
)

data class VideoResponse(
    val id: String = "",
    val video_url: String = "",
    val thumbnail_url: String = "",
    val description: String = "",
    val user_id: String = "",
    val username: String = "User",
    val avatar_url: String? = null,
    val is_verified: Boolean = false,
    val created_at: String = "",
    val views: Int = 0
)

data class UserProfileFull(
    val id: String,
    val username: String,
    val avatar_url: String?,
    val bio: String?,
    val is_verified: Boolean,
    val followers_count: Int,
    val following_count: Int,
    val total_wings: Int,
    val total_sounds: Int,
    val total_audio_plays: Int,
    val total_audio_likes: Int,
    val is_following: Boolean
)

data class UserResponse(
    val id: String = "",
    val username: String = "",
    val avatar_url: String? = null,
    val bio: String? = null,
    val is_verified: Boolean = false,
    val followers_count: Int = 0,
    val total_sounds: Int = 0,
    val zodiac_sign: String? = null,
    val total_audio_plays: Int = 0,
    val total_audio_likes: Int = 0,
    val is_following: Boolean = false
)

data class LoginResponse(
    val access_token: String,
    val token_type: String,
    val user_id: String,
    val username: String,
    val avatar_url: String?,
    val is_verified: Boolean
)

data class RegisterRequest(
    val username: String,
    val password: String,
    val email: String? = null,
    val phone_number: String? = null
)

data class RegisterResponse(
    val id: String,
    val username: String,
    val email: String?,
    val avatar_url: String?,
    val created_at: String
)

data class UploadSoundResponse(
    val status: String,
    val sound_id: String
)

data class ProjectResponse(
    val id: String = "",
    val title: String = "",
    val description: String = "",
    val cover_url: String? = null,
    val created_at: String = "",
    val listeners_count: Int = 0,
    val visitors_count: Int = 0,
    val likes_count: Int = 0,
    val comments_count: Int = 0,
    val followers_count: Int = 0
)

data class StoryResponse(
    val id: String = "",
    val media_url: String = "",
    val media_type: String = "image",
    val user: UserResponse = UserResponse(),
    val created_at: String = ""
)

data class Comment(
    val id: String = "",
    val username: String? = null,
    val text: String = "",
    val created_at: String = "",
    val content: String? = null,
    val avatar_url: String? = null,
    val is_verified: Boolean = false,
    val user_id: String? = null
)

data class CommentRequest(
    val text: String
)

data class SoundDetailsResponse(
    val sound: Sound = Sound(),
    val videos: List<VideoResponse> = emptyList()
)

data class ProjectCreate(
    val title: String,
    val baseSoundId: String? = null
)

data class CollaboratorAdd(
    val userId: String,
    val role: String = "editor"
)

data class LikeResponse(
    val liked: Boolean
)

data class NotificationResponse(
    val id: String = "",
    val title: String = "",
    val message: String = "",
    val unread: Boolean = true,
    val created_at: String = "",
    val type: String = ""
)

data class SearchMetadata(
    val agent_signature: String? = null,
    val is_verified_agent: Boolean = false
)

data class SearchResponse(
    val results: List<Sound> = emptyList(),
    val metadata: SearchMetadata = SearchMetadata()
)

data class SearchResponseUsers(
    val results: List<UserResponse> = emptyList(),
    val metadata: SearchMetadata = SearchMetadata()
)

sealed class ProjectEvent {
    data class ProjectUpdated(val by: String, val changesJson: String) : ProjectEvent()
    data class PlaylistUpdated(val addedBy: String, val soundId: String) : ProjectEvent()
    data class VoiceMessageReceived(val senderId: String, val senderName: String, val audioUrl: String) : ProjectEvent()
}

data class SoundLikesCountResponse(
    val sound_id: String,
    val likes_count: Int,
    val has_liked: Boolean
)

data class VerifiedUserInfo(
    val id: String,
    val username: String
)

data class VerifiedCommentersData(
    val verified_users_count: Int,
    val verified_users: List<VerifiedUserInfo>
)

data class SoundCommentsCountResponse(
    val sound_id: String,
    val total_comments: Int,
    val unique_commenters_count: Int,
    val is_identifier_count_id_and_name_is_verified: VerifiedCommentersData
)

data class SoundShortResponse(
    val sound_id: String,
    val name: String,
    val covers: String?,
    val ondes_des_sound: String?,
    val creator_is_verified: Boolean,
    val tags: String?,
    val mini_description: String?,
    val status: String?,
    val sound_ecoute_voila: String
)
