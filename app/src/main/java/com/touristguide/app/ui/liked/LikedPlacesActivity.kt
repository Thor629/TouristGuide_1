package com.touristguide.app.ui.liked

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import com.touristguide.app.R
import com.touristguide.app.data.api.RetrofitClient
import com.touristguide.app.data.model.Place
import com.touristguide.app.databinding.ActivityLikedPlacesBinding
import com.touristguide.app.ui.main.PlacesAdapter
import com.touristguide.app.ui.placedetails.PlaceDetailsActivity
import com.touristguide.app.utils.hide
import com.touristguide.app.utils.show
import com.touristguide.app.utils.showToast
import kotlinx.coroutines.launch

class LikedPlacesActivity : AppCompatActivity() {
    
    private lateinit var binding: ActivityLikedPlacesBinding
    private lateinit var placesAdapter: PlacesAdapter
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLikedPlacesBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        setupToolbar()
        setupRecyclerView()
        setupSwipeRefresh()
        
        loadLikedPlaces()
    }
    
    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = getString(R.string.liked_places)
        binding.toolbar.setNavigationOnClickListener { finish() }
    }
    
    private fun setupRecyclerView() {
        placesAdapter = PlacesAdapter(
            onPlaceClick = { place ->
                openPlaceDetails(place)
            },
            showAdminActions = false  // No edit/delete in liked places
        )
        
        binding.rvLikedPlaces.apply {
            layoutManager = GridLayoutManager(this@LikedPlacesActivity, 2)
            adapter = placesAdapter
        }
    }
    
    private fun setupSwipeRefresh() {
        binding.swipeRefresh.setOnRefreshListener {
            loadLikedPlaces()
        }
    }
    
    private fun loadLikedPlaces() {
        showLoading()
        
        lifecycleScope.launch {
            try {
                val response = RetrofitClient.apiService.getLikedPlaces()
                
                if (response.isSuccessful) {
                    val body = response.body()
                    if (body?.success == true) {
                        val places = body.data ?: emptyList()
                        android.util.Log.d("LikedPlaces", "Loaded ${places.size} liked places")
                        placesAdapter.submitList(places)
                        
                        if (places.isEmpty()) {
                            binding.tvNoPlaces.show()
                            binding.rvLikedPlaces.hide()
                        } else {
                            binding.tvNoPlaces.hide()
                            binding.rvLikedPlaces.show()
                        }
                    } else {
                        android.util.Log.e("LikedPlaces", "Response not successful: ${body?.message}")
                        showToast(body?.message ?: "Failed to load liked places")
                        binding.tvNoPlaces.show()
                        binding.rvLikedPlaces.hide()
                    }
                } else {
                    android.util.Log.e("LikedPlaces", "HTTP error: ${response.code()}")
                    showToast("Failed to load liked places: ${response.code()}")
                    binding.tvNoPlaces.show()
                    binding.rvLikedPlaces.hide()
                }
            } catch (e: Exception) {
                android.util.Log.e("LikedPlaces", "Exception loading places", e)
                showToast("Error: ${e.localizedMessage ?: e.message}")
                binding.tvNoPlaces.show()
                binding.rvLikedPlaces.hide()
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
    
    private fun showLoading() {
        binding.swipeRefresh.isRefreshing = true
    }
    
    private fun hideLoading() {
        binding.swipeRefresh.isRefreshing = false
    }
    
    override fun onResume() {
        super.onResume()
        loadLikedPlaces()
    }
}
