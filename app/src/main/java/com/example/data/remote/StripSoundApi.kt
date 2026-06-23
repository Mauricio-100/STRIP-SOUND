package com.example.data.remote

import com.example.domain.model.*
import retrofit2.http.*

interface StripSoundApi {
    @GET("projects")
    suspend fun getProjects(): List<ProjectResponse>

    @GET("projects/{id}")
    suspend fun getProjectDetails(@Path("id") id: String): ProjectResponse

    @GET("search")
    suspend fun searchUsers(
        @Query("q") query: String,
        @Query("type") type: String? = null,
        @Query("limit") limit: Int? = null
    ): SearchResponseUsers

    @POST("projects")
    suspend fun createProject(@Body project: ProjectCreate): ProjectResponse

    @POST("projects/{projectId}/collaborators")
    suspend fun addProjectCollaborator(
        @Path("projectId") projectId: String,
        @Body collaborator: CollaboratorAdd
    )

    @GET("sounds/recommendations")
    suspend fun getRecommendedSounds(@Query("limit") limit: Int? = null): List<Sound>

    @GET("search")
    suspend fun search(
        @Query("q") query: String,
        @Query("type") type: String,
        @Query("limit") limit: Int
    ): SearchResponse

    @GET("stories")
    suspend fun getActiveStories(): List<StoryResponse>

    @DELETE("sounds/{id}")
    suspend fun deleteSound(@Path("id") id: String)

    @POST("sounds/{id}/like")
    suspend fun likeSound(@Path("id") id: String): LikeResponse

    @GET("sounds/{id}/likes/count")
    suspend fun getSoundLikesCount(@Path("id") id: String): SoundLikesCountResponse

    @GET("sounds/{id}/short")
    suspend fun getSoundShort(@Path("id") id: String): SoundShortResponse

    @GET("sounds/{id}/comments/count")
    suspend fun getSoundCommentsCount(@Path("id") id: String): SoundCommentsCountResponse

    @GET("sounds/{id}")
    suspend fun getSoundDetails(@Path("id") id: String): SoundDetailsResponse

    @POST("users/{id}/unfollow")
    suspend fun unfollowUser(@Path("id") id: String)

    @POST("users/{id}/follow")
    suspend fun followUser(@Path("id") id: String)

    @GET("sounds/{id}/comments")
    suspend fun getComments(@Path("id") id: String): List<Comment>

    @POST("sounds/{id}/comments")
    suspend fun postComment(
        @Path("id") id: String,
        @Body comment: CommentRequest
    ): Comment

    @GET("users/{id}/sounds")
    suspend fun getUserSounds(@Path("id") id: String): List<Sound>

    @GET("profile/{id}/full")
    suspend fun getFullUserProfile(@Path("id") id: String): UserProfileFull
    
    @GET("users/{id}/profile")
    suspend fun getUserProfile(@Path("id") id: String): UserResponse

    @GET("notifications")
    suspend fun getNotifications(
        @Query("unreadOnly") unreadOnly: Boolean,
        @Query("limit") limit: Int
    ): List<NotificationResponse>

    @POST("notifications/{id}/read")
    suspend fun markNotificationRead(@Path("id") id: String)

    @POST("sounds/{id}/play")
    suspend fun incrementSoundPlay(@Path("id") id: String)

    @POST("videos/{id}/view")
    suspend fun incrementVideoView(@Path("id") id: String)

    @FormUrlEncoded
    @POST("token")
    suspend fun login(
        @Field("username") username: String,
        @Field("password") password: String
    ): LoginResponse

    @POST("users/register")
    suspend fun register(
        @Body user: RegisterRequest
    ): RegisterResponse

    @Multipart
    @POST("projects/{id}/cover")
    suspend fun updateProjectCover(
        @Path("id") id: String,
        @Part cover_file: okhttp3.MultipartBody.Part
    ): ProjectResponse

    @Multipart
    @POST("sounds/upload")
    suspend fun uploadSound(
        @Part audio_file: okhttp3.MultipartBody.Part,
        @Part cover_file: okhttp3.MultipartBody.Part?,
        @Part("title") title: okhttp3.RequestBody,
        @Part("description") description: okhttp3.RequestBody,
        @Part("category") category: okhttp3.RequestBody,
        @Part("tags") tags: okhttp3.RequestBody?,
        @Part("status") status: okhttp3.RequestBody?
    ): UploadSoundResponse
}
