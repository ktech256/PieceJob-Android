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
        return prefs.getString("country_code", "ZA")
    }

    // Dynamic Workspace Config
    fun saveCurrencySymbol(symbol: String) {
        prefs.edit().putString("currency_symbol", symbol).apply()
    }

    fun getCurrencySymbol(): String {
        return prefs.getString("currency_symbol", "R") ?: "R"
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
