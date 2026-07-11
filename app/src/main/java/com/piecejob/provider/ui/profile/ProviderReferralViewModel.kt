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
    private val repository: UserRepository,
    private val configRepository: com.piecejob.core.data.repository.ConfigRepository
) : ViewModel() {

    private val _stats = MutableStateFlow<ReferralStatsDto?>(null)
    val stats: StateFlow<ReferralStatsDto?> = _stats

    val isReferralEnabled = MutableStateFlow(configRepository.isReferralEnabled())
    val referralBaseUrl = MutableStateFlow(configRepository.getReferralBaseUrl())
    val qrBrandingType = MutableStateFlow(configRepository.getQrBrandingType())

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    init {
        loadStats()
    }

    fun loadStats() {
        viewModelScope.launch {
            _isLoading.value = true
            // Refresh config
            configRepository.refreshWorkspaceConfig()
            isReferralEnabled.value = configRepository.isReferralEnabled()
            referralBaseUrl.value = configRepository.getReferralBaseUrl()
            qrBrandingType.value = configRepository.getQrBrandingType()

            val res = repository.getReferralStats()
            if (res.success) {
                _stats.value = res.data
            }
            _isLoading.value = false
        }
    }
}
