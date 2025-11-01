package com.touristguide.app.ui.placedetails

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.touristguide.app.R
import com.touristguide.app.databinding.ItemImageSlideBinding

class ImageSliderAdapter(
    private val imageUrls: List<String>
) : RecyclerView.Adapter<ImageSliderAdapter.ImageViewHolder>() {
    
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ImageViewHolder {
        val binding = ItemImageSlideBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ImageViewHolder(binding)
    }
    
    override fun onBindViewHolder(holder: ImageViewHolder, position: Int) {
        holder.bind(imageUrls[position])
    }
    
    override fun getItemCount(): Int = imageUrls.size
    
    class ImageViewHolder(
        private val binding: ItemImageSlideBinding
    ) : RecyclerView.ViewHolder(binding.root) {
        
        fun bind(imageUrl: String) {
            Glide.with(itemView.context)
                .load(imageUrl)
                .placeholder(R.drawable.ic_logo)
                .error(R.drawable.ic_logo)
                .centerCrop()
                .into(binding.ivSlide)
        }
    }
}
