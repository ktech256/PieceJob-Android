package com.piecejob.core.data.remote

import okhttp3.Interceptor
import okhttp3.Response
import java.io.IOException
import java.util.Collections
import javax.inject.Inject
import javax.inject.Singleton

/**
 * PRODUCTION HARDENING: Global Request Guard
 * 
 * This interceptor prevents duplicate concurrent requests for identical write operations (POST, PUT, DELETE).
 * It works by tracking active request keys (Method + Path) and blocking subsequent identical requests
 * while one is already in progress.
 * 
 * Use Case: User rapid taps a button that is not yet disabled, or Activity recreation triggers
 * a duplicate request.
 */
@Singleton
class GlobalRequestGuardInterceptor @Inject constructor() : Interceptor {

    private val activeRequests = Collections.synchronizedSet(mutableSetOf<String>())

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        
        // GET requests are always allowed to run concurrently (idempotent by nature)
        if (request.method == "GET") {
            return chain.proceed(request)
        }

        // Generate a unique key for this request based on method and path
        // We exclude query parameters to be strict about the endpoint action
        val requestKey = "${request.method}:${request.url.newBuilder().query(null).build()}"

        if (!activeRequests.add(requestKey)) {
            android.util.Log.w("REQUEST_GUARD", "BLOCKED: Duplicate request in progress: $requestKey")
            // Throwing IOException prevents the request from reaching the network
            // and triggers the standard error handling in the ViewModel/Repository.
            throw IOException("CONCURRENCY_GUARD: A request for this action is already in progress.")
        }

        return try {
            android.util.Log.d("REQUEST_GUARD", "START: $requestKey")
            chain.proceed(request)
        } catch (e: Exception) {
            android.util.Log.e("REQUEST_GUARD", "FAILED: $requestKey | Error: ${e.message}")
            throw e
        } finally {
            android.util.Log.d("REQUEST_GUARD", "FINISH: $requestKey")
            activeRequests.remove(requestKey)
        }
    }
}
