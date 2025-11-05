package com.touristguide.app.utils

import android.content.Context
import android.view.View
import android.widget.ImageView
import android.widget.Toast
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.google.android.material.snackbar.Snackbar
import com.touristguide.app.BuildConfig
import com.touristguide.app.R

fun Context.showToast(message: String, duration: Int = Toast.LENGTH_SHORT) {
    Toast.makeText(this, message, duration).show()
}

fun View.show() {
    visibility = View.VISIBLE
}

fun View.hide() {
    visibility = View.GONE
}

fun View.invisible() {
    visibility = View.INVISIBLE
}

fun View.showSnackbar(message: String, duration: Int = Snackbar.LENGTH_SHORT) {
    Snackbar.make(this, message, duration).show()
}

fun String.isValidEmail(): Boolean {
    return android.util.Patterns.EMAIL_ADDRESS.matcher(this).matches()
}

fun String.isValidPassword(): Boolean {
    return this.length >= 6
}

/**
 * Load image from URL or path into ImageView
 * Handles both full URLs (http/https) and relative paths
 */
fun ImageView.loadImage(imagePath: String?, placeholder: Int = R.drawable.ic_logo) {
    if (imagePath.isNullOrEmpty()) {
        setImageResource(placeholder)
        return
    }
    
    val imageUrl = if (imagePath.startsWith("http://") || imagePath.startsWith("https://")) {
        // Full URL - use as is
        imagePath
    } else {
        // Relative path - prepend base URL
        BuildConfig.BASE_URL.removeSuffix("/") + imagePath
    }
    
    Glide.with(this.context)
        .load(imageUrl)
        .placeholder(placeholder)
        .error(placeholder)
        .diskCacheStrategy(DiskCacheStrategy.ALL)
        .centerCrop()
        .into(this)
}
