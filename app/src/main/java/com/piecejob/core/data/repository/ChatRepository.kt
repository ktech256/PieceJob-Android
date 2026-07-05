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

    suspend fun requestPhotos(jobId: String): ApiResponse<Unit> {
        return try {
            api.requestPhotos(jobId)
        } catch (e: Exception) {
            ApiResponse(false, e.message, null, ApiError("500", e.message ?: "Unknown error"))
        }
    }

    suspend fun markPhotosSeen(jobId: String): ApiResponse<Unit> {
        return try {
            api.markPhotosSeen(jobId)
        } catch (e: Exception) {
            ApiResponse(false, e.message, null, ApiError("500", e.message ?: "Unknown error"))
        }
    }

    suspend fun uploadTaskPhotos(jobId: String, photos: List<String>): ApiResponse<Unit> {
        return try {
            api.uploadPhotos(jobId, UploadPhotosRequest(photos))
        } catch (e: Exception) {
            ApiResponse(false, e.message, null, ApiError("500", e.message ?: "Unknown error"))
        }
    }

    suspend fun uploadFile(base64: String, mimeType: String, folder: String): ApiResponse<FileUploadResponse> {
        return try {
            api.uploadFile(FileUploadRequest(base64, mimeType, folder))
        } catch (e: Exception) {
            ApiResponse(false, e.message, null, ApiError("500", e.message ?: "Unknown error"))
        }
    }

    suspend fun proposePrice(jobId: String, amount: Double): ApiResponse<PriceProposalDto> {
        return try {
            api.proposePrice(ProposePriceRequest(jobId, amount))
        } catch (e: Exception) {
            ApiResponse(false, e.message, null, ApiError("500", e.message ?: "Unknown error"))
        }
    }

    suspend fun respondToProposal(proposalId: String, action: String): ApiResponse<PriceProposalDto> {
        return try {
            api.respondToProposal(proposalId, RespondProposalRequest(action))
        } catch (e: Exception) {
            ApiResponse(false, e.message, null, ApiError("500", e.message ?: "Unknown error"))
        }
    }
}
