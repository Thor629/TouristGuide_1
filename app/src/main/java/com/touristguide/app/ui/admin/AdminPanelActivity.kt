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
            try {
                val response = RetrofitClient.apiService.approvePlace(placeId)
                if (response.isSuccessful) {
                    val raw = response.body()?.string()?.trim()
                    // Accept either JSON success or plain string
                    if (raw.isNullOrEmpty() || raw.contains("\"success\":true") || 
                        raw.lowercase().contains("approved") || raw.lowercase().contains("success")) {
                        showToast(getString(R.string.place_approved))
                        // Refresh the list to show updated pending places
                        loadPendingPlaces()
                    } else {
                        // Try to parse as JSON to get error message
                        if (raw.startsWith("{")) {
                            showToast("Approve failed: $raw")
                        } else {
                            showToast(raw)
                            loadPendingPlaces() // Still refresh even if message unclear
                        }
                    }
                } else {
                    val errorBody = response.errorBody()?.string()
                    showToast("Failed to approve place: ${response.code()} - ${errorBody ?: "Unknown error"}")
                    // Still refresh to get current state
                    loadPendingPlaces()
                }
            } catch (e: Exception) {
                showToast("Error: ${e.message}")
                // Refresh anyway to get current state
                loadPendingPlaces()
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
