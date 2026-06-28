package com.piecejob.core.ui.communication

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.piecejob.core.data.repository.CallRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CallViewModel @Inject constructor(
    private val repository: CallRepository
) : ViewModel() {

    private var currentCallId: String? = null

    fun initiateCall(jobId: String, receiverId: String) {
        android.util.Log.d("FORENSIC", "CALL_SIGNAL_SENT | Job: $jobId | To: $receiverId")
        viewModelScope.launch {
            val res = repository.logCallInitiation(jobId, receiverId)
            if (res.success && res.data != null) {
                currentCallId = res.data.callId
                android.util.Log.d("FORENSIC", "CALL_RECORD_CREATED | ID: $currentCallId")
            } else {
                android.util.Log.e("FORENSIC", "CALL_INIT_FAILED | Error: ${res.message}")
            }
        }
    }

    fun endCall(status: String, duration: Int) {
        val callId = currentCallId ?: return
        android.util.Log.d("FORENSIC", "CALL_ENDED | Status: $status | Duration: $duration")
        viewModelScope.launch {
            repository.updateCallStatus(callId, status, duration)
        }
    }

    fun setCallId(callId: String) {
        currentCallId = callId
    }
}
