package com.piecejob.core.data.repository

import com.piecejob.core.data.remote.PieceJobApi
import com.piecejob.core.data.remote.dto.*
import com.piecejob.core.data.remote.ApiResponse
import com.piecejob.core.data.remote.ApiError
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ChatRepository @Inject constructor(
    private val api: PieceJobApi
) {
    suspend fun getConversations(): ApiResponse<List<ConversationDto>> {
        return try {
            api.getConversations()
        } catch (e: Exception) {
            ApiResponse(false, e.message, null, ApiError("500", e.message ?: "Unknown error"))
        }
    }

    suspend fun getChatMessages(jobId: String): ApiResponse<List<MessageDto>> {
        android.util.Log.d("FORENSIC", "CUSTOMER_CHAT_REPOSITORY | getChatMessages | Job: $jobId")
        return try {
            api.getChatMessages(jobId)
        } catch (e: Exception) {
            ApiResponse(false, e.message, null, ApiError("500", e.message ?: "Unknown error"))
        }
    }

    suspend fun sendMessage(request: SendMessageRequest): ApiResponse<MessageDto> {
        android.util.Log.d("FORENSIC", "CUSTOMER_CHAT_REPOSITORY | sendMessage | To: ${request.receiverId}")
        return try {
            api.sendMessage(request)
        } catch (e: Exception) {
            ApiResponse(false, e.message, null, ApiError("500", e.message ?: "Unknown error"))
        }
    }
}
