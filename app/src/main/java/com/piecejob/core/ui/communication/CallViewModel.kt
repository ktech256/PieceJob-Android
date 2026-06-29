package com.piecejob.core.ui.communication

import android.app.Application
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.os.CountDownTimer
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.piecejob.core.communication.CallManager
import com.piecejob.core.data.repository.CallRepository
import com.piecejob.core.socket.SocketManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import javax.inject.Inject

@HiltViewModel
class CallViewModel @Inject constructor(
    application: Application,
    private val repository: CallRepository,
    private val socketManager: SocketManager,
    private val callManager: CallManager
) : AndroidViewModel(application) {

    private var mediaPlayer: MediaPlayer? = null
    private var timer: CountDownTimer? = null
    
    val connectionStatus: StateFlow<String> = callManager.connectionStatus
    val isCallActive: StateFlow<Boolean> = callManager.isCallActive
    val isMuted: StateFlow<Boolean> = callManager.isMuted
    val isSpeakerOn: StateFlow<Boolean> = callManager.isSpeakerOn

    private val _peerName = MutableStateFlow("")
    val peerName: StateFlow<String> = _peerName

    private val _peerPhoto = MutableStateFlow<String?>(null)
    val peerPhoto: StateFlow<String?> = _peerPhoto

    private val _callDuration = MutableStateFlow(0)
    val callDuration: StateFlow<Int> = _callDuration

    init {
        observeSignals()
    }

    private fun observeSignals() {
        socketManager.callSignalFlow
            .onEach { json ->
                val signal = json.optString("signal")
                val jobId = json.optString("jobId")
                
                Log.d("FORENSIC", "CALL_SIGNAL_OBSERVER | Received: $signal for Job: $jobId")
                
                if (jobId == callManager.activeJobId) {
                    when (signal) {
                        "ACCEPTED" -> {
                            Log.d("FORENSIC", "CALL_SIGNAL_RECEIVED | Remote Accepted")
                            // No action needed, LiveKit will handle the participant connection
                        }
                        "REJECTED" -> {
                            Log.d("FORENSIC", "CALL_SIGNAL_RECEIVED | Remote Rejected")
                            endCallLocal("REJECTED")
                        }
                        "BUSY" -> {
                            Log.d("FORENSIC", "CALL_SIGNAL_RECEIVED | Remote Busy")
                            endCallLocal("BUSY")
                        }
                        "ENDED" -> {
                            Log.d("FORENSIC", "CALL_SIGNAL_RECEIVED | Remote Ended")
                            endCallLocal("ENDED")
                        }
                    }
                }
            }
            .launchIn(viewModelScope)
    }

    fun initiateCall(jobId: String, receiverId: String) {
        if (callManager.isCallActive.value) return
        callManager.setCallActive(true)
        callManager.activeJobId = jobId
        callManager.targetUserId = receiverId
        
        Log.d("FORENSIC", "CALL_SIGNAL_SENT | Job: $jobId | To: $receiverId")
        // Use a scope that isn't tied to the ViewModel to ensure the call initiation and media connection finish
        // even if the user navigates away or the screen recomposes.
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            val res = repository.logCallInitiation(jobId, receiverId)
            if (res.success && res.data != null) {
                callManager.currentCallId = res.data.callId
                Log.d("FORENSIC", "CALL_RECORD_CREATED | ID: ${callManager.currentCallId}")
                val tokenRes = repository.getLiveKitToken(jobId)
                if (tokenRes.success && tokenRes.data != null) {
                    callManager.connect(tokenRes.data.token)
                }
            } else {
                Log.e("FORENSIC", "CALL_INIT_FAILED | Error: ${res.message}")
                callManager.setCallActive(false)
            }
        }
        startTimeoutCounter()
    }

    fun acceptIncomingCall(jobId: String, callId: String, callerId: String) {
        if (callManager.isCallActive.value && callManager.activeJobId == jobId) {
             Log.d("FORENSIC", "CALL_VIEWMODEL | Already in this call.")
             return
        }
        
        Log.d("FORENSIC", "CALL_VIEWMODEL | Accepting Call: $callId for Job: $jobId")
        callManager.setCallActive(true)
        callManager.currentCallId = callId
        callManager.activeJobId = jobId
        callManager.targetUserId = callerId
        
        socketManager.sendCallSignal(jobId, callerId, "ACCEPTED")
        
        // Use a persistent scope because IncomingCallScreen is about to be popped/cleared
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            val tokenRes = repository.getLiveKitToken(jobId)
            if (tokenRes.success && tokenRes.data != null) {
                Log.d("FORENSIC", "CALL_VIEWMODEL | Receiver LiveKit Token Acquired. Connecting...")
                callManager.connect(tokenRes.data.token)
            } else {
                Log.e("FORENSIC", "CALL_VIEWMODEL | Receiver failed to get token: ${tokenRes.message}")
            }
        }
    }

    fun rejectIncomingCall(jobId: String, callerId: String) {
        socketManager.sendCallSignal(jobId, callerId, "REJECTED")
        endCall("REJECTED", 0)
    }

    fun startRinging(context: android.content.Context) {
        try {
            val notification = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)
            mediaPlayer = MediaPlayer.create(context, notification)
            mediaPlayer?.isLooping = true
            mediaPlayer?.start()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun stopRinging() {
        mediaPlayer?.stop()
        mediaPlayer?.release()
        mediaPlayer = null
    }

    private fun startTimeoutCounter() {
        timer?.cancel()
        timer = object : CountDownTimer(30000, 1000) {
            override fun onTick(millisUntilFinished: Long) {}
            override fun onFinish() {
                if (callManager.currentCallId != null && callManager.connectionStatus.value != "Connected") {
                    endCall("MISSED", 0)
                }
            }
        }.start()
    }

    fun toggleMute() {
        callManager.toggleMute()
    }

    fun toggleSpeaker() {
        callManager.toggleSpeaker()
    }

    private fun endCallLocal(status: String) {
        stopRinging()
        timer?.cancel()
        callManager.disconnect()
    }

    fun endCall(status: String, duration: Int) {
        timer?.cancel()
        stopRinging()
        
        // Signal remote
        callManager.activeJobId?.let { jId ->
            callManager.targetUserId?.let { uId ->
                socketManager.sendCallSignal(jId, uId, "ENDED")
            }
        }

        val callId = callManager.currentCallId
        callManager.disconnect()
        
        // Persistent scope for DB update
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            if (callId != null) {
                Log.d("FORENSIC", "CALL_ENDED | Status: $status | Duration: $duration")
                repository.updateCallStatus(callId, status, duration)
            }
        }
    }

    fun setCallId(callId: String) {
        callManager.currentCallId = callId
    }

    override fun onCleared() {
        super.onCleared()
        stopRinging()
        timer?.cancel()
    }
}
