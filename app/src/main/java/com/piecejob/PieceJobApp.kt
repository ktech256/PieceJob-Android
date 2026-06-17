package com.piecejob

import android.app.Application
import com.google.android.libraries.places.api.Places
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class PieceJobApp : Application() {
    override fun onCreate() {
        super.onCreate()
        
        // Initialize Google Places SDK
        val apiKey = BuildConfig.GOOGLE_MAPS_API_KEY
        if (apiKey.isBlank()) {
            android.util.Log.e("MAPS_DEBUG", "GOOGLE_MAPS_API_KEY is missing! Check your gradle.properties.")
        } else {
            android.util.Log.d("MAPS_DEBUG", "Using API key: ${apiKey.take(5)}...${apiKey.takeLast(5)}")
            
            if (!Places.isInitialized()) {
                Places.initialize(applicationContext, apiKey)
                android.util.Log.d("MAPS_DEBUG", "Places initialized")
            }
        }
        
        // Maps SDK initialization is often implicit in the first GoogleMap usage, 
        // but we can log that the configuration is ready.
        android.util.Log.d("MAPS_DEBUG", "Map SDK configuration ready")
    }
}
