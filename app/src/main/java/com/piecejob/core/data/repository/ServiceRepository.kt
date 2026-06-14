package com.piecejob.core.data.repository

import com.piecejob.core.data.remote.PieceJobApi
import com.piecejob.core.data.remote.ServicesResponseDto
import com.piecejob.core.data.remote.dto.*
import com.piecejob.core.data.remote.ApiResponse
import com.piecejob.core.data.remote.ApiError
import com.piecejob.core.data.remote.ServiceDto
import com.piecejob.core.data.local.SessionManager
import javax.inject.Inject

class ServiceRepository @Inject constructor(
    private val api: PieceJobApi,
    private val sessionManager: SessionManager
) : BaseRepository() {
    suspend fun getServices(explicitGender: String? = null): ApiResponse<ServicesResponseDto> {
        return try {
            val gender = explicitGender ?: sessionManager.getGender()
            api.getServices(gender)
        } catch (e: Exception) {
            handleError(e)
        }
    }

    fun hasStoredGender(): Boolean {
        return sessionManager.getGender() != null
    }
}
