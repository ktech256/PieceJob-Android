package com.piecejob.core.ui.chat

import android.util.Log
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
        Log.d("FORENSIC", "CHAT_LOAD_HISTORY | Job: $jobId")
        loadMessages(jobId)
        socketManager.joinJob(jobId)
        
        viewModelScope.launch {
            socketManager.messageEventFlow.collect { json ->
                handleIncomingMessage(json)
            }
        }
    }

    private fun loadMessages(jobId: String) {
        viewModelScope.launch {
            _isLoading.value = true
            val response = repository.getChatMessages(jobId)
            if (response.success && response.data != null) {
                Log.d("FORENSIC", "CHAT_HISTORY_LOADED | Count: ${response.data.size}")
                _messages.value = response.data
            } else {
                Log.e("FORENSIC", "CHAT_HISTORY_FAILED | Error: ${response.message}")
            }
            _isLoading.value = false
        }
    }

    fun sendMessage(receiverId: String, text: String) {
        val jobId = currentJobId ?: return
        val tag = if (com.piecejob.BuildConfig.FLAVOR == "provider") "PROVIDER_CHAT_SEND" else "CUSTOMER_CHAT_SEND"
        Log.d("FORENSIC", "$tag | To: $receiverId | Text: $text")
        
        viewModelScope.launch {
            val request = SendMessageRequest(jobId, receiverId, text)
            val res = repository.sendMessage(request)
            if (res.success) {
                Log.d("FORENSIC", "CHAT_DATABASE_SAVE | Success")
            } else {
                Log.e("FORENSIC", "CHAT_DATABASE_SAVE | Failed: ${res.message}")
            }
        }
    }

    fun requestPhotos() {
        val jobId = currentJobId ?: return
        viewModelScope.launch {
            repository.requestPhotos(jobId)
        }
    }

    fun uploadTaskPhotos(uris: List<android.net.Uri>) {
        val jobId = currentJobId ?: return
        viewModelScope.launch {
            val dummyUrls = uris.map { "https://piecejob.com/simulated-upload/${it.lastPathSegment ?: "image"}" }
            repository.uploadTaskPhotos(jobId, dummyUrls)
        }
    }

    fun proposePrice(amount: Double, note: String?) {
        val jobId = currentJobId ?: return
        viewModelScope.launch {
            repository.proposePrice(jobId, amount, note)
        }
    }

    fun respondToProposal(proposalId: String, action: String) {
        viewModelScope.launch {
            repository.respondToProposal(proposalId, action)
        }
    }

    private fun handleIncomingMessage(json: JSONObject) {
        try {
            val jobId = json.optString("jobId")
            if (jobId != currentJobId) return

            Log.d("FORENSIC", "CHAT_SOCKET_RECEIVED | Parsing message...")

            // Handle senderId being a string (old) or an object (populated)
            val senderJson = json.optJSONObject("senderId")
            val senderId = if (senderJson != null) {
                UserSummaryDto(
                    _id = senderJson.optString("_id"),
                    firstName = senderJson.optString("firstName"),
                    lastName = senderJson.optString("lastName"),
                    role = senderJson.optString("role"),
                    profilePicture = senderJson.optString("profilePicture")
                )
            } else {
                UserSummaryDto(
                    _id = json.optString("senderId"),
                    firstName = "",
                    lastName = "",
                    role = ""
                )
            }

            val message = MessageDto(
                id = json.optString("_id").ifEmpty { json.optString("id") },
                jobId = jobId,
                senderId = senderId,
                receiverId = json.optString("receiverId"),
                text = json.optString("text"),
                mediaUrl = json.optString("mediaUrl"),
                mediaType = json.optString("mediaType"),
                isRead = json.optBoolean("isRead", false),
                metadata = json.optJSONObject("metadata")?.let { meta ->
                    val map = mutableMapOf<String, Any>()
                    val keys = meta.keys()
                    while (keys.hasNext()) {
                        val key = keys.next()
                        if (key is String) {
                            map[key] = meta.get(key)
                        }
                    }
                    map
                },
                createdAt = json.optString("createdAt")
            )

            // Deduplication
            if (_messages.value.none { it.id == message.id }) {
                _messages.value = _messages.value + message
                Log.d("FORENSIC", "CHAT_RECOMPOSE | Message Added")
            }
        } catch (e: Exception) {
            Log.e("FORENSIC", "CHAT_PARSE_ERROR", e)
        }
    }
}
