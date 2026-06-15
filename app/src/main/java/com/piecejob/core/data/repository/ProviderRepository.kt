package com.piecejob.core.data.repository

import com.piecejob.core.data.remote.*
import com.piecejob.core.data.remote.dto.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ProviderRepository @Inject constructor(
    private val api: PieceJobApi
) : BaseRepository() {

    suspend fun getDashboardStats(): ApiResponse<ProviderStatsDto> {
        return try {
            api.getProviderDashboardStats()
        } catch (e: Exception) {
            handleError(e)
        }
    }

    suspend fun getProfile(): ApiResponse<UserDto> {
        return try {
            api.getProfile()
        } catch (e: Exception) {
            handleError(e)
        }
    }

    suspend fun getProviderFullProfile(): ApiResponse<ProviderFullDto> {
        return try {
            api.getProviderProfile()
        } catch (e: Exception) {
            handleError(e)
        }
    }

    suspend fun updateProfile(request: UpdateProfileRequest): ApiResponse<ProviderFullDto> {
        return try {
            api.updateProviderProfile(request)
        } catch (e: Exception) {
            handleError(e)
        }
    }

    suspend fun getMyServices(): ApiResponse<MyServicesResponse> {
        return try {
            api.getMyServices()
        } catch (e: Exception) {
            handleError(e)
        }
    }

    suspend fun updateMyServices(codes: List<String>): ApiResponse<UpdateServicesResponse> {
        return try {
            api.updateMyServices(UpdateServicesRequest(codes))
        } catch (e: Exception) {
            handleError(e)
        }
    }

    suspend fun getMyEquipment(): ApiResponse<List<EquipmentDto>> {
        return try {
            api.getMyEquipment()
        } catch (e: Exception) {
            handleError(e)
        }
    }

    suspend fun addEquipment(equipment: EquipmentDto): ApiResponse<List<EquipmentDto>> {
        return try {
            api.addEquipment(equipment)
        } catch (e: Exception) {
            handleError(e)
        }
    }

    suspend fun getMyCertifications(): ApiResponse<List<CertificationDto>> {
        return try {
            api.getMyCertifications()
        } catch (e: Exception) {
            handleError(e)
        }
    }

    suspend fun addCertification(cert: CertificationDto): ApiResponse<List<CertificationDto>> {
        return try {
            api.addCertification(cert)
        } catch (e: Exception) {
            handleError(e)
        }
    }

    suspend fun getMyExperience(): ApiResponse<List<ExperienceDto>> {
        return try {
            api.getMyExperience()
        } catch (e: Exception) {
            handleError(e)
        }
    }

    suspend fun addExperience(exp: ExperienceDto): ApiResponse<List<ExperienceDto>> {
        return try {
            api.addExperience(exp)
        } catch (e: Exception) {
            handleError(e)
        }
    }

    suspend fun getBankDetails(): ApiResponse<BankDetailsDto> {
        return try {
            api.getBankDetails()
        } catch (e: Exception) {
            handleError(e)
        }
    }

    suspend fun updateBankDetails(request: UpdateBankDetailsRequest): ApiResponse<BankDetailsDto> {
        return try {
            api.updateBankDetails(request)
        } catch (e: Exception) {
            handleError(e)
        }
    }

    suspend fun updateWalletSettings(settings: PayoutPreferencesDto): ApiResponse<PayoutPreferencesDto> {
        return try {
            api.updateWalletSettings(settings)
        } catch (e: Exception) {
            handleError(e)
        }
    }

    suspend fun updateNotificationSettings(settings: NotificationSettingsDto): ApiResponse<NotificationSettingsDto> {
        return try {
            api.updateNotificationSettings(settings)
        } catch (e: Exception) {
            handleError(e)
        }
    }

    suspend fun getAvailability(): ApiResponse<AvailabilityDto> {
        return try {
            api.getAvailability()
        } catch (e: Exception) {
            handleError(e)
        }
    }

    suspend fun updateAvailability(availability: AvailabilityDto): ApiResponse<AvailabilityDto> {
        return try {
            api.updateAvailability(availability)
        } catch (e: Exception) {
            handleError(e)
        }
    }

    suspend fun getMyReviews(): ApiResponse<List<ReviewDto>> {
        return try {
            api.getMyReviews()
        } catch (e: Exception) {
            handleError(e)
        }
    }

    suspend fun updateStatus(isOnline: Boolean): ApiResponse<Unit> {
        return try {
            api.updateProviderStatus(com.piecejob.core.data.remote.dto.ProviderStatusRequest(isOnline))
        } catch (e: Exception) {
            handleError(e)
        }
    }

    suspend fun sendHeartbeat(lat: Double, lng: Double, hardwareId: String? = null, isMock: Boolean = false): ApiResponse<Unit> {
        return try {
            api.sendHeartbeat(HeartbeatRequest(listOf(lng, lat), hardwareId, isMock))
        } catch (e: Exception) {
            handleError(e)
        }
    }

    suspend fun getVerificationStatus(): ApiResponse<VerificationStatusDto> {
        return try {
            api.getVerificationStatus()
        } catch (e: Exception) {
            handleError(e)
        }
    }

    suspend fun getVerificationRequirements(): ApiResponse<com.piecejob.core.data.remote.dto.VerificationRequirementsDto> {
        return try {
            api.getVerificationRequirements()
        } catch (e: Exception) {
            handleError(e)
        }
    }

    suspend fun submitVerification(request: SubmitVerificationRequest): ApiResponse<Unit> {
        return try {
            api.submitVerification(request)
        } catch (e: Exception) {
            handleError(e)
        }
    }

    suspend fun uploadFile(base64: String, mimeType: String, folder: String): ApiResponse<FileUploadResponse> {
        return try {
            api.uploadFile(FileUploadRequest(base64, mimeType, folder))
        } catch (e: Exception) {
            handleError(e)
        }
    }

    suspend fun getMyDisputes(): ApiResponse<List<DisputeDto>> {
        return try {
            api.getMyDisputes()
        } catch (e: Exception) {
            handleError(e)
        }
    }

    suspend fun raiseDispute(request: RaiseDisputeRequest): ApiResponse<Unit> {
        return try {
            api.raiseDispute(request)
        } catch (e: Exception) {
            handleError(e)
        }
    }
}
