package com.piecejob.core.data.repository

import com.google.gson.Gson
import com.piecejob.core.data.remote.ApiError
import com.piecejob.core.data.remote.ApiResponse
import retrofit2.HttpException

open class BaseRepository {
    protected val gson = Gson()

    protected fun <T> handleError(e: Exception): ApiResponse<T> {
        return if (e is HttpException) {
            try {
                val errorBody = e.response()?.errorBody()?.string()
                val errorResponse = gson.fromJson(errorBody, ApiResponse::class.java)
                ApiResponse(
                    success = false,
                    message = errorResponse?.message ?: e.message(),
                    data = null,
                    error = ApiError(e.code().toString(), errorResponse?.message ?: e.message())
                )
            } catch (jsonEx: Exception) {
                ApiResponse(false, e.message(), null, ApiError(e.code().toString(), e.message()))
            }
        } else {
            ApiResponse(false, e.message, null, ApiError("500", e.message ?: "Connection error"))
        }
    }
}
