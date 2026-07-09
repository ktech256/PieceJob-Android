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
    @SerializedName("_id", alternate = ["id"]) val id: String,
    @SerializedName("serviceCode") val serviceCode: String?,
    @SerializedName("serviceName") val serviceName: String?,
    @SerializedName("status") val status: String,
    @SerializedName("customerId") val customerId: String?,
    @SerializedName("providerId") val providerId: String?,
    @SerializedName("providerInfo") val providerInfo: ProviderInfoDto?,
    @SerializedName("customerInfo") val customerInfo: CustomerInfoDto?,
    @SerializedName("bookingFee") val bookingFee: Double?,
    @SerializedName("serviceFee") val serviceFee: Double?,
    @SerializedName("currency") val currency: String?,
    @SerializedName("location") val location: LocationDto?,
    @SerializedName("taskPhotosRequested") val taskPhotosRequested: Boolean?,
    @SerializedName("taskPhotosSeen") val taskPhotosSeen: Boolean?,
    @SerializedName("taskPhotos") val taskPhotos: List<String>?,
    @SerializedName("agreedPrice") val agreedPrice: Double?,
    @SerializedName("priceStatus") val priceStatus: String?,
    @SerializedName("negotiationRounds") val negotiationRounds: Int?,
    @SerializedName("currentNegotiationPhase") val currentNegotiationPhase: String?,
    @SerializedName("activeProposal") val activeProposal: PriceProposalDto?,
    @SerializedName("photoSharingRequired") val photoSharingRequired: Boolean?,
    @SerializedName("priceNegotiationRequired") val priceNegotiationRequired: Boolean?,
    @SerializedName("isForSomeoneElse") val isForSomeoneElse: Boolean?,
    @SerializedName("recipientName") val recipientName: String?,
    @SerializedName("recipientPhone") val recipientPhone: String?,
    @SerializedName("customerRated") val customerRated: Boolean?,
    @SerializedName("providerRated") val providerRated: Boolean?,
    @SerializedName("customerRatingDismissed") val customerRatingDismissed: Boolean?,
    @SerializedName("providerRatingDismissed") val providerRatingDismissed: Boolean?,
    @SerializedName("createdAt") val createdAt: String?,
    @SerializedName("paymentReference") val paymentReference: String? = null,
    @SerializedName("cancellationReason") val cancellationReason: String? = null,
    @SerializedName("cancelledBy") val cancelledBy: String? = null,
    @SerializedName("cancelledByName") val cancelledByName: String? = null
)

data class PayBookingFeeResponse(
    @SerializedName("paymentUrl") val paymentUrl: String?,
    @SerializedName("reference") val reference: String?,
    @SerializedName("job") val job: JobDto
)

data class ProviderInfoDto(
    @SerializedName("firstName") val firstName: String,
    @SerializedName("lastName") val lastName: String,
    @SerializedName("phoneNumber") val phoneNumber: String? = null,
    @SerializedName("ratingAvg") val ratingAvg: Double? = null,
    @SerializedName("jobsCompleted") val jobsCompleted: Int? = null,
    @SerializedName("profilePicture") val profilePicture: String? = null
)

data class CustomerInfoDto(
    @SerializedName("firstName") val firstName: String,
    @SerializedName("lastName") val lastName: String,
    @SerializedName("phoneNumber") val phoneNumber: String? = null,
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

// --- CALLS ---
data class LogCallRequest(
    val jobId: String,
    val receiverId: String
)

data class UpdateCallStatusRequest(
    val status: String,
    val duration: Int? = null
)

data class CallInitiationResponse(
    val callId: String
)

data class CallDto(
    val id: String,
    val jobId: String,
    val callerId: String,
    val receiverId: String,
    val status: String,
    val startTime: String,
    val endTime: String?,
    val duration: Int?
)

data class LiveKitTokenDto(
    val token: String
)
