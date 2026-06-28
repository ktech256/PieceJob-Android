package com.piecejob.customer.ui.tracking

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.piecejob.core.data.repository.JobRepository
import com.piecejob.core.data.remote.dto.JobDto
import com.piecejob.core.data.remote.dto.ProviderDto
import com.piecejob.core.socket.SocketManager
import com.piecejob.core.data.local.SessionManager
import com.google.android.gms.maps.model.LatLng
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

    private val _providerHeading = MutableStateFlow(0f)
    val providerHeading: StateFlow<Float> = _providerHeading

    private val _routePoints = MutableStateFlow<List<LatLng>>(emptyList())
    val routePoints: StateFlow<List<LatLng>> = _routePoints

    private val _eta = MutableStateFlow("Calculating...")
    val eta: StateFlow<String> = _eta

    private val _distance = MutableStateFlow("")
    val distance: StateFlow<String> = _distance

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
        socketManager.onLocationUpdated { lat, lng, heading ->
            android.util.Log.d("JobTracking", "Provider location update: $lat, $lng, heading: $heading")
            _providerLocation.value = lat to lng
            _providerHeading.value = heading
            calculateLiveMetrics(lat, lng)
        }

        socketManager.onStatusUpdated { status ->
            android.util.Log.d("ForensicLog", "JOB_STATUS_EVENT | Status: $status")
            // CRITICAL: Immediately refresh full job details on any status change to ensure consistency
            _job.value?.id?.let { refreshJobDetails(it) }
            
            if (!isSearching(status)) {
                _nearbyProviders.value = emptyList() 
            }
        }

        socketManager.onJobAccepted { acceptedJobId, providerId ->
            if (acceptedJobId == _job.value?.id) {
                android.util.Log.d("ForensicLog", "JOB_ACCEPTED_EVENT | Refreshing details...")
                refreshJobDetails(acceptedJobId)
            }
        }
    }

    private fun calculateLiveMetrics(lat: Double, lng: Double) {
        val dest = _job.value?.location?.coordinates ?: return
        val destLat = dest[1]
        val destLng = dest[0]

        val distMeters = calculateDistance(lat, lng, destLat, destLng)
        _distance.value = if (distMeters < 1000) "${distMeters.toInt()} m" else String.format("%.1f km", distMeters / 1000)

        // Assume 40km/h average speed (11.1 m/s)
        val timeSeconds = distMeters / 11.1
        _eta.value = if (timeSeconds < 60) "1 min" else "${(timeSeconds / 60).toInt()} mins"
    }

    private fun calculateDistance(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val r = 6371e3
        val phi1 = lat1 * Math.PI / 180
        val phi2 = lat2 * Math.PI / 180
        val deltaPhi = (lat2 - lat1) * Math.PI / 180
        val deltaLambda = (lon2 - lon1) * Math.PI / 180

        val a = Math.sin(deltaPhi / 2) * Math.sin(deltaPhi / 2) +
                Math.cos(phi1) * Math.cos(phi2) *
                Math.sin(deltaLambda / 2) * Math.sin(deltaLambda / 2)
        val c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a))

        return r * c
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
                delay(10000)
            }
        }
    }

    private fun isSearching(status: String): Boolean {
        return status == "BROADCASTED" || status == "BROADCASTING" || status == "PAYMENT_PENDING" || status == "BOOKING_FEE_PAID" || status == "DRAFT"
    }

    fun cancelJob() {
        _job.value?.let {
            android.util.Log.d("ForensicLog", "CUSTOMER_CANCEL_REQUEST | Job: ${it.id}")
            viewModelScope.launch {
                val res = jobRepository.cancelJob(it.id)
                if (res.success) {
                    android.util.Log.d("ForensicLog", "CUSTOMER_CANCEL_SUCCESS | Job: ${it.id}")
                    _job.value = _job.value?.copy(status = "CANCELLED")
                } else {
                    android.util.Log.e("ForensicLog", "CUSTOMER_CANCEL_FAILED | Job: ${it.id} | Error: ${res.message}")
                }
            }
        }
    }
}
