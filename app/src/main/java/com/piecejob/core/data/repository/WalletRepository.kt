package com.piecejob.core.data.repository

import com.piecejob.core.data.remote.PieceJobApi
import com.piecejob.core.data.remote.dto.*
import com.piecejob.core.data.remote.ApiResponse
import com.piecejob.core.data.remote.ApiError
import javax.inject.Inject

class WalletRepository @Inject constructor(
    private val api: PieceJobApi
) {
    suspend fun getWalletBalance(): ApiResponse<WalletDto> {
        return try {
            api.getWalletBalance()
        } catch (e: Exception) {
            ApiResponse(false, e.message, null, ApiError("500", e.message ?: "Unknown error"))
        }
    }

    suspend fun getWalletHistory(): ApiResponse<List<WalletTransactionDto>> {
        return try {
            api.getWalletHistory()
        } catch (e: Exception) {
            ApiResponse(false, e.message, null, ApiError("500", e.message ?: "Unknown error"))
        }
    }

    suspend fun getInvoices(): ApiResponse<List<InvoiceDto>> {
        return try {
            api.getInvoices()
        } catch (e: Exception) {
            ApiResponse(false, e.message, null, ApiError("500", e.message ?: "Unknown error"))
        }
    }
}
