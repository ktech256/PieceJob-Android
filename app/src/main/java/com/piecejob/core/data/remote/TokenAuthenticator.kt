package com.piecejob.core.data.remote

import com.piecejob.core.data.local.SessionManager
import com.piecejob.core.data.remote.dto.RefreshRequest
import okhttp3.Authenticator
import okhttp3.Request
import okhttp3.Response
import okhttp3.Route
import javax.inject.Inject
import javax.inject.Provider

class TokenAuthenticator @Inject constructor(
    private val sessionManager: SessionManager,
    private val apiProvider: Provider<PieceJobApi> // Use Provider to avoid circular dependency
) : Authenticator {

    override fun authenticate(route: Route?, response: Response): Request? {
        // 1. Only attempt refresh once
        if (response.countPriorResponse() >= 2) {
            return null
        }

        val refreshToken = sessionManager.getRefreshToken() ?: return null

        synchronized(this) {
            // 2. Check if token was already refreshed by another thread
            val currentToken = sessionManager.getAuthToken()
            val requestToken = response.request.header("Authorization")?.replace("Bearer ", "")
            
            if (currentToken != requestToken) {
                // Token already updated, retry original request with new token
                return response.request.newBuilder()
                    .header("Authorization", "Bearer $currentToken")
                    .build()
            }

            // 3. Perform synchronous refresh
            return try {
                val api = apiProvider.get()
                val refreshResponse = kotlinx.coroutines.runBlocking {
                    api.refreshToken(RefreshRequest(refreshToken))
                }

                if (refreshResponse.success && refreshResponse.data != null) {
                    val newToken = refreshResponse.data!!.token
                    val newRefresh = refreshResponse.data!!.refreshToken
                    
                    sessionManager.saveAuthToken(newToken)
                    sessionManager.saveRefreshToken(newRefresh)

                    response.request.newBuilder()
                        .header("Authorization", "Bearer $newToken")
                        .build()
                } else {
                    // Refresh failed, clear session and logout
                    sessionManager.clearSession()
                    null
                }
            } catch (e: Exception) {
                null
            }
        }
    }

    private fun Response.countPriorResponse(): Int {
        var result = 1
        var prior = priorResponse
        while (prior != null) {
            result++
            prior = prior.priorResponse
        }
        return result
    }
}
