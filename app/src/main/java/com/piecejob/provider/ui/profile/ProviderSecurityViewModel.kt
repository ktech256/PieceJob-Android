package com.piecejob.provider.ui.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.piecejob.core.data.repository.AuthRepository
import com.piecejob.core.data.repository.ProviderRepository
import com.piecejob.core.data.remote.dto.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ProviderSecurityViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val providerRepository: ProviderRepository
) : ViewModel() {

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message

    fun changePassword(current: String, next: String) {
        viewModelScope.launch {
            _isLoading.value = true
            val res = authRepository.changePassword(current, next)
            if (res.success) {
                _message.value = "Password changed successfully"
            } else {
                _message.value = res.message ?: "Failed to change password"
            }
            _isLoading.value = false
        }
    }

    fun logoutAllDevices() {
        viewModelScope.launch {
            _isLoading.value = true
            val res = authRepository.logoutAll()
            if (res.success) {
                _message.value = "Logged out from all devices"
            } else {
                _message.value = res.message ?: "Action failed"
            }
            _isLoading.value = false
        }
    }

    private val _otpSent = MutableStateFlow(false)
    val otpSent: StateFlow<Boolean> = _otpSent

    private val _emailCodeSent = MutableStateFlow(false)
    val emailCodeSent: StateFlow<Boolean> = _emailCodeSent

    fun requestPhoneChange(newPhone: String) {
        viewModelScope.launch {
            _isLoading.value = true
            val res = authRepository.requestPhoneChange(newPhone)
            if (res.success) {
                _otpSent.value = true
                _message.value = "OTP sent to new number"
            } else {
                _message.value = res.message ?: "Request failed"
            }
            _isLoading.value = false
        }
    }

    fun verifyPhoneChange(newPhone: String, otp: String) {
        viewModelScope.launch {
            _isLoading.value = true
            val res = authRepository.verifyPhoneChange(newPhone, otp)
            if (res.success) {
                _otpSent.value = false
                _message.value = "Phone number updated successfully"
            } else {
                _message.value = res.message ?: "Verification failed"
            }
            _isLoading.value = false
        }
    }

    fun requestEmailChange(newEmail: String) {
        viewModelScope.launch {
            _isLoading.value = true
            val res = authRepository.requestEmailChange(newEmail)
            if (res.success) {
                _emailCodeSent.value = true
                _message.value = "Verification code sent to new email"
            } else {
                _message.value = res.message ?: "Request failed"
            }
            _isLoading.value = false
        }
    }

    fun verifyEmailChange(newEmail: String, code: String) {
        viewModelScope.launch {
            _isLoading.value = true
            val res = authRepository.verifyEmailChange(newEmail, code)
            if (res.success) {
                _emailCodeSent.value = false
                _message.value = "Email updated successfully"
            } else {
                _message.value = res.message ?: "Verification failed"
            }
            _isLoading.value = false
        }
    }

    fun clearMessage() {
        _message.value = null
    }
}
