package com.piecejob.provider

import android.app.KeyguardManager
import android.content.Context
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.lifecycleScope
import com.piecejob.core.ui.theme.PieceJobTheme
import com.piecejob.provider.ui.components.JobRequestBanner
import com.piecejob.provider.ui.main.ProviderMainViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class IncomingJobActivity : ComponentActivity() {

    private val viewModel: ProviderMainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Ensure Activity shows over lock screen
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        } else {
            @Suppress("DEPRECATION")
            window.addFlags(
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                        WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON or
                        WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON or
                        WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD
            )
        }
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val keyguardManager = getSystemService(Context.KEYGUARD_SERVICE) as KeyguardManager
            keyguardManager.requestDismissKeyguard(this, null)
        }

        enableEdgeToEdge()

        setContent {
            PieceJobTheme(isProvider = true) {
                val activeJobRequest by viewModel.notificationState.activeJobRequest.collectAsState()
                val isAccepting by viewModel.notificationState.isAccepting.collectAsState()

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.5f)),
                    contentAlignment = Alignment.Center
                ) {
                    activeJobRequest?.let { job ->
                        JobRequestBanner(
                            job = job,
                            isAccepting = isAccepting,
                            onAccept = { 
                                viewModel.acceptJob(it)
                            },
                            onDecline = { 
                                viewModel.declineJob(it)
                                finish()
                            }
                        )
                    }
                }
            }
        }
        
        // Collect navigation events to finish activity on success
        lifecycleScope.launch {
            viewModel.navigationEvent.collect {
                // If we navigated (e.g. to Negotiation), close this overlay activity
                finish()
            }
        }

        // Finish if job is dismissed by termination signal
        lifecycleScope.launch {
            viewModel.notificationState.activeJobRequest.collect { job ->
                if (job == null) {
                    finish()
                }
            }
        }
    }
    
    override fun onPause() {
        super.onPause()
        // If the user navigates away or job is gone, close this activity
        if (viewModel.notificationState.activeJobRequest.value == null) {
            finish()
        }
    }
}
