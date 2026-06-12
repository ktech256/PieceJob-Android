package com.piecejob.provider.ui.dashboard

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.piecejob.core.data.repository.ProviderRepository
import com.piecejob.core.data.repository.JobRepository
import com.piecejob.core.data.remote.dto.*
import com.piecejob.core.location.LocationService
import com.piecejob.core.socket.SocketManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import javax.inject.Inject

@HiltViewModel
class ProviderDashboardViewModel @Inject constructor(
    private val repository: ProviderRepository,
    private val jobRepository: JobRepository,
    private val socketManager: SocketManager
) : ViewModel() {

    private val _stats = MutableStateFlow<ProviderStatsDto?>(null)
    val stats: StateFlow<ProviderStatsDto?> = _stats

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _isOnline = MutableStateFlow(false)
    val isOnline: StateFlow<Boolean> = _isOnline

    private val _isShadowBanned = MutableStateFlow(false)
    val isShadowBanned: StateFlow<Boolean> = _isShadowBanned

    private val _availableJobs = MutableStateFlow<List<JobDto>>(emptyList())
    val availableJobs: StateFlow<List<JobDto>> = _availableJobs

    private val _activeJob = MutableStateFlow<JobDto?>(null)
    val activeJob: StateFlow<JobDto?> = _activeJob

    init {
        loadStats()
        loadProfile()
        observeSocket()
    }

    private fun observeSocket() {
        socketManager.onStatusUpdated { status ->
            _activeJob.value?.let { currentJob ->
                _activeJob.value = currentJob.copy(status = status)
            }
        }
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

    fun toggleOnlineStatus(context: Context) {
        viewModelScope.launch {
            val newStatus = !_isOnline.value
            val response = repository.updateStatus(newStatus)
            if (response.success) {
                _isOnline.value = newStatus
                if (newStatus) {
                    socketManager.connect("https://piecejob-backend.onrender.com")
                    LocationService.startService(context)
                    loadAvailableJobs()
                } else {
                    LocationService.stopService(context)
                    socketManager.disconnect()
                    _availableJobs.value = emptyList()
                }
            }
        }
    }

    fun loadAvailableJobs() {
        viewModelScope.launch {
            val response = jobRepository.getAvailableJobs()
            if (response.success) {
                _availableJobs.value = response.data ?: emptyList()
            }
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
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun acceptJob(jobId: String) {
        viewModelScope.launch {
            val response = jobRepository.acceptJob(jobId)
            if (response.success && response.data != null) {
                _activeJob.value = response.data
                _availableJobs.value = _availableJobs.value.filter { it.id != jobId }
                
                // Track this job in the location service for live updates
                LocationService.activeJobId = jobId
                socketManager.joinJob(jobId)
            }
        }
    }

    fun markArrival(jobId: String) {
        viewModelScope.launch {
            val response = jobRepository.markArrival(jobId)
            if (response.success) {
                _activeJob.value = _activeJob.value?.copy(status = "ARRIVED")
            }
        }
    }

    fun startJob(jobId: String) {
        viewModelScope.launch {
            val response = jobRepository.startJob(jobId)
            if (response.success) {
                _activeJob.value = _activeJob.value?.copy(status = "STARTED")
            }
        }
    }

    fun completeJob(jobId: String) {
        viewModelScope.launch {
            val response = jobRepository.completeJob(jobId)
            if (response.success) {
                _activeJob.value = null
                LocationService.activeJobId = null
                loadStats()
            }
        }
    }
}
