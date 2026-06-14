package com.piecejob.customer.ui.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.piecejob.core.data.remote.ServiceDto
import com.piecejob.core.data.remote.GroupedServicesDto
import com.piecejob.core.data.repository.ServiceRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CustomerDashboardViewModel @Inject constructor(
    private val serviceRepository: ServiceRepository
) : ViewModel() {

    private val _services = MutableStateFlow<List<ServiceDto>>(emptyList())
    val services: StateFlow<List<ServiceDto>> = _services

    private val _groupedServices = MutableStateFlow<List<GroupedServicesDto>>(emptyList())
    val groupedServices: StateFlow<List<GroupedServicesDto>> = _groupedServices

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    init {
        // Only auto-load if we're likely already logged in (Customer App standard flow)
        // For Provider onboarding, we will call loadServices(gender) explicitly.
        if (serviceRepository.hasStoredGender()) {
            loadServices()
        }
    }

    fun loadServices(gender: String? = null) {
        viewModelScope.launch {
            _isLoading.value = true
            val response = serviceRepository.getServices(gender)
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
