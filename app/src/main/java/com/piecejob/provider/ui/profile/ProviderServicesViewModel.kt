package com.piecejob.provider.ui.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.piecejob.core.data.repository.ProviderRepository
import com.piecejob.core.data.repository.ServiceRepository
import com.piecejob.core.data.remote.ServiceDto
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ProviderServicesViewModel @Inject constructor(
    private val providerRepository: ProviderRepository,
    private val serviceRepository: ServiceRepository
) : ViewModel() {

    private val _myServices = MutableStateFlow<List<ServiceDto>>(emptyList())
    val myServices: StateFlow<List<ServiceDto>> = _myServices

    private val _allServices = MutableStateFlow<List<ServiceDto>>(emptyList())
    val allServices: StateFlow<List<ServiceDto>> = _allServices

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    init {
        loadData()
    }

    private fun loadData() {
        viewModelScope.launch {
            _isLoading.value = true
            launch {
                val res = providerRepository.getMyServices()
                if (res.success) _myServices.value = res.data ?: emptyList()
            }
            launch {
                val res = serviceRepository.getServices()
                if (res.success) _allServices.value = res.data ?: emptyList()
            }
            _isLoading.value = false
        }
    }

    fun toggleService(code: String) {
        val currentCodes = _myServices.value.map { it.code }.toMutableList()
        if (currentCodes.contains(code)) {
            currentCodes.remove(code)
        } else {
            currentCodes.add(code)
        }

        viewModelScope.launch {
            val res = providerRepository.updateMyServices(currentCodes)
            if (res.success) {
                // Refresh list
                val resMy = providerRepository.getMyServices()
                if (resMy.success) _myServices.value = resMy.data ?: emptyList()
            }
        }
    }
}
