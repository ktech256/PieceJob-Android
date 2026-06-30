package com.piecejob.core.data.repository

import com.piecejob.core.data.remote.PieceJobApi
import com.piecejob.core.data.remote.dto.*
import com.piecejob.core.data.remote.ApiResponse
import com.piecejob.core.data.remote.ApiError
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DashboardRepository @Inject constructor(
    private val api: PieceJobApi
) {
    suspend fun getCustomerDashboard(lat: Double? = null, lng: Double? = null): ApiResponse<CustomerDashboardDto> {
        return try {
            api.getCustomerDashboard(lat, lng)
        } catch (e: Exception) {
            ApiResponse(false, e.message, null, ApiError("500", e.message ?: "Unknown error"))
        }
    }
}
