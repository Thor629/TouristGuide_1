package com.touristguide.app.ui.main

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.touristguide.app.R
import com.touristguide.app.data.model.Place
import com.touristguide.app.databinding.ItemPlaceCardBinding
import com.touristguide.app.utils.loadImage

class PlacesAdapter(
    private val onPlaceClick: (Place) -> Unit,
    private val onEditClick: ((Place) -> Unit)? = null,
    private val onDeleteClick: ((Place) -> Unit)? = null
) : ListAdapter<Place, PlacesAdapter.PlaceViewHolder>(PlaceDiffCallback()) {
    
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PlaceViewHolder {
        val binding = ItemPlaceCardBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return PlaceViewHolder(binding)
    }
    
    override fun onBindViewHolder(holder: PlaceViewHolder, position: Int) {
        holder.bind(getItem(position))
    }
    
    inner class PlaceViewHolder(
        private val binding: ItemPlaceCardBinding
    ) : RecyclerView.ViewHolder(binding.root) {
        
        fun bind(place: Place) {
            binding.apply {
                tvPlaceName.text = place.name
                tvPlaceLocation.text = place.location
                tvCategory.text = place.category.name
                tvLikesCount.text = "${place.likesCount} likes"
                
                // Load image using extension function
                ivPlaceImage.loadImage(place.images.firstOrNull())
                
                // Show rating if available
                if (place.averageRating > 0) {
                    tvRating.text = String.format("%.1f ⭐", place.averageRating)
                } else {
                    tvRating.text = "New"
                }
                
                root.setOnClickListener {
                    onPlaceClick(place)
                }
                
                // Show/hide admin actions based on permissions
                // ONLY show if user is explicitly allowed (owner or admin)
                val permissions = place.permissions
                val canShowActions = permissions != null && 
                                    (permissions.isOwner == true || permissions.isAdmin == true) &&
                                    (permissions.canEdit == true || permissions.canDelete == true)
                
                if (canShowActions) {
                    llAdminActions.visibility = android.view.View.VISIBLE
                    
                    // Show edit button only if explicitly allowed
                    btnEdit.visibility = if (permissions?.canEdit == true) {
                        android.view.View.VISIBLE
                    } else {
                        android.view.View.GONE
                    }
                    
                    // Show delete button only if explicitly allowed
                    btnDelete.visibility = if (permissions?.canDelete == true) {
                        android.view.View.VISIBLE
                    } else {
                        android.view.View.GONE
                    }
                    
                    // Set click listeners
                    btnEdit.setOnClickListener {
                        onEditClick?.invoke(place)
                    }
                    
                    btnDelete.setOnClickListener {
                        onDeleteClick?.invoke(place)
                    }
                } else {
                    // Hide all admin actions for regular users
                    llAdminActions.visibility = android.view.View.GONE
                }
            }
        }
    }
    
    class PlaceDiffCallback : DiffUtil.ItemCallback<Place>() {
        override fun areItemsTheSame(oldItem: Place, newItem: Place): Boolean {
            return oldItem.id == newItem.id
        }
        
        override fun areContentsTheSame(oldItem: Place, newItem: Place): Boolean {
            return oldItem == newItem
        }
    }
}
