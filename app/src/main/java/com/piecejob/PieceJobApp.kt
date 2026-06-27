package com.piecejob

import android.app.Application
import com.google.android.libraries.places.api.Places
import com.google.firebase.FirebaseApp
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class PieceJobApp : Application() {
    override fun onCreate() {
        android.util.Log.e("PIECEJOB_STARTUP", "Application started")
        super.onCreate()
        
        // FORENSIC: Firebase Initialization Audit
        try {
            android.util.Log.e("PIECEJOB_FCM", "Checking Firebase Configuration")
            val app = FirebaseApp.initializeApp(this)
            if (app == null) {
                android.util.Log.e("FCM_AUDIT", "FIREBASE_INIT_FAILED: FirebaseApp.initializeApp returned null.")
            } else {
                val options = app.options
                android.util.Log.e("PIECEJOB_FCM", "Firebase Project ID: ${options.projectId}")
                android.util.Log.e("PIECEJOB_FCM", "Firebase App ID: ${options.applicationId}")
                android.util.Log.e("PIECEJOB_FCM", "GCM Sender ID: ${options.gcmSenderId}")
                android.util.Log.e("PIECEJOB_FCM", "API Key: ${options.apiKey?.take(5)}...")
                android.util.Log.e("PIECEJOB_FCM", "Package Name: $packageName")

                android.util.Log.e("PIECEJOB_FCM", "Requesting Firebase token")
                // Fetch token for startup log
                com.google.firebase.messaging.FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        android.util.Log.e("PIECEJOB_FCM", "Token = ${task.result}")
                    } else {
                        android.util.Log.e("PIECEJOB_FCM", "Token retrieval failed. Error type: ${task.exception?.javaClass?.simpleName}")
                        android.util.Log.e("PIECEJOB_FCM", "Error Message: ${task.exception?.message}")
                        
                        // Check if it's an FIS error specifically
                        if (task.exception?.message?.contains("FIS") == true) {
                            android.util.Log.e("PIECEJOB_FCM", "CRITICAL: Firebase Installations Service error detected. Check API Key restrictions in Google Cloud Console.")
                        }
                    }
                }
            }
        } catch (e: Exception) {
            android.util.Log.e("FCM_AUDIT", "FIREBASE_INIT_CRASH: ${e.message}", e)
        }
        
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
