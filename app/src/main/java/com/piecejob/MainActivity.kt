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
        // Handle results
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

                // Sync FCM Token on Startup if logged in
                LaunchedEffect(Unit) {
                    if (authViewModel.isLoggedIn()) {
                        try {
                            val token = FirebaseMessaging.getInstance().token.await()
                            android.util.Log.d("FCM", "Current token synced: $token")
                            userRepository.updateFcmToken(token)
                        } catch (e: Exception) {
                            android.util.Log.e("FCM", "Token sync failed", e)
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
