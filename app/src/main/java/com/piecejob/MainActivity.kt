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
import com.piecejob.core.utils.Constants
import com.piecejob.core.utils.ReferrerManager
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

import com.piecejob.core.data.local.SessionManager
import com.piecejob.core.location.LocationService
import kotlinx.coroutines.flow.MutableStateFlow

import com.google.android.libraries.navigation.NavigationApi
import com.google.android.libraries.navigation.Navigator

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

    @Inject
    lateinit var referrerManager: ReferrerManager

    private val lifecycleScope = kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.Main + kotlinx.coroutines.SupervisorJob())

    override fun onResume() {
        super.onResume()
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

        if (intent.getStringExtra("type") == "NEW_JOB_BROADCAST" || intent.getStringExtra("type") == "INCOMING_CALL") {
            val km = getSystemService(android.content.Context.KEYGUARD_SERVICE) as android.app.KeyguardManager
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                km.requestDismissKeyguard(this, null)
            }
        }

        val permissions = mutableListOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.CAMERA
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

                LaunchedEffect(currentIntent) {
                    val intent = currentIntent ?: return@LaunchedEffect
                    val type = intent.getStringExtra("type")
                    val jobId = intent.getStringExtra("jobId")
                    
                    if (!authViewModel.isLoggedIn()) {
                        referrerManager.startTracking { code ->
                            authViewModel.referralCode.value = code
                        }
                    }

                    val data = intent.data
                    val configReferralUrl = sessionManager.getReferralBaseUrl()
                    val configHost = try { android.net.Uri.parse(configReferralUrl).host } catch (e: Exception) { null }
                    
                    val isReferralHost = data?.host == configHost || 
                                        data?.host == Constants.PRODUCTION_DOMAIN || 
                                        data?.host == "www.${Constants.PRODUCTION_DOMAIN}"

                    if (data != null && isReferralHost && (data.path?.startsWith("/r/") == true || data.path?.contains("/referral/") == true)) {
                        val code = data.lastPathSegment
                        if (!code.isNullOrBlank()) {
                            android.util.Log.d("REFERRAL_AUDIT", "Detected Referral Code from Deep Link: $code")
                            authViewModel.referralCode.value = code
                            
                            if (sessionManager.isReferralEnabled() && !authViewModel.isLoggedIn()) {
                                navController.navigate(Screen.RegistrationDetails.route) {
                                    launchSingleTop = true
                                }
                            }
                        }
                    }

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
                            
                            jobId?.hashCode()?.let { id ->
                                val nm = getSystemService(android.content.Context.NOTIFICATION_SERVICE) as android.app.NotificationManager
                                nm.cancel(id)
                            }

                            if (jobId != null && callerId != null && callId != null) {
                                android.util.Log.d("FORENSIC", "INCOMING_CALL_SCREEN_LAUNCH | From: $callerName | AutoAccept: $autoAccept")
                                navController.navigate(Screen.IncomingCall.passArgs(jobId, callerId, callId, callerName ?: "Someone", callerPhone ?: "", callerPhoto, autoAccept)) {
                                    launchSingleTop = true
                                }
                            }
                        }
                        "PRICE_PROPOSAL", "PRICE_ACCEPTED", "PRICE_REJECTED", "PHOTO_REQUEST", "PHOTO_UPLOAD", "PHOTOS_SEEN" -> {
                            val otherUserId = intent.getStringExtra("senderId")
                            if (jobId != null && otherUserId != null) {
                                android.util.Log.d("FORENSIC", "NEGOTIATION_RECOVERY_SIGNAL | Job: $jobId | OtherUser: $otherUserId")
                                navController.navigate(Screen.Negotiation.passArgs(jobId, otherUserId)) {
                                    launchSingleTop = true
                                }
                            }
                        }
                    }
                    incomingIntentQueue.value = null
                }

                val authState by authViewModel.authState.collectAsState()
                LaunchedEffect(authState) {
                    if (authViewModel.isLoggedIn()) {
                        try {
                            android.util.Log.d("SOCKET_AUDIT", "Connecting socket globally...")
                            socketManager.connect(Constants.SOCKET_URL)
                            
                            if (isProvider) {
                                android.util.Log.d("LOCATION_AUDIT", "Starting LocationService for Provider")
                                LocationService.startService(this@MainActivity)
                            }

                            val userId = sessionManager.getUserId()
                            if (userId != null) {
                                socketManager.joinUser(userId)
                            }

                            sessionManager.getCountryCode()?.let { code ->
                                socketManager.joinWorkspace(code)
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

                LaunchedEffect(Unit) {
                    socketManager.statusEventFlow.collect { event ->
                        android.util.Log.d("FORENSIC", "STATUS_OBSERVER | Job: ${event.jobId} Status: ${event.status}")
                        
                        val currentRoute = navController.currentDestination?.route

                        if (!isProvider && event.status == "PROVIDER_ACCEPTED") {
                            val isAlreadyOnActionScreen = currentRoute?.contains("tracking") == true || 
                                                        currentRoute?.contains("rating") == true ||
                                                        currentRoute?.contains("negotiation") == true
                            
                            if (isAlreadyOnActionScreen) return@collect

                            val providerId = event.providerInfo?.optString("_id") 
                                ?: event.providerInfo?.optString("id") 
                                ?: ""
                            if (providerId.isNotEmpty()) {
                                navController.navigate(Screen.Negotiation.passArgs(event.jobId, providerId))
                            }
                        }

                        if (!isProvider) {
                            when (event.status) {
                                "COMPLETED", "CANCELLED", "RATED", "CLOSED" -> {
                                    LocationService.activeJobId = null
                                    LocationService.stopService(this@MainActivity)
                                }
                            }
                        }

                        if (event.status == "COMPLETED") {
                            callManager.disconnect("Ended")
                            
                            if (currentRoute?.startsWith("rating") == true) return@collect

                            navController.navigate(Screen.Rating.passJobId(event.jobId)) {
                                popUpTo(Screen.Dashboard.route)
                                launchSingleTop = true
                            }
                        }

                        if (isProvider) {
                            val terminalStates = listOf("COMPLETED", "CANCELLED", "CUSTOMER_CANCELLED", "PROVIDER_CANCELLED", "EXPIRED", "FAILED", "TIMED_OUT", "NO_PROVIDER_FOUND", "RATED", "CLOSED")
                            if (terminalStates.contains(event.status)) {
                                android.util.Log.d("FORENSIC", "NAV_TERMINATE | Terminal state ${event.status} detected.")
                                try {
                                    NavigationApi.getNavigator(this@MainActivity, object : NavigationApi.NavigatorListener {
                                        override fun onNavigatorReady(nav: Navigator) {
                                            nav.stopGuidance()
                                            nav.clearDestinations()
                                            nav.setAudioGuidance(Navigator.AudioGuidance.SILENT)
                                            nav.cleanup()
                                        }
                                        override fun onError(errorCode: Int) {}
                                    })
                                } catch (e: Exception) {}
                            }
                        }
                    }
                }

                LaunchedEffect(Unit) {
                    socketManager.callEventFlow.collect { json ->
                        val jobId = json.optString("jobId")
                        val callerId = json.optString("callerId")
                        val callId = json.optString("callId")
                        val callerName = json.optString("callerName")
                        val callerPhone = json.optString("callerPhone")
                        val callerPhoto = json.optString("callerPhoto")
                        
                        if (callManager.isCallActive.value) {
                            socketManager.sendCallSignal(jobId, callerId, "BUSY")
                        } else {
                            navController.navigate(Screen.IncomingCall.passArgs(jobId, callerId, callId, callerName, callerPhone, callerPhoto)) {
                                launchSingleTop = true
                            }
                        }
                    }
                }

                LaunchedEffect(Unit) {
                    socketManager.messageEventFlow.collect { json ->
                        val senderName = json.optJSONObject("senderId")?.optString("firstName") ?: "Someone"
                        android.util.Log.d("FORENSIC", "CHAT_NOTIFICATION_RECEIVED | From: $senderName")
                    }
                }

                LaunchedEffect(Unit) {
                    socketManager.repairFcmFlow.collect { json ->
                        syncFcmToken()
                    }
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        android.util.Log.d("FORENSIC", "MAIN_ACTIVITY_DESTROYED | Cleaning up navigation.")
        try {
            NavigationApi.getNavigator(this, object : NavigationApi.NavigatorListener {
                override fun onNavigatorReady(nav: Navigator) {
                    nav.cleanup()
                }
                override fun onError(errorCode: Int) {}
            })
        } catch (e: Exception) {}
    }
}
