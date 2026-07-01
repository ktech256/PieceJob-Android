package com.piecejob.customer.ui.dashboard

import android.content.Context
import android.location.Geocoder
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.piecejob.core.data.remote.ServiceDto
import com.piecejob.core.data.remote.GroupedServicesDto
import com.piecejob.core.data.remote.dto.*
import com.piecejob.core.data.repository.ServiceRepository
import com.piecejob.core.data.repository.JobRepository
import com.piecejob.core.data.repository.DashboardRepository
import com.piecejob.core.location.LocationService
import com.piecejob.core.socket.SocketManager
import com.piecejob.core.data.local.SessionManager
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@HiltViewModel
class CustomerDashboardViewModel @Inject constructor(
    private val serviceRepository: ServiceRepository,
    private val jobRepository: JobRepository,
    private val dashboardRepository: DashboardRepository,
    private val socketManager: SocketManager,
    private val sessionManager: SessionManager,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _dashboardData = MutableStateFlow<CustomerDashboardDto?>(null)
    val dashboardData: StateFlow<CustomerDashboardDto?> = _dashboardData.asStateFlow()

    private val _realtimePromotions = MutableStateFlow<List<PromotionDto>>(emptyList())
    val realtimePromotions: StateFlow<List<PromotionDto>> = _realtimePromotions.asStateFlow()

    private val _services = MutableStateFlow<List<ServiceDto>>(emptyList())
    val services: StateFlow<List<ServiceDto>> = _services

    private val _groupedServices = MutableStateFlow<List<GroupedServicesDto>>(emptyList())
    val groupedServices: StateFlow<List<GroupedServicesDto>> = _groupedServices

    private val _categories = MutableStateFlow<List<com.piecejob.core.data.remote.ServiceCategoryDto>>(emptyList())
    val categories: StateFlow<List<com.piecejob.core.data.remote.ServiceCategoryDto>> = _categories

    private val _activeJob = MutableStateFlow<JobDto?>(null)
    val activeJob: StateFlow<JobDto?> = _activeJob

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    val currentAddress = MutableStateFlow("Determining location...")

    private val _bookAgainServices = MutableStateFlow<List<ServiceDto>>(emptyList())
    val bookAgainServices: StateFlow<List<ServiceDto>> = _bookAgainServices.asStateFlow()

    private val _searchResults = MutableStateFlow<List<Any>>(emptyList())
    val searchResults: StateFlow<List<Any>> = _searchResults.asStateFlow()

    private var lastLoadedLocation: android.location.Location? = null

    init {
        setupSocket()
        observeLocationAndLoad()
    }

    private fun setupSocket() {
        socketManager.connect("https://piecejob-backend.onrender.com")
        sessionManager.getUserId()?.let { socketManager.joinUser(it) }
        
        viewModelScope.launch {
            socketManager.statusEventFlow.collect { event ->
                Log.d("FORENSIC", "VM | Socket Status Update: ${event.status}. Reloading.")
                if (event.status == "PROMOTIONS_REFRESH") {
                    refreshPromotionsOnly()
                } else {
                    loadDashboard(lastLoadedLocation?.latitude, lastLoadedLocation?.longitude)
                }
            }
        }
    }

    private fun refreshPromotionsOnly() {
        viewModelScope.launch {
            val response = dashboardRepository.getCustomerPromotions()
            if (response.success && response.data != null) {
                Log.d("FORENSIC", "VM | Promotions Refreshed. Count: ${response.data.size}")
                _realtimePromotions.value = response.data
                // Also update aggregated data to keep consistency if needed, but UI will prefer realtimePromotions
                _dashboardData.value = _dashboardData.value?.copy(promotions = response.data)
            }
        }
    }

    private fun observeLocationAndLoad() {
        viewModelScope.launch {
            // Priority 1: Instant load to get user profile and existing address
            loadDashboard()

            LocationService.currentLocation.collectLatest { location ->
                if (location != null) {
                    val distance = lastLoadedLocation?.distanceTo(location) ?: Float.MAX_VALUE
                    if (distance > 100) { // 100m threshold
                        Log.d("FORENSIC", "VM | Significant location change detected ($distance m). Reloading dashboard.")
                        lastLoadedLocation = location
                        loadDashboard(lat = location.latitude, lng = location.longitude)
                    }
                }
            }
        }
    }

    fun onSearch(query: String) {
        if (query.length < 2) {
            _searchResults.value = emptyList()
            return
        }
        
        viewModelScope.launch {
            val response = dashboardRepository.globalSearch(query)
            if (response.success && response.data != null) {
                val results = mutableListOf<Any>()
                results.addAll(response.data.services)
                results.addAll(response.data.categories)
                results.addAll(response.data.providers)
                
                _dashboardData.value?.profile?.savedLocations?.filter {
                    it.name.contains(query, ignoreCase = true) || it.address.contains(query, ignoreCase = true)
                }?.let { results.addAll(it) }

                _searchResults.value = results
            }
        }
    }

    fun loadDashboard(lat: Double? = null, lng: Double? = null) {
        viewModelScope.launch {
            _isLoading.value = true
            Log.d("FORENSIC", "VM | loadDashboard(lat=$lat, lng=$lng) started")
            try {
                val response = dashboardRepository.getCustomerDashboard(lat, lng)
                if (response.success && response.data != null) {
                    val data = response.data
                    Log.d("FORENSIC", "VM | Dashboard API Success. User: ${data.profile.firstName}")
                    
                    _dashboardData.value = data
                    _activeJob.value = data.activeJob
                    _realtimePromotions.value = data.promotions
                    
                    // ISSUE 2: Sync Location Service for Provider or stop if null for customer
                    if (sessionManager.getRole() == "provider" && data.activeJob != null && isTrackingRequired(data.activeJob.status)) {
                        Log.d("LOCATION_AUDIT", "Provider active job found. Starting service.")
                        LocationService.activeJobId = data.activeJob.id
                        LocationService.startService(context)
                    } else if (data.activeJob == null || !isTrackingRequired(data.activeJob.status)) {
                        // For customers, the TrackingScreen handles starting. We only ensure it's stopped here if no job.
                        if (sessionManager.getRole() != "provider") {
                            Log.d("LOCATION_AUDIT", "No tracking required for customer. Ensuring service stopped.")
                            LocationService.activeJobId = null
                            LocationService.stopService(context)
                        }
                    }

                    // Priority Location Resolution (Issue 1)
                    resolveDisplayAddress(lat, lng, data.profile)

                    // Process Book Again
                    processBookAgain(data)

                    // Ensure services are loaded
                    performServiceLoad(lat = lat, lng = lng)
                } else {
                    Log.e("FORENSIC", "VM | Dashboard API Failure: ${response.message}")
                    _error.value = response.message
                    
                    // Even if aggregated dashboard fails, attempt to load services
                    performServiceLoad(lat = lat, lng = lng)
                    
                    if (currentAddress.value == "Determining location...") {
                        currentAddress.value = "Location Offline"
                    }
                }
            } catch (e: Exception) {
                Log.e("FORENSIC", "VM | loadDashboard CRITICAL ERROR", e)
                _error.value = e.message
            } finally {
                _isLoading.value = false
            }
        }
    }

    private suspend fun performServiceLoad(gender: String? = null, lat: Double? = null, lng: Double? = null) {
        withContext(Dispatchers.IO) {
            try {
                launch {
                    val catRes = serviceRepository.getCategories()
                    if (catRes.success) _categories.value = catRes.data ?: emptyList()
                }

                val response = serviceRepository.getServices(gender, lat, lng)
                if (response.success && response.data != null) {
                    _services.value = response.data.services
                    _groupedServices.value = response.data.grouped
                }
            } catch (e: Exception) {
                Log.e("FORENSIC", "VM | performServiceLoad Error", e)
            }
        }
    }

    private fun resolveDisplayAddress(lat: Double?, lng: Double?, profile: DashboardProfileDto) {
        if (lat != null && lng != null) {
            viewModelScope.launch(Dispatchers.IO) {
                try {
                    val geocoder = Geocoder(context)
                    @Suppress("DEPRECATION")
                    val addresses = geocoder.getFromLocation(lat, lng, 1)
                    if (!addresses.isNullOrEmpty()) {
                        val addr = addresses[0]
                        val city = addr.locality ?: addr.subAdminArea ?: ""
                        val country = addr.countryCode ?: ""
                        val resolved = "$city, $country".trim().removePrefix(",").removeSuffix(",")
                        currentAddress.value = if (resolved.isNotBlank()) resolved else "Current Location"
                    } else {
                        Log.w("FORENSIC", "VM | Geocoder returned empty for $lat, $lng. Falling back.")
                        fallbackAddress(profile)
                    }
                } catch (e: Exception) {
                    Log.e("FORENSIC", "VM | Geocoding Exception", e)
                    fallbackAddress(profile)
                }
            }
        } else {
            fallbackAddress(profile)
        }
    }

    private fun fallbackAddress(profile: DashboardProfileDto) {
        val addr = profile.addresses?.find { it.isDefault }?.address 
            ?: profile.addresses?.firstOrNull()?.address
            ?: profile.savedLocations?.firstOrNull()?.address
            ?: "Current Location"
        currentAddress.value = addr
    }

    private fun processBookAgain(data: CustomerDashboardDto) {
        val recentCodes = data.latestActivity.filter { it.type == "JOB" }.map { it.serviceCode }.distinct()
        // Try to find full service details from the registry or recommendations
        val pool = (_services.value + data.recommendations).distinctBy { it.code }
        val fromPool = pool.filter { recentCodes.contains(it.code) }
        _bookAgainServices.value = fromPool
        Log.d("FORENSIC", "VM | Book Again processed: ${fromPool.size} items")
    }

    private fun isTrackingRequired(status: String): Boolean {
        return when (status) {
            "ACCEPTED", "EN_ROUTE", "ARRIVED", "STARTED", "IN_PROGRESS" -> true
            else -> false
        }
    }

    fun loadActiveJob() {
        viewModelScope.launch {
            val response = jobRepository.getActiveJob()
            if (response.success) {
                _activeJob.value = response.data
            }
        }
    }

    fun loadServices(gender: String? = null, lat: Double? = null, lng: Double? = null) {
        viewModelScope.launch {
            performServiceLoad(gender, lat, lng)
        }
    }
}
