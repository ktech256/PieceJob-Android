package com.piecejob.core.data.repository

import com.piecejob.core.data.remote.PieceJobApi
import com.piecejob.core.data.remote.dto.*
import com.piecejob.core.data.remote.ApiResponse
import com.piecejob.core.data.remote.ApiError
import okhttp3.MultipartBody
import javax.inject.Inject

class VerificationRepository @Inject constructor(
    private val api: PieceJobApi
) {
    suspend fun uploadDocuments(idDocument: MultipartBody.Part?, license: MultipartBody.Part?): ApiResponse<Unit> {
        return try {
            api.uploadDocuments(idDocument, license)
        } catch (e: Exception) {
            ApiResponse(false, e.message, null, ApiError("500", e.message ?: "Unknown error"))
        }
    }
}
