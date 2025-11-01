package com.touristguide.app.data.model

data class LikeResponse(
    val success: Boolean,
    val message: String?,
    val data: LikeData?
)

data class LikeData(
    val isLiked: Boolean,
    val likesCount: Int
)

data class LikeStatusResponse(
    val success: Boolean,
    val data: LikeData?
)
