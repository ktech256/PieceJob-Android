package com.piecejob.provider.ui.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.piecejob.core.data.repository.AuthRepository
import com.piecejob.core.data.remote.dto.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ProviderDeviceViewModel @Inject constructor(
    private val repository: AuthRepository
) : ViewModel() {

    private val _devices = MutableStateFlow<List<DeviceDto>>(emptyList())
    val devices: StateFlow<List<DeviceDto>> = _devices

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    init {
        loadDevices()
    }

    fun loadDevices() {
        viewModelScope.launch {
            _isLoading.value = true
            val res = repository.getAuthorizedDevices()
            if (res.success) {
                _devices.value = res.data ?: emptyList()
            }
            _isLoading.value = false
        }
    }

    fun removeDevice(id: String) {
        viewModelScope.launch {
            _isLoading.value = true
            val res = repository.removeDevice(id)
            if (res.success) {
                loadDevices()
            }
            _isLoading.value = false
        }
    }
}
