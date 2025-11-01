package com.touristguide.app.ui.placedetails

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.touristguide.app.BuildConfig
import com.touristguide.app.R
import com.touristguide.app.TouristGuideApp
import com.touristguide.app.data.api.RetrofitClient
import com.touristguide.app.data.model.AddReviewRequest
import com.touristguide.app.data.model.Place
import com.touristguide.app.data.model.Review
import com.touristguide.app.databinding.ActivityPlaceDetailsBinding
import com.touristguide.app.databinding.DialogAddReviewBinding
import com.touristguide.app.utils.hide
import com.touristguide.app.utils.show
import com.touristguide.app.utils.showToast
import kotlinx.coroutines.launch

class PlaceDetailsActivity : AppCompatActivity() {
    
    private lateinit var binding: ActivityPlaceDetailsBinding
    private val preferenceManager by lazy { (application as TouristGuideApp).preferenceManager }
    private lateinit var reviewsAdapter: ReviewsAdapter
    private var placeId: String? = null
    private var currentPlace: Place? = null
    private var isLiked = false
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPlaceDetailsBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        placeId = intent.getStringExtra("PLACE_ID")
        
        if (placeId == null) {
            showToast("Error: Place not found")
            finish()
            return
        }
        
        setupToolbar()
        setupRecyclerView()
        setupClickListeners()
        
