package com.touristguide.app.ui.addplace

import android.net.Uri
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.touristguide.app.R
import com.touristguide.app.databinding.ItemSelectedImageBinding

class SelectedImagesAdapter(
    private val onRemoveClick: (Uri) -> Unit
) : ListAdapter<Uri, SelectedImagesAdapter.ImageViewHolder>(UriDiffCallback()) {
    
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ImageViewHolder {
        val binding = ItemSelectedImageBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ImageViewHolder(binding)
    }
    
    override fun onBindViewHolder(holder: ImageViewHolder, position: Int) {
        holder.bind(getItem(position))
    }
    
    inner class ImageViewHolder(
        private val binding: ItemSelectedImageBinding
    ) : RecyclerView.ViewHolder(binding.root) {
        
        fun bind(uri: Uri) {
            Glide.with(itemView.context)
                .load(uri)
                .placeholder(R.drawable.ic_logo)
                .centerCrop()
                .into(binding.ivImage)
            
            binding.btnRemove.setOnClickListener {
                onRemoveClick(uri)
            }
        }
    }
    
    class UriDiffCallback : DiffUtil.ItemCallback<Uri>() {
        override fun areItemsTheSame(oldItem: Uri, newItem: Uri): Boolean {
            return oldItem == newItem
        }
        
        override fun areContentsTheSame(oldItem: Uri, newItem: Uri): Boolean {
            return oldItem == newItem
        }
    }
}
