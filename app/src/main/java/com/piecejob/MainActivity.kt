package com.piecejob

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import com.piecejob.core.ui.auth.AuthViewModel
import com.piecejob.core.ui.navigation.NavGraph
import com.piecejob.core.ui.theme.PieceJobTheme
import androidx.navigation.compose.rememberNavController
import dagger.hilt.android.AndroidEntryPoint
import androidx.activity.result.contract.ActivityResultContracts
import android.Manifest
import android.os.Build
import androidx.compose.runtime.LaunchedEffect
import androidx.navigation.NavGraph.Companion.findStartDestination

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    
    private val authViewModel: AuthViewModel by viewModels()

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

                // Handle Notification Deep Links
                LaunchedEffect(intent) {
                    val type = intent.getStringExtra("type")
                    if (type == "VERIFICATION_UPDATE") {
                        navController.navigate("verification_docs") {
                            popUpTo(navController.graph.findStartDestination().id) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
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
