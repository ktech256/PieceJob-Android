package com.piecejob.core.data.repository

import com.piecejob.core.data.remote.PieceJobApi
import com.piecejob.core.data.remote.dto.*
import com.piecejob.core.data.remote.ApiResponse
import com.piecejob.core.data.remote.ApiError
import com.piecejob.core.data.remote.SubmitTicketRequest
import com.piecejob.core.data.remote.TicketDto
import com.piecejob.core.data.remote.SendTicketMessageRequest
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SupportRepository @Inject constructor(
    private val api: PieceJobApi
) {
    suspend fun submitTicket(request: SubmitTicketRequest): ApiResponse<Unit> {
        return try {
            api.submitTicket(request)
        } catch (e: Exception) {
            ApiResponse(false, e.message, null, ApiError("500", e.message ?: "Unknown error"))
        }
    }

    suspend fun getMyTickets(): ApiResponse<List<TicketDto>> {
        return try {
            api.getMyTickets()
        } catch (e: Exception) {
            ApiResponse(false, e.message, null, ApiError("500", e.message ?: "Unknown error"))
        }
    }

    suspend fun getTicketDetails(ticketId: String): ApiResponse<TicketDto> {
        return try {
            api.getTicketDetails(ticketId)
        } catch (e: Exception) {
            ApiResponse(false, e.message, null, ApiError("500", e.message ?: "Unknown error"))
        }
    }

    suspend fun sendTicketMessage(ticketId: String, text: String, attachments: List<String>): ApiResponse<TicketDto> {
        return try {
            api.sendTicketMessage(ticketId, SendTicketMessageRequest(text, attachments))
        } catch (e: Exception) {
            ApiResponse(false, e.message, null, ApiError("500", e.message ?: "Unknown error"))
        }
    }
}
