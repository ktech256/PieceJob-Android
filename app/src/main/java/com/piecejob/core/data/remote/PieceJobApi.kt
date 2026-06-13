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

    @POST("auth/change-password")
    suspend fun changePassword(@Body request: ChangePasswordRequest): ApiResponse<Unit>

    @POST("auth/logout-all")
    suspend fun logoutAllDevices(): ApiResponse<Unit>

    @GET("auth/devices")
    suspend fun getAuthorizedDevices(): ApiResponse<List<DeviceDto>>

    @DELETE("auth/devices/{id}")
    suspend fun removeDevice(@Path("id") deviceId: String): ApiResponse<Unit>

    @GET("users/profile")
    suspend fun getProfile(): ApiResponse<UserDto>

    @PATCH("users/fcm-token")
    suspend fun updateFcmToken(@Body request: FcmTokenRequest): ApiResponse<Unit>

    @GET("users/referrals")
    suspend fun getReferralStats(): ApiResponse<ReferralStatsDto>

    @GET("wallets/balance")
    suspend fun getWalletBalance(): ApiResponse<WalletDto>

    @GET("wallets/history")
    suspend fun getWalletHistory(): ApiResponse<List<WalletTransactionDto>>

    @GET("wallets/payouts")
    suspend fun getPayouts(): ApiResponse<List<PayoutDto>>

    @GET("wallets/statements")
    suspend fun getStatements(): ApiResponse<List<StatementDto>>

    @GET("wallets/invoices")
    suspend fun getInvoices(): ApiResponse<List<InvoiceDto>>

    @GET("wallets/commission-rate")
    suspend fun getCommissionRate(): ApiResponse<CommissionRateDto>

    @GET("providers/profile")
    suspend fun getProviderProfile(): ApiResponse<ProviderFullDto>

    @PATCH("providers/profile")
    suspend fun updateProviderProfile(@Body request: UpdateProfileRequest): ApiResponse<ProviderFullDto>

    @GET("providers/services")
    suspend fun getMyServices(): ApiResponse<List<ServiceDto>>

    @POST("providers/services")
    suspend fun updateMyServices(@Body request: UpdateServicesRequest): ApiResponse<UpdateServicesResponse>

    @GET("providers/equipment")
    suspend fun getMyEquipment(): ApiResponse<List<EquipmentDto>>

    @POST("providers/equipment")
    suspend fun addEquipment(@Body request: EquipmentDto): ApiResponse<List<EquipmentDto>>

    @GET("providers/certifications")
    suspend fun getMyCertifications(): ApiResponse<List<CertificationDto>>

    @POST("providers/certifications")
    suspend fun addCertification(@Body request: CertificationDto): ApiResponse<List<CertificationDto>>

    @GET("providers/experience")
    suspend fun getMyExperience(): ApiResponse<List<ExperienceDto>>

    @POST("providers/experience")
    suspend fun addExperience(@Body request: ExperienceDto): ApiResponse<List<ExperienceDto>>

    @GET("providers/bank")
    suspend fun getBankDetails(): ApiResponse<BankDetailsDto>

    @POST("providers/bank")
    suspend fun updateBankDetails(@Body request: UpdateBankDetailsRequest): ApiResponse<BankDetailsDto>

    @PATCH("providers/wallet-settings")
    suspend fun updateWalletSettings(@Body request: PayoutPreferencesDto): ApiResponse<PayoutPreferencesDto>

    @PATCH("providers/notifications")
    suspend fun updateNotificationSettings(@Body request: NotificationSettingsDto): ApiResponse<NotificationSettingsDto>

    @GET("providers/dashboard-stats")
    suspend fun getProviderDashboardStats(): ApiResponse<ProviderStatsDto>

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

    @PATCH("providers/jobs/{jobId}/arrive")
    suspend fun markArrival(@Path("jobId") jobId: String): ApiResponse<Unit>

    @PATCH("providers/jobs/{jobId}/start")
    suspend fun startJob(@Path("jobId") jobId: String): ApiResponse<Unit>

    @PATCH("providers/jobs/{jobId}/complete")
    suspend fun completeJob(@Path("jobId") jobId: String): ApiResponse<Unit>

    @POST("sos/trigger")
    suspend fun triggerSos(@Body request: SosRequest): ApiResponse<SosResponse>

    @POST("sos/{id}/audio")
    suspend fun uploadSosAudio(@Path("id") id: String, @Body request: AudioUploadRequest): ApiResponse<Unit>

    @POST("sos/{id}/photo")
    suspend fun uploadSosPhoto(@Path("id") id: String, @Body request: PhotoUploadRequest): ApiResponse<Unit>

    @PATCH("providers/me/status")
    suspend fun updateProviderStatus(@Body request: ProviderStatusRequest): ApiResponse<Unit>

    @POST("providers/heartbeat")
    suspend fun sendHeartbeat(@Body request: HeartbeatRequest): ApiResponse<Unit>

    @GET("providers/verification/status")
    suspend fun getVerificationStatus(): ApiResponse<VerificationStatusDto>

    @POST("providers/verification/submit")
    suspend fun submitVerification(@Body request: SubmitVerificationRequest): ApiResponse<Unit>

    @Multipart
    @PATCH("providers/me/documents")
    suspend fun uploadDocuments(
        @Part idDocument: MultipartBody.Part?,
        @Part license: MultipartBody.Part?
    ): ApiResponse<Unit>

    @GET("config/workspace")
    suspend fun getWorkspaceConfig(): ApiResponse<WorkspaceConfigDto>

    @GET("config/services")
    suspend fun getServices(
        @Query("gender") gender: String? = null
    ): ApiResponse<List<ServiceDto>>

    @GET("config/pricing/estimate")
    suspend fun getPriceEstimate(
        @Query("serviceCode") serviceCode: String,
        @Query("zoneId") zoneId: String?,
        @Query("isEmergency") isEmergency: Boolean
    ): ApiResponse<PriceEstimateDto>

    @GET("config/zones/resolve")
    suspend fun resolveZone(
        @Query("lat") lat: Double,
        @Query("lng") lng: Double
    ): ApiResponse<ZoneDto>

    @GET("config/countries")
    suspend fun getCountries(): ApiResponse<List<CountryDto>>

    @GET("config/languages")
    suspend fun getLanguages(): ApiResponse<List<LanguageDto>>

    // =========================
    // ✅ CORPORATE B2B ROUTES
    // =========================
    @GET("corporate/profile")
    suspend fun getCompanyProfile(): ApiResponse<CompanyDto>

    @GET("corporate/employees")
    suspend fun getEmployees(): ApiResponse<List<UserDto>>

    @GET("corporate/schedules")
    suspend fun getSchedules(): ApiResponse<List<CorporateScheduleDto>>

    // =========================
    // ✅ SUPPORT & DISPUTE ROUTES
    // =========================
    @POST("support/tickets")
    suspend fun submitTicket(@Body request: SubmitTicketRequest): ApiResponse<Unit>

    @GET("support/tickets")
    suspend fun getMyTickets(): ApiResponse<List<TicketDto>>

    @GET("disputes/me")
    suspend fun getMyDisputes(): ApiResponse<List<DisputeDto>>

    @POST("disputes")
    suspend fun raiseDispute(@Body request: RaiseDisputeRequest): ApiResponse<Unit>

    // =========================
    // ✅ CHAT ROUTES
    // =========================
    @GET("chat/{jobId}")
    suspend fun getChatMessages(@Path("jobId") jobId: String): ApiResponse<List<MessageDto>>

    @POST("chat")
    suspend fun sendMessage(@Body request: SendMessageRequest): ApiResponse<MessageDto>

    // =========================
    // ✅ ANALYTICS ROUTES
    // =========================
    @GET("analytics/provider/summary")
    suspend fun getProviderAnalytics(): ApiResponse<ProviderAnalyticsDto>

    @GET("analytics/customer/summary")
    suspend fun getCustomerAnalytics(): ApiResponse<CustomerAnalyticsDto>
}

