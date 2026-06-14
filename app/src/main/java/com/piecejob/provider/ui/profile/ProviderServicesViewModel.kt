package com.piecejob.provider.ui.profile

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.piecejob.core.data.repository.ProviderRepository
import com.piecejob.core.data.repository.ServiceRepository
import com.piecejob.core.data.remote.ServiceDto
import com.piecejob.core.data.remote.dto.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
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

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    private val _pendingRequirements = MutableStateFlow<Map<String, ServiceRequirementDto>>(emptyMap())
    val pendingRequirements: StateFlow<Map<String, ServiceRequirementDto>> = _pendingRequirements

    private val _providerLevel = MutableStateFlow("STANDARD")
    val providerLevel: StateFlow<String> = _providerLevel

    val canSave: StateFlow<Boolean> = combine(_initialServiceCodes, _tempServiceCodes, _isLoading) { initial, temp, loading ->
        initial != temp && !loading
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    init {
        loadData()
    }

    fun loadData() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                Log.d("ServicesVM", "Loading services data...")
                // 1. Load Profile for Level
                val profileRes = providerRepository.getProviderFullProfile()
                if (profileRes.success && profileRes.data != null) {
                    _providerLevel.value = profileRes.data.verificationLevel
                    Log.d("ServicesVM", "Provider level: ${_providerLevel.value}")
                }

                // 2. Load Active Services
                val res = providerRepository.getMyServices()
                if (res.success && res.data != null) {
                    val combinedCodes = (res.data.approved.map { it.code } + res.data.pending.map { it.code }).toSet()
                    Log.d("ServicesVM", "My services: $combinedCodes")
                    _initialServiceCodes.value = combinedCodes
                    _tempServiceCodes.value = combinedCodes
                    _myServices.value = res.data.approved + res.data.pending
                }

                // 3. Load All Services
                val resAll = serviceRepository.getServices()
                if (resAll.success && resAll.data != null) {
                    Log.d("ServicesVM", "All services loaded: ${resAll.data.services.size}")
                    _allServices.value = resAll.data.services
                }
            } catch (e: Exception) {
                Log.e("ServicesVM", "Error loading data", e)
                _error.value = "Failed to sync services: ${e.message}"
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
            if (current.size < 3) {
                current.add(code)
            }
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
            val codesToSave = _tempServiceCodes.value.toList()
            val res = providerRepository.updateMyServices(codesToSave)
            if (res.success && res.data != null) {
                // 1. Handle dynamic verification requirements
                _pendingRequirements.value = res.data.requirements ?: emptyMap()
                
                // 2. Update local state immediately from response to ensure persistence in UI
                val combinedCodes = (res.data.approved + res.data.pending).toSet()
                _initialServiceCodes.value = combinedCodes
                _tempServiceCodes.value = combinedCodes
                
                // 3. Optional: Trigger a silent background refresh to sync full objects
                launch {
                    val resMy = providerRepository.getMyServices()
                    if (resMy.success && resMy.data != null) {
                        _myServices.value = resMy.data.approved + resMy.data.pending
                    }
                }

                _saveSuccess.value = true
            } else {
                _error.value = res.message ?: "Update failed"
                _saveSuccess.value = false
            }
            _isLoading.value = false
        }
    }

    fun resetSaveState() {
        _saveSuccess.value = null
        _error.value = null
        _pendingRequirements.value = emptyMap()
    }
}
