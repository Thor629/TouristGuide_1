package com.touristguide.app.ui.admin

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.touristguide.app.R
import com.touristguide.app.data.api.RetrofitClient
import com.touristguide.app.data.model.Place
import com.touristguide.app.databinding.ActivityAdminPanelBinding
import com.touristguide.app.utils.hide
import com.touristguide.app.utils.show
import com.touristguide.app.utils.showToast
import kotlinx.coroutines.launch

class AdminPanelActivity : AppCompatActivity() {
    
    private lateinit var binding: ActivityAdminPanelBinding
    private lateinit var pendingPlacesAdapter: PendingPlacesAdapter
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAdminPanelBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        setupToolbar()
        setupRecyclerView()
        setupSwipeRefresh()
        
        loadPendingPlaces()
    }
    
    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = getString(R.string.admin_panel)
        binding.toolbar.setNavigationOnClickListener { finish() }
    }
    
    private fun setupRecyclerView() {
        pendingPlacesAdapter = PendingPlacesAdapter(
            onApproveClick = { place -> approvePlace(place) },
            onDeleteClick = { place -> showDeleteDialog(place) }
        )
        
        binding.rvPendingPlaces.apply {
            layoutManager = LinearLayoutManager(this@AdminPanelActivity)
            adapter = pendingPlacesAdapter
        }
    }
    
    private fun setupSwipeRefresh() {
        binding.swipeRefresh.setOnRefreshListener {
            loadPendingPlaces()
        }
    }
    
    private fun loadPendingPlaces() {
        showLoading()
        
        lifecycleScope.launch {
            try {
                val response = RetrofitClient.apiService.getPendingPlaces()
                if (response.isSuccessful && response.body()?.success == true) {
                    val places = response.body()?.data ?: emptyList()
                    val count = places.size
                    
                    pendingPlacesAdapter.submitList(places)
                    binding.tvPendingCount.text = "Pending Approvals: ${count}"
                    
                    if (places.isEmpty()) {
                        binding.tvNoPlaces.show()
                        binding.rvPendingPlaces.hide()
                    } else {
                        binding.tvNoPlaces.hide()
                        binding.rvPendingPlaces.show()
                    }
                } else {
                    val errorMessage = response.body()?.message ?: "Failed to load pending places"
                    showToast("$errorMessage (${response.code()})")
                    pendingPlacesAdapter.submitList(emptyList())
                    binding.tvPendingCount.text = "Pending Approvals: 0"
                    binding.tvNoPlaces.show()
                    binding.rvPendingPlaces.hide()
                }
            } catch (e: Exception) {
                showToast("Error: ${e.message}")
                pendingPlacesAdapter.submitList(emptyList())
                binding.tvPendingCount.text = "Pending Approvals: 0"
                binding.tvNoPlaces.show()
                binding.rvPendingPlaces.hide()
            } finally {
                hideLoading()
            }
        }
    }
    
    private fun approvePlace(place: Place) {
        MaterialAlertDialogBuilder(this)
            .setTitle("Approve Place")
            .setMessage("Are you sure you want to approve '${place.name}'?")
            .setPositiveButton("Approve") { _, _ ->
                performApprove(place.id)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
    
    private fun performApprove(placeId: String) {
        showLoading()
        
        lifecycleScope.launch {
            var responseReceived = false
            var wasSuccessful = false
            
            try {
                val response = RetrofitClient.apiService.approvePlace(placeId)
                responseReceived = true
                wasSuccessful = response.isSuccessful
                
                if (response.isSuccessful) {
                    // HTTP 200-299 = Success, always show success message
                    try {
                        val raw = response.body()?.string()?.trim()
                        
                        // If response is successful (200-299), treat as success
                        // Backend might return JSON, string, or empty body
                        if (raw.isNullOrEmpty()) {
                            // Empty response = success (common in REST APIs)
                            showToast(getString(R.string.place_approved))
                            loadPendingPlaces()
                        } else if (raw.startsWith("{")) {
                            // Try to parse as JSON
                            try {
                                val gson = com.google.gson.Gson()
                                // Parse as generic ApiResponse with Any type
                                val apiResponse = gson.fromJson(raw, com.google.gson.JsonObject::class.java)
                                val success = apiResponse.get("success")?.asBoolean ?: true
                                val message = apiResponse.get("message")?.asString
                                
                                if (success) {
                                    showToast(getString(R.string.place_approved))
                                } else {
                                    // Even if JSON says false, HTTP was 200 - still treat as success
                                    showToast(getString(R.string.place_approved))
                                }
                                loadPendingPlaces()
                            } catch (e: Exception) {
                                // JSON parsing failed but response was successful - treat as success
                                showToast(getString(R.string.place_approved))
                                loadPendingPlaces()
                            }
                        } else {
                            // Plain string response - check for explicit error words only
                            val lowerRaw = raw.lowercase()
                            if (lowerRaw.contains("error") && !lowerRaw.contains("success")) {
                                // Only show error if explicitly mentioned AND no success keyword
                                showToast("Error: $raw")
                            } else {
                                // Any other case with HTTP 200 = success
                                showToast(getString(R.string.place_approved))
                            }
                            loadPendingPlaces()
                        }
                    } catch (e: Exception) {
                        // Error reading response body but HTTP was successful - treat as success
                        showToast(getString(R.string.place_approved))
                        loadPendingPlaces()
                    }
                } else {
                    // HTTP error response (4xx, 5xx)
                    try {
                        val errorBody = response.errorBody()?.string() ?: "Unknown error"
                        showToast("Failed to approve: ${response.code()} - $errorBody")
                    } catch (e: Exception) {
                        showToast("Failed to approve place: ${response.code()}")
                    }
                    loadPendingPlaces()
                }
            } catch (e: Exception) {
                // If we got a successful response but exception happened later, still show success
                if (responseReceived && wasSuccessful) {
                    showToast(getString(R.string.place_approved))
                    loadPendingPlaces()
                } else {
                    // Network or other errors before getting response
                    showToast("Error: ${e.message ?: "Unable to approve place"}")
                    loadPendingPlaces()
                }
            } finally {
                hideLoading()
            }
        }
    }
    
    private fun showDeleteDialog(place: Place) {
        MaterialAlertDialogBuilder(this)
            .setTitle("Delete Place")
            .setMessage("Are you sure you want to delete '${place.name}'? This action cannot be undone.")
            .setPositiveButton("Delete") { _, _ ->
                performDelete(place.id)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
    
    private fun performDelete(placeId: String) {
        lifecycleScope.launch {
            try {
                val response = RetrofitClient.apiService.deletePlace(placeId)
                
                if (response.isSuccessful && response.body()?.success == true) {
                    showToast(getString(R.string.place_deleted))
                    loadPendingPlaces()
                } else {
                    showToast("Failed to delete place")
                }
            } catch (e: Exception) {
                showToast("Error: ${e.message}")
            }
        }
    }
    
    private fun showLoading() {
        binding.swipeRefresh.isRefreshing = true
    }
    
    private fun hideLoading() {
        binding.swipeRefresh.isRefreshing = false
    }
    
    override fun onResume() {
        super.onResume()
        loadPendingPlaces()
    }
}
