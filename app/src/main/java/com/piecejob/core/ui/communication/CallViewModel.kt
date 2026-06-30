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
                            // LiveKit Connected event in CallManager will handle the UI state transition to Connected
                        }
                        "REJECTED" -> {
                            Log.d("FORENSIC", "CALL_SIGNAL_RECEIVED | Remote Rejected")
                            endCallLocal("Declined")
                        }
                        "BUSY" -> {
                            Log.d("FORENSIC", "CALL_SIGNAL_RECEIVED | Remote Busy")
                            endCallLocal("Busy")
                        }
                        "ENDED" -> {
                            Log.d("FORENSIC", "CALL_SIGNAL_RECEIVED | Remote Ended")
                            endCallLocal("Ended")
                        }
                    }
                }
            }
            .launchIn(viewModelScope)
    }

    fun initiateCall(jobId: String, receiverId: String) {
        Log.d("FORENSIC", "CALL_BUTTON_CLICKED | Job: $jobId | To: $receiverId")
        if (callManager.isCallActive.value && callManager.activeJobId == jobId) {
            Log.w("FORENSIC", "CALL_INIT_IGNORED | Already active in this job")
            return
        }

        // WhatsApp Style: Immediate UI State Transition
        Log.d("FORENSIC", "CALL_SCREEN_LAUNCH | Triggering immediate state change")
        callManager.setCallActive(true) // Status -> Calling...
        callManager.activeJobId = jobId
        callManager.targetUserId = receiverId
        
        // Persistent logic flow (survives screen rotation/navigation)
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            Log.d("FORENSIC", "CALL_BACKEND_REQUEST | Creating session on server")
            val res = repository.logCallInitiation(jobId, receiverId)
            
            if (res.success && res.data != null) {
                val callId = res.data.callId
                Log.d("FORENSIC", "CALL_SESSION_CREATED | ID: $callId")
                callManager.currentCallId = callId
                
                Log.d("FORENSIC", "CALL_TOKEN_FETCH | Requesting LiveKit token")
                val tokenRes = repository.getLiveKitToken(jobId)
                if (tokenRes.success && tokenRes.data != null) {
                    Log.d("FORENSIC", "CALL_MEDIA_JOIN | Joining LiveKit room")
                    callManager.connect(tokenRes.data.token)
                } else {
                    Log.e("FORENSIC", "CALL_TOKEN_FAILED | ${tokenRes.message}")
                    endCallLocal("Failed")
                }
            } else {
                Log.e("FORENSIC", "CALL_BACKEND_FAILED | ${res.message}")
                endCallLocal("Failed")
            }
        }
        startTimeoutCounter()
    }

    fun acceptIncomingCall(jobId: String, callId: String, callerId: String) {
        Log.d("FORENSIC", "CALL_ACCEPT_CLICKED | Job: $jobId | Call: $callId")
        
        callManager.setCallActive(true) // Status -> Calling...
        callManager.currentCallId = callId
        callManager.activeJobId = jobId
        callManager.targetUserId = callerId
        
        Log.d("FORENSIC", "CALL_SIGNAL_ANSWER | Sending ACCEPTED signal")
        socketManager.sendCallSignal(jobId, callerId, "ACCEPTED")
        
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            Log.d("FORENSIC", "CALL_TOKEN_FETCH | Fetching LiveKit token for receiver")
            val tokenRes = repository.getLiveKitToken(jobId)
            if (tokenRes.success && tokenRes.data != null) {
                Log.d("FORENSIC", "CALL_MEDIA_JOIN | Connecting to LiveKit")
                callManager.connect(tokenRes.data.token)
            } else {
                Log.e("FORENSIC", "CALL_TOKEN_FAILED | ${tokenRes.message}")
                endCallLocal("Failed")
            }
        }
    }

    fun rejectIncomingCall(jobId: String, callerId: String) {
        Log.d("FORENSIC", "CALL_REJECT_CLICKED | Job: $jobId")
        socketManager.sendCallSignal(jobId, callerId, "REJECTED")
        endCallLocal("Declined")
    }

    fun startRinging(context: android.content.Context) {
        try {
            Log.d("FORENSIC", "CALL_UI | Starting ringing sound")
            val notification = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)
            mediaPlayer = MediaPlayer.create(context, notification)
            mediaPlayer?.isLooping = true
            mediaPlayer?.start()
        } catch (e: Exception) {
            Log.e("FORENSIC", "CALL_RING_ERROR", e)
        }
    }

    fun stopRinging() {
        Log.d("FORENSIC", "CALL_UI | Stopping ringing sound")
        mediaPlayer?.stop()
        mediaPlayer?.release()
        mediaPlayer = null
    }

    private fun startTimeoutCounter() {
        timer?.cancel()
        timer = object : CountDownTimer(35000, 1000) {
            override fun onTick(millisUntilFinished: Long) {}
            override fun onFinish() {
                if (callManager.isCallActive.value && callManager.connectionStatus.value != "Connected") {
                    Log.d("FORENSIC", "CALL_TIMEOUT | No answer after 35s")
                    endCall("Missed", 0)
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
        Log.d("FORENSIC", "CALL_LOCAL_END | Status: $status")
        stopRinging()
        timer?.cancel()
        callManager.disconnect(status)
    }

    fun endCall(status: String, duration: Int) {
        Log.d("FORENSIC", "CALL_USER_END | Requesting termination. Final duration: $duration")
        timer?.cancel()
        stopRinging()
        
        val jId = callManager.activeJobId
        val uId = callManager.targetUserId
        val callId = callManager.currentCallId

        if (jId != null && uId != null) {
            Log.d("FORENSIC", "CALL_SIGNAL_END | Notifying other participant")
            socketManager.sendCallSignal(jId, uId, "ENDED")
        }

        callManager.disconnect("Ended")
        
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            if (callId != null) {
                Log.d("FORENSIC", "CALL_DB_SYNC | Updating status: $status")
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
