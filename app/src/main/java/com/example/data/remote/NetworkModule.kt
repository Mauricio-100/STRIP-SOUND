package com.example.data.remote

import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import java.util.concurrent.TimeUnit

object NetworkModule {
    const val BASE_URL = "https://hoosthubs-g.onrender.com/api/"

    private val moshi = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()

    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY
    }

    var tokenProvider: (() -> String?)? = null

    private val client = OkHttpClient.Builder()
        .addInterceptor(loggingInterceptor)
        .addInterceptor { chain ->
            val request = chain.request()
            val requestBuilder = request.newBuilder()
            val path = request.url.encodedPath
            // Avoid adding Authorization headers to dynamic token retrieval or registry endpoints
            if (!path.endsWith("/token") && !path.endsWith("/register") && !path.endsWith("/login")) {
                tokenProvider?.invoke()?.let { token ->
                    if (token.isNotBlank()) {
                        requestBuilder.addHeader("Authorization", "Bearer $token")
                    }
                }
            }
            chain.proceed(requestBuilder.build())
        }
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    val retrofit: Retrofit = Retrofit.Builder()
        .baseUrl(BASE_URL)
        .client(client)
        .addConverterFactory(MoshiConverterFactory.create(moshi).asLenient())
        .build()

    val api: StripSoundApi = retrofit.create(StripSoundApi::class.java)
}
