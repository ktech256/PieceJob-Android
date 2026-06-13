package com.piecejob.provider.ui.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.piecejob.core.data.repository.JobRepository
import com.piecejob.core.data.remote.dto.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ProviderDisputeViewModel @Inject constructor(
    private val repository: JobRepository
) : ViewModel() {

    private val _disputes = MutableStateFlow<List<DisputeDto>>(emptyList())
    val disputes: StateFlow<List<DisputeDto>> = _disputes

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    init {
        loadDisputes()
    }

    fun loadDisputes() {
        viewModelScope.launch {
            _isLoading.value = true
            val response = repository.getMyDisputes()
            if (response.success) {
                _disputes.value = response.data ?: emptyList()
            }
            _isLoading.value = false
        }
    }

    fun raiseDispute(jobId: String, reason: String, description: String) {
        viewModelScope.launch {
            _isLoading.value = true
            val res = repository.raiseDispute(RaiseDisputeRequest(jobId, reason, description))
            if (res.success) {
                loadDisputes()
            }
            _isLoading.value = false
        }
    }
}
