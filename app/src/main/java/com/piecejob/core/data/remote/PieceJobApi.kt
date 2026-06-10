package com.piecejob.core.data.remote

import com.piecejob.core.data.remote.dto.*
import retrofit2.http.*

interface PieceJobApi {

    @POST("auth/request-otp")
    suspend fun requestOtp(@Body request: OtpRequest): ApiResponse<Unit>

    @POST("auth/verify-otp")
    suspend fun verifyOtp(@Body request: OtpVerifyRequest): ApiResponse<Unit>

    @POST("auth/register/customer")
    suspend fun registerCustomer(@Body request: CustomerRegisterRequest): ApiResponse<Unit>

    @POST("auth/register/provider")
    suspend fun registerProvider(@Body request: ProviderRegisterRequest): ApiResponse<Unit>

    @POST("auth/login")
    suspend fun login(@Body request: LoginRequest): ApiResponse<LoginResponse>

    @POST("auth/refresh-token")
    suspend fun refreshToken(@Body request: RefreshRequest): ApiResponse<RefreshResponse>

    @GET("users/profile")
    suspend fun getProfile(): ApiResponse<UserDto>

    @GET("wallets/balance")
    suspend fun getWalletBalance(): ApiResponse<WalletDto>
}

data class ApiResponse<T>(
    val success: Boolean,
    val message: String?,
    val data: T?,
    val error: ApiError?
)

data class ApiError(
    val code: String,
    val message: String
)
