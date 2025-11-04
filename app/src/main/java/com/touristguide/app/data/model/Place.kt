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
    val approvedBy: String? = null,
    val approvedAt: String? = null,
    val likesCount: Int = 0,
    val reviewsCount: Int = 0,
    val averageRating: Double = 0.0,
    val createdAt: String,
    val permissions: PlacePermissions? = null
)

data class PlacePermissions(
    val canEdit: Boolean = false,
    val canDelete: Boolean = false,
    val isOwner: Boolean = false,
    val isAdmin: Boolean = false
)

data class PlaceResponse(
    val success: Boolean,
    val count: Int? = null,
    val data: List<Place>? = null,
    val message: String? = null,
    val user: UserInfo? = null
)

data class UserInfo(
    val id: String,
    val role: String,
    val isAdmin: Boolean
)

data class SinglePlaceResponse(
    val success: Boolean,
    val data: Place?,
    val message: String?,
    val user: UserInfo? = null
)
