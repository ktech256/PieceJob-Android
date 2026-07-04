package com.piecejob.core.data.remote.dto

data class MessageDto(
    val id: String,
    val jobId: String,
    val senderId: UserSummaryDto,
    val receiverId: String,
    val text: String?,
    val mediaUrl: String?,
    val mediaType: String?,
    val isRead: Boolean,
    val metadata: Map<String, Any>? = null,
    val createdAt: String
)

data class UserSummaryDto(
    val _id: String,
    val firstName: String,
    val lastName: String,
    val role: String,
    val profilePicture: String? = null
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