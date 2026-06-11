package com.piecejob.core.data.repository

import com.piecejob.core.data.remote.PieceJobApi
import com.piecejob.core.data.remote.ApiResponse
import com.piecejob.core.data.remote.ApiError
import com.piecejob.core.data.remote.ZoneDto
import javax.inject.Inject

class ZoneRepository @Inject constructor(
    private val api: PieceJobApi
) {
    suspend fun resolveZone(lat: Double, lng: Double): ApiResponse<ZoneDto> {
        return try {
            api.resolveZone(lat, lng)
        } catch (e: Exception) {
            ApiResponse(false, e.message, null, ApiError("500", e.message ?: "Unknown error"))
        }
    }
}
