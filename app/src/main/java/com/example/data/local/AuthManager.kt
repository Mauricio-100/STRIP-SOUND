package com.example.data.local

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class AuthManager(context: Context) {
    private val prefs = context.getSharedPreferences("auth_prefs", Context.MODE_PRIVATE)

    private val _isLoggedIn = MutableStateFlow(!getToken().isNullOrBlank())
    val isLoggedIn: StateFlow<Boolean> = _isLoggedIn.asStateFlow()

    // Key-value set of liked sounds
    private val likedSounds = mutableSetOf<String>().apply {
        addAll(prefs.getStringSet("liked_sounds", emptySet()) ?: emptySet())
    }

    fun getToken(): String? {
        return prefs.getString("token", null)
    }

    fun getUserId(): String? {
        return prefs.getString("user_id", null)
    }

    fun getUsername(): String? {
        return prefs.getString("username", null)
    }

    fun isSoundLiked(soundId: String): Boolean {
        synchronized(likedSounds) {
            return likedSounds.contains(soundId)
        }
    }

    fun setSoundLiked(soundId: String, liked: Boolean) {
        synchronized(likedSounds) {
            if (liked) {
                likedSounds.add(soundId)
            } else {
                likedSounds.remove(soundId)
            }
            prefs.edit().putStringSet("liked_sounds", likedSounds.toSet()).apply()
        }
    }

    fun saveSession(token: String, userId: String, username: String) {
        prefs.edit()
            .putString("token", token)
            .putString("user_id", userId)
            .putString("username", username)
            .apply()
        _isLoggedIn.value = true
    }

    fun clearSession() {
        prefs.edit()
            .remove("token")
            .remove("user_id")
            .remove("username")
            .apply()
        _isLoggedIn.value = false
    }
}
