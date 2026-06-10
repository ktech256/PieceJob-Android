package com.piecejob.core.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.piecejob.core.data.repository.AuthRepository
import com.piecejob.core.data.remote.dto.LoginResponse
import com.piecejob.core.data.remote.ApiResponse
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

class AuthViewModel @Inject constructor(
    private val repository: AuthRepository
) : ViewModel() {

    private val _authState = MutableStateFlow<AuthState>(AuthState.Idle)
    val authState: StateFlow<AuthState> = _authState

    fun requestOtp(phoneNumber: String) {
        viewModelScope.launch {
            _authState.value = AuthState.Loading
            val response = repository.requestOtp(phoneNumber)
            if (response.success) {
                _authState.value = AuthState.OtpSent
            } else {
                _authState.value = AuthState.Error(response.error?.message ?: "Failed to send OTP")
            }
        }
    }

    fun verifyOtp(phoneNumber: String, otp: String) {
        viewModelScope.launch {
            _authState.value = AuthState.Loading
            val response = repository.verifyOtp(phoneNumber, otp)
            if (response.success) {
                _authState.value = AuthState.OtpVerified
            } else {
                _authState.value = AuthState.Error(response.error?.message ?: "Invalid OTP")
            }
        }
    }

    fun login(identifier: String, password: String) {
        viewModelScope.launch {
            _authState.value = AuthState.Loading
            val response = repository.login(identifier, password, null)
            if (response.success && response.data != null) {
                _authState.value = AuthState.Authenticated(response.data)
            } else {
                _authState.value = AuthState.Error(response.error?.message ?: "Login failed")
            }
        }
    }
}

sealed class AuthState {
    object Idle : AuthState()
    object Loading : AuthState()
    object OtpSent : AuthState()
    object OtpVerified : AuthState()
    data class Authenticated(val data: LoginResponse) : AuthState()
    data class Error(val message: String) : AuthState()
}
