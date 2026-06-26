package com.piecejob.provider.ui.dashboard

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.piecejob.core.data.repository.ProviderRepository
import com.piecejob.core.data.repository.JobRepository
import com.piecejob.core.data.remote.dto.*
import com.piecejob.core.location.LocationService
import com.piecejob.core.socket.SocketManager
import com.google.gson.Gson
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import javax.inject.Inject

@HiltViewModel
class ProviderDashboardViewModel @Inject constructor(
    private val repository: ProviderRepository,
    private val jobRepository: JobRepository,
    private val socketManager: SocketManager
) : ViewModel() {

    private val _stats = MutableStateFlow<ProviderStatsDto?>(null)
    val stats: StateFlow<ProviderStatsDto?> = _stats

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _isOnline = MutableStateFlow(false)
    val isOnline: StateFlow<Boolean> = _isOnline

    private val _isShadowBanned = MutableStateFlow(false)
    val isShadowBanned: StateFlow<Boolean> = _isShadowBanned

    private val _availableJobs = MutableStateFlow<List<JobDto>>(emptyList())
    val availableJobs: StateFlow<List<JobDto>> = _availableJobs

    private val _activeJob = MutableStateFlow<JobDto?>(null)
    val activeJob: StateFlow<JobDto?> = _activeJob

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    init {
        loadDashboard()
        observeSocket()
    }

    private fun loadDashboard() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                // Parallel fetching
                launch { loadStats() }
                launch { loadProfile() }
                launch { loadAvailableJobs() }
                launch { fetchActiveJob() }
            } catch (e: Exception) {
                Log.e("ProviderDashboard", "Error loading dashboard", e)
            } finally {
                _isLoading.value = false
            }
        }
    }

    private suspend fun fetchActiveJob() {
        // Find if there's any job currently in progress for this provider
        val response = jobRepository.getAvailableJobs() // We reuse this or have a specific 'active' endpoint
        if (response.success) {
            val active = response.data?.find { it.status in listOf("ACCEPTED", "ARRIVED", "STARTED") }
            _activeJob.value = active
            if (active != null) {
                LocationService.activeJobId = active.id
                socketManager.joinJob(active.id)
            }
        }
    }

    private fun observeSocket() {
        socketManager.onStatusUpdated { status ->
            _activeJob.value?.let { currentJob ->
                _activeJob.value = currentJob.copy(status = status)
            }
        }
        
        socketManager.onNewBroadcast { data ->
            try {
                val job = Gson().fromJson(data.toString(), JobDto::class.java)
                val current = _availableJobs.value.toMutableList()
                if (current.none { it.id == job.id }) {
                    current.add(0, job)
                    _availableJobs.value = current
                }
            } catch (e: Exception) {
                Log.e("ProviderDashboard", "Error parsing broadcast job", e)
            }
        }
    }

    fun refresh() {
        loadDashboard()
    }

    private suspend fun loadProfile() {
        try {
            val response = repository.getProfile()
            if (response.success) {
                _isShadowBanned.value = response.data?.isShadowBanned ?: false
            }
            
            // Also fetch provider-specific data like isOnline
            val providerResponse = repository.getProviderFullProfile()
            if (providerResponse.success) {
                _isOnline.value = providerResponse.data?.isOnline ?: false
            }
        } catch (e: Exception) {
            Log.e("ProviderDashboard", "Error loading profile info", e)
        }
    }

    private suspend fun loadStats() {
        try {
            val response = repository.getDashboardStats()
            if (response.success) {
                _stats.value = response.data
            }
        } catch (e: Exception) {}
    }

    fun toggleOnlineStatus(context: Context) {
        viewModelScope.launch {
            val newStatus = !_isOnline.value
            _error.value = null

            try {
                var lat: Double? = null
                var lng: Double? = null

                if (newStatus) {
                    // Check permissions
                    if (androidx.core.content.ContextCompat.checkSelfPermission(context, android.Manifest.permission.ACCESS_FINE_LOCATION) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                        _error.value = "Location permission is required to go online."
                        return@launch
                    }

                    val fusedLocationClient = com.google.android.gms.location.LocationServices.getFusedLocationProviderClient(context)
                    // Try to get last location first, then current location if needed
                    val location = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                        try {
                            val lastTask = fusedLocationClient.lastLocation
                            var loc = com.google.android.gms.tasks.Tasks.await(lastTask)
                            if (loc == null) {
                                val currentTask = fusedLocationClient.getCurrentLocation(
                                    com.google.android.gms.location.Priority.PRIORITY_HIGH_ACCURACY,
                                    null
                                )
                                loc = com.google.android.gms.tasks.Tasks.await(currentTask)
                            }
                            loc
                        } catch (e: Exception) {
                            null
                        }
                    }
                    lat = location?.latitude
                    lng = location?.longitude

                    if (lat == null || lng == null) {
                        _error.value = "Unable to retrieve GPS location. Please ensure GPS is enabled."
                        return@launch
                    }
                }

                val response = repository.updateStatus(newStatus, lat, lng)
                if (response.success) {
                    _isOnline.value = newStatus
                    if (newStatus) {
                        socketManager.connect("https://piecejob-backend.onrender.com")
                        LocationService.startService(context)
                        loadAvailableJobs()
                    } else {
                        LocationService.stopService(context)
                        socketManager.disconnect()
                        _availableJobs.value = emptyList()
                    }
                } else {
                    _error.value = response.message ?: "Failed to update status"
                }
            } catch (e: Exception) {
                _error.value = "An unexpected error occurred"
                Log.e("ProviderDashboard", "Toggle Online Error", e)
            }
        }
    }

    fun loadAvailableJobs() {
        viewModelScope.launch {
            val response = jobRepository.getAvailableJobs()
            if (response.success) {
                _availableJobs.value = response.data ?: emptyList()
            }
        }
    }

    fun acceptJob(jobId: String) {
        viewModelScope.launch {
            val response = jobRepository.acceptJob(jobId)
            if (response.success && response.data != null) {
                _activeJob.value = response.data
                _availableJobs.value = _availableJobs.value.filter { it.id != jobId }
                
                // Track this job in the location service for live updates
                LocationService.activeJobId = jobId
                socketManager.joinJob(jobId)
            }
        }
    }

    fun markArrival(jobId: String) {
        viewModelScope.launch {
            val response = jobRepository.markArrival(jobId)
            if (response.success) {
                _activeJob.value = _activeJob.value?.copy(status = "ARRIVED")
            }
        }
    }

    fun startJob(jobId: String) {
        viewModelScope.launch {
            val response = jobRepository.startJob(jobId)
            if (response.success) {
                _activeJob.value = _activeJob.value?.copy(status = "STARTED")
            }
        }
    }

    fun completeJob(jobId: String) {
        viewModelScope.launch {
            val response = jobRepository.completeJob(jobId)
            if (response.success) {
                _activeJob.value = null
                LocationService.activeJobId = null
                loadStats()
            }
        }
    }
}
