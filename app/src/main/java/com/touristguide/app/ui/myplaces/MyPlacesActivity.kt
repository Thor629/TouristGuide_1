package com.touristguide.app.ui.myplaces

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.touristguide.app.data.api.RetrofitClient
import com.touristguide.app.data.model.Place
import com.touristguide.app.databinding.ActivityMyPlacesBinding
import com.touristguide.app.ui.main.PlacesAdapter
import com.touristguide.app.ui.placedetails.PlaceDetailsActivity
import com.touristguide.app.utils.hide
import com.touristguide.app.utils.show
import com.touristguide.app.utils.showToast
import kotlinx.coroutines.launch

class MyPlacesActivity : AppCompatActivity() {
    
    private lateinit var binding: ActivityMyPlacesBinding
    private lateinit var placesAdapter: PlacesAdapter
    private val places = mutableListOf<Place>()
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMyPlacesBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        setupToolbar()
        setupRecyclerView()
        setupSwipeRefresh()
        loadMyPlaces()
    }
    
    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        binding.toolbar.setNavigationOnClickListener { finish() }
    }
    
    private fun setupRecyclerView() {
        placesAdapter = PlacesAdapter { place ->
            val intent = Intent(this, PlaceDetailsActivity::class.java)
            intent.putExtra("PLACE_ID", place.id)
            startActivity(intent)
        }
        
        binding.rvPlaces.apply {
            layoutManager = LinearLayoutManager(this@MyPlacesActivity)
            adapter = placesAdapter
        }
    }
    
    private fun setupSwipeRefresh() {
        binding.swipeRefresh.setOnRefreshListener {
            loadMyPlaces()
        }
    }
    
    private fun loadMyPlaces() {
        showLoading()
        
        lifecycleScope.launch {
            try {
                val response = RetrofitClient.apiService.getMyPlaces()
                
                if (response.isSuccessful && response.body()?.success == true) {
                    places.clear()
                    response.body()?.data?.let { places.addAll(it) }
                    
                    if (places.isEmpty()) {
                        showEmptyState()
                    } else {
                        hideEmptyState()
                        placesAdapter.submitList(places)
                    }
                } else {
                    showToast(response.body()?.message ?: "Failed to load places")
                }
            } catch (e: Exception) {
                showToast("Error: ${e.message}")
            } finally {
                hideLoading()
                binding.swipeRefresh.isRefreshing = false
            }
        }
    }
    
    private fun showLoading() {
        binding.progressBar.show()
    }
    
    private fun hideLoading() {
        binding.progressBar.hide()
    }
    
    private fun showEmptyState() {
        binding.llEmptyState.show()
        binding.rvPlaces.hide()
    }
    
    private fun hideEmptyState() {
        binding.llEmptyState.hide()
        binding.rvPlaces.show()
    }
    
    override fun onResume() {
        super.onResume()
        loadMyPlaces()
    }
}
