package com.touristguide.app.ui.splash

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import com.touristguide.app.TouristGuideApp
import com.touristguide.app.ui.auth.AuthActivity
import com.touristguide.app.ui.main.MainActivity

class SplashActivity : AppCompatActivity() {
    
    private val TAG = "SplashActivity"
    private val splashDelay = 2000L // 2 seconds
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        try {
            Log.d(TAG, "SplashActivity started")
            
            // Check if user is already logged in
            Handler(Looper.getMainLooper()).postDelayed({
                try {
                    navigateToNextScreen()
                } catch (e: Exception) {
                    Log.e(TAG, "Error navigating to next screen", e)
                    // Fallback to AuthActivity
                    startActivity(Intent(this, AuthActivity::class.java))
                    finish()
                }
            }, splashDelay)
        } catch (e: Exception) {
            Log.e(TAG, "Error in onCreate", e)
            // Fallback to AuthActivity
            startActivity(Intent(this, AuthActivity::class.java))
            finish()
        }
    }
    
    private fun navigateToNextScreen() {
        try {
            val preferenceManager = (application as TouristGuideApp).preferenceManager
            
            val intent = if (preferenceManager.isLoggedIn()) {
                Log.d(TAG, "User is logged in, going to MainActivity")
                Intent(this, MainActivity::class.java)
            } else {
                Log.d(TAG, "User not logged in, going to AuthActivity")
                Intent(this, AuthActivity::class.java)
            }
            
            startActivity(intent)
            finish()
        } catch (e: Exception) {
            Log.e(TAG, "Error in navigateToNextScreen", e)
            throw e
        }
    }
}
