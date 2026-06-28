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
    private val jobRepository: JobRepository
) : ViewModel() {

    private val _availableJobs = MutableStateFlow<List<JobDto>>(emptyList())
    val availableJobs: StateFlow<List<JobDto>> = _availableJobs

    private val _activeJobs = MutableStateFlow<List<JobDto>>(emptyList())
    val activeJobs: StateFlow<List<JobDto>> = _activeJobs

    private val _completedJobs = MutableStateFlow<List<JobDto>>(emptyList())
    val completedJobs: StateFlow<List<JobDto>> = _completedJobs

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _navigationEvent = MutableSharedFlow<String>()
    val navigationEvent: SharedFlow<String> = _navigationEvent

    init {
        loadJobs()
    }

    fun loadJobs() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val availableRes = jobRepository.getAvailableJobs()
                if (availableRes.success) {
                    _availableJobs.value = availableRes.data?.filter { it.status == "BROADCASTED" } ?: emptyList()
                    _activeJobs.value = availableRes.data?.filter { it.status in listOf("ACCEPTED", "ARRIVED", "STARTED") } ?: emptyList()
                }
                
                // History logic could go here if there's a specific history endpoint
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
            if (response.success) {
                loadJobs()
                _navigationEvent.emit(jobId)
            } else {
                _isLoading.value = false
            }
        }
    }
}
