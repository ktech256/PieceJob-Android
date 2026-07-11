package com.piecejob.core.data.local

import android.content.Context
import android.content.SharedPreferences
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SessionManager @Inject constructor(
    @ApplicationContext context: Context
) {
    private val prefs: SharedPreferences = context.getSharedPreferences("piecejob_prefs", Context.MODE_PRIVATE)

    fun saveAuthToken(token: String) {
        prefs.edit().putString("auth_token", token).apply()
    }

    fun getAuthToken(): String? {
        return prefs.getString("auth_token", null)
    }

    fun saveRefreshToken(token: String) {
        prefs.edit().putString("refresh_token", token).apply()
    }

    fun getRefreshToken(): String? {
        return prefs.getString("refresh_token", null)
    }

    fun saveDeviceId(deviceId: String) {
        prefs.edit().putString("device_id", deviceId).apply()
    }

    fun getDeviceId(): String? {
        var id = prefs.getString("device_id", null)
        if (id == null) {
            id = java.util.UUID.randomUUID().toString()
            saveDeviceId(id)
        }
        return id
    }

    fun saveCountryCode(countryCode: String) {
        prefs.edit().putString("country_code", countryCode).apply()
    }

    fun getCountryCode(): String? {
        return prefs.getString("country_code", null)
    }

    // Dynamic Workspace Config
    fun saveCurrencySymbol(symbol: String) {
        prefs.edit().putString("currency_symbol", symbol).apply()
    }

    fun getCurrencySymbol(): String {
        return prefs.getString("currency_symbol", "") ?: ""
    }

    fun saveTimezone(timezone: String) {
        prefs.edit().putString("timezone", timezone).apply()
    }

    fun getTimezone(): String {
        return prefs.getString("timezone", "Africa/Johannesburg") ?: "Africa/Johannesburg"
    }

    fun saveLocale(locale: String) {
        prefs.edit().putString("locale", locale).apply()
    }

    fun getLocale(): String {
        return prefs.getString("locale", "en-ZA") ?: "en-ZA"
    }

    fun saveEscrowEnabled(enabled: Boolean) {
        prefs.edit().putBoolean("escrow_enabled", enabled).apply()
    }

    fun isEscrowEnabled(): Boolean {
        return prefs.getBoolean("escrow_enabled", false)
    }

    fun saveReferralEnabled(enabled: Boolean) {
        prefs.edit().putBoolean("referral_enabled", enabled).apply()
    }

    fun isReferralEnabled(): Boolean {
        return prefs.getBoolean("referral_enabled", true)
    }

    fun saveReferralSettings(
        rewardAmount: Double,
        currencyCode: String?,
        rewardType: String?,
        minJobs: Int,
        maxRewards: Int,
        delayDays: Int,
        expiryDays: Int,
        baseUrl: String?,
        qrBranding: String?,
        fraudPhone: Boolean,
        fraudEmail: Boolean,
        fraudHardware: Boolean,
        fraudCircular: Boolean
    ) {
        prefs.edit().apply {
            putFloat("referral_reward_amount", rewardAmount.toFloat())
            putString("referral_currency_code", currencyCode)
            putString("referral_reward_type", rewardType)
            putInt("referral_min_jobs", minJobs)
            putInt("referral_max_rewards", maxRewards)
            putInt("referral_delay_days", delayDays)
            putInt("referral_expiry_days", expiryDays)
            putString("referral_base_url", baseUrl)
            putString("qr_branding_type", qrBranding)
            putBoolean("referral_fraud_phone", fraudPhone)
            putBoolean("referral_fraud_email", fraudEmail)
            putBoolean("referral_fraud_hardware", fraudHardware)
            putBoolean("referral_fraud_circular", fraudCircular)
        }.apply()
    }

    fun getReferralBaseUrl(): String {
        val fallback = "https://${com.piecejob.core.utils.Constants.PRODUCTION_DOMAIN}${com.piecejob.core.utils.Constants.REFERRAL_PATH}"
        return prefs.getString("referral_base_url", fallback) ?: fallback
    }

    fun getQrBrandingType(): String {
        return prefs.getString("qr_branding_type", "NONE") ?: "NONE"
    }

    fun getReferralRewardAmount(): Double = prefs.getFloat("referral_reward_amount", 0f).toDouble()
    fun getReferralCurrencyCode(): String = prefs.getString("referral_currency_code", "USD") ?: "USD"

    fun saveLastPhoneNumber(phone: String) {
        prefs.edit().putString("last_phone", phone).apply()
    }

    fun getLastPhoneNumber(): String? = prefs.getString("last_phone", null)

    fun saveUser(userId: String, role: String, firstName: String, gender: String? = null) {
        prefs.edit().apply {
            putString("user_id", userId)
            putString("role", role)
            putString("first_name", firstName)
            putString("gender", gender)
            putBoolean("is_provider", role.equals("provider", ignoreCase = true))
        }.apply()
    }

    fun getUserId(): String? = prefs.getString("user_id", null)
    fun getRole(): String? = prefs.getString("role", null)
    fun getFirstName(): String? = prefs.getString("first_name", null)
    fun getGender(): String? = prefs.getString("gender", null)
    fun isProvider(): Boolean = prefs.getBoolean("is_provider", false)

    fun saveStagedDoc(type: String, path: String) {
        prefs.edit().putString("staged_doc_$type", path).apply()
    }

    fun getStagedDoc(type: String): String? {
        return prefs.getString("staged_doc_$type", null)
    }

    fun removeStagedDoc(type: String) {
        prefs.edit().remove("staged_doc_$type").apply()
    }

    fun clearStagedDocs() {
        val editor = prefs.edit()
        prefs.all.keys.filter { it.startsWith("staged_doc_") }.forEach {
            editor.remove(it)
        }
        editor.apply()
    }

    fun clearSession() {
        prefs.edit().remove("auth_token").apply()
        clearStagedDocs()
    }
}
