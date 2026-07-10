package com.piecejob.core.data.remote.dto

import com.google.gson.annotations.SerializedName

data class OtpRequest(val phoneNumber: String)
data class OtpVerifyRequest(val phoneNumber: String, val otp: String)
data class LoginRequest(
    val identifier: String,
    val password: String,
    val deviceId: String?,
    val hardwareId: String? = null,
    val fcmToken: String? = null,
    val appType: String? = null
)
data class LoginResponse(val token: String, val refreshToken: String, val user: UserDto)

data class RefreshRequest(val refreshToken: String)
data class RefreshResponse(val token: String, val refreshToken: String)

data class ChangePasswordRequest(val currentPassword: String, val newPassword: String)

data class PhoneChangeRequest(val newPhoneNumber: String)
data class PhoneVerifyRequest(val newPhoneNumber: String, val otp: String)
data class EmailChangeRequest(val newEmail: String)
data class EmailVerifyRequest(val newEmail: String, val code: String)

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
    @SerializedName("_id", alternate = ["id"])
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
    val emergencyContact: EmergencyContactDto? = null,
    val emergencyContacts: List<EmergencyContactDto>? = null,
    val pendingAddress: PendingAddressDto? = null,
    val addresses: List<AddressDto>? = null,
    val savedLocations: List<SavedLocationDto>? = null,
    val paymentMethods: List<UserCardDto>? = null,
    val language: String? = null,
    val country: String? = null,
    val privacySettings: PrivacySettingsDto? = null,
    val subscription: SubscriptionDto? = null
)

data class AddressDto(
    @SerializedName("_id", alternate = ["id"])
    val _id: String? = null,
    val label: String,
    val address: String,
    val coordinates: List<Double>,
    val isDefault: Boolean,
    val usageCount: Int = 1,
    val lastUsedAt: String? = null
)

data class SavedLocationDto(
    @SerializedName("_id", alternate = ["id"])
    val _id: String? = null,
    val name: String,
    val address: String,
    val coordinates: List<Double>,
    val usageCount: Int = 1,
    val lastUsedAt: String? = null
)

data class PrivacySettingsDto(
    val profileVisibility: String,
    val shareLocation: Boolean,
    val dataSharing: Boolean,
    val marketingPreferences: Boolean
)

data class SubscriptionDto(
    val plan: String,
    val status: String,
    val startDate: String,
    val expiryDate: String
)

data class UserCardDto(
    @SerializedName("_id", alternate = ["id"])
    val _id: String? = null,
    val brand: String,
    val last4: String,
    val expMonth: Int,
    val expYear: Int,
    val token: String,
    val isDefault: Boolean
)

data class PendingAddressDto(
    val province: String,
    val city: String,
    val address: String,
    val proofOfResidenceUrl: String,
    val submittedAt: String,
    val status: String
)

data class WalletDto(
    val balanceMain: Double,
    val balanceEscrow: Double,
    val balanceCredit: Double,
    val balanceReferral: Double,
    val balanceBonus: Double,
    val serviceFeeBalance: Double = 0.0,
    val isSuspended: Boolean = false,
    val currency: String,
    val lastServiceFeeDetails: ServiceFeeDetailsDto? = null,
    val recentServiceFees: List<RecentServiceFeeDto>? = null
)

data class RecentServiceFeeDto(
    val jobId: String,
    val date: String,
    val acceptedPrice: Double,
    val serviceFeeAmount: Double,
    val originalFee: Double,
    val outstandingBalance: Double,
    val status: String
)

data class ServiceFeeDetailsDto(
    val serviceFeePercentage: Double,
    val bookingFeePaid: Double,
    val acceptedPrice: Double,
    val serviceFeeAmount: Double,
    val providerKeeps: Double,
    val outstandingBalance: Double
)

data class WalletTransactionDto(
    val transactionId: String,
    val amount: Double,
    val type: String,
    val status: String,
    val description: String?,
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
    val platformServiceFee: Double,
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

data class WithdrawRequest(
    val amount: Double
)

data class NotificationDto(
    val id: String,
    val title: String,
    val body: String,
    val type: String,
    val status: String,
    val createdAt: String,
    val payload: Map<String, String>? = null
)

data class EmergencyContactDto(
    val name: String,
    val phone: String,
    val relationship: String,
    @SerializedName("_id", alternate = ["id"])
    val _id: String? = null
)