data class SubmitTicketRequest(
    val jobId: String?,
    val type: String,
    val subject: String,
    val description: String,
    val priority: String = "MEDIUM"
)

data class TicketDto(
    val id: String,
    val type: String,
    val subject: String,
    val status: String,
    val createdAt: String
)

data class PriceEstimateDto(
    val basePrice: Double,
    val hourlyPrice: Double,
    val bookingFee: Double,
    val taxAmount: Double,
    val totalAmount: Double,
    val currency: String,
    val surgeMultiplier: Double,
    val surcharges: List<SurchargeDto>
)

data class SurchargeDto(
    val type: String,
    val amount: Double
)

data class CommissionRateDto(
    val commissionRate: Double
)

data class WorkspaceConfigDto(
    val country: CountryDto,
    val settings: SettingsDto
)

data class CountryDto(
    val name: String,
    val code: String,
    val currency: String,
    val phoneCode: String? = null,
    val flagEmoji: String? = null
)

data class LanguageDto(
    val code: String,
    val name: String
)

data class SettingsDto(
    val matchingRadiusKm: Int,
    val sosAlertRadiusKm: Int,
    val baseBookingFee: Double,
    val referralRewardAmount: Double,
    val bookingFee: Double,
    val platformFee: Double,
    val minimumCharge: Double,
    val taxPercentage: Double,
    val currencyCode: String,
    val nightFeeEnabled: Boolean,
    val weekendFeeEnabled: Boolean
)

data class ServiceDto(
    val id: String,
    val code: String,
    val name: String,
    val category: String,
    val genderRule: String,
    val verificationLevel: String,
    val equipmentRequired: List<String>,
    val isActive: Boolean
)

data class ZoneDto(
    val id: String,
    val name: String,
    val zoneCode: String,
    val cityName: String
)

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

data class HeartbeatRequest(
    val coordinates: List<Double>,
    val hardwareId: String? = null,
    val isMockLocation: Boolean = false
)

data class ProviderStatusRequest(
    val isOnline: Boolean
)

data class AudioUploadRequest(
    val url: String,
    val duration: Int
)

data class PhotoUploadRequest(
    val url: String,
    val coordinates: List<Double>
)

data class SosRequest(
    val coordinates: List<Double>,
    val jobId: String? = null
)

data class SosResponse(
    val success: Boolean,
    val incidentId: String,
    val _id: String
)

data class VerificationStatusDto(
    val currentLevel: String,
    val currentStatus: String,
    val latestRequest: VerificationRequestDto?
)

data class VerificationRequestDto(
    val id: String,
    val type: String,
    val status: String,
    val documents: List<VerificationDocDto>,
    val rejectionReason: String?
)

data class VerificationDocDto(
    val type: String,
    val url: String,
    val status: String,
    val rejectionReason: String?
)

data class SubmitVerificationRequest(
    val type: String,
    val documents: List<VerificationDocDto>,
    val extraData: Map<String, Any>? = null
)
