package com.piecejob.core.data.repository

import com.piecejob.core.data.remote.PieceJobApi
import com.piecejob.core.data.remote.dto.*
import com.piecejob.core.data.remote.ApiResponse
import com.piecejob.core.data.remote.ApiError
import com.piecejob.core.data.remote.ServiceDto
import com.piecejob.core.data.local.SessionManager
import javax.inject.Inject

class ServiceRepository @Inject constructor(
    private val api: PieceJobApi,
    private val sessionManager: SessionManager
) {
    suspend fun getServices(explicitGender: String? = null): ApiResponse<List<ServiceDto>> {
        return try {
            val gender = explicitGender ?: sessionManager.getGender()
            api.getServices(gender)
        } catch (e: Exception) {
            ApiResponse(false, e.message, null, ApiError("500", e.message ?: "Unknown error"))
        }
    }

    fun hasStoredGender(): Boolean {
        return sessionManager.getGender() != null
    }
}
