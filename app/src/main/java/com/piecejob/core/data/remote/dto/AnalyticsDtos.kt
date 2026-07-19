package com.piecejob.core.data.remote.dto

data class ProviderAnalyticsDto(
    val dailyEarnings: List<ChartPointDto>,
    val weeklyEarnings: List<ChartPointDto>,
    val monthlyEarnings: List<ChartPointDto>,
    val lifetimeEarnings: Double,
    val totalJobsAccepted: Int,
    val totalJobsCompleted: Int,
    val totalJobsCancelled: Int,
    val acceptanceRate: Double,
    val completionRate: Double,
    val arrivalRate: Double,
    val reliabilityScore: Double,
    val cancellationScore: Double,
    val healthScore: Double,
    val healthStatus: String,
    val averageArrivalTime: String,
    val averageJobDuration: String,
    val currentRank: Int,
    val cityRank: Int,
    val provinceRank: Int,
    val badges: List<String>,
    val mostRequestedService: String,
    val activeSince: String,
    val lastActive: String,
    val ratingTrend: List<ChartPointDto>,
    val reliabilityTrend: List<ChartPointDto>,
    val tierProgression: Int // 0-100%
)

data class CustomerAnalyticsDto(
    val totalBookings: Int,
    val totalSpending: Double,
    val topCategories: List<CategorySpendDto>,
    val spendingHistory: List<ChartPointDto>
)

data class ChartPointDto(
    val label: String,
    val value: Double
)

data class CategorySpendDto(
    val categoryName: String,
    val amount: Double,
    val count: Int
)
