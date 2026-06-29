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
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
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
                
                if (jobId == callManager.activeJobId) {
                    when (signal) {
                        "ACCEPTED" -> {
                            Log.d("FORENSIC", "CALL_SIGNAL_RECEIVED | Remote Accepted")
                        }
                        "REJECTED" -> {
                            Log.d("FORENSIC", "CALL_SIGNAL_RECEIVED | Remote Rejected")
                            endCall("REJECTED", 0)
                        }
                        "BUSY" -> {
                            Log.d("FORENSIC", "CALL_SIGNAL_RECEIVED | Remote Busy")
                            endCall("BUSY", 0)
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
        viewModelScope.launch {
            val res = repository.logCallInitiation(jobId, receiverId)
            if (res.success && res.data != null) {
                callManager.currentCallId = res.data.callId
                Log.d("FORENSIC", "CALL_RECORD_CREATED | ID: ${callManager.currentCallId}")
                connectToLiveKit(jobId)
                startTimeoutCounter()
            } else {
                Log.e("FORENSIC", "CALL_INIT_FAILED | Error: ${res.message}")
                callManager.setCallActive(false)
            }
        }
    }

    fun acceptIncomingCall(jobId: String, callId: String, callerId: String) {
        if (callManager.isCallActive.value) return
        callManager.setCallActive(true)
        callManager.currentCallId = callId
        callManager.activeJobId = jobId
        callManager.targetUserId = callerId
        
        socketManager.sendCallSignal(jobId, callerId, "ACCEPTED")
        connectToLiveKit(jobId)
    }

    fun rejectIncomingCall(jobId: String, callerId: String) {
        socketManager.sendCallSignal(jobId, callerId, "REJECTED")
        endCall("REJECTED", 0)
    }

    private fun connectToLiveKit(jobId: String) {
        viewModelScope.launch {
            val tokenRes = repository.getLiveKitToken(jobId)
            if (tokenRes.success && tokenRes.data != null) {
                callManager.connect(tokenRes.data.token)
            }
        }
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

        viewModelScope.launch {
            val callId = callManager.currentCallId
            callManager.disconnect()
            
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
