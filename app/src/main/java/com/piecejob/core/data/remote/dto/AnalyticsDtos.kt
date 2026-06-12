package com.piecejob.core.data.remote.dto

data class ProviderAnalyticsDto(
    val dailyEarnings: List<ChartPointDto>,
    val weeklyEarnings: List<ChartPointDto>,
    val monthlyEarnings: List<ChartPointDto>,
    val totalJobsCompleted: Int,
    val acceptanceRate: Float,
    val completionRate: Float,
    val ratingTrend: List<ChartPointDto>,
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
