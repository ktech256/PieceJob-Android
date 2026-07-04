package com.piecejob.core.data.remote.dto

data class ProposePriceRequest(
    val jobId: String,
    val amount: Double,
    val note: String? = null
)

data class RespondProposalRequest(
    val action: String // 'ACCEPT' or 'REJECT'
)

data class PriceProposalDto(
    val id: String,
    val jobId: String,
    val senderId: String,
    val receiverId: String,
    val amount: Double,
    val note: String?,
    val status: String,
    val round: Int,
    val countryCode: String,
    val createdAt: String
)

data class UploadPhotosRequest(
    val photos: List<String>
)

data class PayCommissionRequest(
    val vendor: String,
    val voucherNumber: String
)

data class PayCommissionResponse(
    val paymentAmount: Double,
    val outstandingCommission: Double,
    val balanceMain: Double,
    val isSuspended: Boolean
)
