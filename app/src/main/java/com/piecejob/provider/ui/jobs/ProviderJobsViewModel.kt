package com.piecejob.provider.ui.jobs

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.piecejob.core.data.repository.JobRepository
import com.piecejob.core.data.remote.dto.JobDto
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ProviderJobsViewModel @Inject constructor(
    private val jobRepository: JobRepository,
    private val socketManager: com.piecejob.core.socket.SocketManager
) : ViewModel() {

    private val _availableJobs = MutableStateFlow<List<JobDto>>(emptyList())
    val availableJobs: StateFlow<List<JobDto>> = _availableJobs

    private val _activeJobs = MutableStateFlow<List<JobDto>>(emptyList())
    val activeJobs: StateFlow<List<JobDto>> = _activeJobs

    private val _scheduledJobs = MutableStateFlow<List<JobDto>>(emptyList())
    val scheduledJobs: StateFlow<List<JobDto>> = _scheduledJobs

    private val _completedJobs = MutableStateFlow<List<JobDto>>(emptyList())
    val completedJobs: StateFlow<List<JobDto>> = _completedJobs

    private val _cancelledJobs = MutableStateFlow<List<JobDto>>(emptyList())
    val cancelledJobs: StateFlow<List<JobDto>> = _cancelledJobs

    private val _disputedJobs = MutableStateFlow<List<JobDto>>(emptyList())
    val disputedJobs: StateFlow<List<JobDto>> = _disputedJobs

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _navigationEvent = MutableSharedFlow<String>()
    val navigationEvent: SharedFlow<String> = _navigationEvent

    init {
        loadJobs()
        observeSocket()
    }

    private fun observeSocket() {
        viewModelScope.launch {
            socketManager.statusEventFlow.collect { event ->
                loadJobs()
            }
        }
        viewModelScope.launch {
            socketManager.broadcastEventFlow.collect { event ->
                loadJobs()
            }
        }
    }

    fun loadJobs() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                // 1. Available (Broadcasted)
                val availableRes = jobRepository.getAvailableJobs()
                if (availableRes.success) {
                    _availableJobs.value = availableRes.data ?: emptyList()
                }

                // 2. Active
                val activeRes = jobRepository.getProviderJobs("ACTIVE")
                if (activeRes.success) {
                    _activeJobs.value = activeRes.data ?: emptyList()
                }

                // 3. Scheduled
                val scheduledRes = jobRepository.getProviderJobs("SCHEDULED")
                if (scheduledRes.success) {
                    _scheduledJobs.value = scheduledRes.data ?: emptyList()
                }

                // 4. Completed
                val completedRes = jobRepository.getProviderJobs("COMPLETED")
                if (completedRes.success) {
                    _completedJobs.value = completedRes.data ?: emptyList()
                }

                // 5. Cancelled
                val cancelledRes = jobRepository.getProviderJobs("CANCELLED")
                if (cancelledRes.success) {
                    _cancelledJobs.value = cancelledRes.data ?: emptyList()
                }

                // 6. Disputed
                val disputedRes = jobRepository.getProviderJobs("DISPUTED")
                if (disputedRes.success) {
                    _disputedJobs.value = disputedRes.data ?: emptyList()
                }
            } catch (e: Exception) {
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun acceptJob(jobId: String) {
        viewModelScope.launch {
            _isLoading.value = true
            val response = jobRepository.acceptJob(jobId)
            if (response.success && response.data != null) {
                loadJobs()
                
                val job = response.data
                if (job.status == "PROVIDER_ACCEPTED" || job.priceNegotiationRequired == true || job.photoSharingRequired == true) {
                    _navigationEvent.emit("NEGOTIATION:${jobId}:${job.customerId}")
                } else {
                    _navigationEvent.emit("TRACKING:${jobId}")
                }
            } else {
                _isLoading.value = false
            }
        }
    }
}
