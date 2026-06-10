package com.piecejob.core.data.remote.dto

data class SosRequest(
    val jobId: String?,
    val coordinates: List<Double>
)

data class SosResponse(
    val alertId: String
)
