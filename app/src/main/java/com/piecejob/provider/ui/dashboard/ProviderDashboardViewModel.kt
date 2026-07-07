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
import com.piecejob.core.data.local.SessionManager
import com.google.gson.Gson
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

@HiltViewModel
class ProviderDashboardViewModel @Inject constructor(
    private val repository: ProviderRepository,
    private val userRepository: com.piecejob.core.data.repository.UserRepository,
    private val jobRepository: JobRepository,
    private val dashboardRepository: com.piecejob.core.data.repository.DashboardRepository,
    private val configRepository: com.piecejob.core.data.repository.ConfigRepository,
    private val socketManager: SocketManager,
    private val sessionManager: SessionManager
) : ViewModel() {

    private val _stats = MutableStateFlow<ProviderStatsDto?>(null)
    val stats: StateFlow<ProviderStatsDto?> = _stats

    private val _userProfile = MutableStateFlow<DashboardProfileDto?>(null)
    val userProfile: StateFlow<DashboardProfileDto?> = _userProfile

    val currencySymbol = MutableStateFlow(configRepository.getCurrencySymbol())

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

    private val _recentActivity = MutableStateFlow<List<ActivityDto>>(emptyList())
    val recentActivity: StateFlow<List<ActivityDto>> = _recentActivity

    private val _providerLocation = MutableStateFlow<com.google.android.gms.maps.model.LatLng?>(null)
    val providerLocation: StateFlow<com.google.android.gms.maps.model.LatLng?> = _providerLocation

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    private var locationTrackingJob: kotlinx.coroutines.Job? = null

    private val _navigationEvent = MutableSharedFlow<String>()
    val navigationEvent: SharedFlow<String> = _navigationEvent

    init {
        loadDashboard()
        observeSocket()
        startLocationTracking()
    }

    private fun startLocationTracking() {
        locationTrackingJob?.cancel()
        locationTrackingJob = viewModelScope.launch {
            LocationService.currentLocation.collect { loc ->
                if (loc != null) {
                    _providerLocation.value = com.google.android.gms.maps.model.LatLng(loc.latitude, loc.longitude)
                }
            }
        }
    }

    private fun loadDashboard() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                // Refresh workspace config for currency symbols
                launch {
                    configRepository.refreshWorkspaceConfig()
                    currencySymbol.value = configRepository.getCurrencySymbol()
                }

                val response = dashboardRepository.getProviderDashboard()
                if (response.success && response.data != null) {
                    val data = response.data
                    _userProfile.value = data.profile
                    _stats.value = data.stats
                    _activeJob.value = data.activeJob
                    _recentActivity.value = data.recentActivity
                    _isOnline.value = data.stats.isOnline
                    
                    if (data.activeJob != null) {
                        LocationService.activeJobId = data.activeJob.id
                        socketManager.joinJob(data.activeJob.id)
                    }
                } else {
                    // Fallback to legacy individual calls if unified dashboard fails
                    launch { loadStats() }
                    launch { loadProfile() }
                    launch { loadAvailableJobs() }
                    launch { fetchActiveJob() }
                }
            } catch (e: Exception) {
                Log.e("ProviderDashboard", "Error loading dashboard", e)
                _error.value = "Failed to load dashboard data."
            } finally {
                _isLoading.value = false
            }
        }
    }

    private suspend fun fetchActiveJob() {
        // Find if there's any job currently in progress for this provider
        val response = jobRepository.getActiveJob()
        if (response.success) {
            val active = response.data
            _activeJob.value = active
            if (active != null) {
                LocationService.activeJobId = active.id
                socketManager.joinJob(active.id)
            }
        }
    }

    private fun observeSocket() {
        viewModelScope.launch {
            socketManager.statusEventFlow.collect { event ->
                if (event.status == "COMPLETED" || event.status == "CANCELLED" || event.status == "RATED") {
                    fetchActiveJob()
                    loadDashboard()
                } else {
                    _activeJob.value?.let { currentJob ->
                        if (currentJob.id == event.jobId) {
                            _activeJob.value = currentJob.copy(status = event.status)
                        }
                    }
                }
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
            if (response.success && response.data != null) {
                val u = response.data
                _userProfile.value = DashboardProfileDto(
                    firstName = u.firstName,
                    lastName = u.lastName,
                    email = u.email,
                    photo = u.profilePhoto,
                    addresses = u.addresses,
                    savedLocations = u.savedLocations
                )
                _isShadowBanned.value = u.isShadowBanned ?: false
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

                    // FORENSIC FIX: Ensure FCM Token is valid and synced BEFORE going online
                    try {
                        val token = com.google.firebase.messaging.FirebaseMessaging.getInstance().token.await()
                        if (!token.isNullOrBlank()) {
                            android.util.Log.d("FCM_AUDIT", "Syncing token before going online...")
                            userRepository.updateFcmToken(token)
                        }
                    } catch (e: Exception) {
                        android.util.Log.e("FCM_AUDIT", "Failed to refresh token during online toggle: ${e.message}")
                        // We don't block going online if FCM fails (might have socket), but we log it.
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
                        sessionManager.getUserId()?.let { socketManager.joinUser(it) }
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
                val job = response.data
                _activeJob.value = job
                _availableJobs.value = _availableJobs.value.filter { it.id != jobId }
                
                // Track this job in the location service for live updates
                LocationService.activeJobId = jobId
                socketManager.joinJob(jobId)

                // FORENSIC FIX: Trigger navigation based on negotiation requirement
                if (job.status == "PROVIDER_ACCEPTED" || job.priceStatus == "PENDING") {
                    _navigationEvent.emit("NEGOTIATION:${job.id}:${job.customerId}")
                } else {
                    _navigationEvent.emit("TRACKING:${job.id}")
                }
            }
        }
    }

    fun markArrival(jobId: String) {
        viewModelScope.launch {
            _isLoading.value = true
            val response = jobRepository.markArrival(jobId)
            if (response.success && response.data != null) {
                _activeJob.value = response.data
                _error.value = null
            } else {
                _error.value = response.message ?: response.error?.message ?: "Failed to mark arrival"
            }
            _isLoading.value = false
        }
    }

    fun startJob(jobId: String) {
        android.util.Log.d("ForensicLog", "DASHBOARD_START_JOB_CLICKED | Job: $jobId")
        viewModelScope.launch {
            _isLoading.value = true
            val loc = _providerLocation.value
            val coords = if (loc != null) listOf(loc.longitude, loc.latitude) else null
            
            android.util.Log.d("ForensicLog", "DASHBOARD_START_JOB_SENDING | Coords: $coords")
            val response = jobRepository.startJob(jobId, coords)
            if (response.success && response.data != null) {
                android.util.Log.d("ForensicLog", "DASHBOARD_START_JOB_SUCCESS")
                _activeJob.value = response.data
                _error.value = null
            } else {
                val errorMsg = response.message ?: response.error?.message ?: "Failed to start job"
                android.util.Log.e("ForensicLog", "DASHBOARD_START_JOB_FAILED: $errorMsg")
                _error.value = errorMsg
            }
            _isLoading.value = false
        }
    }

    fun completeJob(jobId: String) {
        viewModelScope.launch {
            _isLoading.value = true
            val response = jobRepository.completeJob(jobId)
            if (response.success && response.data != null) {
                _activeJob.value = response.data
                LocationService.activeJobId = null
                socketManager.leaveJob(jobId)
                _error.value = null
                loadStats()
            } else {
                _error.value = response.message ?: response.error?.message ?: "Failed to complete job"
            }
            _isLoading.value = false
        }
    }
}
