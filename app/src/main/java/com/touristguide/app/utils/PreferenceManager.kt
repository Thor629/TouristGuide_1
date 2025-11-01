package com.touristguide.app.utils

import android.content.Context
import android.content.SharedPreferences

class PreferenceManager(context: Context) {
    
    private val prefs: SharedPreferences = context.getSharedPreferences(
        "TouristGuidePrefs",
        Context.MODE_PRIVATE
    )
    
    companion object {
        private const val KEY_TOKEN = "token"
        private const val KEY_USER_ID = "user_id"
        private const val KEY_USER_NAME = "user_name"
        private const val KEY_USER_EMAIL = "user_email"
        private const val KEY_USER_ROLE = "user_role"
        private const val KEY_IS_LOGGED_IN = "is_logged_in"
    }
    
    fun saveAuthData(token: String, userId: String, name: String, email: String, role: String) {
        prefs.edit().apply {
            putString(KEY_TOKEN, token)
            putString(KEY_USER_ID, userId)
            putString(KEY_USER_NAME, name)
            putString(KEY_USER_EMAIL, email)
            putString(KEY_USER_ROLE, role)
            putBoolean(KEY_IS_LOGGED_IN, true)
            apply()
        }
    }
    
    fun getToken(): String? = prefs.getString(KEY_TOKEN, null)
    
    fun getUserId(): String? = prefs.getString(KEY_USER_ID, null)
    
    fun getUserName(): String? = prefs.getString(KEY_USER_NAME, null)
    
    fun getUserEmail(): String? = prefs.getString(KEY_USER_EMAIL, null)
    
    fun getUserRole(): String? = prefs.getString(KEY_USER_ROLE, null)
    
    fun isLoggedIn(): Boolean = prefs.getBoolean(KEY_IS_LOGGED_IN, false)
    
    fun isAdmin(): Boolean = getUserRole() == "admin"
    
    fun clearAuthData() {
        prefs.edit().apply {
            remove(KEY_TOKEN)
            remove(KEY_USER_ID)
            remove(KEY_USER_NAME)
            remove(KEY_USER_EMAIL)
            remove(KEY_USER_ROLE)
            putBoolean(KEY_IS_LOGGED_IN, false)
            apply()
        }
    }
}
