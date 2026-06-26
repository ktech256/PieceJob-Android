package com.piecejob.customer.ui.tracking

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.piecejob.core.data.repository.JobRepository
import com.piecejob.core.data.remote.dto.JobDto
import com.piecejob.core.data.remote.dto.ProviderDto
import com.piecejob.core.socket.SocketManager
import com.piecejob.core.data.local.SessionManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import javax.inject.Inject

@HiltViewModel
class JobTrackingViewModel @Inject constructor(
    private val jobRepository: JobRepository,
    private val socketManager: SocketManager,
    private val sessionManager: SessionManager
) : ViewModel() {

    private val _job = MutableStateFlow<JobDto?>(null)
    val job: StateFlow<JobDto?> = _job

    private val _nearbyProviders = MutableStateFlow<List<ProviderDto>>(emptyList())
    val nearbyProviders: StateFlow<List<ProviderDto>> = _nearbyProviders

    private val _providerLocation = MutableStateFlow<Pair<Double, Double>?>(null)
    val providerLocation: StateFlow<Pair<Double, Double>?> = _providerLocation

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    fun initTracking(jobId: String) {
        viewModelScope.launch {
            _isLoading.value = true
            val response = jobRepository.getJobById(jobId)
            if (response.success && response.data != null) {
                _job.value = response.data
                
                // Initialize Socket
                socketManager.connect("https://piecejob-backend.onrender.com")
                socketManager.joinJob(jobId)
                sessionManager.getUserId()?.let { socketManager.joinUser(it) }

                setupSocketListeners(jobId)
                
                // If still searching, load nearby providers
                if (isSearching(response.data.status)) {
                    startNearbyProvidersPolling()
                }
            } else {
                _error.value = response.error?.message ?: "Failed to load job details"
            }
            _isLoading.value = false
        }
    }

    private fun setupSocketListeners(jobId: String) {
        socketManager.onLocationUpdated { lat, lng ->
            android.util.Log.d("JobTracking", "Provider location update: $lat, $lng")
            _providerLocation.value = lat to lng
        }

        socketManager.onStatusUpdated { status ->
            android.util.Log.d("JobTracking", "Job status update: $status")
            _job.value = _job.value?.copy(status = status)
            if (!isSearching(status)) {
                _nearbyProviders.value = emptyList() // Clear nearby when assigned
            }
        }

        socketManager.onJobAccepted { acceptedJobId, providerId ->
            if (acceptedJobId == jobId) {
                android.util.Log.d("JobTracking", "Job accepted by provider: $providerId")
                refreshJobDetails(jobId)
            }
        }
    }

    private fun refreshJobDetails(jobId: String) {
        viewModelScope.launch {
            val response = jobRepository.getJobById(jobId)
            if (response.success) {
                _job.value = response.data
            }
        }
    }

    private fun startNearbyProvidersPolling() {
        viewModelScope.launch {
            while (isSearching(_job.value?.status ?: "")) {
                val coords = _job.value?.location?.coordinates
                if (coords != null && coords.size >= 2) {
                    val res = jobRepository.getOnlineProviders(lat = coords[1], lng = coords[0])
                    if (res.success) {
                        _nearbyProviders.value = res.data ?: emptyList()
                    }
                }
                delay(10000) // Poll every 10 seconds while searching
            }
        }
    }

    private fun isSearching(status: String): Boolean {
        return status == "BROADCASTED" || status == "BROADCASTING" || status == "PAYMENT_PENDING" || status == "BOOKING_FEE_PAID" || status == "DRAFT"
    }

    fun cancelJob() {
        _job.value?.let {
            viewModelScope.launch {
                val res = jobRepository.cancelJob(it.id)
                if (res.success) {
                    // Navigate back or show cancelled state
                }
            }
        }
    }
}
