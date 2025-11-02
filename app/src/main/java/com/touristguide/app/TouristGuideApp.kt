package com.touristguide.app

import android.app.Application
import com.touristguide.app.data.api.RetrofitClient
import com.touristguide.app.utils.PreferenceManager

class TouristGuideApp : Application() {
    
    lateinit var preferenceManager: PreferenceManager
        private set
    
    override fun onCreate() {
        super.onCreate()

        // Initialize preference manager
        preferenceManager = PreferenceManager(this)
        
        // Initialize Retrofit with preference manager
        RetrofitClient.init(preferenceManager)
    }
}
