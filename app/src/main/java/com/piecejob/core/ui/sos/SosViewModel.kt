package com.piecejob.core.ui.sos

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.piecejob.core.data.repository.SosRepository
import com.piecejob.core.socket.SocketManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SosViewModel @Inject constructor(
    private val repository: SosRepository,
    private val socketManager: SocketManager
) : ViewModel() {

    private val _activeIncidentId = MutableStateFlow<String?>(null)
    val activeIncidentId: StateFlow<String?> = _activeIncidentId

    private var pingJob: Job? = null

    fun triggerSos(lat: Double, lng: Double, jobId: String?) {
        viewModelScope.launch {
            val response = repository.triggerSos(lat, lng, jobId)
            if (response.success && response.data != null) {
                _activeIncidentId.value = response.data._id
                startHighFrequencyTracking(response.data._id)
            }
        }
    }

    private fun startHighFrequencyTracking(incidentId: String) {
        pingJob?.cancel()
        pingJob = viewModelScope.launch {
            while (true) {
                // In production, get real live location from a LocationProvider
                val lat = -26.2041
                val lng = 28.0473
                socketManager.sendSosGpsPing(incidentId, lat, lng)
                delay(5000)
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        pingJob?.cancel()
    }
}
