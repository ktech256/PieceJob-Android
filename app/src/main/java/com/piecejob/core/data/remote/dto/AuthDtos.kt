package com.piecejob.core.data.remote.dto

data class OtpRequest(val phoneNumber: String)
data class OtpVerifyRequest(val phoneNumber: String, val otp: String)
data class LoginRequest(val identifier: String, val password: String, val deviceId: String?, val hardwareId: String? = null)
data class LoginResponse(val token: String, val refreshToken: String, val user: UserDto)

data class RefreshRequest(val refreshToken: String)
data class RefreshResponse(val token: String)

data class ChangePasswordRequest(val currentPassword: String, val newPassword: String)

data class FcmTokenRequest(val fcmToken: String)

data class DeviceDto(
    val id: String,
    val name: String,
    val platform: String,
    val lastLogin: String
)

data class ReferralStatsDto(
    val referralCode: String,
    val totalReferrals: Int,
    val pendingRewards: Double,
    val paidRewards: Double,
    val history: List<ReferralUserDto>
)

data class ReferralUserDto(
    val firstName: String,
    val lastName: String,
    val createdAt: String,
    val isVerified: Boolean
)

data class UserDto(
    val id: String,
    val firstName: String,
    val lastName: String,
    val email: String,
    val phoneNumber: String,
    val role: String,
    val gender: String? = null,
    val countryCode: String,
    val tier: String? = null,
    val ratingAvg: Double? = null,
    val isShadowBanned: Boolean? = false,
    val profilePhoto: String? = null,
    val city: String? = null,
    val address: String? = null,
    val dob: String? = null,
    val idOrPassportNumber: String? = null,
    val province: String? = null,
    val emergencyContact: EmergencyContactDto? = null
)

data class WalletDto(
    val balanceMain: Double,
    val balanceEscrow: Double,
    val balanceCredit: Double,
    val balanceReferral: Double,
    val balanceBonus: Double
)

data class WalletTransactionDto(
    val transactionId: String,
    val amount: Double,
    val type: String,
    val status: String,
    val createdAt: String,
    val metadata: Map<String, String>?
)

data class PayoutDto(
    val id: String,
    val totalAmount: Double,
    val currency: String,
    val status: String,
    val createdAt: String
)

data class StatementDto(
    val id: String,
    val periodStart: String,
    val periodEnd: String,
    val summary: StatementSummaryDto,
    val pdfUrl: String
)

data class StatementSummaryDto(
    val grossEarnings: Double,
    val platformCommission: Double,
    val netEarnings: Double,
    val jobCount: Int
)

data class InvoiceDto(
    val id: String,
    val invoiceNumber: String,
    val jobId: String,
    val amount: Double,
    val currency: String,
    val status: String,
    val pdfUrl: String?,
    val createdAt: String
)

data class CompanyDto(
    val id: String,
    val name: String,
    val registrationNumber: String,
    val taxNumber: String?,
    val status: String,
    val contactPerson: String
)

data class CorporateScheduleDto(
    val id: String,
    val serviceCode: String,
    val frequency: String,
    val nextRunDate: String,
    val isActive: Boolean
)
