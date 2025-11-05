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
    private val onDeleteClick: ((Place) -> Unit)? = null,
    private val showAdminActions: Boolean = false
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
                // Default: Hide everything first
                llAdminActions.visibility = android.view.View.GONE
                btnEdit.visibility = android.view.View.GONE
                btnDelete.visibility = android.view.View.GONE
                
                // Only show admin actions if enabled for this adapter instance
                if (showAdminActions) {
                    val permissions = place.permissions
                    
                    // Only show if explicitly allowed AND user is owner/admin
                    if (permissions != null) {
                        val isOwnerOrAdmin = permissions.isOwner == true || permissions.isAdmin == true
                        val canEdit = permissions.canEdit == true
                        val canDelete = permissions.canDelete == true
                        
                        if (isOwnerOrAdmin && (canEdit || canDelete)) {
                            llAdminActions.visibility = android.view.View.VISIBLE
                            
                            if (canEdit) {
                                btnEdit.visibility = android.view.View.VISIBLE
                                btnEdit.setOnClickListener {
                                    onEditClick?.invoke(place)
                                }
                            }
                            
                            if (canDelete) {
                                btnDelete.visibility = android.view.View.VISIBLE
                                btnDelete.setOnClickListener {
                                    onDeleteClick?.invoke(place)
                                }
                            }
                        }
                    }
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
