package com.piecejob.core.ui.communication

import android.media.MediaPlayer
import android.media.RingtoneManager
import android.os.CountDownTimer
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.piecejob.core.data.repository.CallRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CallViewModel @Inject constructor(
    private val repository: CallRepository
) : ViewModel() {

    private var currentCallId: String? = null
    private var mediaPlayer: MediaPlayer? = null
    private var timer: CountDownTimer? = null

    private val _isMuted = MutableStateFlow(false)
    val isMuted: StateFlow<Boolean> = _isMuted

    private val _isSpeakerOn = MutableStateFlow(false)
    val isSpeakerOn: StateFlow<Boolean> = _isSpeakerOn

    fun initiateCall(jobId: String, receiverId: String) {
        android.util.Log.d("FORENSIC", "CALL_SIGNAL_SENT | Job: $jobId | To: $receiverId")
        viewModelScope.launch {
            val res = repository.logCallInitiation(jobId, receiverId)
            if (res.success && res.data != null) {
                currentCallId = res.data.callId
                android.util.Log.d("FORENSIC", "CALL_RECORD_CREATED | ID: $currentCallId")
                startTimeoutCounter()
            } else {
                android.util.Log.e("FORENSIC", "CALL_INIT_FAILED | Error: ${res.message}")
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
                if (currentCallId != null) {
                    endCall("MISSED", 0)
                }
            }
        }.start()
    }

    fun toggleMute() {
        _isMuted.value = !_isMuted.value
    }

    fun toggleSpeaker() {
        _isSpeakerOn.value = !_isSpeakerOn.value
    }

    fun endCall(status: String, duration: Int) {
        timer?.cancel()
        stopRinging()
        val callId = currentCallId ?: return
        android.util.Log.d("FORENSIC", "CALL_ENDED | Status: $status | Duration: $duration")
        viewModelScope.launch {
            repository.updateCallStatus(callId, status, duration)
        }
    }

    fun setCallId(callId: String) {
        currentCallId = callId
    }

    override fun onCleared() {
        super.onCleared()
        stopRinging()
        timer?.cancel()
    }
}
