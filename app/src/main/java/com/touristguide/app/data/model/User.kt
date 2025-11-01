package com.touristguide.app.data.model

import com.google.gson.annotations.SerializedName

data class User(
    @SerializedName("_id")
    val id: String,
    val name: String,
    val email: String,
    val role: String,
    val likedPlaces: List<String>? = null,
    val createdAt: String? = null,
    val lastLogin: String? = null
)

data class AuthResponse(
    val success: Boolean,
    val message: String,
    val data: AuthData?
)

data class AuthData(
    val id: String,
    val name: String,
    val email: String,
    val role: String,
    val token: String
)

data class LoginRequest(
    val email: String,
    val password: String
)

data class RegisterRequest(
    val name: String,
    val email: String,
    val password: String
)
