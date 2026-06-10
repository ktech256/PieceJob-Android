package com.piecejob.core.data.repository

import com.piecejob.core.data.remote.PieceJobApi
import com.piecejob.core.data.remote.dto.*
import com.piecejob.core.data.remote.ApiResponse
import com.piecejob.core.data.remote.ApiError
import javax.inject.Inject

class NotificationRepository @Inject constructor(
    private val api: PieceJobApi
) {
    // In V3.1 this will handle FCM token registration and notification listing
    suspend fun registerFcmToken(token: String): ApiResponse<Unit> {
        // Placeholder for future FCM registration endpoint
        return ApiResponse(true, "Token registered", null, null)
    }
}
