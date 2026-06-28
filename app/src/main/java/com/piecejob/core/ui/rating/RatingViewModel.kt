package com.piecejob.core.ui.rating

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.piecejob.core.data.repository.JobRepository
import com.piecejob.core.data.remote.dto.JobDto
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class RatingViewModel @Inject constructor(
    private val jobRepository: JobRepository
) : ViewModel() {

    private val _job = MutableStateFlow<JobDto?>(null)
    val job: StateFlow<JobDto?> = _job

    private val _isSubmitting = MutableStateFlow(false)
    val isSubmitting: StateFlow<Boolean> = _isSubmitting

    private val _isSuccess = MutableStateFlow(false)
    val isSuccess: StateFlow<Boolean> = _isSuccess

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    fun loadJob(jobId: String) {
        viewModelScope.launch {
            val res = jobRepository.getJobById(jobId)
            if (res.success) {
                _job.value = res.data
            } else {
                _error.value = res.message ?: "Failed to load job details"
            }
        }
    }

    fun submitRating(jobId: String, rating: Int, comment: String) {
        if (rating == 0) {
            _error.value = "Please select a star rating"
            return
        }

        viewModelScope.launch {
            _isSubmitting.value = true
            _error.value = null
            val res = jobRepository.rateJob(jobId, rating, comment)
            if (res.success) {
                _isSuccess.value = true
            } else {
                _error.value = res.message ?: "Failed to submit rating"
            }
            _isSubmitting.value = false
        }
    }
}
