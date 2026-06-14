package com.example.domain.model

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class LoginResponse(
    val access_token: String,
    val token_type: String,
    val user_id: String,
    val username: String,
    val avatar_url: String?,
    val is_verified: Boolean
)

@JsonClass(generateAdapter = true)
data class RegisterRequest(
    val username: String,
    val password: String,
    val email: String? = null,
    val phone_number: String? = null
)

@JsonClass(generateAdapter = true)
data class RegisterResponse(
    val id: String,
    val username: String,
    val email: String?,
    val avatar_url: String?,
    val created_at: String
)
