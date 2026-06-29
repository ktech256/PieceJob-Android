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
        Log.d("FORENSIC", "CALL_VIEWMODEL | initiateCall triggered. Job: $jobId | To: $receiverId")
        if (callManager.isCallActive.value) {
            Log.w("FORENSIC", "CALL_VIEWMODEL | initiateCall ignored - call already active")
            return
        }
        callManager.setCallActive(true) // This sets status to "Calling..."
        callManager.activeJobId = jobId
        callManager.targetUserId = receiverId
        
        Log.d("FORENSIC", "CALL_SIGNAL_OUTGOING | Signal logic starting")
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            val res = repository.logCallInitiation(jobId, receiverId)
            if (res.success && res.data != null) {
                Log.d("FORENSIC", "CALL_RECORD_CREATED | ID: ${res.data.callId}")
                callManager.currentCallId = res.data.callId
                
                Log.d("FORENSIC", "CALL_TOKEN_FETCH | Requesting LiveKit token")
                val tokenRes = repository.getLiveKitToken(jobId)
                if (tokenRes.success && tokenRes.data != null) {
                    Log.d("FORENSIC", "CALL_TOKEN_ACQUIRED | Length: ${tokenRes.data.token.length}")
                    callManager.connect(tokenRes.data.token)
                } else {
                    Log.e("FORENSIC", "CALL_TOKEN_FAILED | Error: ${tokenRes.message}")
                    endCallLocal("Failed")
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
             Log.d("FORENSIC", "CALL_VIEWMODEL | acceptIncomingCall: already in this call")
             return
        }
        
        Log.d("FORENSIC", "CALL_VIEWMODEL | acceptIncomingCall (Answer pressed) | ID: $callId")
        callManager.setCallActive(true) // Status -> Calling... (will be updated to Connecting... in connect)
        callManager.currentCallId = callId
        callManager.activeJobId = jobId
        callManager.targetUserId = callerId
        
        Log.d("FORENSIC", "CALL_SIGNAL_ANSWER | Sending ACCEPTED signal")
        socketManager.sendCallSignal(jobId, callerId, "ACCEPTED")
        
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            Log.d("FORENSIC", "CALL_TOKEN_FETCH | Requesting token for receiver")
            val tokenRes = repository.getLiveKitToken(jobId)
            if (tokenRes.success && tokenRes.data != null) {
                Log.d("FORENSIC", "CALL_TOKEN_ACQUIRED | Length: ${tokenRes.data.token.length}")
                callManager.connect(tokenRes.data.token)
            } else {
                Log.e("FORENSIC", "CALL_TOKEN_FAILED | Error: ${tokenRes.message}")
                endCallLocal("Failed")
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
        callManager.disconnect(status)
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
        callManager.disconnect("Ended")
        
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
