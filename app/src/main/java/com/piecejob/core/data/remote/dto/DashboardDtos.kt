package com.piecejob.core.data.remote.dto

import com.google.gson.annotations.SerializedName
import com.piecejob.core.data.remote.ServiceDto

data class CustomerDashboardDto(
    val profile: DashboardProfileDto,
    val wallet: WalletDto,
    val activeJob: JobDto?,
    val activeJobs: List<JobDto>? = emptyList(),
    val promotions: List<PromotionDto>,
    val referralCampaign: ReferralCampaignDto?,
    val latestActivity: List<ActivityDto>,
    val topRatedNearby: List<TopProviderDto>,
    val recommendations: List<ServiceDto>
)

data class ReferralCampaignDto(
    @SerializedName("_id", alternate = ["id"]) val id: String,
    val title: String,
    val description: String,
    val rewardAmount: Double,
    val currency: String,
    val bannerUrl: String?
)

data class DashboardProfileDto(
    val firstName: String,
    val lastName: String,
    val email: String,
    val photo: String?,
    val addresses: List<AddressDto>?,
    val savedLocations: List<SavedLocationDto>?
)

data class PromotionDto(
    @SerializedName("_id", alternate = ["id"]) val id: String,
    val title: String,
    val description: String,
    val imageUrl: String?,
    val ctaText: String,
    val deepLink: String?
)

data class ActivityDto(
    @SerializedName("_id", alternate = ["id"]) val id: String,
    val type: String,
    val status: String,
    val title: String? = null,
    val serviceCode: String? = null,
    val serviceName: String? = null,
    val address: String? = null,
    val amount: Double?,
    val isNegotiated: Boolean? = true,
    val startedAt: String? = null,
    val completedAt: String? = null,
    val cancelledAt: String? = null,
    val cancelledBy: String? = null,
    val createdAt: String
)

data class ProviderDashboardDto(
    val profile: DashboardProfileDto,
    val stats: ProviderStatsDto,
    val activeJob: JobDto?,
    val recentActivity: List<ActivityDto>,
    val currency: String
)

data class TopProviderDto(
    @SerializedName("_id", alternate = ["id"]) val id: String,
    val name: String,
    val photo: String?,
    val rating: Double,
    val tier: String,
    val services: List<String>,
    val distance: Double? = null
)

data class GlobalSearchDto(
    val services: List<ServiceDto>,
    val categories: List<com.piecejob.core.data.remote.ServiceCategoryDto>,
    val providers: List<TopProviderDto>
)
