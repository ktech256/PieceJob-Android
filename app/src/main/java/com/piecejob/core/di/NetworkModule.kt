package com.piecejob.core.di

import com.piecejob.core.data.remote.PieceJobApi
import com.piecejob.core.data.remote.GoogleMapsApi
import com.piecejob.core.data.remote.AuthInterceptor
import com.piecejob.core.data.local.SessionManager
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import javax.inject.Singleton
import java.net.InetAddress
import okhttp3.Dns
import okhttp3.logging.HttpLoggingInterceptor
import java.util.concurrent.TimeUnit

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    // RENDER PUBLIC ENDPOINT
    private const val BASE_URL = "https://piecejob-backend.onrender.com/api/v1/"

    @Provides
    @Singleton
    fun provideAuthInterceptor(sessionManager: SessionManager): AuthInterceptor {
        return AuthInterceptor(sessionManager)
    }

    @Provides
    @Singleton
    fun provideTokenAuthenticator(
        sessionManager: SessionManager,
        apiProvider: javax.inject.Provider<PieceJobApi>
    ): com.piecejob.core.data.remote.TokenAuthenticator {
        return com.piecejob.core.data.remote.TokenAuthenticator(sessionManager, apiProvider)
    }

    @Provides
    @Singleton
    fun provideOkHttpClient(
        authInterceptor: AuthInterceptor,
        tokenAuthenticator: com.piecejob.core.data.remote.TokenAuthenticator
    ): OkHttpClient {
        val logging = okhttp3.logging.HttpLoggingInterceptor().apply {
            level = okhttp3.logging.HttpLoggingInterceptor.Level.BODY
        }
        return OkHttpClient.Builder()
            .addInterceptor(authInterceptor)
            .authenticator(tokenAuthenticator)
            .addInterceptor(logging)
            .addInterceptor { chain ->
                val request = chain.request()
                android.util.Log.d("FCM_AUDIT", "NETWORK_REQUEST: ${request.method} ${request.url}")
                try {
                    val response = chain.proceed(request)
                    android.util.Log.d("FCM_AUDIT", "NETWORK_RESPONSE: ${response.code}")
                    response
                } catch (e: Exception) {
                    android.util.Log.e("FCM_AUDIT", "NETWORK_FAILED: ${e.message}")
                    throw e
                }
            }
            .dns(object : okhttp3.Dns {
                override fun lookup(hostname: String): List<java.net.InetAddress> {
                    return try {
                        val addresses = okhttp3.Dns.SYSTEM.lookup(hostname)
                        android.util.Log.d("FCM_AUDIT", "DNS_LOOKUP: $hostname -> $addresses")
                        addresses
                    } catch (e: Exception) {
                        android.util.Log.e("FCM_AUDIT", "DNS_FAILED: $hostname. Error: ${e.message}")
                        throw e
                    }
                }
            })
            .connectTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
            .readTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
            .build()
    }

    @Provides
    @Singleton
    fun providePieceJobApi(okHttpClient: OkHttpClient): PieceJobApi {
        return Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(PieceJobApi::class.java)
    }

    @Provides
    @Singleton
    fun provideGoogleMapsApi(): GoogleMapsApi {
        return Retrofit.Builder()
            .baseUrl("https://maps.googleapis.com/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(GoogleMapsApi::class.java)
    }
}
