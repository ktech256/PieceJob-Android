package com.piecejob.core.data.repository

import com.piecejob.core.data.remote.PieceJobApi
import com.piecejob.core.data.local.SessionManager
import com.piecejob.core.data.remote.ApiResponse
import com.piecejob.core.data.remote.WorkspaceConfigDto
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ConfigRepository @Inject constructor(
    private val api: PieceJobApi,
    private val sessionManager: SessionManager
) : BaseRepository() {

    suspend fun refreshWorkspaceConfig(): ApiResponse<WorkspaceConfigDto> {
        return try {
            val response = api.getWorkspaceConfig()
            if (response.success && response.data != null) {
                val config = response.data
                config.country.code.let { sessionManager.saveCountryCode(it) }
                config.settings.currencySymbol?.let { sessionManager.saveCurrencySymbol(it) }
                config.country.timezone?.let { sessionManager.saveTimezone(it) }
                config.country.locale?.let { sessionManager.saveLocale(it) }
            }
            response
        } catch (e: Exception) {
            handleError(e)
        }
    }

    fun getCurrencySymbol(): String = sessionManager.getCurrencySymbol()
    fun getTimezone(): String = sessionManager.getTimezone()
    fun getLocale(): String = sessionManager.getLocale()
}
