package com.piecejob.core.data.repository

import com.piecejob.core.data.remote.PieceJobApi
import com.piecejob.core.data.remote.ApiResponse
import com.piecejob.core.data.remote.dto.*
import com.piecejob.core.data.remote.HeartbeatRequest
import com.piecejob.core.data.remote.ProviderStatusRequest
import com.piecejob.core.data.remote.VerificationStatusDto
import com.piecejob.core.data.remote.SubmitVerificationRequest
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

    suspend fun updateStatus(isOnline: Boolean): ApiResponse<Unit> {
        return try {
            api.updateProviderStatus(ProviderStatusRequest(isOnline))
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

    suspend fun submitVerification(request: SubmitVerificationRequest): ApiResponse<Unit> {
        return try {
            api.submitVerification(request)
        } catch (e: Exception) {
            ApiResponse(false, e.message, null, null)
        }
    }
}
