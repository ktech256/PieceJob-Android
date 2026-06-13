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
class ProviderWalletSettingsViewModel @Inject constructor(
    private val repository: ProviderRepository
) : ViewModel() {

    private val _payoutPrefs = MutableStateFlow<PayoutPreferencesDto?>(null)
    val payoutPrefs: StateFlow<PayoutPreferencesDto?> = _payoutPrefs

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    init {
        loadSettings()
    }

    fun loadSettings() {
        viewModelScope.launch {
            _isLoading.value = true
            val response = repository.getProviderFullProfile()
            if (response.success) {
                _payoutPrefs.value = response.data?.payoutPreferences
            }
            _isLoading.value = false
        }
    }

    fun updateSettings(frequency: String, method: String) {
        viewModelScope.launch {
            _isLoading.value = true
            val response = repository.updateWalletSettings(PayoutPreferencesDto(frequency, method))
            if (response.success) {
                _payoutPrefs.value = response.data
            }
            _isLoading.value = false
        }
    }
}
