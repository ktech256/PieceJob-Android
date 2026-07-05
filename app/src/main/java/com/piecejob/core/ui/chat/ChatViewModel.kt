package com.piecejob.core.ui.chat

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Base64
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.piecejob.core.data.remote.*
import com.piecejob.core.data.remote.dto.*
import com.piecejob.core.data.repository.ChatRepository
import com.piecejob.core.socket.SocketManager
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import javax.inject.Inject

@HiltViewModel
class ChatViewModel @Inject constructor(
    private val repository: ChatRepository,
    private val socketManager: SocketManager,
    private val jobRepository: com.piecejob.core.data.repository.JobRepository,
    private val serviceRepository: com.piecejob.core.data.repository.ServiceRepository,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _messages = MutableStateFlow<List<MessageDto>>(emptyList())
    val messages: StateFlow<List<MessageDto>> = _messages

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _uploadProgress = MutableStateFlow("")
    val uploadProgress: StateFlow<String> = _uploadProgress

    private val _uploadError = MutableStateFlow<String?>(null)
    val uploadError: StateFlow<String?> = _uploadError

    private val _uploadSuccess = MutableStateFlow(false)
    val uploadSuccess: StateFlow<Boolean> = _uploadSuccess

    private val _serviceConfig = MutableStateFlow<ServiceDto?>(null)
    val serviceConfig: StateFlow<ServiceDto?> = _serviceConfig

    private val _jobState = MutableStateFlow<JobDto?>(null)
    val jobState: StateFlow<JobDto?> = _jobState

    private var currentJobId: String? = null

    fun initChat(jobId: String) {
        currentJobId = jobId
        Log.d("FORENSIC", "CHAT_LOAD_HISTORY | Job: $jobId")
        loadMessages(jobId)
        loadJobConfig(jobId)
        socketManager.joinJob(jobId)
        
        viewModelScope.launch {
            socketManager.statusEventFlow.collect { event ->
                if (event.jobId == jobId) {
                    loadJobConfig(jobId)
                }
            }
        }

        viewModelScope.launch {
            socketManager.messageEventFlow.collect { json ->
                handleIncomingMessage(json)
            }
        }
    }

    private fun loadJobConfig(jobId: String) {
        viewModelScope.launch {
            val jobRes = jobRepository.getJobById(jobId)
            if (jobRes.success && jobRes.data != null) {
                _jobState.value = jobRes.data
                val serviceCode = jobRes.data.serviceCode
                android.util.Log.d("FORENSIC", "CHAT_LOAD_CONFIG | Job: $jobId | Service: $serviceCode")
                if (serviceCode != null) {
                    val servRes = serviceRepository.getServiceDetails(serviceCode)
                    if (servRes.success && servRes.data != null) {
                        android.util.Log.d("FORENSIC", "CHAT_CONFIG_LOADED | Photos: ${servRes.data.photoSharingRequired}, Neg: ${servRes.data.priceNegotiationRequired}")
                        _serviceConfig.value = servRes.data
                    } else {
                        android.util.Log.e("FORENSIC", "CHAT_CONFIG_FAILED | Error: ${servRes.message}")
                    }
                }
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
        
        // UI Side Lock
        val job = _jobState.value
        val isNegotiating = listOf("PROVIDER_ACCEPTED", "ACCEPTED").contains(job?.status) || job?.priceStatus == "PENDING"
        
        if (isNegotiating) {
            Log.w("FORENSIC", "CHAT_LOCKED | Free text blocked during negotiation/pre-dispatch")
            return
        }

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

    fun markPhotosSeen() {
        val jobId = currentJobId ?: return
        viewModelScope.launch {
            repository.markPhotosSeen(jobId)
            loadJobConfig(jobId) // Refresh local state to enable pricing
        }
    }

    fun uploadTaskPhotos(uris: List<android.net.Uri>) {
        val jobId = currentJobId ?: return
        viewModelScope.launch {
            _isLoading.value = true
            _uploadError.value = null
            _uploadSuccess.value = false
            try {
                val uploadedUrls = mutableListOf<String>()
                val total = uris.size
                uris.forEachIndexed { index, uri ->
                    _uploadProgress.value = "Uploading ${index + 1} of $total..."
                    
                    val bitmap = context.contentResolver.openInputStream(uri)?.use { 
                        BitmapFactory.decodeStream(it)
                    } ?: throw Exception("Failed to decode image")

                    // Compression
                    val out = ByteArrayOutputStream()
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 70, out)
                    val bytes = out.toByteArray()
                    
                    val base64 = Base64.encodeToString(bytes, Base64.NO_WRAP)
                    val res = repository.uploadFile(base64, "image/jpeg", "task-photos/$jobId")
                    
                    if (res.success && res.data != null) {
                        uploadedUrls.add(res.data.url)
                    } else {
                        throw Exception(res.message ?: "File upload failed")
                    }
                }

                if (uploadedUrls.isNotEmpty()) {
                    _uploadProgress.value = "Saving metadata..."
                    val res = repository.uploadTaskPhotos(jobId, uploadedUrls)
                    if (res.success) {
                        _uploadSuccess.value = true
                        _uploadProgress.value = "Photos Uploaded Successfully"
                    } else {
                        throw Exception(res.message ?: "Failed to save photo metadata")
                    }
                }
            } catch (e: Exception) {
                Log.e("FORENSIC", "CHAT_UPLOAD_FAILED", e)
                _uploadError.value = e.message ?: "Upload failed"
            } finally {
                _isLoading.value = false
                loadJobConfig(jobId)
            }
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

    fun confirmDispatch() {
        val jobId = currentJobId ?: return
        viewModelScope.launch {
            val res = jobRepository.confirmDispatch(jobId)
            if (res.success) {
                // Socket listener should catch the status update, but we refresh anyway
                loadJobConfig(jobId)
            }
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
                            val value = meta.get(key)
                            if (value is org.json.JSONArray) {
                                val list = mutableListOf<String>()
                                for (i in 0 until value.length()) {
                                    list.add(value.getString(i))
                                }
                                map[key] = list
                            } else {
                                map[key] = value
                            }
                        }
                    }
                    map
                },
                createdAt = json.optString("createdAt")
            )

            // IF it's a negotiation message, refresh job state
            val type = message.metadata?.get("type") as? String
            if (type != null && listOf("PRICE_PROPOSAL", "PRICE_ACCEPTED", "PRICE_REJECTED", "PHOTO_REQUEST", "PHOTO_UPLOAD", "PHOTOS_SEEN").contains(type)) {
                Log.d("FORENSIC", "CHAT_SOCKET_RECEIVED | Negotiation Event ($type). Refreshing Job Config.")
                loadJobConfig(jobId)
            }

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
