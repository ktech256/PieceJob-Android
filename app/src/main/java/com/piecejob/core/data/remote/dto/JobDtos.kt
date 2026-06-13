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

data class ProviderStatusRequest(
    val isOnline: Boolean
)
