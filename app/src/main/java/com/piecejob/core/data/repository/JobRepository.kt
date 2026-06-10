package com.piecejob.core.data.repository

import com.piecejob.core.data.remote.PieceJobApi
import com.piecejob.core.data.remote.dto.*
import com.piecejob.core.data.remote.ApiResponse
import com.piecejob.core.data.remote.ApiError
import javax.inject.Inject

class JobRepository @Inject constructor(
    private val api: PieceJobApi
) {
    suspend fun createJob(request: CreateJobRequest): ApiResponse<JobDto> {
        return try {
            api.createJob(request)
        } catch (e: Exception) {
            ApiResponse(false, e.message, null, ApiError("500", e.message ?: "Unknown error"))
        }
    }

    suspend fun getJobById(jobId: String): ApiResponse<JobDto> {
        return try {
            api.getJobById(jobId)
        } catch (e: Exception) {
            ApiResponse(false, e.message, null, ApiError("500", e.message ?: "Unknown error"))
        }
    }

    suspend fun cancelJob(jobId: String): ApiResponse<Unit> {
        return try {
            api.cancelJob(jobId)
        } catch (e: Exception) {
            ApiResponse(false, e.message, null, ApiError("500", e.message ?: "Unknown error"))
        }
    }

    suspend fun getAvailableJobs(): ApiResponse<List<JobDto>> {
        return try {
            api.getAvailableJobs()
        } catch (e: Exception) {
            ApiResponse(false, e.message, null, ApiError("500", e.message ?: "Unknown error"))
        }
    }

    suspend fun acceptJob(jobId: String): ApiResponse<JobDto> {
        return try {
            api.acceptJob(jobId)
        } catch (e: Exception) {
            ApiResponse(false, e.message, null, ApiError("500", e.message ?: "Unknown error"))
        }
    }
}
