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

        // 1. Inject JWT Bearer Token
        sessionManager.getAuthToken()?.let { token ->
            requestBuilder.addHeader("Authorization", "Bearer $token")
        }

        // 2. Inject Device Binding Header
        sessionManager.getDeviceId()?.let { deviceId ->
            requestBuilder.addHeader("x-device-id", deviceId)
        }

        // 3. Inject Tenant/Country Code
        sessionManager.getCountryCode()?.let { countryCode ->
            requestBuilder.addHeader("x-country-code", countryCode)
        }

        return chain.proceed(requestBuilder.build())
    }
}
