package com.piecejob

import android.Manifest
import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
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
import com.piecejob.core.communication.CallManager
import com.piecejob.core.data.repository.UserRepository
import com.piecejob.core.socket.SocketManager
import com.piecejob.core.ui.auth.AuthViewModel
import com.piecejob.core.ui.navigation.NavGraph
import com.piecejob.core.ui.navigation.Screen
import com.piecejob.core.ui.theme.PieceJobTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

import com.piecejob.core.data.local.SessionManager
import com.piecejob.core.location.LocationService
import kotlinx.coroutines.flow.MutableStateFlow

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {
    
    private val authViewModel: AuthViewModel by viewModels()

    @Inject
    lateinit var userRepository: UserRepository

    @Inject
    lateinit var socketManager: SocketManager

    @Inject
    lateinit var callManager: CallManager
    
    @Inject
    lateinit var sessionManager: SessionManager

    private val lifecycleScope = kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.Main + kotlinx.coroutines.SupervisorJob())

    override fun onResume() {
        super.onResume()
        // FORENSIC REPAIR: Force token sync whenever app returns to foreground
        if (authViewModel.isLoggedIn()) {
            syncFcmToken()
        }
    }

    private fun syncFcmToken() {
        lifecycleScope.launch {
            try {
                android.util.Log.d("FCM_AUDIT", "Force syncing FCM token...")
                val token = FirebaseMessaging.getInstance().token.await()
                if (!token.isNullOrBlank()) {
                    userRepository.updateFcmToken(token)
                }
            } catch (e: Exception) {
                android.util.Log.e("FCM_AUDIT", "Manual token sync failed: ${e.message}")
            }
        }
    }

    // Forensic: Intent queue to ensure no signals are lost during UI transitions
    private val incomingIntentQueue = MutableStateFlow<Intent?>(null)

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        permissions.entries.forEach {
            android.util.Log.d("PERMISSIONS_AUDIT", "${it.key} granted: ${it.value}")
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        incomingIntentQueue.value = intent
        android.util.Log.d("FORENSIC", "MAIN_ACTIVITY_ON_NEW_INTENT | Type: ${intent.getStringExtra("type")}")
        
        // ISSUE 3: Ensure screen wakes up and activity is shown for broadcasts
        if (intent.getStringExtra("type") == "NEW_JOB_BROADCAST" || intent.getStringExtra("type") == "INCOMING_CALL") {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
                setShowWhenLocked(true)
                setTurnScreenOn(true)
            }
            val km = getSystemService(android.content.Context.KEYGUARD_SERVICE) as android.app.KeyguardManager
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                km.requestDismissKeyguard(this, null)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        incomingIntentQueue.value = intent
        android.util.Log.d("FORENSIC", "MAIN_ACTIVITY_ON_CREATE | Type: ${intent.getStringExtra("type")}")
        enableEdgeToEdge()

        // Call background/lockscreen support
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        } else {
            @Suppress("DEPRECATION")
            window.addFlags(
                android.view.WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                        android.view.WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON or
                        android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
            )
        }

        // ISSUE 3: Handle cold start with NEW_JOB_BROADCAST intent
        if (intent.getStringExtra("type") == "NEW_JOB_BROADCAST" || intent.getStringExtra("type") == "INCOMING_CALL") {
            val km = getSystemService(android.content.Context.KEYGUARD_SERVICE) as android.app.KeyguardManager
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                km.requestDismissKeyguard(this, null)
            }
        }

        // Request Critical Permissions for Real Life Testing
        val permissions = mutableListOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.RECORD_AUDIO
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissions.add(Manifest.permission.POST_NOTIFICATIONS)
        }
        requestPermissionLauncher.launch(permissions.toTypedArray())

        val isProvider = BuildConfig.FLAVOR == "provider"
        setContent {
            PieceJobTheme(isProvider = isProvider) {
                val navController = rememberNavController()
                val currentIntent by incomingIntentQueue.collectAsState()

                // ORCHESTRATOR: Handle All Navigation Signals (Intents & Deep Links)
                LaunchedEffect(currentIntent) {
                    val intent = currentIntent ?: return@LaunchedEffect
                    val type = intent.getStringExtra("type")
                    val jobId = intent.getStringExtra("jobId")
                    
                    android.util.Log.d("FORENSIC", "MAIN_ACTIVITY_NAV_SIGNAL | Type: $type | Job: $jobId")

                    when (type) {
                        "VERIFICATION_UPDATE" -> {
                            navController.navigate(Screen.VerificationDocs.route) {
                                popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                        "NEW_JOB_BROADCAST" -> {
                            if (jobId != null) {
                                android.util.Log.d("FCM_NAV", "Navigating to Dashboard for broadcast $jobId")
                                // Re-navigate to ensure the ProviderMainScreen is active and can show the popup
                                navController.navigate(Screen.Dashboard.route) {
                                    launchSingleTop = true
                                }
                            }
                        }
                        "INCOMING_CALL" -> {
                            val callerId = intent.getStringExtra("callerId")
                            val callId = intent.getStringExtra("callId")
                            val callerName = intent.getStringExtra("callerName")
                            val callerPhone = intent.getStringExtra("callerPhone")
                            val callerPhoto = intent.getStringExtra("callerPhoto")
                            val autoAccept = intent.getBooleanExtra("autoAccept", false)
                            
                            // Clear notification
                            jobId?.hashCode()?.let { id ->
                                val nm = getSystemService(android.content.Context.NOTIFICATION_SERVICE) as android.app.NotificationManager
                                nm.cancel(id)
                            }

                            if (jobId != null && callerId != null && callId != null) {
                                android.util.Log.d("FORENSIC", "INCOMING_CALL_SCREEN_LAUNCH | From: $callerName | AutoAccept: $autoAccept")
                                navController.navigate(Screen.IncomingCall.passArgs(jobId, callerId, callId, callerName ?: "Someone", callerPhone ?: "", callerPhoto, autoAccept)) {
                                    launchSingleTop = true
                                }
                            } else {
                                android.util.Log.e("FORENSIC", "INCOMING_CALL_INVALID_DATA | JobId=$jobId, callerId=$callerId, callId=$callId")
                            }
                        }
                    }
                    // Reset to null to avoid repeat triggers, but cold start intent is already handled
                    incomingIntentQueue.value = null
                }

                // GLOBAL SOCKET & FCM STATE MANAGEMENT
                val authState by authViewModel.authState.collectAsState()
                LaunchedEffect(authState) {
                    if (authViewModel.isLoggedIn()) {
                        try {
                            android.util.Log.d("SOCKET_AUDIT", "Connecting socket globally...")
                            socketManager.connect("https://piecejob-backend.onrender.com")
                            
                            // ISSUE 2: Start Location Service only for providers by default
                            // Customers only start it during active job tracking
                            if (isProvider) {
                                android.util.Log.d("LOCATION_AUDIT", "Starting LocationService for Provider")
                                LocationService.startService(this@MainActivity)
                            }

                            val userId = sessionManager.getUserId()
                            if (userId != null) {
                                socketManager.joinUser(userId)
                                android.util.Log.d("SOCKET_AUDIT", "Joined user room: user_$userId")
                            }

                            // JOIN WORKSPACE ROOM (ISSUE 1 FIX)
                            sessionManager.getCountryCode()?.let { code ->
                                socketManager.joinWorkspace(code)
                                android.util.Log.d("SOCKET_AUDIT", "Joined workspace room: workspace_$code")
                            }

                            syncFcmToken()
                        } catch (e: Exception) {
                            android.util.Log.e("GLOBAL_SYNC", "Startup sync failed: ${e.message}")
                        }
                    } else {
                        socketManager.disconnect()
                        LocationService.stopService(this@MainActivity)
                    }
                }
                
                NavGraph(
                    navController = navController,
                    authViewModel = authViewModel
                )

                // GLOBAL OBSERVER: Call Signals (Sync Actions)
                LaunchedEffect(Unit) {
                    socketManager.callSignalFlow.collect { json ->
                        val signal = json.optString("signal")
                        val jobId = json.optString("jobId")
                        if (signal == "ENDED" || signal == "REJECTED" || signal == "BUSY") {
                            jobId?.hashCode()?.let { id ->
                                val nm = getSystemService(android.content.Context.NOTIFICATION_SERVICE) as android.app.NotificationManager
                                nm.cancel(id)
                            }
                        }
                    }
                }

                // GLOBAL OBSERVER: Job Completion & Location Service Lifecycle (Issue 2)
                LaunchedEffect(Unit) {
                    socketManager.statusEventFlow.collect { event ->
                        android.util.Log.d("FORENSIC", "STATUS_OBSERVER | Job: ${event.jobId} Status: ${event.status}")
                        
                        val currentRoute = navController.currentDestination?.route

                        // ISSUE 1: Auto-navigate customer to negotiation when provider accepts
                        // GUARD: Don't interrupt if already on Tracking, Rating, or if job is terminal
                        if (!isProvider && event.status == "PROVIDER_ACCEPTED") {
                            val isAlreadyOnActionScreen = currentRoute?.contains("tracking") == true || 
                                                        currentRoute?.contains("rating") == true ||
                                                        currentRoute?.contains("negotiation") == true
                            
                            if (isAlreadyOnActionScreen) {
                                android.util.Log.d("FORENSIC", "STATUS_OBSERVER | Already on action screen ($currentRoute). Ignoring PROVIDER_ACCEPTED signal.")
                                return@collect
                            }

                            val providerId = event.providerInfo?.optString("_id") 
                                ?: event.providerInfo?.optString("id") 
                                ?: ""
                            if (providerId.isNotEmpty()) {
                                android.util.Log.d("FORENSIC", "STATUS_OBSERVER | Provider Accepted! Auto-navigating to Negotiation.")
                                navController.navigate(Screen.Negotiation.passArgs(event.jobId, providerId))
                            }
                        }

                        // Handle Location Service for Customers (Terminal States Only - Safety Net)
                        if (!isProvider) {
                            when (event.status) {
                                "COMPLETED", "CANCELLED", "RATED", "CLOSED" -> {
                                    android.util.Log.d("LOCATION_AUDIT", "Stopping LocationService for Customer - Job ended")
                                    LocationService.activeJobId = null
                                    LocationService.stopService(this@MainActivity)
                                }
                            }
                        }

                        if (event.status == "COMPLETED") {
                            callManager.disconnect("Ended")
                            
                            if (currentRoute?.startsWith("rating") == true) {
                                android.util.Log.d("FORENSIC", "STATUS_OBSERVER | Already on Rating screen. Skipping duplicate navigation.")
                                return@collect
                            }

                            android.util.Log.d("FORENSIC", "STATUS_OBSERVER | Job Completed! Force navigating to Rating.")
                            navController.navigate(Screen.Rating.passJobId(event.jobId)) {
                                popUpTo(Screen.Dashboard.route)
                                launchSingleTop = true
                            }
                        }
                    }
                }

                // GLOBAL OBSERVER: Socket Incoming Calls (Foreground)
                LaunchedEffect(Unit) {
                    socketManager.callEventFlow.collect { json ->
                        android.util.Log.d("FORENSIC", "SOCKET_CALL_OBSERVER | Received: $json")
                        val jobId = json.optString("jobId")
                        val callerId = json.optString("callerId")
                        val callId = json.optString("callId")
                        val callerName = json.optString("callerName")
                        val callerPhone = json.optString("callerPhone")
                        val callerPhoto = json.optString("callerPhoto")
                        
                        if (callManager.isCallActive.value) {
                            android.util.Log.d("FORENSIC", "CALL_ORCHESTRATOR | Busy replying to $callerName")
                            socketManager.sendCallSignal(jobId, callerId, "BUSY")
                        } else {
                            android.util.Log.d("FORENSIC", "CALL_ORCHESTRATOR | Prompting Incoming Call screen. From: $callerName")
                            navController.navigate(Screen.IncomingCall.passArgs(jobId, callerId, callId, callerName, callerPhone, callerPhoto)) {
                                launchSingleTop = true
                            }
                        }
                    }
                }

                // GLOBAL OBSERVER: Chat Notifications
                LaunchedEffect(Unit) {
                    socketManager.messageEventFlow.collect { json ->
                        val senderName = json.optJSONObject("senderId")?.optString("firstName") ?: "Someone"
                        android.util.Log.d("FORENSIC", "CHAT_NOTIFICATION_RECEIVED | From: $senderName")
                        // Implementation for in-app banner can be added here
                    }
                }

                // GLOBAL OBSERVER: FCM Token Repair Signal
                LaunchedEffect(Unit) {
                    socketManager.repairFcmFlow.collect { json ->
                        android.util.Log.d("FCM_AUDIT", "REPAIR_SIGNAL_RECEIVED: Attempting immediate sync...")
                        syncFcmToken()
                    }
                }
            }
        }
    }
}
