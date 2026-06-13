package com.piecejob.core.data.repository

import com.piecejob.core.data.remote.*
import com.piecejob.core.data.remote.dto.*
import okhttp3.MultipartBody
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class VerificationRepository @Inject constructor(
    private val api: PieceJobApi
) : BaseRepository() {

    suspend fun getVerificationStatus(): ApiResponse<VerificationStatusDto> {
        return try {
            api.getVerificationStatus()
        } catch (e: Exception) {
            handleError(e)
        }
    }

    suspend fun getVerificationRequirements(): ApiResponse<com.piecejob.core.data.remote.dto.VerificationRequirementsDto> {
        return try {
            api.getVerificationRequirements()
        } catch (e: Exception) {
            handleError(e)
        }
    }

    suspend fun submitVerification(request: SubmitVerificationRequest): ApiResponse<Unit> {
        return try {
            api.submitVerification(request)
        } catch (e: Exception) {
            handleError(e)
        }
    }

    suspend fun uploadDocuments(idDocument: MultipartBody.Part?, license: MultipartBody.Part?): ApiResponse<Unit> {
        return try {
            api.uploadDocuments(idDocument, license)
        } catch (e: Exception) {
            handleError(e)
        }
    }
}
