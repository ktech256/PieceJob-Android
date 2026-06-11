package com.piecejob.core.data.repository

import com.piecejob.core.data.remote.PieceJobApi
import com.piecejob.core.data.remote.dto.*
import com.piecejob.core.data.remote.ApiResponse
import com.piecejob.core.data.remote.ApiError
import com.piecejob.core.data.remote.ServiceDto
import javax.inject.Inject

class ServiceRepository @Inject constructor(
    private val api: PieceJobApi
) {
    suspend fun getServices(): ApiResponse<List<ServiceDto>> {
        return try {
            api.getServices()
        } catch (e: Exception) {
            ApiResponse(false, e.message, null, ApiError("500", e.message ?: "Unknown error"))
        }
    }
}
