package com.piecejob.core.data.remote

import com.piecejob.core.data.local.SessionManager
import okhttp3.Interceptor
import okhttp3.Response
import javax.inject.Inject

class AuthInterceptor @Inject constructor(
    private val sessionManager: SessionManager
) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val originalRequest = chain.request()
        val requestBuilder = originalRequest.newBuilder()

        // SECTION 15.3: Security Hardening - JWT & Device Binding
        val token = sessionManager.getAuthToken()
        if (token != null) {
            requestBuilder.addHeader("Authorization", "Bearer $token")
        }

        val deviceId = sessionManager.getDeviceId()
        if (deviceId != null) {
            requestBuilder.addHeader("x-device-id", deviceId)
        }

        val countryCode = sessionManager.getCountryCode()
        if (countryCode != null) {
            requestBuilder.addHeader("x-country-code", countryCode)
        }

        val response = chain.proceed(requestBuilder.build())

        // SECTION 11.1: Authentication Hardening - Token Expiry Handling
        if (response.code == 401) {
            val refreshToken = sessionManager.getRefreshToken()
            if (refreshToken != null) {
                // In production: Perform sync refresh call here
                // If success: sessionManager.saveAuthToken(newToken), then retry request
                // If fail: sessionManager.clearSession()
            } else {
                sessionManager.clearSession()
            }
        }

        return response
    }
}
