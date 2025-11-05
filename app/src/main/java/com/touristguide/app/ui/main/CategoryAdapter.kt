package com.touristguide.app.ui.main

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.touristguide.app.data.model.Category
import com.touristguide.app.databinding.ItemCategoryBinding

class CategoryAdapter(
    private val onCategoryClick: (Category) -> Unit
) : ListAdapter<Category, CategoryAdapter.CategoryViewHolder>(CategoryDiffCallback()) {
    
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CategoryViewHolder {
        val binding = ItemCategoryBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return CategoryViewHolder(binding)
    }
    
    override fun onBindViewHolder(holder: CategoryViewHolder, position: Int) {
        holder.bind(getItem(position))
    }
    
    inner class CategoryViewHolder(
        private val binding: ItemCategoryBinding
    ) : RecyclerView.ViewHolder(binding.root) {
        
        fun bind(category: Category) {
            binding.apply {
                tvCategoryName.text = category.name
                
                // Set icon based on category name
                val iconRes = when (category.name.lowercase()) {
                    "restaurant", "restaurants" -> com.touristguide.app.R.drawable.ic_restaurant
                    "hotel", "hotels" -> com.touristguide.app.R.drawable.ic_hotel
                    "beach", "beaches" -> com.touristguide.app.R.drawable.ic_beach
                    "park", "parks" -> com.touristguide.app.R.drawable.ic_park
                    "museum", "museums" -> com.touristguide.app.R.drawable.ic_museum
                    "shopping", "mall", "malls" -> com.touristguide.app.R.drawable.ic_shopping
                    else -> com.touristguide.app.R.drawable.ic_logo
                }
                ivCategoryIcon.setImageResource(iconRes)
                
                cardCategory.setOnClickListener {
                    onCategoryClick(category)
                }
            }
        }
    }
    
    class CategoryDiffCallback : DiffUtil.ItemCallback<Category>() {
        override fun areItemsTheSame(oldItem: Category, newItem: Category): Boolean {
            return oldItem.id == newItem.id
        }
        
        override fun areContentsTheSame(oldItem: Category, newItem: Category): Boolean {
            return oldItem == newItem
        }
    }
}
