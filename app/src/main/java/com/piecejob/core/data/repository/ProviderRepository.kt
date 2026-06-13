package com.piecejob.core.data.repository

import com.piecejob.core.data.remote.*
import com.piecejob.core.data.remote.dto.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ProviderRepository @Inject constructor(
    private val api: PieceJobApi
) {
    suspend fun getDashboardStats(): ApiResponse<ProviderStatsDto> {
        return api.getProviderDashboardStats()
    }

    suspend fun getProfile(): ApiResponse<UserDto> {
        return try {
            api.getProfile()
        } catch (e: Exception) {
            ApiResponse(false, e.message, null, null)
        }
    }

    suspend fun getProviderFullProfile(): ApiResponse<ProviderFullDto> {
        return try {
            api.getProviderProfile()
        } catch (e: Exception) {
            ApiResponse(false, e.message, null, null)
        }
    }

    suspend fun updateProfile(request: UpdateProfileRequest): ApiResponse<ProviderFullDto> {
        return try {
            api.updateProviderProfile(request)
        } catch (e: Exception) {
            ApiResponse(false, e.message, null, null)
        }
    }

    suspend fun getMyServices(): ApiResponse<MyServicesResponse> {
        return try {
            api.getMyServices()
        } catch (e: Exception) {
            ApiResponse(false, e.message, null, null)
        }
    }

    suspend fun updateMyServices(codes: List<String>): ApiResponse<UpdateServicesResponse> {
        return try {
            api.updateMyServices(UpdateServicesRequest(codes))
        } catch (e: Exception) {
            ApiResponse(false, e.message, null, null)
        }
    }

    suspend fun getMyEquipment(): ApiResponse<List<EquipmentDto>> {
        return try {
            api.getMyEquipment()
        } catch (e: Exception) {
            ApiResponse(false, e.message, null, null)
        }
    }

    suspend fun addEquipment(equipment: EquipmentDto): ApiResponse<List<EquipmentDto>> {
        return try {
            api.addEquipment(equipment)
        } catch (e: Exception) {
            ApiResponse(false, e.message, null, null)
        }
    }

    suspend fun getMyCertifications(): ApiResponse<List<CertificationDto>> {
        return try {
            api.getMyCertifications()
        } catch (e: Exception) {
            ApiResponse(false, e.message, null, null)
        }
    }

    suspend fun addCertification(cert: CertificationDto): ApiResponse<List<CertificationDto>> {
        return try {
            api.addCertification(cert)
        } catch (e: Exception) {
            ApiResponse(false, e.message, null, null)
        }
    }

    suspend fun getMyExperience(): ApiResponse<List<ExperienceDto>> {
        return try {
            api.getMyExperience()
        } catch (e: Exception) {
            ApiResponse(false, e.message, null, null)
        }
    }

    suspend fun addExperience(exp: ExperienceDto): ApiResponse<List<ExperienceDto>> {
        return try {
            api.addExperience(exp)
        } catch (e: Exception) {
            ApiResponse(false, e.message, null, null)
        }
    }

    suspend fun getBankDetails(): ApiResponse<BankDetailsDto> {
        return try {
            api.getBankDetails()
        } catch (e: Exception) {
            ApiResponse(false, e.message, null, null)
        }
    }

    suspend fun updateBankDetails(request: UpdateBankDetailsRequest): ApiResponse<BankDetailsDto> {
        return try {
            api.updateBankDetails(request)
        } catch (e: Exception) {
            ApiResponse(false, e.message, null, null)
        }
    }

    suspend fun updateWalletSettings(settings: PayoutPreferencesDto): ApiResponse<PayoutPreferencesDto> {
        return try {
            api.updateWalletSettings(settings)
        } catch (e: Exception) {
            ApiResponse(false, e.message, null, null)
        }
    }

    suspend fun updateNotificationSettings(settings: NotificationSettingsDto): ApiResponse<NotificationSettingsDto> {
        return try {
            api.updateNotificationSettings(settings)
        } catch (e: Exception) {
            ApiResponse(false, e.message, null, null)
        }
    }

    suspend fun updateStatus(isOnline: Boolean): ApiResponse<Unit> {
        return try {
            api.updateProviderStatus(com.piecejob.core.data.remote.dto.ProviderStatusRequest(isOnline))
        } catch (e: Exception) {
            ApiResponse(false, e.message, null, null)
        }
    }

    suspend fun sendHeartbeat(lat: Double, lng: Double, hardwareId: String? = null, isMock: Boolean = false): ApiResponse<Unit> {
        return try {
            api.sendHeartbeat(HeartbeatRequest(listOf(lng, lat), hardwareId, isMock))
        } catch (e: Exception) {
            ApiResponse(false, e.message, null, null)
        }
    }

    suspend fun getVerificationStatus(): ApiResponse<VerificationStatusDto> {
        return try {
            api.getVerificationStatus()
        } catch (e: Exception) {
            ApiResponse(false, e.message, null, null)
        }
    }

    suspend fun getVerificationRequirements(): ApiResponse<VerificationRequirementsDto> {
        return try {
            api.getVerificationRequirements()
        } catch (e: Exception) {
            ApiResponse(false, e.message, null, null)
        }
    }

    suspend fun submitVerification(request: SubmitVerificationRequest): ApiResponse<Unit> {
        return try {
            api.submitVerification(request)
        } catch (e: Exception) {
            ApiResponse(false, e.message, null, null)
        }
    }
}
