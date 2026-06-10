package com.piecejob.core.data.remote

import com.piecejob.core.data.remote.dto.*
import retrofit2.http.*
import okhttp3.MultipartBody

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

    // =========================
    // ✅ JOB & MATCHING ROUTES
    // =========================
    @POST("jobs")
    suspend fun createJob(@Body request: CreateJobRequest): ApiResponse<JobDto>

    @GET("jobs/{jobId}")
    suspend fun getJobById(@Path("jobId") jobId: String): ApiResponse<JobDto>

    @PATCH("jobs/{jobId}/cancel")
    suspend fun cancelJob(@Path("jobId") jobId: String): ApiResponse<Unit>

    @GET("providers/jobs/broadcasted")
    suspend fun getAvailableJobs(): ApiResponse<List<JobDto>>

    @PATCH("providers/jobs/{jobId}/accept")
    suspend fun acceptJob(@Path("jobId") jobId: String): ApiResponse<JobDto>

    @POST("sos/trigger")
    suspend fun triggerSos(@Body request: SosRequest): ApiResponse<SosResponse>

    @PATCH("providers/me/status")
    suspend fun updateProviderStatus(@Body request: ProviderStatusRequest): ApiResponse<Unit>

    @Multipart
    @PATCH("providers/me/documents")
    suspend fun uploadDocuments(
        @Part idDocument: MultipartBody.Part?,
        @Part license: MultipartBody.Part?
    ): ApiResponse<Unit>
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
