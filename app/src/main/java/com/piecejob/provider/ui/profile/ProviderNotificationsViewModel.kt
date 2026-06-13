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
class ProviderNotificationsViewModel @Inject constructor(
    private val repository: ProviderRepository
) : ViewModel() {

    private val _settings = MutableStateFlow<NotificationSettingsDto?>(null)
    val settings: StateFlow<NotificationSettingsDto?> = _settings

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
                _settings.value = response.data?.notificationSettings
            }
            _isLoading.value = false
        }
    }

    fun updateSettings(settings: NotificationSettingsDto) {
        viewModelScope.launch {
            _isLoading.value = true
            val response = repository.updateNotificationSettings(settings)
            if (response.success) {
                _settings.value = response.data
            }
            _isLoading.value = false
        }
    }
}
