package com.piecejob.core.data.repository

import com.piecejob.core.data.remote.PieceJobApi
import com.piecejob.core.data.remote.dto.*
import com.piecejob.core.data.remote.ApiResponse
import com.piecejob.core.data.remote.ApiError
import javax.inject.Inject

class PayoutRepository @Inject constructor(
    private val api: PieceJobApi
) {
    suspend fun getPayouts(): ApiResponse<List<PayoutDto>> {
        return try {
            api.getPayouts()
        } catch (e: Exception) {
            ApiResponse(false, e.message, null, ApiError("500", e.message ?: "Unknown error"))
        }
    }

    suspend fun getStatements(): ApiResponse<List<StatementDto>> {
        return try {
            api.getStatements()
        } catch (e: Exception) {
            ApiResponse(false, e.message, null, ApiError("500", e.message ?: "Unknown error"))
        }
    }
}
