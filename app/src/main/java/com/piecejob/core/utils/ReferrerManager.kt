package com.piecejob.core.utils

import android.content.Context
import com.android.installreferrer.api.InstallReferrerClient
import com.android.installreferrer.api.InstallReferrerStateListener
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ReferrerManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    fun startTracking(onCodeRecovered: (String) -> Unit) {
        val referrerClient = InstallReferrerClient.newBuilder(context).build()
        referrerClient.startConnection(object : InstallReferrerStateListener {
            override fun onInstallReferrerSetupFinished(responseCode: Int) {
                when (responseCode) {
                    InstallReferrerClient.InstallReferrerResponse.OK -> {
                        try {
                            val response = referrerClient.installReferrer
                            val referrerUrl = response.installReferrer
                            android.util.Log.d("REFERRAL_AUDIT", "INSTALL_REFERRER_URL: $referrerUrl")
                            
                            // Parse utm_campaign which contains our code from the website
                            if (referrerUrl.contains("utm_campaign=")) {
                                val code = referrerUrl.substringAfter("utm_campaign=").substringBefore("&")
                                if (code.isNotEmpty()) {
                                    android.util.Log.d("REFERRAL_AUDIT", "RECOVERED_CODE_FROM_INSTALL: $code")
                                    onCodeRecovered(code)
                                }
                            }
                            referrerClient.endConnection()
                        } catch (e: Exception) {
                            android.util.Log.e("REFERRAL_AUDIT", "REFERRER_ERROR", e)
                        }
                    }
                    else -> {
                        android.util.Log.w("REFERRAL_AUDIT", "INSTALL_REFERRER_FAILED_CODE: $responseCode")
                    }
                }
            }

            override fun onInstallReferrerServiceDisconnected() {
                // Try again later?
            }
        })
    }
}
