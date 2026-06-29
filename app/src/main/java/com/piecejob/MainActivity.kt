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
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

import com.piecejob.core.data.local.SessionManager
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

    private val incomingIntent = MutableStateFlow<Intent?>(null)

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
        incomingIntent.value = intent
        android.util.Log.d("FORENSIC", "MAIN_ACTIVITY_ON_NEW_INTENT | Type: ${intent.getStringExtra("type")}")
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        incomingIntent.value = intent
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
                val currentIntent by incomingIntent.collectAsState()

                // Handle Notification Deep Links & Incoming Calls
                LaunchedEffect(currentIntent) {
                    val intent = currentIntent ?: return@LaunchedEffect
                    val type = intent.getStringExtra("type")
                    val jobId = intent.getStringExtra("jobId")
                    
                    android.util.Log.d("FORENSIC", "MAIN_ACTIVITY_INTENT_PROCESS | Type: $type | Job: $jobId")

                    if (type == "VERIFICATION_UPDATE") {
                        navController.navigate("verification_docs") {
                            popUpTo(navController.graph.findStartDestination().id) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    } else if (type == "NEW_JOB_BROADCAST" && jobId != null) {
                        android.util.Log.d("FCM_NAV", "Tapped broadcast notification for $jobId")
                        navController.navigate(Screen.ProviderHome.route)
                    } else if (type == "INCOMING_CALL") {
                        val callerId = intent.getStringExtra("callerId")
                        val callId = intent.getStringExtra("callId")
                        val callerName = intent.getStringExtra("callerName")
                        val callerPhone = intent.getStringExtra("callerPhone")
                        val callerPhoto = intent.getStringExtra("callerPhoto")
                        
                        if (jobId != null && callerId != null && callId != null) {
                            android.util.Log.d("FORENSIC", "INCOMING_CALL_INTENT | Navigating to IncomingCall screen. From: $callerName")
                            navController.navigate(Screen.IncomingCall.passArgs(jobId, callerId, callId, callerName ?: "Someone", callerPhone ?: "", callerPhoto)) {
                                launchSingleTop = true
                            }
                        } else {
                            android.util.Log.e("FORENSIC", "INCOMING_CALL_INTENT | Missing required args. JobId=$jobId, callerId=$callerId, callId=$callId")
                        }
                    }
                    
                    incomingIntent.value = null
                }

                // GLOBAL SOCKET CONNECTION & FCM SYNC
                val authState by authViewModel.authState.collectAsState()
                LaunchedEffect(authState) {
                    if (authViewModel.isLoggedIn()) {
                        try {
                            android.util.Log.d("SOCKET_AUDIT", "Connecting socket globally...")
                            socketManager.connect("https://piecejob-backend.onrender.com")
                            
                            val userId = sessionManager.getUserId()
                            if (userId != null) {
                                socketManager.joinUser(userId)
                                android.util.Log.d("SOCKET_AUDIT", "Joined user room: user_$userId")
                            }

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
                    } else {
                        socketManager.disconnect()
                    }
                }
                
                NavGraph(
                    navController = navController,
                    authViewModel = authViewModel
                )

                // GLOBAL OBSERVER: Job Completion -> Auto Rating (Issue 2)
                LaunchedEffect(Unit) {
                    socketManager.statusEventFlow.collect { event ->
                        android.util.Log.d("FORENSIC", "GLOBAL_NAV_OBSERVER | Received status_updated: ${event.status} for Job: ${event.jobId}")
                        if (event.status == "COMPLETED") {
                            callManager.disconnect("Ended")
                            navController.navigate(Screen.Rating.passJobId(event.jobId)) {
                                launchSingleTop = true
                            }
                        }
                    }
                }

                // GLOBAL OBSERVER: Incoming Calls (Issue 2)
                LaunchedEffect(Unit) {
                    socketManager.callEventFlow.collect { json ->
                        android.util.Log.d("FORENSIC", "GLOBAL_NAV_OBSERVER | Received incoming_call_intent: $json")
                        val jobId = json.optString("jobId")
                        val callerId = json.optString("callerId")
                        val callId = json.optString("callId")
                        val callerName = json.optString("callerName")
                        val callerPhone = json.optString("callerPhone")
                        val callerPhoto = json.optString("callerPhoto")
                        
                        if (callManager.isCallActive.value) {
                            android.util.Log.d("FORENSIC", "CALL_BUSY | From: $callerName")
                            socketManager.sendCallSignal(jobId, callerId, "BUSY")
                        } else {
                            android.util.Log.d("FORENSIC", "CALL_RINGING | Navigating to IncomingCall screen. From: $callerName | CallID: $callId")
                            navController.navigate(Screen.IncomingCall.passArgs(jobId, callerId, callId, callerName, callerPhone, callerPhoto)) {
                                launchSingleTop = true
                            }
                        }
                    }
                }

                // GLOBAL OBSERVER: Chat Notifications (Issue 1)
                LaunchedEffect(Unit) {
                    socketManager.messageEventFlow.collect { json ->
                        val jobId = json.optString("jobId")
                        val senderJson = json.optJSONObject("senderId")
                        val senderName = senderJson?.optString("firstName") ?: "Someone"
                        android.util.Log.d("FORENSIC", "CHAT_SOCKET_RECEIVED | From: $senderName")
                        // TODO: Show in-app notification banner if not on chat screen
                    }
                }
            }
        }
    }
}
