package com.piecejob.core.data.repository

import com.piecejob.core.data.remote.PieceJobApi
import com.piecejob.core.data.remote.dto.*
import com.piecejob.core.data.remote.ApiResponse
import com.piecejob.core.data.remote.ApiError
import javax.inject.Inject

class SosRepository @Inject constructor(
    private val api: PieceJobApi
) {
    suspend fun triggerSos(jobId: String?, coordinates: List<Double>): ApiResponse<SosResponse> {
        return try {
            api.triggerSos(SosRequest(jobId, coordinates))
        } catch (e: Exception) {
            ApiResponse(false, e.message, null, ApiError("500", e.message ?: "Unknown error"))
        }
    }
}
