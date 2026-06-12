package com.piecejob.core.ui.sos

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.piecejob.core.data.repository.SosRepository
import com.piecejob.core.location.LocationService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SosViewModel @Inject constructor(
    private val repository: SosRepository
) : ViewModel() {

    private val _activeIncidentId = MutableStateFlow<String?>(null)
    val activeIncidentId: StateFlow<String?> = _activeIncidentId

    fun triggerSos(context: Context, lat: Double, lng: Double, jobId: String?) {
        viewModelScope.launch {
            val response = repository.triggerSos(lat, lng, jobId)
            if (response.success && response.data != null) {
                _activeIncidentId.value = response.data._id
                
                // Activate High Frequency tracking in the service
                LocationService.startService(context)
                LocationService.isSosActive = true
                LocationService.activeIncidentId = response.data._id
            }
        }
    }

    fun resolveSos() {
        LocationService.isSosActive = false
        LocationService.activeIncidentId = null
        _activeIncidentId.value = null
    }

    override fun onCleared() {
        super.onCleared()
        resolveSos()
    }
}
