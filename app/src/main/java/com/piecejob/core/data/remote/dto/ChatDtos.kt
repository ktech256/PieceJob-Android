package com.piecejob.core.data.remote.dto

import com.google.gson.annotations.SerializedName

data class MessageDto(
    @SerializedName("_id", alternate = ["id"]) val id: String,
    @SerializedName("jobId") val jobId: String,
    @SerializedName("senderId") val senderId: UserSummaryDto,
    @SerializedName("receiverId") val receiverId: String,
    @SerializedName("text") val text: String?,
    @SerializedName("mediaUrl") val mediaUrl: String?,
    @SerializedName("mediaType") val mediaType: String?,
    @SerializedName("isRead") val isRead: Boolean,
    @SerializedName("metadata") val metadata: Map<String, Any>? = null,
    @SerializedName("createdAt") val createdAt: String
)

data class UserSummaryDto(
    @SerializedName("_id", alternate = ["id"]) val _id: String,
    @SerializedName("firstName") val firstName: String,
    @SerializedName("lastName") val lastName: String,
    @SerializedName("role") val role: String,
    @SerializedName("profilePicture") val profilePicture: String? = null
)

data class SendMessageRequest(
    val jobId: String,
    val receiverId: String,
    val text: String? = null,
    val mediaUrl: String? = null,
    val mediaType: String? = null
)

data class ConversationDto(
    val jobId: String,
    val serviceName: String,
    val status: String,
    val otherUser: UserSummaryDto,
    val lastMessage: String,
    val lastMessageTime: String,
    val unreadCount: Int
)

data class FileUploadRequest(
    val base64: String,
    val mimeType: String,
    val folder: String
)

data class FileUploadResponse(
    val url: String
)