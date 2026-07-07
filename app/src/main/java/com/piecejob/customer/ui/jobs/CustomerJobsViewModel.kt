package com.piecejob.customer.ui.jobs

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.piecejob.core.data.repository.JobRepository
import com.piecejob.core.data.remote.dto.JobDto
import com.piecejob.core.socket.SocketManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CustomerJobsViewModel @Inject constructor(
    private val jobRepository: JobRepository,
    private val socketManager: SocketManager
) : ViewModel() {

    private val _activeJobs = MutableStateFlow<List<JobDto>>(emptyList())
    val activeJobs: StateFlow<List<JobDto>> = _activeJobs

    private val _completedJobs = MutableStateFlow<List<JobDto>>(emptyList())
    val completedJobs: StateFlow<List<JobDto>> = _completedJobs

    private val _cancelledJobs = MutableStateFlow<List<JobDto>>(emptyList())
    val cancelledJobs: StateFlow<List<JobDto>> = _cancelledJobs

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _navigationEvent = MutableSharedFlow<Pair<String, String>>()
    val navigationEvent: SharedFlow<Pair<String, String>> = _navigationEvent

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
    }

    fun loadJobs() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                // 1. Active Jobs
                val activeRes = jobRepository.getProviderJobs("ACTIVE") // getMyJobs works for both
                if (activeRes.success) {
                    _activeJobs.value = activeRes.data ?: emptyList()
                }

                // 2. Completed
                val completedRes = jobRepository.getProviderJobs("COMPLETED")
                if (completedRes.success) {
                    _completedJobs.value = completedRes.data ?: emptyList()
                }

                // 3. Cancelled
                val cancelledRes = jobRepository.getProviderJobs("CANCELLED")
                if (cancelledRes.success) {
                    _cancelledJobs.value = cancelledRes.data ?: emptyList()
                }
            } catch (e: Exception) {
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun openJob(job: JobDto) {
        viewModelScope.launch {
            val route = when {
                job.status == "PROVIDER_ACCEPTED" || job.priceNegotiationRequired == true || job.photoSharingRequired == true || job.priceStatus == "PENDING" ->
                    com.piecejob.core.ui.navigation.Screen.Negotiation.passArgs(job.id, job.providerId ?: "")
                job.status == "COMPLETED" -> "rating/${job.id}"
                else -> "customer_tracking/${job.id}"
            }
            _navigationEvent.emit(job.id to route)
        }
    }
}
