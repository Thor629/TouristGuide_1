package com.touristguide.app.ui.main

import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
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
        setupBottomNavigation()
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
        placesAdapter = PlacesAdapter(
            onPlaceClick = { place ->
                openPlaceDetails(place)
            },
            onEditClick = { place ->
                editPlace(place)
            },
            onDeleteClick = { place ->
                confirmDeletePlace(place)
            }
        )

        binding.rvPlaces.apply {
            layoutManager = GridLayoutManager(this@MainActivity, 2)
            adapter = placesAdapter
        }
    }

    private fun setupBottomNavigation() {
        binding.bottomNavigation.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.navigation_home -> {
                    // Already on home, do nothing or refresh
                    true
                }
                R.id.navigation_liked -> {
                    startActivity(Intent(this, LikedPlacesActivity::class.java))
                    true
                }
                R.id.navigation_add -> {
                    startActivity(Intent(this, AddPlaceActivity::class.java))
                    true
                }
                R.id.navigation_my_places -> {
                    startActivity(Intent(this, MyPlacesActivity::class.java))
                    true
                }
                R.id.navigation_account -> {
                    showAccountOptions()
                    true
                }
                else -> false
            }
        }
        // Set home as selected
        binding.bottomNavigation.selectedItemId = R.id.navigation_home
    }
    
    private fun showAccountOptions() {
        val options = if (preferenceManager.isAdmin()) {
            arrayOf("Admin Panel", "Logout")
        } else {
            arrayOf("Logout")
        }
        
        MaterialAlertDialogBuilder(this)
            .setTitle("Account")
            .setItems(options) { _, which ->
                when (options[which]) {
                    "Admin Panel" -> startActivity(Intent(this, AdminPanelActivity::class.java))
                    "Logout" -> showLogoutDialog()
                }
            }
            .show()
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
                    val placeResponse = response.body()
                    if (placeResponse != null && placeResponse.success) {
                        val places = placeResponse.data ?: emptyList()
                        // Filter to only show approved places (backend should only return approved, but filter anyway)
                        val approvedPlaces = places.filter { it.isApproved }
                        // Debug log (remove in production)
                        android.util.Log.d("MainActivity", "Loaded ${places.size} places, ${approvedPlaces.size} approved")
                        placesAdapter.submitList(approvedPlaces)
                        if (approvedPlaces.isEmpty()) {
                            binding.tvNoPlaces.show()
                            binding.rvPlaces.hide()
                        } else {
                            binding.tvNoPlaces.hide()
                            binding.rvPlaces.show()
                        }
                    } else {
                        val errorMessage = placeResponse?.message ?: "Failed to load places"
                        if (!isFirstLoad) {
                            showToast(errorMessage)
                        }
                        placesAdapter.submitList(emptyList())
                        binding.tvNoPlaces.show()
                        binding.rvPlaces.hide()
                    }
                } else {
                    if (!isFirstLoad) {
                        showToast("Failed to load places: ${response.code()}")
                    }
                    placesAdapter.submitList(emptyList())
                    binding.tvNoPlaces.show()
                    binding.rvPlaces.hide()
                }
            } catch (e: Exception) {
                android.util.Log.e("MainActivity", "Error loading places", e)
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
                    val placeResponse = response.body()
                    if (placeResponse != null && placeResponse.success) {
                        val places = placeResponse.data ?: emptyList()
                        val approvedPlaces = places.filter { it.isApproved }
                        placesAdapter.submitList(approvedPlaces)
                        if (approvedPlaces.isEmpty()) {
                            binding.tvNoPlaces.show()
                            binding.rvPlaces.hide()
                        } else {
                            binding.tvNoPlaces.hide()
                            binding.rvPlaces.show()
                        }
                    } else {
                        showToast(placeResponse?.message ?: "No places found")
                        placesAdapter.submitList(emptyList())
                        binding.tvNoPlaces.show()
                        binding.rvPlaces.hide()
                    }
                } else {
                    showToast("Failed to load places")
                    placesAdapter.submitList(emptyList())
                    binding.tvNoPlaces.show()
                    binding.rvPlaces.hide()
                }
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
    
    private fun editPlace(place: Place) {
        val intent = Intent(this, AddPlaceActivity::class.java)
        intent.putExtra("PLACE_ID", place.id)
        intent.putExtra("EDIT_MODE", true)
        startActivity(intent)
    }
    
    private fun confirmDeletePlace(place: Place) {
        MaterialAlertDialogBuilder(this)
            .setTitle("Delete Place")
            .setMessage("Are you sure you want to delete \"${place.name}\"? This action cannot be undone.")
            .setPositiveButton("Delete") { _, _ ->
                deletePlace(place)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
    
    private fun deletePlace(place: Place) {
        showLoading()
        lifecycleScope.launch {
            try {
                val response = RetrofitClient.apiService.deletePlace(place.id)
                if (response.isSuccessful && response.body()?.success == true) {
                    showToast("Place deleted successfully")
                    loadPlaces() // Reload the list
                } else {
                    val errorMessage = response.body()?.message ?: "Failed to delete place"
                    showToast(errorMessage)
                }
            } catch (e: Exception) {
                android.util.Log.e("MainActivity", "Error deleting place", e)
                showToast("Error: ${e.message}")
            } finally {
                hideLoading()
            }
        }
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
        // Load places on first resume (app startup) and when returning from other activities
        // This ensures approved places appear after returning from AdminPanel
        loadPlaces()
    }
}