        loadPlaceDetails()
        loadReviews()
        loadLikeStatus()
    }
    
    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        binding.toolbar.setNavigationOnClickListener { finish() }
    }
    
    private fun setupRecyclerView() {
        reviewsAdapter = ReviewsAdapter(
            currentUserId = preferenceManager.getUserId() ?: "",
            onDeleteClick = { review -> deleteReview(review) }
        )
        
        binding.rvReviews.apply {
            layoutManager = LinearLayoutManager(this@PlaceDetailsActivity)
            adapter = reviewsAdapter
        }
    }
    
    private fun setupClickListeners() {
        binding.btnLike.setOnClickListener {
            toggleLike()
        }
        
        binding.btnAddReview.setOnClickListener {
            showAddReviewDialog()
        }
        
        binding.btnVisitWebsite.setOnClickListener {
            currentPlace?.link?.let { link ->
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(link))
                startActivity(intent)
            }
        }
    }
    
    private fun loadPlaceDetails() {
        showLoading()
        
        lifecycleScope.launch {
            try {
                val response = RetrofitClient.apiService.getPlace(placeId!!)
                
                if (response.isSuccessful && response.body()?.success == true) {
                    response.body()?.data?.let { place ->
                        currentPlace = place
                        displayPlaceDetails(place)
                    }
                } else {
                    showToast("Failed to load place details")
                    finish()
                }
            } catch (e: Exception) {
                showToast("Error: ${e.message}")
                finish()
            } finally {
                hideLoading()
            }
        }
    }
    
    private fun displayPlaceDetails(place: Place) {
        supportActionBar?.title = place.name
        
        binding.apply {
            tvPlaceName.text = place.name
            tvLocation.text = place.location
            tvDescription.text = place.description
            tvCategory.text = place.category.name
            tvLikesCount.text = "${place.likesCount} likes"
            
            if (place.averageRating > 0) {
                tvRating.text = String.format("%.1f ⭐ (${place.reviewsCount} reviews)", place.averageRating)
            } else {
                tvRating.text = "No ratings yet"
            }
            
            // Setup image slider
            val imageUrls = place.images.map { BuildConfig.BASE_URL.removeSuffix("/") + it }
            val imageAdapter = ImageSliderAdapter(imageUrls)
            viewPagerImages.adapter = imageAdapter
            
            // Show website button only if link exists
            if (place.link.isNullOrEmpty()) {
                btnVisitWebsite.hide()
            } else {
                btnVisitWebsite.show()
            }
        }
    }
    
    private fun loadReviews() {
        lifecycleScope.launch {
            try {
                val response = RetrofitClient.apiService.getReviews(placeId!!)
                
                if (response.isSuccessful && response.body()?.success == true) {
                    val reviews = response.body()?.data ?: emptyList()
                    reviewsAdapter.submitList(reviews)
                    
                    if (reviews.isEmpty()) {
                        binding.tvNoReviews.show()
                        binding.rvReviews.hide()
                    } else {
                        binding.tvNoReviews.hide()
                        binding.rvReviews.show()
                    }
                }
            } catch (e: Exception) {
                // Silently fail
            }
        }
    }
    
    private fun loadLikeStatus() {
        lifecycleScope.launch {
            try {
                val response = RetrofitClient.apiService.getLikeStatus(placeId!!)
                
                if (response.isSuccessful && response.body()?.success == true) {
                    isLiked = response.body()?.data?.isLiked ?: false
                    updateLikeButton()
                }
            } catch (e: Exception) {
                // Silently fail
            }
        }
    }
    
    private fun toggleLike() {
        lifecycleScope.launch {
            try {
                val response = RetrofitClient.apiService.toggleLike(placeId!!)
                
                if (response.isSuccessful && response.body()?.success == true) {
                    isLiked = response.body()?.data?.isLiked ?: false
                    val likesCount = response.body()?.data?.likesCount ?: 0
                    
                    binding.tvLikesCount.text = "$likesCount likes"
                    updateLikeButton()
                    
                    showToast(response.body()?.message ?: "Success")
                }
            } catch (e: Exception) {
                showToast("Error: ${e.message}")
            }
        }
    }
    
    private fun updateLikeButton() {
        if (isLiked) {
            binding.btnLike.text = "Unlike"
            binding.btnLike.setIconResource(R.drawable.ic_favorite)
        } else {
            binding.btnLike.text = "Like"
            binding.btnLike.setIconResource(R.drawable.ic_favorite_border)
        }
    }
    
    private fun showAddReviewDialog() {
        val dialogBinding = DialogAddReviewBinding.inflate(layoutInflater)
        
        MaterialAlertDialogBuilder(this)
            .setTitle("Add Review")
            .setView(dialogBinding.root)
            .setPositiveButton("Submit") { _, _ ->
                val rating = dialogBinding.ratingBar.rating.toInt()
                val comment = dialogBinding.etComment.text.toString().trim()
                
                if (rating > 0 && comment.isNotEmpty()) {
                    submitReview(rating, comment)
                } else {
                    showToast("Please provide rating and comment")
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
    
    private fun submitReview(rating: Int, comment: String) {
        lifecycleScope.launch {
            try {
                val response = RetrofitClient.apiService.addReview(
                    placeId!!,
                    AddReviewRequest(rating, comment)
                )
                
                if (response.isSuccessful && response.body()?.success == true) {
                    showToast("Review added successfully")
                    loadReviews()
                    loadPlaceDetails() // Refresh to update rating
                } else {
                    showToast(response.body()?.message ?: "Failed to add review")
                }
            } catch (e: Exception) {
                showToast("Error: ${e.message}")
            }
        }
    }
    
    private fun deleteReview(review: Review) {
        MaterialAlertDialogBuilder(this)
            .setTitle("Delete Review")
            .setMessage("Are you sure you want to delete this review?")
            .setPositiveButton("Delete") { _, _ ->
                performDeleteReview(review.id)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
    
    private fun performDeleteReview(reviewId: String) {
        lifecycleScope.launch {
            try {
                val response = RetrofitClient.apiService.deleteReview(reviewId)
                
                if (response.isSuccessful && response.body()?.success == true) {
                    showToast("Review deleted")
                    loadReviews()
                    loadPlaceDetails()
                } else {
                    showToast("Failed to delete review")
                }
            } catch (e: Exception) {
                showToast("Error: ${e.message}")
            }
        }
    }
    
    private fun showLoading() {
        binding.progressBar.show()
    }
    
    private fun hideLoading() {
        binding.progressBar.hide()
    }
}
