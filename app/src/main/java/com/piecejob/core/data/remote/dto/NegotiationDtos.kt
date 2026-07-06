package com.piecejob.core.data.remote.dto

import com.google.gson.annotations.SerializedName

data class ProposePriceRequest(
    val jobId: String,
    val amount: Double
)

data class RespondProposalRequest(
    val action: String // 'ACCEPT' or 'REJECT'
)

data class PriceProposalDto(
    @SerializedName("_id", alternate = ["id"]) val id: String,
    val jobId: String,
    val senderId: String,
    val receiverId: String,
    val amount: Double,
    val status: String,
    val round: Int,
    val countryCode: String,
    val createdAt: String
)

data class UploadPhotosRequest(
    val photos: List<String>
)

data class PayServiceFeeRequest(
    val vendor: String,
    val voucherNumber: String
)

data class PayServiceFeeResponse(
    val paymentAmount: Double,
    val serviceFeeBalance: Double,
    val balanceMain: Double,
    val isSuspended: Boolean
)
