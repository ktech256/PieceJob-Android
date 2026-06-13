package com.piecejob.core.data.repository

import com.piecejob.core.data.remote.*
import com.piecejob.core.data.remote.dto.*
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

    suspend fun markArrival(jobId: String): ApiResponse<Unit> {
        return try {
            api.markArrival(jobId)
        } catch (e: Exception) {
            ApiResponse(false, e.message, null, ApiError("500", e.message ?: "Unknown error"))
        }
    }

    suspend fun startJob(jobId: String): ApiResponse<Unit> {
        return try {
            api.startJob(jobId)
        } catch (e: Exception) {
            ApiResponse(false, e.message, null, ApiError("500", e.message ?: "Unknown error"))
        }
    }

    suspend fun completeJob(jobId: String): ApiResponse<Unit> {
        return try {
            api.completeJob(jobId)
        } catch (e: Exception) {
            ApiResponse(false, e.message, null, ApiError("500", e.message ?: "Unknown error"))
        }
    }

    suspend fun resolveZone(lat: Double, lng: Double): ApiResponse<ZoneDto> {
        return try {
            api.resolveZone(lat, lng)
        } catch (e: Exception) {
            ApiResponse(false, e.message, null, ApiError("500", e.message ?: "Unknown error"))
        }
    }

    suspend fun getPriceEstimate(serviceCode: String, zoneId: String?, isEmergency: Boolean): ApiResponse<PriceEstimateDto> {
        return try {
            api.getPriceEstimate(serviceCode, zoneId, isEmergency)
        } catch (e: Exception) {
            ApiResponse(false, e.message, null, ApiError("500", e.message ?: "Unknown error"))
        }
    }

    suspend fun getMyDisputes(): ApiResponse<List<DisputeDto>> {
        return try {
            api.getMyDisputes()
        } catch (e: Exception) {
            ApiResponse(false, e.message, null, ApiError("500", e.message ?: "Unknown error"))
        }
    }

    suspend fun raiseDispute(request: RaiseDisputeRequest): ApiResponse<Unit> {
        return try {
            api.raiseDispute(request)
        } catch (e: Exception) {
            ApiResponse(false, e.message, null, ApiError("500", e.message ?: "Unknown error"))
        }
    }
}
