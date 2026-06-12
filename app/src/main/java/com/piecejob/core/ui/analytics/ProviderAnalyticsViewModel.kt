package com.piecejob.core.ui.analytics

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.piecejob.core.data.remote.dto.*
import com.piecejob.core.data.repository.AnalyticsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ProviderAnalyticsViewModel @Inject constructor(
    private val repository: AnalyticsRepository
) : ViewModel() {

    private val _analytics = MutableStateFlow<ProviderAnalyticsDto?>(null)
    val analytics: StateFlow<ProviderAnalyticsDto?> = _analytics

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    init {
        loadAnalytics()
    }

    fun loadAnalytics() {
        viewModelScope.launch {
            _isLoading.value = true
            val response = repository.getProviderAnalytics()
            if (response.success) {
                _analytics.value = response.data
            }
            _isLoading.value = false
        }
    }
}
