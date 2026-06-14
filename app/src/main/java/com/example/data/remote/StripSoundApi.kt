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

    @GET("search")
    suspend fun search(
        @Query("q") query: String,
        @Query("type") type: String = "sounds",
        @Query("limit") limit: Int = 20
    ): List<Sound>
}
