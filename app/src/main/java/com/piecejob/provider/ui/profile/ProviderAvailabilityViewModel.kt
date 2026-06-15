package com.piecejob.provider.ui.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.piecejob.core.data.remote.dto.AvailabilityDto
import com.piecejob.core.data.remote.dto.WorkingDayDto
import com.piecejob.core.data.repository.ProviderRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ProviderAvailabilityViewModel @Inject constructor(
    private val repository: ProviderRepository
) : ViewModel() {

    private val _availability = MutableStateFlow<AvailabilityDto?>(null)
    val availability: StateFlow<AvailabilityDto?> = _availability

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _isSuccess = MutableStateFlow(false)
    val isSuccess: StateFlow<Boolean> = _isSuccess

    init {
        loadAvailability()
    }

    fun loadAvailability() {
        viewModelScope.launch {
            _isLoading.value = true
            val response = repository.getAvailability()
            if (response.success) {
                _availability.value = response.data
            }
            _isLoading.value = false
        }
    }

    fun updateAvailability(vacationMode: Boolean, workingHours: List<WorkingDayDto>) {
        viewModelScope.launch {
            _isLoading.value = true
            val response = repository.updateAvailability(AvailabilityDto(vacationMode, workingHours))
            if (response.success) {
                _availability.value = response.data
                _isSuccess.value = true
            }
            _isLoading.value = false
        }
    }
    
    fun resetSuccess() { _isSuccess.value = false }
}
