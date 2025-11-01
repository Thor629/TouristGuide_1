package com.touristguide.app.data.model

import com.google.gson.annotations.SerializedName

data class Place(
    @SerializedName("_id")
    val id: String,
    val name: String,
    val location: String,
    val city: String,
    val description: String,
    val images: List<String>,
    val link: String?,
    val category: Category,
    val addedBy: User?,
    val isApproved: Boolean,
    val approvedBy: User? = null,
    val approvedAt: String? = null,
    val likesCount: Int = 0,
    val reviewsCount: Int = 0,
    val averageRating: Double = 0.0,
    val createdAt: String
)

data class PlaceResponse(
    val success: Boolean,
    val count: Int? = null,
    val data: List<Place>? = null,
    val message: String? = null
)

data class SinglePlaceResponse(
    val success: Boolean,
    val data: Place?,
    val message: String?
)
