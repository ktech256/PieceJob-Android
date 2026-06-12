package com.piecejob.core.ui.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.piecejob.core.data.remote.dto.*
import com.piecejob.core.data.repository.ChatRepository
import com.piecejob.core.socket.SocketManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import org.json.JSONObject
import javax.inject.Inject

@HiltViewModel
class ChatViewModel @Inject constructor(
    private val repository: ChatRepository,
    private val socketManager: SocketManager
) : ViewModel() {

    private val _messages = MutableStateFlow<List<MessageDto>>(emptyList())
    val messages: StateFlow<List<MessageDto>> = _messages

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private var currentJobId: String? = null

    fun initChat(jobId: String) {
        currentJobId = jobId
        loadMessages(jobId)
        socketManager.joinJob(jobId)
        socketManager.onNewMessage { json ->
            handleIncomingMessage(json)
        }
    }

    private fun loadMessages(jobId: String) {
        viewModelScope.launch {
            _isLoading.value = true
            val response = repository.getChatMessages(jobId)
            if (response.success && response.data != null) {
                _messages.value = response.data
            }
            _isLoading.value = false
        }
    }

    fun sendMessage(receiverId: String, text: String) {
        val jobId = currentJobId ?: return
        viewModelScope.launch {
            val request = SendMessageRequest(jobId, receiverId, text)
            repository.sendMessage(request)
            // Socket will broadcast the message back to us as well
        }
    }

    private fun handleIncomingMessage(json: JSONObject) {
        val message = MessageDto(
            id = json.optString("_id"),
            jobId = json.optString("jobId"),
            senderId = UserSummaryDto(
                _id = json.optString("senderId"),
                firstName = "", // We might not have full names in simple broadcast
                lastName = "",
                role = ""
            ),
            receiverId = "",
            text = json.optString("text"),
            mediaUrl = json.optString("mediaUrl"),
            mediaType = json.optString("mediaType"),
            isRead = false,
            createdAt = json.optString("createdAt")
        )
        
        if (message.jobId == currentJobId) {
            _messages.value = _messages.value + message
        }
    }
}
