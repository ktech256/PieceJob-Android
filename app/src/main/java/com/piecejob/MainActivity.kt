package com.piecejob

import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.rememberNavController
import com.google.firebase.messaging.FirebaseMessaging
import com.piecejob.core.data.repository.UserRepository
import com.piecejob.core.ui.auth.AuthViewModel
import com.piecejob.core.ui.navigation.NavGraph
import com.piecejob.core.ui.theme.PieceJobTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    
    private val authViewModel: AuthViewModel by viewModels()

    @Inject
    lateinit var userRepository: UserRepository

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        permissions.entries.forEach {
            android.util.Log.d("PERMISSIONS_AUDIT", "${it.key} granted: ${it.value}")
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Request Critical Permissions for Real Life Testing
        val permissions = mutableListOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissions.add(Manifest.permission.POST_NOTIFICATIONS)
        }
        requestPermissionLauncher.launch(permissions.toTypedArray())

        val isProvider = BuildConfig.FLAVOR == "provider"
        setContent {
            PieceJobTheme(isProvider = isProvider) {
                val navController = rememberNavController()

                // Handle Notification Deep Links & New Job Broadcasts
                LaunchedEffect(intent) {
                    val type = intent.getStringExtra("type")
                    val jobId = intent.getStringExtra("jobId")
                    
                    if (type == "VERIFICATION_UPDATE") {
                        navController.navigate("verification_docs") {
                            popUpTo(navController.graph.findStartDestination().id) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    } else if (type == "NEW_JOB_BROADCAST" && jobId != null) {
                        // Background tap handling
                        android.util.Log.d("FCM_NAV", "Tapped broadcast notification for $jobId")
                    }
                }

                // Sync FCM Token on Startup and Login
                val authState by authViewModel.authState.collectAsState()
                LaunchedEffect(authState) {
                    if (authViewModel.isLoggedIn()) {
                        try {
                            android.util.Log.d("FCM_AUDIT", "FORENSIC_STARTUP: Logged in user detected. Syncing...")
                            val token = FirebaseMessaging.getInstance().token.await()
                            if (token.isNullOrBlank()) {
                                android.util.Log.e("FCM_AUDIT", "FORENSIC_FAILED: Generated token is NULL or BLANK.")
                            } else {
                                android.util.Log.d("FCM_AUDIT", "FCM_TOKEN_ACQUIRED: Token acquired. Len=${token.length}")
                                android.util.Log.d("FCM_AUDIT", "FCM_UPLOAD_START: Sending to backend...")
                                val response = userRepository.updateFcmToken(token)
                                if (response.success) {
                                    android.util.Log.d("FCM_AUDIT", "FCM_UPLOAD_SUCCESS: Server accepted token.")
                                } else {
                                    android.util.Log.e("FCM_AUDIT", "FCM_UPLOAD_FAILED: Code=${response.error?.code}, Msg=${response.message}")
                                }
                            }
                        } catch (e: Exception) {
                            android.util.Log.e("FCM_AUDIT", "FORENSIC_CRITICAL: Generation/Upload failed. Error: ${e.message}", e)
                        }
                    }
                }
                
                NavGraph(
                    navController = navController,
                    authViewModel = authViewModel
                )
            }
        }
    }
}
