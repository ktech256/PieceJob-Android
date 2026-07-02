package com.piecejob.core.data.remote

import com.google.gson.annotations.SerializedName
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

    @POST("auth/request-phone-change")
    suspend fun requestPhoneChange(@Body request: PhoneChangeRequest): ApiResponse<Unit>

    @POST("auth/verify-phone-change")
    suspend fun verifyPhoneChange(@Body request: PhoneVerifyRequest): ApiResponse<Unit>

    @POST("auth/request-email-change")
    suspend fun requestEmailChange(@Body request: EmailChangeRequest): ApiResponse<Unit>

    @POST("auth/verify-email-change")
    suspend fun verifyEmailChange(@Body request: EmailVerifyRequest): ApiResponse<Unit>

    @GET("auth/devices")
    suspend fun getAuthorizedDevices(): ApiResponse<List<DeviceDto>>

    @DELETE("auth/devices/{id}")
    suspend fun removeDevice(@Path("id") deviceId: String): ApiResponse<Unit>

    @GET("users/profile")
    suspend fun getProfile(): ApiResponse<UserDto>

    @PUT("users/profile")
    suspend fun updateProfile(@Body request: UpdateProfileRequest): ApiResponse<UserDto>

    @PATCH("users/fcm-token")
    suspend fun updateFcmToken(@Body request: FcmTokenRequest): ApiResponse<Unit>

    @GET("users/referrals")
    suspend fun getReferralStats(): ApiResponse<ReferralStatsDto>

    @GET("dashboard/customer")
    suspend fun getCustomerDashboard(
        @Query("lat") lat: Double? = null,
        @Query("lng") lng: Double? = null
    ): ApiResponse<CustomerDashboardDto>

    @GET("dashboard/customer/promotions")
    suspend fun getCustomerPromotions(): ApiResponse<List<PromotionDto>>

    @GET("users/addresses")
    suspend fun getAddresses(): ApiResponse<List<AddressDto>>

    @POST("users/addresses")
    suspend fun addAddress(@Body request: AddressDto): ApiResponse<List<AddressDto>>

    @PUT("users/addresses/{addressId}")
    suspend fun updateAddress(@Path("addressId") addressId: String, @Body request: AddressDto): ApiResponse<List<AddressDto>>

    @DELETE("users/addresses/{addressId}")
    suspend fun deleteAddress(@Path("addressId") addressId: String): ApiResponse<List<AddressDto>>

    @GET("users/saved-locations")
    suspend fun getSavedLocations(): ApiResponse<List<SavedLocationDto>>

    @POST("users/saved-locations")
    suspend fun addSavedLocation(@Body request: SavedLocationDto): ApiResponse<List<SavedLocationDto>>

    @PUT("users/saved-locations/{locationId}")
    suspend fun updateSavedLocation(@Path("locationId") locationId: String, @Body request: SavedLocationDto): ApiResponse<List<SavedLocationDto>>

    @DELETE("users/saved-locations/{locationId}")
    suspend fun deleteSavedLocation(@Path("locationId") locationId: String): ApiResponse<List<SavedLocationDto>>

    @GET("users/payment-methods")
    suspend fun getUserPaymentMethods(): ApiResponse<List<UserCardDto>>

    @POST("users/payment-methods")
    suspend fun addUserPaymentMethod(@Body request: UserCardDto): ApiResponse<List<UserCardDto>>

    @DELETE("users/payment-methods/{cardId}")
    suspend fun deleteUserPaymentMethod(@Path("cardId") cardId: String): ApiResponse<List<UserCardDto>>

    @GET("users/emergency-contacts")
    suspend fun getEmergencyContacts(): ApiResponse<List<EmergencyContactDto>>

    @POST("users/emergency-contacts")
    suspend fun addEmergencyContact(@Body request: EmergencyContactDto): ApiResponse<List<EmergencyContactDto>>

    @PUT("users/emergency-contacts/{contactId}")
    suspend fun updateEmergencyContact(@Path("contactId") contactId: String, @Body request: EmergencyContactDto): ApiResponse<List<EmergencyContactDto>>

    @DELETE("users/emergency-contacts/{contactId}")
    suspend fun deleteEmergencyContact(@Path("contactId") contactId: String): ApiResponse<List<EmergencyContactDto>>

    @PATCH("users/preferences")
    suspend fun updatePreferences(@Body request: Map<String, String?>): ApiResponse<UserDto>

    @PATCH("users/privacy")
    suspend fun updatePrivacy(@Body request: PrivacySettingsDto): ApiResponse<PrivacySettingsDto>

    @GET("users/subscription")
    suspend fun getSubscription(): ApiResponse<SubscriptionDto>

    @POST("users/subscription/upgrade")
    suspend fun upgradeSubscription(@Body request: Map<String, String>): ApiResponse<SubscriptionDto>

    @POST("users/subscription/cancel")
    suspend fun cancelSubscription(): ApiResponse<SubscriptionDto>

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

    @POST("wallets/withdraw")
    suspend fun requestWithdrawal(@Body request: WithdrawRequest): ApiResponse<Unit>

    @GET("providers/dashboard")
    suspend fun getProviderDashboard(): ApiResponse<ProviderDashboardDto>

    @GET("providers/profile")
    suspend fun getProviderProfile(): ApiResponse<ProviderFullDto>

    @PATCH("providers/profile")
    suspend fun updateProviderProfile(@Body request: UpdateProfileRequest): ApiResponse<ProviderFullDto>

    @GET("providers/services")
    suspend fun getMyServices(): ApiResponse<MyServicesResponse>

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

    @GET("providers/availability")
    suspend fun getAvailability(): ApiResponse<AvailabilityDto>

    @PATCH("providers/availability")
    suspend fun updateAvailability(@Body request: AvailabilityDto): ApiResponse<AvailabilityDto>

    @GET("providers/reviews")
    suspend fun getMyReviews(): ApiResponse<List<ReviewDto>>

    @GET("providers/dashboard-stats")
    suspend fun getProviderDashboardStats(): ApiResponse<ProviderStatsDto>

    // =========================
    // ✅ JOB & MATCHING ROUTES
    // =========================
    @POST("jobs/request")
    suspend fun createJob(@Body request: CreateJobRequest): ApiResponse<JobDto>

    @GET("jobs/active")
    suspend fun getActiveJob(): ApiResponse<JobDto>

    @GET("jobs/{jobId}")
    suspend fun getJobById(@Path("jobId") jobId: String): ApiResponse<JobDto>

    @POST("jobs/{jobId}/pay-booking-fee")
    suspend fun payBookingFee(@Path("jobId") jobId: String): ApiResponse<PayBookingFeeResponse>

    @POST("jobs/{jobId}/cancel")
    suspend fun cancelJob(@Path("jobId") jobId: String): ApiResponse<Unit>

    @GET("providers/jobs/broadcasted")
    suspend fun getAvailableJobs(): ApiResponse<List<JobDto>>

    @GET("providers/online")
    suspend fun getOnlineProviders(
        @Query("lat") lat: Double? = null,
        @Query("lng") lng: Double? = null
    ): ApiResponse<List<ProviderDto>>

    @PUT("jobs/{jobId}/accept")
    suspend fun acceptJob(@Path("jobId") jobId: String): ApiResponse<JobDto>

    @PATCH("jobs/{jobId}/status")
    suspend fun updateJobStatus(@Path("jobId") jobId: String, @Body request: JobStatusRequest): ApiResponse<JobDto>

    @POST("jobs/{jobId}/rate")
    suspend fun rateJob(@Path("jobId") jobId: String, @Body request: RatingRequest): ApiResponse<Unit>

    @GET("payments/verify/{reference}")
    suspend fun verifyPayment(@Path("reference") reference: String): ApiResponse<JobDto>

    @POST("sos/trigger")
    suspend fun triggerSos(@Body request: SosRequest): ApiResponse<SosResponse>

    @POST("sos/{id}/audio")
    suspend fun uploadSosAudio(@Path("id") id: String, @Body request: AudioUploadRequest): ApiResponse<Unit>

    @POST("sos/{id}/photo")
    suspend fun uploadSosPhoto(@Path("id") id: String, @Body request: PhotoUploadRequest): ApiResponse<Unit>

    @PATCH("providers/me/status")
    suspend fun updateProviderStatus(@Body request: com.piecejob.core.data.remote.dto.ProviderStatusRequest): ApiResponse<Unit>

    @POST("providers/heartbeat")
    suspend fun sendHeartbeat(@Body request: HeartbeatRequest): ApiResponse<Unit>

    @GET("providers/verification/status")
    suspend fun getVerificationStatus(): ApiResponse<VerificationStatusDto>

    @GET("providers/verification/requirements")
    suspend fun getVerificationRequirements(): ApiResponse<com.piecejob.core.data.remote.dto.VerificationRequirementsDto>

    @POST("providers/verification/submit")
    suspend fun submitVerification(@Body request: SubmitVerificationRequest): ApiResponse<Unit>

    @POST("providers/upload-file")
    suspend fun uploadFile(@Body request: FileUploadRequest): ApiResponse<FileUploadResponse>

    @Multipart
    @PATCH("providers/me/documents")
    suspend fun uploadDocuments(
        @Part idDocument: MultipartBody.Part?,
        @Part license: MultipartBody.Part?
    ): ApiResponse<Unit>

    @GET("config/workspace")
    suspend fun getWorkspaceConfig(): ApiResponse<WorkspaceConfigDto>

    @GET("config/categories")
    suspend fun getPublicCategories(): ApiResponse<List<ServiceCategoryDto>>

    @GET("config/services")
    suspend fun getServices(
        @Query("gender") gender: String? = null,
        @Query("lat") lat: Double? = null,
        @Query("lng") lng: Double? = null
    ): ApiResponse<ServicesResponseDto>

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

    @GET("config/search")
    suspend fun globalSearch(
        @Query("q") query: String
    ): ApiResponse<GlobalSearchDto>

    @GET("config/integrations")
    suspend fun getIntegrations(): ApiResponse<IntegrationsConfigDto>

    @GET("config/payment-methods")
    suspend fun getPaymentMethods(): ApiResponse<List<PaymentMethodDto>>

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

    @GET("support/tickets/{ticketId}")
    suspend fun getTicketDetails(@Path("ticketId") ticketId: String): ApiResponse<TicketDto>

    @POST("support/tickets/{ticketId}/messages")
    suspend fun sendTicketMessage(@Path("ticketId") ticketId: String, @Body request: SendTicketMessageRequest): ApiResponse<TicketDto>

    @GET("disputes/me")
    suspend fun getMyDisputes(): ApiResponse<List<DisputeDto>>

    @POST("disputes")
    suspend fun raiseDispute(@Body request: RaiseDisputeRequest): ApiResponse<Unit>

    @GET("notifications")
    suspend fun getMyNotifications(): ApiResponse<List<NotificationDto>>

    @PATCH("notifications/{id}/read")
    suspend fun markNotificationAsRead(@Path("id") id: String): ApiResponse<Unit>

    // =========================
    // ✅ CHAT ROUTES
    // =========================
    @GET("chat/{jobId}")
    suspend fun getChatMessages(@Path("jobId") jobId: String): ApiResponse<List<MessageDto>>

    @POST("chat")
    suspend fun sendMessage(@Body request: SendMessageRequest): ApiResponse<MessageDto>

    // --- CALLS ---
    @POST("calls/init")
    suspend fun logCallInitiation(@Body request: LogCallRequest): ApiResponse<CallInitiationResponse>

    @PATCH("calls/{callId}/status")
    suspend fun updateCallStatus(@Path("callId") callId: String, @Body request: UpdateCallStatusRequest): ApiResponse<Unit>

    @GET("livekit/token")
    suspend fun getLiveKitToken(@Query("jobId") jobId: String): ApiResponse<LiveKitTokenDto>

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
    val createdAt: String,
    val description: String? = null,
    val messages: List<TicketMessageDto> = emptyList()
)

