package com.piecejob.provider.ui.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.piecejob.core.data.repository.UserRepository
import com.piecejob.core.data.remote.dto.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ProviderReferralViewModel @Inject constructor(
    private val repository: UserRepository
) : ViewModel() {

    private val _stats = MutableStateFlow<ReferralStatsDto?>(null)
    val stats: StateFlow<ReferralStatsDto?> = _stats

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    init {
        loadStats()
    }

    fun loadStats() {
        viewModelScope.launch {
            _isLoading.value = true
            val res = repository.getReferralStats()
            if (res.success) {
                _stats.value = res.data
            }
            _isLoading.value = false
        }
    }
}
