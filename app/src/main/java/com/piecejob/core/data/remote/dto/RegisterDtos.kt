package com.piecejob.core.data.remote.dto

data class CustomerRegisterRequest(
    val firstName: String,
    val lastName: String,
    val email: String,
    val phoneNumber: String,
    val password: String,
    val countryCode: String,
    val dob: String,
    val idNumber: String,
    val gender: String,
    val deviceId: String? = null,
    val fcmToken: String? = null
)

data class ProviderRegisterRequest(
    val firstName: String,
    val lastName: String,
    val email: String,
    val phoneNumber: String,
    val password: String,
    val countryCode: String,
    val gender: String,
    val dob: String,
    val nationalityType: String,
    val idOrPassportNumber: String,
    val servicesOffered: List<String>,
    val deviceId: String? = null,
    val fcmToken: String? = null
)
