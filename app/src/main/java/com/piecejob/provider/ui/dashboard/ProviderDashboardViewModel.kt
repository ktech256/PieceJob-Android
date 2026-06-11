package com.piecejob.provider.ui.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.piecejob.core.data.repository.ProviderRepository
import com.piecejob.core.data.remote.dto.ProviderStatsDto
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ProviderDashboardViewModel @Inject constructor(
    private val repository: ProviderRepository
) : ViewModel() {

    private val _stats = MutableStateFlow<ProviderStatsDto?>(null)
    val stats: StateFlow<ProviderStatsDto?> = _stats

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _isOnline = MutableStateFlow(false)
    val isOnline: StateFlow<Boolean> = _isOnline

    private val _isShadowBanned = MutableStateFlow(false)
    val isShadowBanned: StateFlow<Boolean> = _isShadowBanned

    init {
        loadStats()
        loadProfile()
    }

    private fun loadProfile() {
        viewModelScope.launch {
            try {
                val response = repository.getProfile()
                if (response.success) {
                    _isShadowBanned.value = response.data?.isShadowBanned ?: false
                }
            } catch (e: Exception) {}
        }
    }

    fun toggleOnlineStatus() {
        viewModelScope.launch {
            val newStatus = !_isOnline.value
            val response = repository.updateStatus(newStatus)
            if (response.success) {
                _isOnline.value = newStatus
            }
        }
    }

    fun sendHeartbeat(lat: Double, lng: Double) {
        viewModelScope.launch {
            // In a real app, retrieve android ID as hardwareId
            repository.sendHeartbeat(lat, lng, hardwareId = "ANDROID_ID_SIM")
        }
    }

    fun loadStats() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val response = repository.getDashboardStats()
                if (response.success) {
                    _stats.value = response.data
                }
            } catch (e: Exception) {
                // Handle error
            } finally {
                _isLoading.value = false
            }
        }
    }
}
