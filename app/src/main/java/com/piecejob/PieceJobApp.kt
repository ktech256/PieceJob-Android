package com.piecejob

import android.app.Application
import com.google.android.gms.maps.MapsInitializer
import com.google.android.libraries.places.api.Places
import com.google.firebase.FirebaseApp
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class PieceJobApp : Application() {
    override fun onCreate() {
        android.util.Log.e("PIECEJOB_STARTUP", "Application started")
        super.onCreate()

        // FORENSIC: Maps/Navigation Initialization
        try {
            // Initialize Maps SDK with a fallback-safe approach
            MapsInitializer.initialize(this, MapsInitializer.Renderer.LATEST) { renderer ->
                when (renderer) {
                    MapsInitializer.Renderer.LATEST -> android.util.Log.d("MAPS_DEBUG", "The latest version of the renderer is used.")
                    MapsInitializer.Renderer.LEGACY -> android.util.Log.d("MAPS_DEBUG", "The legacy version of the renderer is used.")
                }
            }
        } catch (e: Exception) {
            android.util.Log.e("MAPS_DEBUG", "MapsInitializer failed: ${e.message}")
        }
        
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

                // FORENSIC: Signature Audit
                try {
                    val info = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
                        packageManager.getPackageInfo(packageName, android.content.pm.PackageManager.GET_SIGNING_CERTIFICATES)
                    } else {
                        @Suppress("DEPRECATION")
                        packageManager.getPackageInfo(packageName, android.content.pm.PackageManager.GET_SIGNATURES)
                    }
                    val signatures = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
                        info.signingInfo?.signingCertificateHistory
                    } else {
                        @Suppress("DEPRECATION")
                        info.signatures
                    }
                    signatures?.forEach { signature ->
                        val md = java.security.MessageDigest.getInstance("SHA-1")
                        val publicKey = md.digest(signature.toByteArray())
                        val hexString = publicKey.joinToString(":") { "%02X".format(it) }
                        android.util.Log.e("PIECEJOB_FCM", "SHA-1: $hexString")
                    }
                } catch (e: Exception) {
                    android.util.Log.e("PIECEJOB_FCM", "Failed to get SHA-1: ${e.message}")
                }

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
