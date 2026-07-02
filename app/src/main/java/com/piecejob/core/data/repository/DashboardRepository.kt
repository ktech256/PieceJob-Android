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

    suspend fun getProviderDashboard(): ApiResponse<ProviderDashboardDto> {
        return try {
            api.getProviderDashboard()
        } catch (e: Exception) {
            ApiResponse(false, e.message, null, ApiError("500", e.message ?: "Unknown error"))
        }
    }

    suspend fun getCustomerPromotions(): ApiResponse<List<PromotionDto>> {
        return try {
            api.getCustomerPromotions()
        } catch (e: Exception) {
            ApiResponse(false, e.message, null, ApiError("500", e.message ?: "Unknown error"))
        }
    }

    suspend fun globalSearch(query: String): ApiResponse<GlobalSearchDto> {
        return try {
            api.globalSearch(query)
        } catch (e: Exception) {
            ApiResponse(false, e.message, null, ApiError("500", e.message ?: "Unknown error"))
        }
    }
}
