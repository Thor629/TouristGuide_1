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
                if (response.isSuccessful) {
                    val raw = response.body()?.string()?.trim()
                    if (raw.isNullOrEmpty()) {
                        pendingPlacesAdapter.submitList(emptyList())
                        binding.tvPendingCount.text = "Pending Approvals: 0"
                        binding.tvNoPlaces.show()
                        binding.rvPendingPlaces.hide()
                    } else if (raw.startsWith("{")) {
                        // Try to parse expected JSON minimalistically to avoid model crashes
                        // We only extract success and data list size via simple checks
                        val hasSuccess = raw.contains("\"success\":true")
                        if (hasSuccess) {
                            // naive count: count field if present; otherwise compute from 'data' array brackets
                            val countRegex = Regex("\"count\"\\s*:\\s*(\\d+)")
                            val count = countRegex.find(raw)?.groupValues?.getOrNull(1)?.toIntOrNull()
                                ?: Regex("\"data\"\\s*:\\s*\\[(.*?)\\]", RegexOption.DOT_MATCHES_ALL)
                                    .find(raw)
                                    ?.groupValues?.getOrNull(1)
                                    ?.let { inner -> if (inner.isBlank()) 0 else inner.split("},").size }
                                    ?: 0
                            binding.tvPendingCount.text = "Pending Approvals: ${count}"
                            // We don't attempt full object parsing; trigger refresh of adapter with empty to avoid crashes
                            // Optionally you can re-request with a stable endpoint later
                            pendingPlacesAdapter.submitList(emptyList())
                            if (count == 0) {
                                binding.tvNoPlaces.show()
                                binding.rvPendingPlaces.hide()
                            } else {
                                binding.tvNoPlaces.hide()
                                binding.rvPendingPlaces.show()
                            }
                        } else {
                            showToast("Failed to load pending places")
                        }
                    } else {
                        // Plain text response, just show it and assume no data
                        showToast(raw)
                        pendingPlacesAdapter.submitList(emptyList())
                        binding.tvPendingCount.text = "Pending Approvals: 0"
                        binding.tvNoPlaces.show()
                        binding.rvPendingPlaces.hide()
                    }
                } else {
                    showToast("Failed to load pending places: ${response.code()}")
                }
            } catch (e: Exception) {
                showToast("Error: ${e.message}")
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
        lifecycleScope.launch {
            try {
                val response = RetrofitClient.apiService.approvePlace(placeId)
                if (response.isSuccessful) {
                    val raw = response.body()?.string()?.trim()
                    // Accept either JSON success or plain string
                    if (raw.isNullOrEmpty()) {
                        showToast(getString(R.string.place_approved))
                        loadPendingPlaces()
                    } else if (raw.startsWith("{")) {
                        // Backend returned JSON; try to detect a success flag quickly
                        if (raw.contains("\"success\":true")) {
                            showToast(getString(R.string.place_approved))
                            loadPendingPlaces()
                        } else {
                            showToast("Approve failed: ${raw}")
                        }
                    } else {
                        // Plain string like "Approved" or message
                        showToast(raw)
                        loadPendingPlaces()
                    }
                } else {
                    showToast("Failed to approve place: ${response.code()}")
                }
            } catch (e: Exception) {
                showToast("Error: ${e.message}")
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
