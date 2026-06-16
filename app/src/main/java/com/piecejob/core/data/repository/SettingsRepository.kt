package com.piecejob.core.data.repository

import com.piecejob.core.data.remote.*
import com.piecejob.core.data.remote.dto.*
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

    suspend fun getPaymentMethods(): ApiResponse<List<PaymentMethodDto>> {
        return try {
            api.getPaymentMethods()
        } catch (e: Exception) {
            ApiResponse(false, e.message, null, ApiError("500", e.message ?: "Unknown error"))
        }
    }

    suspend fun getIntegrations(): ApiResponse<IntegrationsConfigDto> {
        return try {
            api.getIntegrations()
        } catch (e: Exception) {
            ApiResponse(false, e.message, null, ApiError("500", e.message ?: "Unknown error"))
        }
    }
}
