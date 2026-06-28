package com.piecejob.core.data.repository

import com.piecejob.core.data.remote.PieceJobApi
import com.piecejob.core.data.remote.ApiResponse
import com.piecejob.core.data.remote.dto.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CallRepository @Inject constructor(
    private val api: PieceJobApi
) {
    suspend fun logCallInitiation(jobId: String, receiverId: String): ApiResponse<CallInitiationResponse> {
        return try {
            api.logCallInitiation(LogCallRequest(jobId, receiverId))
        } catch (e: Exception) {
            ApiResponse(success = false, message = e.message ?: "Unknown error", data = null, error = null)
        }
    }

    suspend fun updateCallStatus(callId: String, status: String, duration: Int? = null): ApiResponse<Unit> {
        return try {
            api.updateCallStatus(callId, UpdateCallStatusRequest(status, duration))
        } catch (e: Exception) {
            ApiResponse(success = false, message = e.message ?: "Unknown error", data = null, error = null)
        }
    }
}
