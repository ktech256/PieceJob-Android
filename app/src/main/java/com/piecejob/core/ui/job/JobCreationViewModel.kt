package com.piecejob.core.ui.job

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.piecejob.core.data.repository.JobRepository
import com.piecejob.core.data.remote.dto.CreateJobRequest
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class JobCreationViewModel @Inject constructor(
    private val jobRepository: JobRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<JobCreationState>(JobCreationState.Idle)
    val uiState: StateFlow<JobCreationState> = _uiState

    fun createJob(serviceCode: String, lat: Double, lng: Double, address: String) {
        viewModelScope.launch {
            _uiState.value = JobCreationState.Loading
            val result = jobRepository.createJob(CreateJobRequest(serviceCode, listOf(lng, lat), address, null))
            if (result.success) {
                _uiState.value = JobCreationState.Success(result.data?.id ?: "")
            } else {
                _uiState.value = JobCreationState.Error(result.message ?: "Failed to create job")
            }
        }
    }
}

sealed class JobCreationState {
    object Idle : JobCreationState()
    object Loading : JobCreationState()
    data class Success(val jobId: String) : JobCreationState()
    data class Error(val message: String) : JobCreationState()
}
