package com.piecejob.core.ui.auth

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.piecejob.BuildConfig
import com.piecejob.core.data.repository.AuthRepository
import com.piecejob.core.data.repository.UserRepository
import com.piecejob.core.data.local.SessionManager
import com.piecejob.core.data.remote.dto.*
import com.piecejob.core.data.remote.ApiResponse
import com.piecejob.core.data.remote.CountryDto
import com.piecejob.core.data.remote.LanguageDto
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val repository: AuthRepository,
    private val userRepository: UserRepository,
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
        Log.d(TAG, "Initializing AuthViewModel")
        loginIdentifier.value = sessionManager.getLastPhoneNumber() ?: ""
        loadConfigs()
    }

    fun loadConfigs() {
        viewModelScope.launch {
            Log.d(TAG, "Loading configurations from backend...")
            try {
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
            } catch (e: Exception) {
                Log.e(TAG, "Exception during loadConfigs: ${e.message}", e)
            }
        }
    }

    fun requestOtp(phone: String) {
        viewModelScope.launch {
            Log.d(TAG, "requestOtp called for $phone")
            _authState.value = AuthState.Loading
            try {
                val response = repository.requestOtp(phone)
                if (response.success) {
                    phoneNumber.value = phone
                    _authState.value = AuthState.OtpSent
                    Log.d(TAG, "requestOtp SUCCESS")
                } else {
                    Log.e(TAG, "requestOtp API ERROR: ${response.error?.message}")
                    _authState.value = AuthState.Error(response.error?.message ?: "Failed to send OTP")
                }
            } catch (e: Exception) {
                Log.e(TAG, "requestOtp CRASH: ${e.message}", e)
                _authState.value = AuthState.Error(e.message ?: "Network error")
            }
        }
    }

    fun verifyOtp(otp: String) {
        viewModelScope.launch {
            Log.d(TAG, "verifyOtp called with $otp")
            _authState.value = AuthState.Loading
            try {
                val response = repository.verifyOtp(phoneNumber.value, otp)
                if (response.success) {
                    _authState.value = AuthState.OtpVerified
                    Log.d(TAG, "verifyOtp SUCCESS")
                } else {
                    Log.e(TAG, "verifyOtp API ERROR: ${response.error?.message}")
                    _authState.value = AuthState.Error(response.error?.message ?: "Invalid OTP")
                }
            } catch (e: Exception) {
                Log.e(TAG, "verifyOtp CRASH: ${e.message}", e)
                _authState.value = AuthState.Error(e.message ?: "Verification failed")
            }
        }
    }

    fun register() {
        viewModelScope.launch {
            Log.d(TAG, "Entering register() flow")
            _authState.value = AuthState.Loading
            
            try {
                val deviceId = sessionManager.getDeviceId()
                val fcmToken = try {
                    com.google.firebase.messaging.FirebaseMessaging.getInstance().token.await()
                } catch (e: Exception) {
                    null
                }

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
                        deviceId = deviceId,
                        fcmToken = fcmToken
                    )
                    Log.d(TAG, "Calling registerCustomer API")
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
                        servicesOffered = selectedServices.value,
                        fcmToken = fcmToken
                    )
                    Log.d(TAG, "Calling registerProvider API")
                    repository.registerProvider(request)
                }

                if (response.success) {
                    Log.d(TAG, "Registration API SUCCESS. Triggering auto-login.")
                    sessionManager.saveLastPhoneNumber(phoneNumber.value)
                    loginInternal(phoneNumber.value, password.value)
                } else {
                    Log.e(TAG, "Registration API ERROR: ${response.error?.message}")
                    _authState.value = AuthState.Error(response.error?.message ?: "Registration failed")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Registration CRASH: ${e.message}", e)
                _authState.value = AuthState.Error("Registration failed: ${e.message}")
            }
        }
    }

    fun login(identifier: String, pass: String) {
        viewModelScope.launch {
            Log.d("FCM_AUDIT", "LOGIN_START: identifier=$identifier")
            loginInternal(identifier, pass)
        }
    }

    private suspend fun loginInternal(identifier: String, pass: String) {
        Log.d("FCM_AUDIT", "LOGIN_START: identifier=$identifier")
        _authState.value = AuthState.Loading
        try {
            val deviceId = sessionManager.getDeviceId()
            val fcmToken = try {
                val t = com.google.firebase.messaging.FirebaseMessaging.getInstance().token.await()
                Log.d("FCM_AUDIT", "FCM_TOKEN_ACQUIRED: Token acquired successfully. Len=${t.length}")
                t
            } catch (e: Exception) {
                Log.e("FCM_AUDIT", "FCM_TOKEN_ACQUIRE_FAILED: ${e.message}")
                null
            }

            Log.d("FCM_AUDIT", "FCM_UPLOAD_START (Login): identifier=$identifier, hasToken=${fcmToken != null}")
            val response = repository.login(identifier, pass, deviceId, fcmToken)
            
            if (response.success && response.data != null) {
                Log.d("FCM_AUDIT", "LOGIN_SUCCESS: Received auth response.")
                val data = response.data
                sessionManager.saveAuthToken(data.token)
                sessionManager.saveRefreshToken(data.refreshToken)
                sessionManager.saveUser(
                    userId = data.user.id,
                    role = data.user.role,
                    firstName = data.user.firstName,
                    gender = data.user.gender
                )
                sessionManager.saveCountryCode(data.user.countryCode)
                sessionManager.saveLastPhoneNumber(identifier)
                _authState.value = AuthState.Authenticated(data)
                
                Log.d("FCM_AUDIT", "SESSION_VERIFY: userId=${sessionManager.getUserId()}, role=${sessionManager.getRole()}")

                // Sync FCM Token immediately after login
                val token = try {
                    val t = com.google.firebase.messaging.FirebaseMessaging.getInstance().token.await()
                    Log.d("FCM_AUDIT", "Post-login: Token acquired. Len=${t.length}")
                    t
                } catch (e: Exception) {
                    null
                }
                
                if (token != null) {
                    Log.d("FCM_AUDIT", "FCM_UPLOAD_START (Sync): Syncing token after login...")
                    val syncRes = userRepository.updateFcmToken(token)
                    if (syncRes.success) {
                        Log.d("FCM_AUDIT", "FCM_UPLOAD_SUCCESS: Post-login token sync success")
                    } else {
                        Log.e("FCM_AUDIT", "FCM_UPLOAD_FAILED: Post-login sync error: ${syncRes.message}")
                    }
                } else {
                    Log.e("FCM_AUDIT", "Post-login token sync failed: token is null")
                }
            } else {
                Log.e(TAG, "loginInternal API ERROR: ${response.error?.message}")
                _authState.value = AuthState.Error(response.error?.message ?: "Login failed")
            }
        } catch (e: Exception) {
            Log.e(TAG, "loginInternal CRASH: ${e.message}", e)
            _authState.value = AuthState.Error("Login failed: ${e.message}")
        }
    }

    fun resetState() {
        Log.d(TAG, "Resetting AuthState to Idle")
        _authState.value = AuthState.Idle
    }

    fun logout() {
        Log.d(TAG, "Logging out user. Should ideally clear FCM token on backend.")
        // Note: Clearing FCM token on backend is recommended but requires a network call.
        // For now, we clear the session immediately.
        sessionManager.clearSession()
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
