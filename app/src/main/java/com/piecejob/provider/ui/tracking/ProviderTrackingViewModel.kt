package com.piecejob.provider.ui.tracking

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.piecejob.core.data.repository.JobRepository
import com.piecejob.core.data.remote.dto.*
import com.piecejob.core.location.LocationService
import com.piecejob.core.socket.SocketManager
import com.google.android.gms.maps.model.LatLng
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject
import kotlin.math.*

@HiltViewModel
class ProviderTrackingViewModel @Inject constructor(
    private val jobRepository: JobRepository,
    private val socketManager: SocketManager
) : ViewModel() {

    private val _job = MutableStateFlow<JobDto?>(null)
    val job: StateFlow<JobDto?> = _job

    private val _providerLocation = MutableStateFlow<LatLng?>(null)
    val providerLocation: StateFlow<LatLng?> = _providerLocation

    private val _eta = MutableStateFlow("Calculating...")
    val eta: StateFlow<String> = _eta

    private val _distance = MutableStateFlow("...")
    val distance: StateFlow<String> = _distance

    private val _showStartReminder = MutableStateFlow(false)
    val showStartReminder: StateFlow<Boolean> = _showStartReminder

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    private var trackingJob: Job? = null
    private var reminderTimer: Job? = null
    private var autoStartTimer: Job? = null

    fun initTracking(jobId: String) {
        android.util.Log.d("FORENSIC", "INIT_TRACKING_STARTED | Job: $jobId (Provider)")
        viewModelScope.launch {
            _isLoading.value = true
            
            // 1. Setup Sockets and Listeners FIRST
            LocationService.activeJobId = jobId
            socketManager.joinJob(jobId)
            observeStatusUpdates()

            // 2. Fetch State
            val res = jobRepository.getJobById(jobId)
            if (res.success && res.data != null) {
                val job = res.data
                Log.d("FORENSIC", "INITIAL_FETCH_COMPLETED | Status: ${job.status}")
                _job.value = job
                startLocationTracking()

                // Recovery logic: if already arrived but not started, start timer
                if (job.status == "ARRIVED") {
                    startReminderTimer()
                }
            } else {
                _error.value = res.message ?: "Failed to load job details"
            }
            _isLoading.value = false
        }
    }

    fun confirmDispatch() {
        val jobId = _job.value?.id ?: return
        viewModelScope.launch {
            _isLoading.value = true
            val res = jobRepository.confirmDispatch(jobId)
            if (res.success && res.data != null) {
                _job.value = res.data
            } else {
                _error.value = res.message ?: "Failed to confirm dispatch"
            }
            _isLoading.value = false
        }
    }

    private fun startLocationTracking() {
        trackingJob?.cancel()
        trackingJob = viewModelScope.launch {
            LocationService.currentLocation.collect { loc ->
                if (_job.value?.status == "COMPLETED" || _job.value?.status == "CANCELLED") {
                    trackingJob?.cancel()
                    return@collect
                }

                if (loc != null) {
                    val currentLatLng = LatLng(loc.latitude, loc.longitude)
                    _providerLocation.value = currentLatLng
                    calculateEtaAndDistance(currentLatLng)
                    checkArrival(currentLatLng)
                }
            }
        }
    }

    private fun calculateEtaAndDistance(providerLatLng: LatLng) {
        val dest = _job.value?.location?.coordinates ?: return
        if (dest.size < 2 || (dest[0] == 0.0 && dest[1] == 0.0)) {
            _eta.value = "Calculating..."
            _distance.value = "..."
            return
        }
        val destLatLng = LatLng(dest[1], dest[0])
        
        val distMeters = calculateDistance(providerLatLng, destLatLng)
        _distance.value = if (distMeters < 1000) "${distMeters.toInt()} m" else java.util.Locale.getDefault().let { 
            String.format(it, "%.1f km", distMeters / 1000) 
        }
        
        // Simple ETA calculation: assuming average speed of 40km/h (11m/s)
        val timeSeconds = distMeters / 11
        _eta.value = if (timeSeconds < 60) "1 min" else "${(timeSeconds / 60).toInt()} mins"
    }

    private fun checkArrival(currentLatLng: LatLng) {
        if (_job.value?.status != "EN_ROUTE") return
        
        val dest = _job.value?.location?.coordinates ?: return
        if (dest.size < 2 || (dest[0] == 0.0 && dest[1] == 0.0)) return
        val destLatLng = LatLng(dest[1], dest[0])
        
        val dist = calculateDistance(currentLatLng, destLatLng)
        if (dist <= 50) { // 50 meter radius
            markArrival()
        }
    }

    private fun markArrival() {
        viewModelScope.launch {
            val jobId = _job.value?.id ?: return@launch
            val res = jobRepository.markArrival(jobId)
            if (res.success && res.data != null) {
                _job.value = res.data
                startReminderTimer()
            }
        }
    }

    private fun startReminderTimer() {
        reminderTimer?.cancel()
        reminderTimer = viewModelScope.launch {
            delay(25 * 60 * 1000) // 25 minutes
            if (_job.value?.status == "ARRIVED") {
                _showStartReminder.value = true
                startAutoStartTimer()
            }
        }
    }

    private fun startAutoStartTimer() {
        autoStartTimer?.cancel()
        autoStartTimer = viewModelScope.launch {
            delay(15000) // 15 seconds
            if (_showStartReminder.value) {
                startJob()
            }
        }
    }

    fun startJob() {
        android.util.Log.d("ForensicLog", "PROVIDER_STARTED_JOB | Job: ${_job.value?.id}")
        _showStartReminder.value = false
        autoStartTimer?.cancel()
        reminderTimer?.cancel()
        
        viewModelScope.launch {
            val jobId = _job.value?.id ?: return@launch
            val loc = _providerLocation.value
            val providerCoords = if (loc != null) listOf(loc.longitude, loc.latitude) else null
            
            val res = jobRepository.startJob(jobId, providerCoords)
            if (res.success && res.data != null) {
                _job.value = res.data
                _error.value = null
            } else {
                _error.value = res.message ?: "Failed to start job"
            }
        }
    }

    fun completeJob() {
        android.util.Log.d("ForensicLog", "PROVIDER_COMPLETED_JOB | Job: ${_job.value?.id}")
        viewModelScope.launch {
            val jobId = _job.value?.id ?: return@launch
            val res = jobRepository.completeJob(jobId)
            if (res.success && res.data != null) {
                _job.value = res.data
                _error.value = null
                LocationService.activeJobId = null
                socketManager.leaveJob(jobId)
            } else {
                _error.value = res.message ?: "Failed to complete job"
            }
        }
    }

    fun cancelJob() {
        viewModelScope.launch {
            val jobId = _job.value?.id ?: return@launch
            val res = jobRepository.cancelJob(jobId)
            if (res.success) {
                _job.value = _job.value?.copy(status = "CANCELLED")
                LocationService.activeJobId = null
            } else {
                _error.value = res.message ?: "Failed to cancel job"
            }
        }
    }

    private fun observeStatusUpdates() {
        viewModelScope.launch {
            socketManager.statusEventFlow.collect { event ->
                if (event.jobId == _job.value?.id) {
                    _job.value = _job.value?.copy(status = event.status)
                    if (event.status == "STARTED") {
                        _showStartReminder.value = false
                        autoStartTimer?.cancel()
                        reminderTimer?.cancel()
                    }
                }
            }
        }
    }

    fun updateStatus(status: String) {
        _job.value = _job.value?.copy(status = status)
    }

    private fun calculateDistance(c1: LatLng, c2: LatLng): Double {
        val R = 6371e3
        val lat1 = c1.latitude * PI / 180
        val lat2 = c2.latitude * PI / 180
        val dLat = (c2.latitude - c1.latitude) * PI / 180
        val dLon = (c2.longitude - c1.longitude) * PI / 180

        val a = sin(dLat / 2).pow(2) + cos(lat1) * cos(lat2) * sin(dLon / 2).pow(2)
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))

        return R * c
    }

    override fun onCleared() {
        super.onCleared()
        trackingJob?.cancel()
        reminderTimer?.cancel()
        autoStartTimer?.cancel()
        _job.value?.id?.let { socketManager.leaveJob(it) }
    }
}
