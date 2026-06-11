package com.piecejob.core.data.repository

import com.piecejob.core.data.remote.PieceJobApi
import com.piecejob.core.data.remote.dto.*
import com.piecejob.core.data.remote.ApiResponse
import com.piecejob.core.data.remote.ApiError
import com.piecejob.core.data.remote.WorkspaceConfigDto
import javax.inject.Inject

class SettingsRepository @Inject constructor(
    private val api: PieceJobApi
) {
    suspend fun getWorkspaceConfig(): ApiResponse<WorkspaceConfigDto> {
        return try {
            api.getWorkspaceConfig()
        } catch (e: Exception) {
            ApiResponse(false, e.message, null, ApiError("500", e.message ?: "Unknown error"))
        }
    }
}
