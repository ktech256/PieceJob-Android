package com.piecejob.provider.ui.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.piecejob.core.data.repository.ProviderRepository
import com.piecejob.core.data.remote.dto.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ProviderCertificationsViewModel @Inject constructor(
    private val repository: ProviderRepository
) : ViewModel() {

    private val _certifications = MutableStateFlow<List<CertificationDto>>(emptyList())
    val certifications: StateFlow<List<CertificationDto>> = _certifications

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    init {
        loadCertifications()
    }

    fun loadCertifications() {
        viewModelScope.launch {
            _isLoading.value = true
            val response = repository.getMyCertifications()
            if (response.success) {
                _certifications.value = response.data ?: emptyList()
            }
            _isLoading.value = false
        }
    }

    fun addCertification(name: String, institution: String, number: String, expiry: String?) {
        viewModelScope.launch {
            _isLoading.value = true
            val response = repository.addCertification(
                CertificationDto(name, institution, number, expiry, "PENDING")
            )
            if (response.success) {
                _certifications.value = response.data ?: emptyList()
            }
            _isLoading.value = false
        }
    }
}
