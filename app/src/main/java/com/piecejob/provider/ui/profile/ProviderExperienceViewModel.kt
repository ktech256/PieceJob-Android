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
class ProviderExperienceViewModel @Inject constructor(
    private val repository: ProviderRepository
) : ViewModel() {

    private val _experience = MutableStateFlow<List<ExperienceDto>>(emptyList())
    val experience: StateFlow<List<ExperienceDto>> = _experience

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    init {
        loadExperience()
    }

    fun loadExperience() {
        viewModelScope.launch {
            _isLoading.value = true
            val response = repository.getMyExperience()
            if (response.success) {
                _experience.value = response.data ?: emptyList()
            }
            _isLoading.value = false
        }
    }

    fun addExperience(company: String, role: String, start: String, end: String?, desc: String?) {
        viewModelScope.launch {
            _isLoading.value = true
            val response = repository.addExperience(
                ExperienceDto(company, role, start, end, desc)
            )
            if (response.success) {
                _experience.value = response.data ?: emptyList()
            }
            _isLoading.value = false
        }
    }
}
