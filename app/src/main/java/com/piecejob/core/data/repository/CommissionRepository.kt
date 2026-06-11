package com.piecejob.core.data.repository

import com.piecejob.core.data.remote.PieceJobApi
import com.piecejob.core.data.remote.dto.*
import com.piecejob.core.data.remote.ApiResponse
import com.piecejob.core.data.remote.ApiError
import com.piecejob.core.data.remote.CommissionRateDto
import javax.inject.Inject

class CommissionRepository @Inject constructor(
    private val api: PieceJobApi
) {
    suspend fun getCommissionRate(): ApiResponse<CommissionRateDto> {
        return try {
            api.getCommissionRate()
        } catch (e: Exception) {
            ApiResponse(false, e.message, null, ApiError("500", e.message ?: "Unknown error"))
        }
    }
}
