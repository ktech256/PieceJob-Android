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

    private val _initialServiceCodes = MutableStateFlow<Set<String>>(emptySet())

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _saveSuccess = MutableStateFlow<Boolean?>(null)
    val saveSuccess: StateFlow<Boolean?> = _saveSuccess

    private val _pendingRequirements = MutableStateFlow<Map<String, ServiceRequirementDto>>(emptyMap())
    val pendingRequirements: StateFlow<Map<String, ServiceRequirementDto>> = _pendingRequirements

    private val _providerLevel = MutableStateFlow("STANDARD")
    val providerLevel: StateFlow<String> = _providerLevel

    init {
        loadData()
    }

    fun loadData() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                // 1. Load Profile for Level
                val profileRes = providerRepository.getProviderFullProfile()
                if (profileRes.success && profileRes.data != null) {
                    _providerLevel.value = profileRes.data.verificationLevel
                }

                // 2. Load Active Services
                val res = providerRepository.getMyServices()
                if (res.success && res.data != null) {
                    _myServices.value = res.data.approved + res.data.pending
                    _initialServiceCodes.value = _myServices.value.map { it.code }.toSet()
                    _tempServiceCodes.value = _initialServiceCodes.value
                }

                // 3. Load All Services
                val resAll = serviceRepository.getServices()
                if (resAll.success) {
                    _allServices.value = resAll.data ?: emptyList()
                }
            } catch (e: Exception) {
                // Handle error
            } finally {
                _isLoading.value = false
            }
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
        return _initialServiceCodes.value != _tempServiceCodes.value
    }

    fun discardChanges() {
        _tempServiceCodes.value = _initialServiceCodes.value
    }

    fun saveChanges() {
        viewModelScope.launch {
            _isLoading.value = true
            val res = providerRepository.updateMyServices(_tempServiceCodes.value.toList())
            if (res.success && res.data != null) {
                _pendingRequirements.value = res.data.requirements ?: emptyMap()
                
                // Refresh local data
                val resMy = providerRepository.getMyServices()
                if (resMy.success && resMy.data != null) {
                    _myServices.value = resMy.data.approved + resMy.data.pending
                    _initialServiceCodes.value = _myServices.value.map { it.code }.toSet()
                    _tempServiceCodes.value = _initialServiceCodes.value
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
        _pendingRequirements.value = emptyMap()
    }
}
