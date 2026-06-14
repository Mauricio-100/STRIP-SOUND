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
    suspend fun likeSound(@Path("sound_id") soundId: String)

    @POST("sounds/{sound_id}/like")
    suspend fun unlikeSound(@Path("sound_id") soundId: String)

    @POST("users/{user_id}/follow")
    suspend fun followUser(@Path("user_id") userId: String)

    @POST("users/{user_id}/follow")
    suspend fun unfollowUser(@Path("user_id") userId: String)

    @GET("users/{user_id}")
    suspend fun getUserProfile(@Path("user_id") userId: String): com.example.domain.model.UserResponse

    @GET("stories")
    suspend fun getActiveStories(): List<com.example.domain.model.StoryResponse>

    @GET("search")
    suspend fun search(
        @Query("q") query: String,
        @Query("type") type: String = "sounds",
        @Query("limit") limit: Int = 20
    ): List<Sound>
}
