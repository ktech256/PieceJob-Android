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
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.json.JSONObject
import kotlinx.coroutines.flow.update
import java.io.ByteArrayOutputStream
import javax.inject.Inject

@HiltViewModel
class ChatViewModel @Inject constructor(
    private val repository: ChatRepository,
    private val socketManager: SocketManager,
    private val jobRepository: com.piecejob.core.data.repository.JobRepository,
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

    private val _jobState = MutableStateFlow<JobDto?>(null)
    val jobState: StateFlow<JobDto?> = _jobState

    private val _eta = MutableStateFlow("-- mins")
    val eta: StateFlow<String> = _eta

    private val _distance = MutableStateFlow("-- km away")
    val distance: StateFlow<String> = _distance

    private var currentJobId: String? = null

    fun initChat(jobId: String) {
        currentJobId = jobId
        Log.d("FORENSIC", "CHAT_LOAD_HISTORY | Job: $jobId")
        loadMessages(jobId)
        loadJobConfig(jobId)
        socketManager.joinJob(jobId)
        
        // Setup Live Metrics (ETA/Distance)
        setupLiveMetrics()
        
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

        // Fallback polling every 5 seconds for negotiation stability
        viewModelScope.launch {
            while (isActive) {
                delay(5000)
                if (currentJobId == jobId) {
                    loadJobConfig(jobId)
                }
            }
        }
    }

    private fun loadJobConfig(jobId: String) {
        viewModelScope.launch {
            val jobRes = jobRepository.getJobById(jobId)
            if (jobRes.success && jobRes.data != null) {
                _jobState.value = jobRes.data
                android.util.Log.d("FORENSIC", "CHAT_CONFIG_LOADED | Photos: ${jobRes.data.photoSharingRequired}, Neg: ${jobRes.data.priceNegotiationRequired}")
            }
        }
    }

    private fun setupLiveMetrics() {
        val isProvider = com.piecejob.BuildConfig.FLAVOR == "provider"
        
        if (isProvider) {
            viewModelScope.launch {
                com.piecejob.core.location.LocationService.currentLocation.collect { location ->
                    location?.let {
                        calculateLiveMetrics(it.latitude, it.longitude)
                    }
                }
            }
        } else {
            socketManager.onLocationUpdated { lat, lng, _ ->
                calculateLiveMetrics(lat, lng)
            }
        }
    }

    private fun calculateLiveMetrics(lat: Double, lng: Double) {
        val dest = _jobState.value?.location?.coordinates ?: return
        if (dest.size < 2) return
        val destLat = dest[1]
        val destLng = dest[0]

        // Reusing standard Haversine from tracking logic
        val r = 6371e3
        val phi1 = lat * Math.PI / 180
        val phi2 = destLat * Math.PI / 180
        val deltaPhi = (destLat - lat) * Math.PI / 180
        val deltaLambda = (destLng - lng) * Math.PI / 180

        val a = Math.sin(deltaPhi / 2) * Math.sin(deltaPhi / 2) +
                Math.cos(phi1) * Math.cos(phi2) *
                Math.sin(deltaLambda / 2) * Math.sin(deltaLambda / 2)
        val c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a))
        val distMeters = r * c

        _distance.value = if (distMeters < 1000) "${distMeters.toInt()} m away" else String.format("%.1f km away", distMeters / 1000)

        // Same ETA logic as tracking screen: 40km/h average
        val timeSeconds = distMeters / 11.1
        _eta.value = if (timeSeconds < 60) "1 min" else "${(timeSeconds / 60).toInt()} mins"
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
        val isNegotiating = job?.status == "PROVIDER_ACCEPTED" && 
                           (job?.priceNegotiationRequired == true || job?.priceStatus == "PENDING" || job?.photoSharingRequired == true)
        
        if (isNegotiating) {
            Log.w("FORENSIC", "CHAT_LOCKED | Free text blocked during negotiation/pre-dispatch")
            return
        }

        val tag = if (com.piecejob.BuildConfig.FLAVOR == "provider") "PROVIDER_CHAT_SEND" else "CUSTOMER_CHAT_SEND"
        Log.d("FORENSIC", "$tag | To: $receiverId | Text: $text")
        
        viewModelScope.launch {
            // Optimistic Update
            val tempId = "temp_${System.currentTimeMillis()}"
            val myId = com.piecejob.core.data.local.SessionManager(context).getUserId() ?: ""
            val optimisticMsg = MessageDto(
                id = tempId,
                jobId = jobId,
                senderId = UserSummaryDto(_id = myId, firstName = "You", lastName = "", role = ""),
                receiverId = receiverId,
                text = text,
                mediaUrl = null,
                mediaType = null,
                isRead = false,
                createdAt = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", java.util.Locale.US).format(java.util.Date())
            )
            _messages.update { it + optimisticMsg }

            val request = SendMessageRequest(jobId, receiverId, text)
            val res = repository.sendMessage(request)
            if (res.success && res.data != null) {
                Log.d("FORENSIC", "CHAT_DATABASE_SAVE | Success. Updating temp message.")
                _messages.update { current ->
                    // Replace temp message with real one, but ONLY if real one hasn't arrived via socket yet
                    if (current.any { it.id == res.data.id }) {
                        current.filter { it.id != tempId }
                    } else {
                        current.map { if (it.id == tempId) res.data else it }
                    }
                }
            } else {
                Log.e("FORENSIC", "CHAT_DATABASE_SAVE | Failed: ${res.message}")
                _messages.update { current -> current.filter { it.id != tempId } }
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
        if (_isLoading.value) return
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
                    
                    // Trace Log
                    android.util.Log.d("FORENSIC", "CHAT_UPLOAD_PIPELINE | Uploading Part ${index + 1} to generic endpoint")
                    
                    val res = repository.uploadFile(base64, "image/jpeg", "task-photos/$jobId")
                    
                    if (res.success && res.data != null) {
                        uploadedUrls.add(res.data.url)
                    } else {
                        throw Exception(res.message ?: "File upload failed")
                    }
                }

                if (uploadedUrls.isNotEmpty()) {
                    _uploadProgress.value = "Saving metadata..."
                    android.util.Log.d("FORENSIC", "CHAT_UPLOAD_PIPELINE | All files uploaded. Saving metadata for Job: $jobId")
                    val res = repository.uploadTaskPhotos(jobId, uploadedUrls)
                    if (res.success) {
                        _uploadSuccess.value = true
                        _uploadProgress.value = "Photos Uploaded Successfully"
                        android.util.Log.d("FORENSIC", "CHAT_UPLOAD_PIPELINE | Metadata saved successfully. Refreshing config.")
                    } else {
                        throw Exception(res.message ?: "Failed to save photo metadata")
                    }
                }
            } catch (e: Exception) {
                android.util.Log.e("FORENSIC", "CHAT_UPLOAD_PIPELINE | FAILED", e)
                _uploadError.value = e.message ?: "Upload failed"
            } finally {
                _isLoading.value = false
                loadJobConfig(jobId)
            }
        }
    }

    fun proposePrice(amount: Double) {
        val jobId = currentJobId ?: return
        viewModelScope.launch {
            repository.proposePrice(jobId, amount)
            loadJobConfig(jobId)
        }
    }

    fun respondToProposal(proposalId: String?, action: String) {
        val jobId = currentJobId ?: return
        if (proposalId == null) {
            Log.e("FORENSIC", "respondToProposal | proposalId is NULL")
            return
        }
        viewModelScope.launch {
            repository.respondToProposal(proposalId, action)
            loadJobConfig(jobId)
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
            Log.d("FORENSIC", "CHAT_SOCKET_RECEIVED | Received jobId: $jobId, Current: $currentJobId")
            
            if (jobId != currentJobId && currentJobId != null) {
                Log.w("FORENSIC", "CHAT_SOCKET_RECEIVED | JobId mismatch. Skipping.")
                return
            }

            Log.d("FORENSIC", "CHAT_SOCKET_RECEIVED | Parsing message payload: $json")

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
                id = json.optString("id").ifEmpty { json.optString("_id") },
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

            if (message.id.isEmpty()) {
                Log.e("FORENSIC", "CHAT_SOCKET_RECEIVED | FAILED: Message ID is empty. Full JSON: $json")
            }

            // IF it's a negotiation message, refresh job state
            val type = message.metadata?.get("type") as? String
            if (type != null && listOf("PRICE_PROPOSAL", "PRICE_ACCEPTED", "PRICE_REJECTED", "PHOTO_REQUEST", "PHOTO_UPLOAD", "PHOTOS_SEEN").contains(type)) {
                Log.d("FORENSIC", "CHAT_SOCKET_RECEIVED | Negotiation Event ($type). Refreshing Job Config.")
                loadJobConfig(jobId)
            }

            // Deduplication and replacement of optimistic messages
            val myId = com.piecejob.core.data.local.SessionManager(context).getUserId() ?: ""
            val isFromMe = message.senderId._id == myId

            _messages.update { current ->
                if (current.any { it.id == message.id }) {
                    Log.d("FORENSIC", "CHAT_RECOMPOSE | Message Ignored (Duplicate ID): ${message.id}")
                    current
                } else {
                    if (isFromMe) {
                        // Check if we have an optimistic message with matching text to replace
                        var replaced = false
                        val updated = current.map { 
                            if (!replaced && it.id.startsWith("temp_") && it.text == message.text) {
                                replaced = true
                                message
                            } else it
                        }
                        if (replaced) {
                            Log.d("FORENSIC", "CHAT_RECOMPOSE | Optimistic message replaced: ${message.id}")
                            updated
                        } else {
                            Log.d("FORENSIC", "CHAT_RECOMPOSE | Message Added (Me): ${message.id}")
                            current + message
                        }
                    } else {
                        Log.d("FORENSIC", "CHAT_RECOMPOSE | Message Added (Other): ${message.id}")
                        current + message
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("FORENSIC", "CHAT_PARSE_ERROR", e)
        }
    }
}
