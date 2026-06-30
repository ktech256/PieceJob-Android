package com.piecejob.customer.ui.dashboard

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
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CustomerDashboardViewModel @Inject constructor(
    private val serviceRepository: ServiceRepository,
    private val jobRepository: JobRepository,
    private val dashboardRepository: DashboardRepository,
    private val socketManager: SocketManager,
    private val sessionManager: SessionManager
) : ViewModel() {

    private val _dashboardData = MutableStateFlow<CustomerDashboardDto?>(null)
    val dashboardData: StateFlow<CustomerDashboardDto?> = _dashboardData.asStateFlow()

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

    private val _searchResults = MutableStateFlow<List<Any>>(emptyList())
    val searchResults: StateFlow<List<Any>> = _searchResults.asStateFlow()

    fun onSearch(query: String) {
        if (query.length < 3) {
            _searchResults.value = emptyList()
            return
        }
        
        viewModelScope.launch {
            // Logic to search through services, categories, and saved locations
            val filteredServices = _services.value.filter { 
                it.name.contains(query, ignoreCase = true) || it.description?.contains(query, ignoreCase = true) == true 
            }
            val filteredCategories = _categories.value.filter {
                it.name.contains(query, ignoreCase = true)
            }
            val filteredLocations = _dashboardData.value?.profile?.savedLocations?.filter {
                it.name.contains(query, ignoreCase = true) || it.address.contains(query, ignoreCase = true)
            } ?: emptyList()
            
            val results = mutableListOf<Any>()
            results.addAll(filteredServices)
            results.addAll(filteredCategories)
            results.addAll(filteredLocations)
            
            _searchResults.value = results
        }
    }

    init {
        observeLocationAndLoad()
        setupSocket()
    }

    private fun setupSocket() {
        socketManager.connect("https://piecejob-backend.onrender.com")
        sessionManager.getUserId()?.let { socketManager.joinUser(it) }
        
        viewModelScope.launch {
            socketManager.statusEventFlow.collect { event ->
                loadDashboard()
            }
        }
    }

    private fun observeLocationAndLoad() {
        viewModelScope.launch {
            LocationService.currentLocation.collectLatest { location ->
                if (location != null) {
                    loadDashboard(lat = location.latitude, lng = location.longitude)
                    // Reverse geocoding would happen here, but for now we rely on backend profile-addresses fallback
                } else {
                    loadDashboard()
                }
            }
        }
    }

    fun loadDashboard(lat: Double? = null, lng: Double? = null) {
        viewModelScope.launch {
            _isLoading.value = true
            val response = dashboardRepository.getCustomerDashboard(lat, lng)
            if (response.success && response.data != null) {
                _dashboardData.value = response.data
                _activeJob.value = response.data.activeJob
                
                // Set current address based on priority (Issue 3)
                // 1. If GPS lat/lng provided, maybe reverse geocode? 
                // For now use first address from profile or "Current Location"
                val profile = response.data.profile
                currentAddress.value = profile.addresses?.find { it.isDefault }?.address 
                    ?: profile.addresses?.firstOrNull()?.address
                    ?: profile.savedLocations?.firstOrNull()?.address
                    ?: "Current Location"

                loadServices(lat = lat, lng = lng)
            }
            _isLoading.value = false
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
            // Parallel loading
            launch {
                val catRes = serviceRepository.getCategories()
                if (catRes.success) _categories.value = catRes.data ?: emptyList()
            }

            val response = serviceRepository.getServices(gender, lat, lng)
            if (response.success && response.data != null) {
                _services.value = response.data.services
                _groupedServices.value = response.data.grouped
                _error.value = null
            }
        }
    }
}
