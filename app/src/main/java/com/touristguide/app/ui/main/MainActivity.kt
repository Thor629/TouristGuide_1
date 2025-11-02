package com.touristguide.app.ui.main

import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SearchView
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.touristguide.app.R
import com.touristguide.app.TouristGuideApp
import com.touristguide.app.data.api.RetrofitClient
import com.touristguide.app.data.model.Category
import com.touristguide.app.data.model.Place
import com.touristguide.app.databinding.ActivityMainBinding
import com.touristguide.app.ui.addplace.AddPlaceActivity
import com.touristguide.app.ui.admin.AdminPanelActivity
import com.touristguide.app.ui.auth.AuthActivity
import com.touristguide.app.ui.liked.LikedPlacesActivity
import com.touristguide.app.ui.myplaces.MyPlacesActivity
import com.touristguide.app.ui.placedetails.PlaceDetailsActivity
import com.touristguide.app.utils.hide
import com.touristguide.app.utils.show
import com.touristguide.app.utils.showToast
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {
    
    private lateinit var binding: ActivityMainBinding
    private val preferenceManager by lazy { (application as TouristGuideApp).preferenceManager }
    private lateinit var placesAdapter: PlacesAdapter
    private val categories = mutableListOf<Category>()
    private var selectedCategoryId: String? = null
    private var isFirstLoad = true
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        setupToolbar()
        setupRecyclerView()
        setupFab()
        setupCategorySpinner()
        setupSearch()
        setupSwipeRefresh()
        
        loadCategories()
    }
    
    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.title = "Tourist Guide - Surat"
        
        val userName = preferenceManager.getUserName() ?: "User"
        binding.tvWelcome.text = "Welcome, $userName!"
    }
    
    private fun setupRecyclerView() {
        placesAdapter = PlacesAdapter { place ->
            openPlaceDetails(place)
        }
        
        binding.rvPlaces.apply {
            layoutManager = GridLayoutManager(this@MainActivity, 2)
            adapter = placesAdapter
        }
    }
    
    private fun setupFab() {
        binding.fabAddPlace.setOnClickListener {
            startActivity(Intent(this, AddPlaceActivity::class.java))
        }
    }
    
    private fun setupCategorySpinner() {
        binding.spinnerCategory.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                selectedCategoryId = if (position == 0) null else categories[position - 1].id
                loadPlaces()
            }
            
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
    }
    
    private fun setupSearch() {
        binding.searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                query?.let { searchPlaces(it) }
                return true
            }
            
            override fun onQueryTextChange(newText: String?): Boolean {
                if (newText.isNullOrEmpty()) {
                    loadPlaces()
                }
                return true
            }
        })
    }
    
    private fun setupSwipeRefresh() {
        binding.swipeRefresh.setOnRefreshListener {
            loadPlaces()
        }
    }
    
    private fun loadCategories() {
        lifecycleScope.launch {
            try {
                val response = RetrofitClient.apiService.getCategories()
                if (response.isSuccessful && response.body()?.success == true) {
                    categories.clear()
                    response.body()?.data?.let { categories.addAll(it) }
                    
                    val categoryNames = mutableListOf("All Categories")
                    categoryNames.addAll(categories.map { it.name })
                    
                    val adapter = ArrayAdapter(
                        this@MainActivity,
                        android.R.layout.simple_spinner_item,
                        categoryNames
                    )
                    adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                    binding.spinnerCategory.adapter = adapter
                }
            } catch (e: Exception) {
                // Silently fail for categories
            }
        }
    }
    
    private fun loadPlaces() {
        showLoading()
        
        lifecycleScope.launch {
            try {
                val response = RetrofitClient.apiService.getPlaces(
                    category = selectedCategoryId,
                    city = "Surat"
                )
                
                if (response.isSuccessful) {
                    val responseBody = response.body()
                    if (responseBody != null && responseBody.success == true) {
                        val places = responseBody.data ?: emptyList()
                        placesAdapter.submitList(places)
                        
                        if (places.isEmpty()) {
                            binding.tvNoPlaces.show()
                            binding.rvPlaces.hide()
                        } else {
                            binding.tvNoPlaces.hide()
                            binding.rvPlaces.show()
                        }
                    } else {
                        // Response body might be null or success is false
                        val errorMessage = responseBody?.message ?: "Failed to load places"
                        showToast(errorMessage)
                        placesAdapter.submitList(emptyList())
                        binding.tvNoPlaces.show()
                        binding.rvPlaces.hide()
                    }
                } else {
                    // Handle HTTP error responses
                    try {
                        val errorBody = response.errorBody()?.string()
                        val errorMessage = if (errorBody?.startsWith("{") == true) {
                            // Try to extract message from JSON error
                            if (errorBody.contains("\"message\"")) {
                                Regex("\"message\"\\s*:\\s*\"([^\"]+)\"").find(errorBody)?.groupValues?.get(1) ?: errorBody
                            } else {
                                errorBody
                            }
                        } else {
                            errorBody ?: "Failed to load places"
                        }
                        showToast("Error: $errorMessage")
                    } catch (e: Exception) {
                        showToast("Failed to load places: ${response.code()}")
                    }
                    placesAdapter.submitList(emptyList())
                    binding.tvNoPlaces.show()
                    binding.rvPlaces.hide()
                }
            } catch (e: IllegalStateException) {
                // Handle JSON parsing errors when backend returns string instead of JSON
                // Don't show error toast on first load (app startup) - just silently handle it
                if (!isFirstLoad) {
                    if (e.message?.contains("BEGIN_OBJECT") == true || e.message?.contains("STRI") == true) {
                        // Silently handle JSON parsing errors - backend might return string instead of JSON
                        // Show user-friendly message only if not first load
                        showToast("Unable to load places. Please pull to refresh.")
                    } else {
                        showToast("Error: ${e.message}")
                    }
                }
                placesAdapter.submitList(emptyList())
                binding.tvNoPlaces.show()
                binding.rvPlaces.hide()
            } catch (e: Exception) {
                // Don't show error toast on first load (app startup)
                if (!isFirstLoad) {
                    showToast("Error: ${e.message}")
                }
                placesAdapter.submitList(emptyList())
                binding.tvNoPlaces.show()
                binding.rvPlaces.hide()
            } finally {
                hideLoading()
                isFirstLoad = false
            }
        }
    }
    
    private fun searchPlaces(query: String) {
        // Check if searching outside Surat
        val lowerQuery = query.lowercase()
        val suratRelated = listOf("surat", "dumas", "nanpura", "adajan", "vesu")
        val isSuratSearch = suratRelated.any { lowerQuery.contains(it) }
        
        if (!isSuratSearch && query.length > 3) {
            MaterialAlertDialogBuilder(this)
                .setTitle("Coming Soon")
                .setMessage(getString(R.string.search_limited_surat))
                .setPositiveButton("OK", null)
                .show()
            return
        }
        
        showLoading()
        
        lifecycleScope.launch {
            try {
                val response = RetrofitClient.apiService.getPlaces(
                    search = query,
                    city = "Surat"
                )
                
                if (response.isSuccessful) {
                    val responseBody = response.body()
                    if (responseBody != null && responseBody.success == true) {
                        val places = responseBody.data ?: emptyList()
                        placesAdapter.submitList(places)
                        
                        if (places.isEmpty()) {
                            binding.tvNoPlaces.show()
                            binding.rvPlaces.hide()
                        } else {
                            binding.tvNoPlaces.hide()
                            binding.rvPlaces.show()
                        }
                    } else {
                        val errorMessage = responseBody?.message ?: "No places found"
                        if (responseBody?.success != false) {
                            showToast(errorMessage)
                        }
                        placesAdapter.submitList(emptyList())
                        binding.tvNoPlaces.show()
                        binding.rvPlaces.hide()
                    }
                } else {
                    try {
                        val errorBody = response.errorBody()?.string()
                        showToast("Search failed: ${errorBody ?: response.code()}")
                    } catch (e: Exception) {
                        showToast("Search failed: ${response.code()}")
                    }
                    placesAdapter.submitList(emptyList())
                    binding.tvNoPlaces.show()
                    binding.rvPlaces.hide()
                }
            } catch (e: IllegalStateException) {
                // Handle JSON parsing errors
                if (e.message?.contains("BEGIN_OBJECT") == true || e.message?.contains("STRI") == true) {
                    showToast("Server returned invalid response format. Please try again.")
                } else {
                    showToast("Error: ${e.message}")
                }
                placesAdapter.submitList(emptyList())
                binding.tvNoPlaces.show()
                binding.rvPlaces.hide()
            } catch (e: Exception) {
                showToast("Error: ${e.message}")
                placesAdapter.submitList(emptyList())
                binding.tvNoPlaces.show()
                binding.rvPlaces.hide()
            } finally {
                hideLoading()
            }
        }
    }
    
    private fun openPlaceDetails(place: Place) {
        val intent = Intent(this, PlaceDetailsActivity::class.java)
        intent.putExtra("PLACE_ID", place.id)
        startActivity(intent)
    }
    
    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)
        
        // Hide admin menu if not admin
        menu?.findItem(R.id.action_admin)?.isVisible = preferenceManager.isAdmin()
        
        return true
    }
    
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_liked_places -> {
                startActivity(Intent(this, LikedPlacesActivity::class.java))
                true
            }
            R.id.action_my_places -> {
                startActivity(Intent(this, MyPlacesActivity::class.java))
                true
            }
            R.id.action_admin -> {
                startActivity(Intent(this, AdminPanelActivity::class.java))
                true
            }
            R.id.action_logout -> {
                showLogoutDialog()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
    
    private fun showLogoutDialog() {
        MaterialAlertDialogBuilder(this)
            .setTitle("Logout")
            .setMessage(getString(R.string.logout_confirm))
            .setPositiveButton("Logout") { _, _ ->
                performLogout()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
    
    private fun performLogout() {
        preferenceManager.clearAuthData()
        val intent = Intent(this, AuthActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }
    
    private fun showLoading() {
        binding.swipeRefresh.isRefreshing = true
    }
    
    private fun hideLoading() {
        binding.swipeRefresh.isRefreshing = false
    }
    
    override fun onResume() {
        super.onResume()
        // Load places on first resume (app startup)
        // Also reload when returning from other activities (like AdminPanel after approval)
        if (isFirstLoad) {
            loadPlaces()
        } else {
            // Refresh places when returning from other activities to show newly approved places
            loadPlaces()
        }
    }
}
