package com.piecejob.core.data.remote.dto

data class ProviderStatsDto(
    val earningsToday: Double,
    val earningsWeekly: Double,
    val earningsMonthly: Double,
    val jobsCompleted: Int,
    val jobsActive: Int,
    val acceptanceRate: Double,
    val completionRate: Double,
    val arrivalRate: Double,
    val tier: String,
    val tierProgress: Double, // 0.0 to 1.0
    val rating: Double,
    val verificationStatus: String,
    val isGhostMode: Boolean = false
)

data class ProviderFullDto(
    val id: String,
    val userId: UserDto,
    val gender: String,
    val dob: String,
    val idOrPassportNumber: String,
    val countryCode: String,
    val verificationStatus: String,
    val tier: String,
    val equipment: List<EquipmentDto>,
    val certifications: List<CertificationDto>,
    val workExperience: List<ExperienceDto>,
    val bankDetails: BankDetailsDto?,
    val payoutPreferences: PayoutPreferencesDto,
    val notificationSettings: NotificationSettingsDto
)

data class NotificationSettingsDto(
    val jobBroadcasts: Boolean,
    val chatMessages: Boolean,
    val walletAlerts: Boolean,
    val payoutAlerts: Boolean,
    val verificationUpdates: Boolean,
    val marketing: Boolean,
    val sosAlerts: Boolean
)

data class UpdateProfileRequest(
    val firstName: String?,
    val lastName: String?,
    val gender: String?,
    val dob: String?,
    val profilePhoto: String?,
    val city: String?,
    val province: String?,
    val address: String?,
    val emergencyContact: EmergencyContactDto?
)

data class EquipmentDto(val name: String, val category: String, val photoUrl: String?, val isVerified: Boolean)
data class CertificationDto(val name: String, val institution: String, val certificateNumber: String, val expiryDate: String?, val status: String)
data class ExperienceDto(val companyName: String, val role: String, val startDate: String, val endDate: String?, val description: String?)
data class BankDetailsDto(
    val bankName: String,
    val accountHolder: String,
    val accountNumberEncrypted: String,
    val branchCode: String,
    val accountType: String?,
    val bankConfirmationUrl: String?,
    val isVerified: Boolean
)

data class PayoutPreferencesDto(val frequency: String, val method: String)
data class EmergencyContactDto(val name: String, val phone: String, val relationship: String)
data class UpdateServicesRequest(val serviceCodes: List<String>)
data class UpdateServicesResponse(val approved: List<String>, val pending: List<String>)

data class UpdateBankDetailsRequest(
    val bankName: String,
    val accountHolder: String,
    val accountNumber: String,
    val branchCode: String,
    val accountType: String,
    val bankConfirmationUrl: String?
)
