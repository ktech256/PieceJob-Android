package com.piecejob.core.data.remote.dto

import com.google.gson.annotations.SerializedName

data class CreateJobRequest(
    @SerializedName("serviceCode") val serviceCode: String,
    @SerializedName("coordinates") val coordinates: List<Double>,
    @SerializedName("address") val address: String,
    @SerializedName("description") val description: String? = null,
    @SerializedName("zoneId") val zoneId: String? = null,
    @SerializedName("isEmergency") val isEmergency: Boolean = false,
    @SerializedName("isForSomeoneElse") val isForSomeoneElse: Boolean = false,
    @SerializedName("recipientName") val recipientName: String? = null,
    @SerializedName("recipientPhone") val recipientPhone: String? = null
)

data class JobStatusRequest(
    @SerializedName("status") val status: String,
    @SerializedName("providerCoordinates") val providerCoordinates: List<Double>? = null
)

data class JobDto(
    @SerializedName("id") val id: String,
    @SerializedName("serviceCode") val serviceCode: String,
    @SerializedName("status") val status: String,
    @SerializedName("customerId") val customerId: String,
    @SerializedName("providerId") val providerId: String?,
    @SerializedName("providerInfo") val providerInfo: ProviderInfoDto?,
    @SerializedName("customerInfo") val customerInfo: CustomerInfoDto?,
    @SerializedName("bookingFee") val bookingFee: Double,
    @SerializedName("serviceFee") val serviceFee: Double?,
    @SerializedName("currency") val currency: String,
    @SerializedName("location") val location: LocationDto?,
    @SerializedName("isForSomeoneElse") val isForSomeoneElse: Boolean,
    @SerializedName("recipientName") val recipientName: String?,
    @SerializedName("recipientPhone") val recipientPhone: String?,
    @SerializedName("createdAt") val createdAt: String,
    @SerializedName("paymentReference") val paymentReference: String? = null
)

data class PayBookingFeeResponse(
    @SerializedName("paymentUrl") val paymentUrl: String?,
    @SerializedName("reference") val reference: String?,
    @SerializedName("job") val job: JobDto
)

data class ProviderInfoDto(
    @SerializedName("firstName") val firstName: String,
    @SerializedName("lastName") val lastName: String,
    @SerializedName("ratingAvg") val ratingAvg: Double,
    @SerializedName("jobsCompleted") val jobsCompleted: Int,
    @SerializedName("profilePicture") val profilePicture: String? = null
)

data class CustomerInfoDto(
    @SerializedName("firstName") val firstName: String,
    @SerializedName("lastName") val lastName: String,
    @SerializedName("profilePicture") val profilePicture: String? = null
)

data class LocationDto(
    @SerializedName("coordinates") val coordinates: List<Double>,
    @SerializedName("address") val address: String?
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

data class ReviewDto(
    val id: String,
    val jobId: String,
    val rating: Int,
    val comment: String?,
    val reviewerName: String,
    val reviewerPhoto: String?,
    val createdAt: String
)

data class RatingRequest(
    @SerializedName("rating") val rating: Int,
    @SerializedName("comment") val comment: String? = null
)
