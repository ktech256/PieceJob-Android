package com.piecejob.core.data.repository

import com.piecejob.core.data.remote.*
import com.piecejob.core.data.remote.dto.*
import okhttp3.MultipartBody
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class VerificationRepository @Inject constructor(
    private val api: PieceJobApi
) {
    suspend fun getVerificationStatus(): ApiResponse<VerificationStatusDto> {
        return try {
            api.getVerificationStatus()
        } catch (e: Exception) {
            ApiResponse(false, e.message, null, ApiError("500", e.message ?: "Unknown error"))
        }
    }

    suspend fun submitVerification(request: SubmitVerificationRequest): ApiResponse<Unit> {
        return try {
            api.submitVerification(request)
        } catch (e: Exception) {
            ApiResponse(false, e.message, null, ApiError("500", e.message ?: "Unknown error"))
        }
    }

    suspend fun uploadDocuments(idDocument: MultipartBody.Part?, license: MultipartBody.Part?): ApiResponse<Unit> {
        return try {
            api.uploadDocuments(idDocument, license)
        } catch (e: Exception) {
            ApiResponse(false, e.message, null, ApiError("500", e.message ?: "Unknown error"))
        }
    }
}
