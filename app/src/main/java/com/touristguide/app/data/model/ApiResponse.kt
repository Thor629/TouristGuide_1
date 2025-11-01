package com.touristguide.app.data.model

data class ApiResponse<T>(
    val success: Boolean,
    val message: String?,
    val data: T?
)

data class ErrorResponse(
    val success: Boolean,
    val message: String
)
