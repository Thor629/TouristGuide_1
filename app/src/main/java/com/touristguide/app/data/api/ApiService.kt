package com.touristguide.app.data.api

import com.touristguide.app.data.model.*
import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.*

interface ApiService {
    
    // Auth endpoints
    @POST("auth/register")
    suspend fun register(@Body request: RegisterRequest): Response<AuthResponse>
    
    @POST("auth/login")
    suspend fun login(@Body request: LoginRequest): Response<AuthResponse>
    
    @GET("auth/me")
    suspend fun getCurrentUser(): Response<ApiResponse<User>>
    
    @POST("auth/logout")
    suspend fun logout(): Response<ApiResponse<Any>>
    
    // Places endpoints
    @GET("places")
    suspend fun getPlaces(
        @Query("category") category: String? = null,
        @Query("search") search: String? = null,
        @Query("city") city: String? = null
    ): Response<PlaceResponse>
    
    @GET("places/user/my-places")
    suspend fun getMyPlaces(): Response<PlaceResponse>
    
    @GET("places/{id}")
    suspend fun getPlace(@Path("id") placeId: String): Response<SinglePlaceResponse>
    
    @Multipart
    @POST("places")
    suspend fun createPlace(
        @Part("name") name: RequestBody,
        @Part("location") location: RequestBody,
        @Part("city") city: RequestBody,
        @Part("description") description: RequestBody,
        @Part("category") category: RequestBody,
        @Part("link") link: RequestBody?,
        @Part images: List<MultipartBody.Part>?
    ): Response<SinglePlaceResponse>
    
    @Multipart
    @PUT("places/{id}")
    suspend fun updatePlace(
        @Path("id") placeId: String,
        @Part("name") name: RequestBody?,
        @Part("location") location: RequestBody?,
        @Part("city") city: RequestBody?,
        @Part("description") description: RequestBody?,
        @Part("category") category: RequestBody?,
        @Part("link") link: RequestBody?,
        @Part images: List<MultipartBody.Part>?
    ): Response<SinglePlaceResponse>
    
    @DELETE("places/{id}")
    suspend fun deletePlace(@Path("id") placeId: String): Response<ApiResponse<Any>>
    
    @GET("places/pending")
    suspend fun getPendingPlaces(): Response<ResponseBody>
    
    @PUT("places/{id}/approve")
    suspend fun approvePlace(@Path("id") placeId: String): Response<ResponseBody>
    
    // Categories endpoints
    @GET("categories")
    suspend fun getCategories(): Response<CategoryResponse>
    
    @POST("categories")
    suspend fun createCategory(@Body category: Map<String, String>): Response<ApiResponse<Category>>
    
    // Likes endpoints
    @POST("likes/{placeId}")
    suspend fun toggleLike(@Path("placeId") placeId: String): Response<LikeResponse>
    
    @GET("likes")
    suspend fun getLikedPlaces(): Response<PlaceResponse>
    
    @GET("likes/{placeId}/status")
    suspend fun getLikeStatus(@Path("placeId") placeId: String): Response<LikeStatusResponse>
    
    // Reviews endpoints
    @GET("reviews/{placeId}")
    suspend fun getReviews(@Path("placeId") placeId: String): Response<ReviewResponse>
    
    @POST("reviews/{placeId}")
    suspend fun addReview(
        @Path("placeId") placeId: String,
        @Body request: AddReviewRequest
    ): Response<SingleReviewResponse>
    
    @PUT("reviews/{reviewId}")
    suspend fun updateReview(
        @Path("reviewId") reviewId: String,
        @Body request: AddReviewRequest
    ): Response<SingleReviewResponse>
    
    @DELETE("reviews/{reviewId}")
    suspend fun deleteReview(@Path("reviewId") reviewId: String): Response<ApiResponse<Any>>
}
