package com.piecejob.provider.ui.verification

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.piecejob.core.data.remote.*
import com.piecejob.core.data.remote.dto.*
import com.piecejob.core.data.repository.VerificationRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ProviderVerificationViewModel @Inject constructor(
    private val repository: VerificationRepository
) : ViewModel() {

    private val _status = MutableStateFlow<VerificationStatusDto?>(null)
    val status: StateFlow<VerificationStatusDto?> = _status

    private val _requirements = MutableStateFlow<VerificationRequirementsDto?>(null)
    val requirements: StateFlow<VerificationRequirementsDto?> = _requirements

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    init {
        loadData()
    }

    fun loadData() {
        viewModelScope.launch {
            _isLoading.value = true
            launch {
                val response = repository.getVerificationStatus()
                if (response.success) _status.value = response.data
            }
            launch {
                val response = repository.getVerificationRequirements()
                if (response.success) _requirements.value = response.data
            }
            _isLoading.value = false
        }
    }

    fun submitVerification(type: String, documents: List<VerificationDocDto>) {
        viewModelScope.launch {
            _isLoading.value = true
            val response = repository.submitVerification(SubmitVerificationRequest(type, documents))
            if (response.success) {
                loadData()
            } else {
                _error.value = response.error?.message ?: "Failed to submit"
            }
            _isLoading.value = false
        }
    }
}
