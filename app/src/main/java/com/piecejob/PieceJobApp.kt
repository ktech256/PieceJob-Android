package com.piecejob

import android.app.Application
import com.google.android.libraries.places.api.Places
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class PieceJobApp : Application() {
    override fun onCreate() {
        super.onCreate()
        
        // Initialize Google Places SDK
        if (!Places.isInitialized()) {
            Places.initialize(applicationContext, BuildConfig.GOOGLE_MAPS_API_KEY)
        }
    }
}
