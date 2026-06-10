package com.piecejob.core.data.remote.dto

data class OtpRequest(val phoneNumber: String)
data class OtpVerifyRequest(val phoneNumber: String, val otp: String)
data class LoginRequest(val identifier: String, val password: String, val deviceId: String?)
data class LoginResponse(val token: String, val refreshToken: String, val user: UserDto)

data class RefreshRequest(val refreshToken: String)
data class RefreshResponse(val token: String)

data class UserDto(
    val id: String,
    val firstName: String,
    val lastName: String,
    val role: String,
    val countryCode: String
)

data class WalletDto(
    val balanceMain: Double,
    val balanceEscrow: Double,
    val balanceCredit: Double,
    val balanceReferral: Double
)
