package com.piecejob.core.data.remote.dto

data class CreateJobRequest(
    val serviceCode: String,
    val coordinates: List<Double>,
    val address: String,
    val description: String? = null
)

data class JobDto(
    val id: String,
    val serviceCode: String,
    val status: String,
    val customerId: String,
    val providerId: String?,
    val bookingFee: Double,
    val serviceFee: Double?,
    val currency: String,
    val location: LocationDto?,
    val createdAt: String
)

data class LocationDto(
    val coordinates: List<Double>,
    val address: String?
)

data class DisputeDto(
    val id: String,
    val jobId: String,
    val reason: String,
    val description: String,
    val status: String,
    val evidenceUrls: List<String>,
    val resolution: String?,
    val adminNotes: String?,
    val createdAt: String
)

data class RaiseDisputeRequest(
    val jobId: String,
    val reason: String,
    val description: String,
    val evidenceUrls: List<String> = emptyList()
)

data class ProviderStatusRequest(
    val isOnline: Boolean
)
