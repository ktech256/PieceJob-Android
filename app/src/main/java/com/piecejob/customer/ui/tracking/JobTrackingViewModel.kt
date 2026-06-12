package com.piecejob.customer.ui.tracking

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.piecejob.core.data.repository.JobRepository
import com.piecejob.core.data.remote.dto.JobDto
import com.piecejob.core.socket.SocketManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class JobTrackingViewModel @Inject constructor(
    private val jobRepository: JobRepository,
    private val socketManager: SocketManager
) : ViewModel() {

    private val _job = MutableStateFlow<JobDto?>(null)
    val job: StateFlow<JobDto?> = _job

    private val _providerLocation = MutableStateFlow<Pair<Double, Double>?>(null)
    val providerLocation: StateFlow<Pair<Double, Double>?> = _providerLocation

    fun initTracking(jobId: String) {
        viewModelScope.launch {
            val response = jobRepository.getJobById(jobId)
            if (response.success) {
                _job.value = response.data
                socketManager.connect("https://piecejob-backend.onrender.com")
                socketManager.joinJob(jobId)
                
                socketManager.onLocationUpdated { lat, lng ->
                    _providerLocation.value = lat to lng
                }
                
                socketManager.onStatusUpdated { status ->
                    _job.value = _job.value?.copy(status = status)
                }
            }
        }
    }

    fun cancelJob() {
        _job.value?.let {
            viewModelScope.launch {
                jobRepository.cancelJob(it.id)
            }
        }
    }
}
