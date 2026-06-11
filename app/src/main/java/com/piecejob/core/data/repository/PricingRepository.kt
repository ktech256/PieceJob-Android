package com.piecejob.core.data.repository

import com.piecejob.core.data.remote.PieceJobApi
import com.piecejob.core.data.remote.dto.*
import com.piecejob.core.data.remote.ApiResponse
import com.piecejob.core.data.remote.ApiError
import com.piecejob.core.data.remote.PriceEstimateDto
import javax.inject.Inject

class PricingRepository @Inject constructor(
    private val api: PieceJobApi
) {
    suspend fun getPriceEstimate(
        serviceCode: String,
        zoneId: String?,
        isEmergency: Boolean
    ): ApiResponse<PriceEstimateDto> {
        return try {
            api.getPriceEstimate(serviceCode, zoneId, isEmergency)
        } catch (e: Exception) {
            ApiResponse(false, e.message, null, ApiError("500", e.message ?: "Unknown error"))
        }
    }
}
