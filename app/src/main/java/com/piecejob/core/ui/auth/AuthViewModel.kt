package com.piecejob.core.ui.auth

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.piecejob.BuildConfig
import com.piecejob.core.data.repository.AuthRepository
import com.piecejob.core.data.local.SessionManager
import com.piecejob.core.data.remote.dto.*
import com.piecejob.core.data.remote.ApiResponse
import com.piecejob.core.data.remote.CountryDto
import com.piecejob.core.data.remote.LanguageDto
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val repository: AuthRepository,
    private val sessionManager: SessionManager
) : ViewModel() {

    private val _authState = MutableStateFlow<AuthState>(AuthState.Idle)
    val authState: StateFlow<AuthState> = _authState

    // Registration Data
    val availableCountries = MutableStateFlow<List<CountryDto>>(emptyList())
    val availableLanguages = MutableStateFlow<List<LanguageDto>>(emptyList())
    
    val selectedCountry = MutableStateFlow<CountryDto?>(null)
    val selectedLanguage = MutableStateFlow<LanguageDto?>(null)
    
    val phoneNumber = MutableStateFlow("")
    val loginIdentifier = MutableStateFlow("") // Prefilled from last session
    
    // User details
    val firstName = MutableStateFlow("")
    val lastName = MutableStateFlow("")
    val email = MutableStateFlow("")
    val dob = MutableStateFlow("")
    val gender = MutableStateFlow("") // New Field
    val idNumber = MutableStateFlow("")
    val password = MutableStateFlow("")

    // Provider Specific
    val selectedServices = MutableStateFlow<List<String>>(emptyList())

    private val TAG = "AuthViewModel"

    init {
        loginIdentifier.value = sessionManager.getLastPhoneNumber() ?: ""
        loadConfigs()
    }

    fun loadConfigs() {
        viewModelScope.launch {
            Log.d(TAG, "Loading configurations from backend...")
            
            val countryRes = repository.getCountries()
            if (countryRes.success) {
                Log.d(TAG, "Countries loaded: ${countryRes.data?.size ?: 0}")
                availableCountries.value = countryRes.data ?: emptyList()
                selectedCountry.value = availableCountries.value.find { it.code == "ZA" } 
                    ?: availableCountries.value.firstOrNull()
            } else {
                Log.e(TAG, "Failed to load countries: ${countryRes.message}")
            }
            
            val langRes = repository.getLanguages()
            if (langRes.success) {
                Log.d(TAG, "Languages loaded: ${langRes.data?.size ?: 0}")
                availableLanguages.value = langRes.data ?: emptyList()
                selectedLanguage.value = availableLanguages.value.find { it.code == "en" }
                    ?: availableLanguages.value.firstOrNull()
            } else {
                Log.e(TAG, "Failed to load languages: ${langRes.message}")
            }
        }
    }

    fun requestOtp(phone: String) {
        viewModelScope.launch {
            _authState.value = AuthState.Loading
            Log.d(TAG, "Requesting OTP for $phone")
            val response = repository.requestOtp(phone)
            if (response.success) {
                phoneNumber.value = phone
                _authState.value = AuthState.OtpSent
            } else {
                _authState.value = AuthState.Error(response.error?.message ?: "Failed to send OTP")
            }
        }
    }

    fun verifyOtp(otp: String) {
        viewModelScope.launch {
            _authState.value = AuthState.Loading
            Log.d(TAG, "Verifying OTP: $otp")
            val response = repository.verifyOtp(phoneNumber.value, otp)
            if (response.success) {
                _authState.value = AuthState.OtpVerified
            } else {
                _authState.value = AuthState.Error(response.error?.message ?: "Invalid OTP")
            }
        }
    }

    fun register() {
        viewModelScope.launch {
            Log.d(TAG, "Entering register() coroutine")
            _authState.value = AuthState.Loading
            
            val deviceId = sessionManager.getDeviceId()
            val countryCode = selectedCountry.value?.code ?: "ZA"
            val isProvider = BuildConfig.FLAVOR == "provider"

            Log.d(TAG, "Registration Details - isProvider: $isProvider, Country: $countryCode, Phone: ${phoneNumber.value}")

            val response = if (!isProvider) {
                val request = CustomerRegisterRequest(
                    firstName = firstName.value,
                    lastName = lastName.value,
                    email = email.value,
                    phoneNumber = phoneNumber.value,
                    password = password.value,
                    countryCode = countryCode,
                    dob = dob.value,
                    idNumber = idNumber.value,
                    gender = gender.value,
                    deviceId = deviceId
                )
                Log.d(TAG, "Submitting Customer Registration API call")
                repository.registerCustomer(request)
            } else {
                val request = ProviderRegisterRequest(
                    firstName = firstName.value,
                    lastName = lastName.value,
                    email = email.value,
                    phoneNumber = phoneNumber.value,
                    password = password.value,
                    countryCode = countryCode,
                    deviceId = deviceId,
                    gender = gender.value,
                    dob = dob.value,
                    nationalityType = "Citizen",
                    idOrPassportNumber = idNumber.value,
                    servicesOffered = selectedServices.value
                )
                Log.d(TAG, "Submitting Provider Registration API call")
                repository.registerProvider(request)
            }

            if (response.success) {
                Log.d(TAG, "Registration Success! Proceeding to auto-login.")
                sessionManager.saveLastPhoneNumber(phoneNumber.value)
                loginInternal(phoneNumber.value, password.value)
            } else {
                Log.e(TAG, "Registration Failed: ${response.error?.message}")
                _authState.value = AuthState.Error(response.error?.message ?: "Registration failed")
            }
        }
    }

    fun login(identifier: String, pass: String) {
        viewModelScope.launch {
            loginInternal(identifier, pass)
        }
    }

    private suspend fun loginInternal(identifier: String, pass: String) {
        Log.d(TAG, "Executing loginInternal for $identifier")
        _authState.value = AuthState.Loading
        val deviceId = sessionManager.getDeviceId()
        val response = repository.login(identifier, pass, deviceId)
        
        if (response.success && response.data != null) {
            Log.d(TAG, "Login Success for $identifier")
            val data = response.data
            sessionManager.saveAuthToken(data.token)
            sessionManager.saveRefreshToken(data.refreshToken)
            sessionManager.saveUser(
                userId = data.user.id,
                role = data.user.role,
                firstName = data.user.firstName
            )
            sessionManager.saveCountryCode(data.user.countryCode)
            sessionManager.saveLastPhoneNumber(identifier)
            _authState.value = AuthState.Authenticated(data)
        } else {
            Log.e(TAG, "Login Failed for $identifier: ${response.error?.message}")
            _authState.value = AuthState.Error(response.error?.message ?: "Login failed")
        }
    }

    fun resetState() {
        _authState.value = AuthState.Idle
    }
    
    fun isLoggedIn(): Boolean {
        return sessionManager.getAuthToken() != null
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
