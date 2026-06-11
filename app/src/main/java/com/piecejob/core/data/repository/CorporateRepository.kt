package com.piecejob.core.data.repository

import com.piecejob.core.data.remote.PieceJobApi
import com.piecejob.core.data.remote.dto.*
import com.piecejob.core.data.remote.ApiResponse
import com.piecejob.core.data.remote.ApiError
import javax.inject.Inject

class CorporateRepository @Inject constructor(
    private val api: PieceJobApi
) {
    suspend fun getCompanyProfile(): ApiResponse<CompanyDto> {
        return try {
            api.getCompanyProfile()
        } catch (e: Exception) {
            ApiResponse(false, e.message, null, ApiError("500", e.message ?: "Unknown error"))
        }
    }

    suspend fun getEmployees(): ApiResponse<List<UserDto>> {
        return try {
            api.getEmployees()
        } catch (e: Exception) {
            ApiResponse(false, e.message, null, ApiError("500", e.message ?: "Unknown error"))
        }
    }

    suspend fun getSchedules(): ApiResponse<List<CorporateScheduleDto>> {
        return try {
            api.getSchedules()
        } catch (e: Exception) {
            ApiResponse(false, e.message, null, ApiError("500", e.message ?: "Unknown error"))
        }
    }
}
