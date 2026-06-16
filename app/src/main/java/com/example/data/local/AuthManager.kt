package com.example.data.local

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class AuthManager(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("auth_prefs", Context.MODE_PRIVATE)

    private val _isLoggedIn = MutableStateFlow(getToken() != null)
    val isLoggedIn: StateFlow<Boolean> = _isLoggedIn.asStateFlow()

    fun saveToken(token: String) {
        prefs.edit().putString("jwt_token", token).apply()
        _isLoggedIn.value = true
    }

    fun getToken(): String? {
        return prefs.getString("jwt_token", null)
    }

    fun saveUser(id: String, username: String) {
        prefs.edit()
            .putString("user_id", id)
            .putString("username", username)
            .apply()
    }

    fun getUsername(): String? = prefs.getString("username", null)
    fun getUserId(): String? = prefs.getString("user_id", null)

    fun savePersonalDetails(userId: String, phone: String, email: String, dob: String) {
        prefs.edit()
            .putString("phone_$userId", phone)
            .putString("email_$userId", email)
            .putString("dob_$userId", dob)
            .apply()
    }

    fun getPhone(userId: String): String = prefs.getString("phone_$userId", "") ?: ""
    fun getEmail(userId: String): String = prefs.getString("email_$userId", "") ?: ""
    fun getDob(userId: String): String = prefs.getString("dob_$userId", "") ?: ""

    fun clear() {
        prefs.edit().clear().apply()
        _isLoggedIn.value = false
    }
}
