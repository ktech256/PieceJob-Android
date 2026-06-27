package com.piecejob.core.data.repository

import com.piecejob.core.data.remote.PieceJobApi
import com.piecejob.core.data.remote.dto.*
import com.piecejob.core.data.remote.ApiResponse
import com.piecejob.core.data.remote.ApiError
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UserRepository @Inject constructor(
    private val api: PieceJobApi
) {
    suspend fun updateFcmToken(fcmToken: String): ApiResponse<Unit> {
        return try {
            android.util.Log.d("FCM_AUDIT", "FCM_UPLOAD_START: Token=${fcmToken.take(15)}...")
            val response = api.updateFcmToken(FcmTokenRequest(fcmToken))
            if (response.success) {
                android.util.Log.d("FCM_AUDIT", "FCM_UPLOAD_SUCCESS")
            } else {
                android.util.Log.e("FCM_AUDIT", "FCM_UPLOAD_FAILED: ${response.message}")
            }
            response
        } catch (e: Exception) {
            android.util.Log.e("FCM_AUDIT", "FCM_UPLOAD_CRASH: ${e.message}")
            ApiResponse(false, e.message, null, ApiError("500", e.message ?: "Unknown error"))
        }
    }

    suspend fun getProfile(): ApiResponse<UserDto> {
        return try {
            api.getProfile()
        } catch (e: Exception) {
            ApiResponse(false, e.message, null, ApiError("500", e.message ?: "Unknown error"))
        }
    }

    suspend fun getReferralStats(): ApiResponse<ReferralStatsDto> {
        return try {
            api.getReferralStats()
        } catch (e: Exception) {
            ApiResponse(false, e.message, null, ApiError("500", e.message ?: "Unknown error"))
        }
    }
}
