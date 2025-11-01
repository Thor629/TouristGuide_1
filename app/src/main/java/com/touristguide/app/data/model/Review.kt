package com.touristguide.app.data.model

import com.google.gson.annotations.SerializedName

data class Review(
    @SerializedName("_id")
    val id: String,
    val place: String,
    val user: User,
    val rating: Int,
    val comment: String,
    val createdAt: String
)

data class ReviewResponse(
    val success: Boolean,
    val count: Int? = null,
    val data: List<Review>? = null,
    val message: String? = null
)

data class SingleReviewResponse(
    val success: Boolean,
    val data: Review?,
    val message: String?
)

data class AddReviewRequest(
    val rating: Int,
    val comment: String
)
