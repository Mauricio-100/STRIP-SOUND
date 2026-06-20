package com.example.data.remote

import com.example.domain.model.Sound
import com.example.domain.model.VideoResponse
import com.example.domain.model.UserResponse
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.http.*
import com.squareup.moshi.JsonClass

interface StripSoundApi {
    @Multipart
    @POST("sounds/upload")
    suspend fun uploadSound(
        @Part audio_file: MultipartBody.Part,
        @Part cover_file: MultipartBody.Part?,
        @Part("title") title: RequestBody,
        @Part("description") description: RequestBody,
        @Part("category") category: RequestBody
    ): com.example.domain.model.UploadResponse

    @FormUrlEncoded
    @POST("token")
    suspend fun login(
        @Field("username") username: String,
        @Field("password") password: String
    ): com.example.domain.model.LoginResponse

    @POST("users/register")
    suspend fun register(@Body request: com.example.domain.model.RegisterRequest): com.example.domain.model.RegisterResponse

    @GET("users/me")
    suspend fun getMyProfile(): com.example.domain.model.UserResponse

    @POST("users/me/verify")
    suspend fun requestVerification(
        @Body criteria: com.example.domain.model.VerificationCriteria
    ): com.example.domain.model.VerificationResult

    @GET("feed/random")
    suspend fun getRandomVideos(@Query("limit") limit: Int = 10): List<VideoResponse>

    @GET("sounds/recommendations")
    suspend fun getRecommendedSounds(@Query("limit") limit: Int = 20): List<Sound>

    @GET("sounds/category/{category_name}")
    suspend fun getSoundsByCategory(
        @Path("category_name") categoryName: String,
        @Query("limit") limit: Int = 20
    ): List<Sound>

    @GET("sounds/{sound_id}/comments")
    suspend fun getComments(@Path("sound_id") soundId: String): List<com.example.domain.model.Comment>

    @POST("sounds/{sound_id}/comment")
    suspend fun postComment(
        @Path("sound_id") soundId: String,
        @Body request: com.example.domain.model.CommentRequest
    ): com.example.domain.model.Comment

    @POST("sounds/{sound_id}/like")
    suspend fun likeSound(@Path("sound_id") soundId: String): com.example.domain.model.LikeResponse

    @POST("users/{user_id}/follow")
    suspend fun followUser(@Path("user_id") userId: String)

    @POST("users/{user_id}/follow")
    suspend fun unfollowUser(@Path("user_id") userId: String)

    @GET("users/{user_id}")
    suspend fun getUserProfile(@Path("user_id") userId: String): com.example.domain.model.UserResponse

    @GET("profile/{user_id}/full")
    suspend fun getFullUserProfile(@Path("user_id") userId: String): com.example.domain.model.UserResponse

    @GET("profile/{user_id}/wings")
    suspend fun getUserWings(
        @Path("user_id") userId: String,
        @Query("page") page: Int = 1,
        @Query("limit") limit: Int = 15
    ): com.example.domain.model.UserWingsResponse

    @GET("users/{user_id}/sounds")
    suspend fun getUserSounds(@Path("user_id") userId: String): List<Sound>

    @POST("sounds/{sound_id}/play")
    suspend fun incrementSoundPlay(@Path("sound_id") soundId: String)

    @POST("videos/{video_id}/view")
    suspend fun incrementVideoView(@Path("video_id") videoId: String)

    @POST("videos/{video_id}/like")
    suspend fun likeVideo(@Path("video_id") videoId: String): com.example.domain.model.LikeResponse

    @GET("stories")
    suspend fun getActiveStories(): List<com.example.domain.model.StoryResponse>

    @Multipart
    @POST("stories")
    suspend fun uploadStory(
        @Part file: MultipartBody.Part,
        @Part("effect") effect: RequestBody? = null
    ): com.example.domain.model.StoryUploadResponse

    @GET("sounds/{sound_id}")
    suspend fun getSoundDetails(@Path("sound_id") soundId: String): com.example.domain.model.SoundDetailsResponse

    @GET("search")
    suspend fun search(
        @Query("q") query: String,
        @Query("type") type: String = "sounds",
        @Query("limit") limit: Int = 20
    ): com.example.domain.model.SearchResponse

    @GET("search")
    suspend fun searchUsers(
        @Query("q") query: String,
        @Query("type") type: String = "users",
        @Query("limit") limit: Int = 20
    ): com.example.domain.model.UserSearchResponse

    @DELETE("sounds/{sound_id}")
    suspend fun deleteSound(@Path("sound_id") soundId: String)
}
