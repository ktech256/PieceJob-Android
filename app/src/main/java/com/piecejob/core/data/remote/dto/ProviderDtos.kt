package com.piecejob.core.data.remote.dto

data class ProviderStatsDto(
    val earningsToday: Double,
    val earningsWeekly: Double,
    val earningsMonthly: Double,
    val jobsCompleted: Int,
    val jobsActive: Int,
    val acceptanceRate: Double,
    val completionRate: Double,
    val arrivalRate: Double,
    val tier: String,
    val tierProgress: Double, // 0.0 to 1.0
    val rating: Double,
    val verificationStatus: String,
    val isGhostMode: Boolean = false
)
