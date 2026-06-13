package com.piecejob.provider.ui.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.piecejob.core.data.repository.ProviderRepository
import com.piecejob.core.data.repository.ServiceRepository
import com.piecejob.core.data.remote.ServiceDto
import com.piecejob.core.data.remote.dto.*
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

    private val _tempServiceCodes = MutableStateFlow<Set<String>>(emptySet())
    val tempServiceCodes: StateFlow<Set<String>> = _tempServiceCodes

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _saveSuccess = MutableStateFlow<Boolean?>(null)
    val saveSuccess: StateFlow<Boolean?> = _saveSuccess

    private val _pendingRequirements = MutableStateFlow<Map<String, ServiceRequirementDto>>(emptyMap())
    val pendingRequirements: StateFlow<Map<String, ServiceRequirementDto>> = _pendingRequirements

    init {
        loadData()
    }

    private fun loadData() {
        viewModelScope.launch {
            _isLoading.value = true
            launch {
                val res = providerRepository.getMyServices()
                if (res.success) {
                    _myServices.value = res.data ?: emptyList()
                    _tempServiceCodes.value = _myServices.value.map { it.code }.toSet()
                }
            }
            launch {
                val res = serviceRepository.getServices()
                if (res.success) _allServices.value = res.data ?: emptyList()
            }
            _isLoading.value = false
        }
    }

    fun toggleService(code: String) {
        val current = _tempServiceCodes.value.toMutableSet()
        if (current.contains(code)) {
            current.remove(code)
        } else {
            current.add(code)
        }
        _tempServiceCodes.value = current
    }

    fun hasUnsavedChanges(): Boolean {
        val original = _myServices.value.map { it.code }.toSet()
        return original != _tempServiceCodes.value
    }

    fun discardChanges() {
        _tempServiceCodes.value = _myServices.value.map { it.code }.toSet()
    }

    fun saveChanges() {
        viewModelScope.launch {
            _isLoading.value = true
            val res = providerRepository.updateMyServices(_tempServiceCodes.value.toList())
            if (res.success && res.data != null) {
                _pendingRequirements.value = res.data.requirements ?: emptyMap()
                val resMy = providerRepository.getMyServices()
                if (resMy.success) {
                    _myServices.value = resMy.data ?: emptyList()
                    _tempServiceCodes.value = _myServices.value.map { it.code }.toSet()
                }
                _saveSuccess.value = true
            } else {
                _saveSuccess.value = false
            }
            _isLoading.value = false
        }
    }

    fun resetSaveState() {
        _saveSuccess.value = null
    }
}
