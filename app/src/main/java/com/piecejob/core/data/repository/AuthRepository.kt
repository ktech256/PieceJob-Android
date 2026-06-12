package com.piecejob.core.data.repository

import com.piecejob.core.data.remote.*
import com.piecejob.core.data.remote.dto.*
import com.piecejob.core.data.remote.ApiResponse
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject

class AuthRepository @Inject constructor(
    private val api: PieceJobApi
) {
    suspend fun requestOtp(phoneNumber: String): ApiResponse<Unit> {
        return try {
            api.requestOtp(OtpRequest(phoneNumber))
        } catch (e: Exception) {
            ApiResponse(false, e.message, null, ApiError("500", e.message ?: "Unknown error"))
        }
    }

    suspend fun verifyOtp(phoneNumber: String, otp: String): ApiResponse<Unit> {
        return try {
            api.verifyOtp(OtpVerifyRequest(phoneNumber, otp))
        } catch (e: Exception) {
            ApiResponse(false, e.message, null, ApiError("500", e.message ?: "Unknown error"))
        }
    }

    suspend fun login(identifier: String, password: String, deviceId: String?): ApiResponse<LoginResponse> {
        return try {
            api.login(LoginRequest(identifier, password, deviceId))
        } catch (e: Exception) {
            ApiResponse(false, e.message, null, ApiError("500", e.message ?: "Unknown error"))
        }
    }

    suspend fun registerCustomer(request: CustomerRegisterRequest): ApiResponse<Unit> {
        return try {
            api.registerCustomer(request)
        } catch (e: Exception) {
            ApiResponse(false, e.message, null, ApiError("500", e.message ?: "Unknown error"))
        }
    }

    suspend fun registerProvider(request: ProviderRegisterRequest): ApiResponse<Unit> {
        return try {
            api.registerProvider(request)
        } catch (e: Exception) {
            ApiResponse(false, e.message, null, ApiError("500", e.message ?: "Unknown error"))
        }
    }

    suspend fun getCountries(): ApiResponse<List<CountryDto>> {
        return try {
            api.getCountries()
        } catch (e: Exception) {
            ApiResponse(false, e.message, null, ApiError("500", e.message ?: "Unknown error"))
        }
    }

    suspend fun getLanguages(): ApiResponse<List<LanguageDto>> {
        return try {
            api.getLanguages()
        } catch (e: Exception) {
            ApiResponse(false, e.message, null, ApiError("500", e.message ?: "Unknown error"))
        }
    }
}
