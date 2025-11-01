package com.touristguide.app.ui.admin

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.touristguide.app.BuildConfig
import com.touristguide.app.R
import com.touristguide.app.data.model.Place
import com.touristguide.app.databinding.ItemPendingPlaceBinding

class PendingPlacesAdapter(
    private val onApproveClick: (Place) -> Unit,
    private val onDeleteClick: (Place) -> Unit
) : ListAdapter<Place, PendingPlacesAdapter.PlaceViewHolder>(PlaceDiffCallback()) {
    
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PlaceViewHolder {
        val binding = ItemPendingPlaceBinding.inflate(
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
        private val binding: ItemPendingPlaceBinding
    ) : RecyclerView.ViewHolder(binding.root) {
        
        fun bind(place: Place) {
            binding.apply {
                tvPlaceName.text = place.name
                tvLocation.text = place.location
                tvCategory.text = place.category.name
                tvAddedBy.text = "Added by: ${place.addedBy?.name ?: "Unknown"}"
                tvDescription.text = place.description
                
                // Load image
                val imageUrl = if (place.images.isNotEmpty()) {
                    BuildConfig.BASE_URL.removeSuffix("/") + place.images[0]
                } else {
                    ""
                }
                
                Glide.with(itemView.context)
                    .load(imageUrl)
                    .placeholder(R.drawable.ic_logo)
                    .error(R.drawable.ic_logo)
                    .centerCrop()
                    .into(ivPlaceImage)
                
                btnApprove.setOnClickListener {
                    onApproveClick(place)
                }
                
                btnDelete.setOnClickListener {
                    onDeleteClick(place)
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
