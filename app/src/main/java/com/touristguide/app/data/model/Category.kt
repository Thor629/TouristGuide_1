package com.touristguide.app.data.model

import com.google.gson.annotations.SerializedName

data class Category(
    @SerializedName("_id")
    val id: String,
    val name: String,
    val icon: String?,
    val description: String?,
    val isActive: Boolean = true
)

data class CategoryResponse(
    val success: Boolean,
    val count: Int,
    val data: List<Category>
)
