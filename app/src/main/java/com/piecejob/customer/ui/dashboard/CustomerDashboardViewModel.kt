package com.piecejob.customer.ui.dashboard

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.piecejob.core.data.remote.ServiceDto
import com.piecejob.core.data.remote.GroupedServicesDto
import com.piecejob.core.data.remote.dto.JobDto
import com.piecejob.core.data.repository.ServiceRepository
import com.piecejob.core.data.repository.JobRepository
import com.piecejob.core.location.LocationService
import com.piecejob.core.socket.SocketManager
import com.piecejob.core.data.local.SessionManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CustomerDashboardViewModel @Inject constructor(
    private val serviceRepository: ServiceRepository,
    private val jobRepository: JobRepository,
    private val socketManager: SocketManager,
    private val sessionManager: SessionManager
) : ViewModel() {

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

    init {
        // Only auto-load if we're likely already logged in (Customer App standard flow)
        if (serviceRepository.hasStoredGender()) {
            observeLocationAndLoad()
            loadActiveJob()
            setupSocket()
        }
    }

    private fun setupSocket() {
        socketManager.connect("https://piecejob-backend.onrender.com")
        sessionManager.getUserId()?.let { socketManager.joinUser(it) }
        
        viewModelScope.launch {
            socketManager.statusEventFlow.collect { event ->
                Log.d("FORENSIC", "DASHBOARD_SOCKET_RECEIVED | Status: ${event.status} | Job: ${event.jobId}")
                loadActiveJob()
            }
        }
    }

    private fun observeLocationAndLoad() {
        viewModelScope.launch {
            LocationService.currentLocation.collectLatest { location ->
                loadServices(lat = location?.latitude, lng = location?.longitude)
            }
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
            _isLoading.value = true
            
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
            } else {
                _error.value = response.error?.message ?: "Failed to load services"
            }
            _isLoading.value = false
        }
    }
}
