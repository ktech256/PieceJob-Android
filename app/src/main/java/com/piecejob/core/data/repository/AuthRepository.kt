package com.piecejob.core.data.repository

import com.piecejob.core.data.remote.*
import com.piecejob.core.data.remote.dto.*
import com.piecejob.core.data.remote.ApiResponse
import retrofit2.HttpException
import javax.inject.Inject

class AuthRepository @Inject constructor(
    private val api: PieceJobApi
) : BaseRepository() {

    suspend fun requestOtp(phoneNumber: String): ApiResponse<Unit> {
        return try {
            api.requestOtp(OtpRequest(phoneNumber))
        } catch (e: Exception) {
            handleError(e)
        }
    }

    suspend fun verifyOtp(phoneNumber: String, otp: String): ApiResponse<Unit> {
        return try {
            api.verifyOtp(OtpVerifyRequest(phoneNumber, otp))
        } catch (e: Exception) {
            handleError(e)
        }
    }

    suspend fun checkPhone(phoneNumber: String): ApiResponse<CheckPhoneResponse> {
        return try {
            api.checkPhone(phoneNumber)
        } catch (e: Exception) {
            handleError(e)
        }
    }

    suspend fun login(
        identifier: String,
        password: String,
        deviceId: String?,
        fcmToken: String? = null,
        appType: String? = null
    ): ApiResponse<LoginResponse> {
        android.util.Log.e("PIECEJOB_NETWORK", "Repository login called")
        return try {
            val request = LoginRequest(identifier, password, deviceId, fcmToken = fcmToken, appType = appType)
            val json = com.google.gson.Gson().toJson(request)
            android.util.Log.d("FCM_AUDIT", "OUTGOING_LOGIN_REQUEST: $json")
            
            val response = api.login(request)
            if (response.success) {
                android.util.Log.d("FCM_AUDIT", "LOGIN_RESPONSE: SUCCESS")
            } else {
                android.util.Log.e("FCM_AUDIT", "LOGIN_RESPONSE: FAILED. Msg=${response.message}")
            }
            response
        } catch (e: Exception) {
            android.util.Log.e("FCM_AUDIT", "LOGIN_NETWORK_CRASH: ${e.message}", e)
            handleError(e)
        }
    }

    suspend fun changePassword(current: String, next: String): ApiResponse<Unit> {
        return try {
            api.changePassword(ChangePasswordRequest(current, next))
        } catch (e: Exception) {
            handleError(e)
        }
    }

    suspend fun logoutAll(): ApiResponse<Unit> {
        return try {
            api.logoutAllDevices()
        } catch (e: Exception) {
            handleError(e)
        }
    }

    suspend fun getAuthorizedDevices(): ApiResponse<List<DeviceDto>> {
        return try {
            api.getAuthorizedDevices()
        } catch (e: Exception) {
            handleError(e)
        }
    }

    suspend fun removeDevice(id: String): ApiResponse<Unit> {
        return try {
            api.removeDevice(id)
        } catch (e: Exception) {
            handleError(e)
        }
    }

    suspend fun requestPhoneChange(phone: String): ApiResponse<Unit> {
        return try {
            api.requestPhoneChange(PhoneChangeRequest(phone))
        } catch (e: Exception) {
            handleError(e)
        }
    }

    suspend fun verifyPhoneChange(phone: String, otp: String): ApiResponse<Unit> {
        return try {
            api.verifyPhoneChange(PhoneVerifyRequest(phone, otp))
        } catch (e: Exception) {
            handleError(e)
        }
    }

    suspend fun requestEmailChange(email: String): ApiResponse<Unit> {
        return try {
            api.requestEmailChange(EmailChangeRequest(email))
        } catch (e: Exception) {
            handleError(e)
        }
    }

    suspend fun verifyEmailChange(email: String, code: String): ApiResponse<Unit> {
        return try {
            api.verifyEmailChange(EmailVerifyRequest(email, code))
        } catch (e: Exception) {
            handleError(e)
        }
    }

    suspend fun registerCustomer(request: CustomerRegisterRequest): ApiResponse<Unit> {
        return try {
            api.registerCustomer(request)
        } catch (e: Exception) {
            handleError(e)
        }
    }

    suspend fun registerProvider(request: ProviderRegisterRequest): ApiResponse<Unit> {
        return try {
            api.registerProvider(request)
        } catch (e: Exception) {
            handleError(e)
        }
    }

    suspend fun getCountries(): ApiResponse<List<CountryDto>> {
        return try {
            api.getCountries()
        } catch (e: Exception) {
            handleError(e)
        }
    }

    suspend fun getLanguages(): ApiResponse<List<LanguageDto>> {
        return try {
            api.getLanguages()
        } catch (e: Exception) {
            handleError(e)
        }
    }

    suspend fun refreshToken(request: RefreshRequest): ApiResponse<RefreshResponse> {
        return try {
            api.refreshToken(request)
        } catch (e: Exception) {
            handleError(e)
        }
    }
}