data class TicketMessageDto(
    val senderId: String,
    val senderRole: String,
    val text: String,
    val timestamp: String,
    val attachments: List<String> = emptyList()
)

data class SendTicketMessageRequest(
    val text: String,
    val attachments: List<String> = emptyList()
)

data class PriceEstimateDto(
    val basePrice: Double,
    val hourlyPrice: Double,
    val bookingFee: Double,
    val taxAmount: Double,
    val totalAmount: Double,
    val currency: String,
    val currencySymbol: String? = null,
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
    val flagEmoji: String? = null,
    val timezone: String? = null,
    val language: String? = null,
    val locale: String? = null
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
    val currencySymbol: String? = null,
    val nightFeeEnabled: Boolean,
    val weekendFeeEnabled: Boolean
)

data class ServiceDto(
    @SerializedName("_id", alternate = ["id"])
    val id: String,
    val code: String,
    val name: String,
    val category: String,
    val genderRule: String,
    val verificationLevel: String,
    val equipmentRequired: List<String>,
    val isActive: Boolean,
    val description: String? = null,
    val onlineCountLabel: String? = null,
    val onlineCount: Int? = 0
)

data class GroupedServicesDto(
    val label: String,
    val requirements: String,
    val services: List<ServiceDto>
)

data class ServicesResponseDto(
    val services: List<ServiceDto>,
    val grouped: List<GroupedServicesDto>
)

data class ServiceCategoryDto(
    val code: String,
    val name: String,
    val description: String?,
    val verificationLevel: String,
    val isActive: Boolean,
    val sortOrder: Int
)

data class PaymentMethodDto(
    val code: String,
    val name: String,
    val publicKey: String?,
    val environment: String
)

data class IntegrationsConfigDto(
    val googleMaps: GoogleMapsConfigDto?
)

data class GoogleMapsConfigDto(
    val apiKey: String?,
    val placesKey: String?
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

data class FileUploadRequest(
    val base64: String,
    val mimeType: String,
    val folder: String
)

data class FileUploadResponse(
    val url: String
)
