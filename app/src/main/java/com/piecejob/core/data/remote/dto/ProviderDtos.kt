package com.piecejob.core.data.remote.dto

import com.piecejob.core.data.remote.ServiceDto

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
    val verificationLevel: String,
    val tier: String,
    val isOnline: Boolean,
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
    val firstName: String? = null,
    val lastName: String? = null,
    val gender: String? = null,
    val dob: String? = null,
    val profilePhoto: String? = null,
    val city: String? = null,
    val province: String? = null,
    val address: String? = null,
    val emergencyContact: EmergencyContactDto? = null,
    val proofOfResidenceUrl: String? = null
)

data class EquipmentDto(val name: String, val category: String, val photoUrl: String?, val isVerified: Boolean)
data class CertificationDto(val name: String, val institution: String, val certificateNumber: String, val expiryDate: String?, val status: String)
data class ExperienceDto(val companyName: String, val role: String, val startDate: String, val endDate: String?, val description: String?)
data class BankDetailsDto(
    val bankName: String?,
    val accountHolder: String?,
    val accountNumberEncrypted: String?,
    val branchCode: String?,
    val accountType: String?,
    val bankConfirmationUrl: String?,
    val isVerified: Boolean
)

data class PayoutPreferencesDto(val frequency: String, val method: String)
data class EmergencyContactDto(val name: String, val phone: String, val relationship: String)
data class UpdateServicesRequest(val serviceCodes: List<String>)
data class UpdateServicesResponse(
    val approved: List<String>,
    val pending: List<String>,
    val requirements: Map<String, ServiceRequirementDto>? = null
)

data class MyServicesResponse(
    val approved: List<ServiceDto>,
    val pending: List<ServiceDto>
)

data class ServiceRequirementDto(val level: String, val docs: List<String>)

data class UpdateBankDetailsRequest(
    val bankName: String,
    val accountHolder: String,
    val accountNumber: String,
    val branchCode: String,
    val accountType: String,
    val bankConfirmationUrl: String?
)

data class ProviderStatusRequest(
    val isOnline: Boolean,
    val coordinates: List<Double>? = null
)

data class VerificationRequirementsDto(
    val currentLevel: String,
    val targetLevel: String,
    val requirements: List<DocRequirementDto>
)

data class DocRequirementDto(
    val type: String,
    val isRequired: Boolean,
    val allowedTypes: List<String>,
    val label: String,
    val group: String,
    val status: String,
    val rejectionReason: String? = null
)

data class AvailabilityDto(
    val vacationMode: Boolean,
    val workingHours: List<WorkingDayDto>
)

data class WorkingDayDto(
    val day: Int,
    val enabled: Boolean,
    val slots: List<TimeSlotDto>
)

data class TimeSlotDto(
    val start: String,
    val end: String
)

data class ProviderDto(
    val id: String,
    val userId: String,
    val firstName: String?,
    val lastName: String?,
    val ratingAvg: Double,
    val jobsCompleted: Int,
    val location: LocationDto,
    val isOnline: Boolean
)
