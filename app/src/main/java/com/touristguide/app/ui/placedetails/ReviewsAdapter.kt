package com.touristguide.app.ui.placedetails

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.touristguide.app.data.model.Review
import com.touristguide.app.databinding.ItemReviewBinding
import com.touristguide.app.utils.hide
import com.touristguide.app.utils.show

class ReviewsAdapter(
    private val currentUserId: String,
    private val onDeleteClick: (Review) -> Unit
) : ListAdapter<Review, ReviewsAdapter.ReviewViewHolder>(ReviewDiffCallback()) {
    
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ReviewViewHolder {
        val binding = ItemReviewBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ReviewViewHolder(binding)
    }
    
    override fun onBindViewHolder(holder: ReviewViewHolder, position: Int) {
        holder.bind(getItem(position))
    }
    
    inner class ReviewViewHolder(
        private val binding: ItemReviewBinding
    ) : RecyclerView.ViewHolder(binding.root) {
        
        fun bind(review: Review) {
            binding.apply {
                tvUserName.text = review.user.name
                tvRating.text = "⭐".repeat(review.rating)
                tvComment.text = review.comment
                tvDate.text = formatDate(review.createdAt)
                
                // Show delete button only for own reviews
                if (review.user.id == currentUserId) {
                    btnDelete.show()
                    btnDelete.setOnClickListener {
                        onDeleteClick(review)
                    }
                } else {
                    btnDelete.hide()
                }
            }
        }
        
        private fun formatDate(dateString: String): String {
            return try {
                // Simple date formatting - can be improved
                dateString.substring(0, 10)
            } catch (e: Exception) {
                dateString
            }
        }
    }
    
    class ReviewDiffCallback : DiffUtil.ItemCallback<Review>() {
        override fun areItemsTheSame(oldItem: Review, newItem: Review): Boolean {
            return oldItem.id == newItem.id
        }
        
        override fun areContentsTheSame(oldItem: Review, newItem: Review): Boolean {
            return oldItem == newItem
        }
    }
}
