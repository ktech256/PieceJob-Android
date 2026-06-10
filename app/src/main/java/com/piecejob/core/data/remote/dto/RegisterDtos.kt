package com.piecejob.core.data.remote.dto

data class CustomerRegisterRequest(
    val firstName: String,
    val lastName: String,
    val email: String,
    val phoneNumber: String,
    val password: String,
    val countryCode: String
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
    val servicesOffered: List<String>
)
