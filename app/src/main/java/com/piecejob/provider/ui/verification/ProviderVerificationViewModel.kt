package com.piecejob.provider.ui.verification

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.util.Base64
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.piecejob.core.data.remote.*
import com.piecejob.core.data.remote.dto.*
import com.piecejob.core.data.repository.VerificationRepository
import com.piecejob.core.data.local.SessionManager
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
import javax.inject.Inject

@HiltViewModel
class ProviderVerificationViewModel @Inject constructor(
    private val repository: VerificationRepository,
    private val sessionManager: SessionManager,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _status = MutableStateFlow<VerificationStatusDto?>(null)
    val status: StateFlow<VerificationStatusDto?> = _status

    private val _requirements = MutableStateFlow<VerificationRequirementsDto?>(null)
    val requirements: StateFlow<VerificationRequirementsDto?> = _requirements

    private val _stagedDocs = MutableStateFlow<Map<String, String>>(emptyMap())
    val stagedDocs: StateFlow<Map<String, String>> = _stagedDocs

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    private val _isSubmitSuccess = MutableStateFlow(false)
    val isSubmitSuccess: StateFlow<Boolean> = _isSubmitSuccess

    init {
        loadData()
        loadStagedDocs()
    }

    fun loadData() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                launch {
                    val response = repository.getVerificationStatus()
                    if (response.success) _status.value = response.data
                }
                launch {
                    val response = repository.getVerificationRequirements()
                    if (response.success) _requirements.value = response.data
                }
            } catch (e: Exception) {
                _error.value = "Failed to load: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    private fun loadStagedDocs() {
        val staged = mutableMapOf<String, String>()
        val allDocTypes = listOf(
            "GOVERNMENT_ID", "SELFIE", "CRIMINAL_CHECK",
            "CERTIFICATION", "EXPERIENCE_VERIFICATION",
            "TRADE_LICENSE", "TOOL_VERIFICATION", "REFERENCES", "INTERVIEW"
        )
        allDocTypes.forEach { type ->
            sessionManager.getStagedDoc(type)?.let { path ->
                // Verify file exists
                if (File(path).exists()) {
                    staged[type] = path
                } else {
                    sessionManager.removeStagedDoc(type)
                }
            }
        }
        _stagedDocs.value = staged
    }

    fun stageDocument(type: String, uri: Uri?, bitmap: Bitmap?) {
        viewModelScope.launch {
            try {
                val extension = if (bitmap != null) "jpg" else {
                    uri?.let { 
                        val typeStr = context.contentResolver.getType(it)
                        if (typeStr?.contains("pdf") == true) "pdf" else "jpg"
                    } ?: "jpg"
                }
                val file = File(context.filesDir, "staged_${type}_${System.currentTimeMillis()}.$extension")
                val out = FileOutputStream(file)
                
                if (bitmap != null) {
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 90, out)
                } else if (uri != null) {
                    context.contentResolver.openInputStream(uri)?.use { input ->
                        input.copyTo(out)
                    }
                }
                out.close()

                val path = file.absolutePath
                val current = _stagedDocs.value.toMutableMap()
                
                // Delete old staged file if exists
                current[type]?.let { oldPath -> File(oldPath).delete() }
                
                current[type] = path
                _stagedDocs.value = current
                sessionManager.saveStagedDoc(type, path)
            } catch (e: Exception) {
                _error.value = "Failed to stage document: ${e.message}"
            }
        }
    }

    fun removeStagedDocument(type: String) {
        val current = _stagedDocs.value.toMutableMap()
        current[type]?.let { path -> File(path).delete() }
        current.remove(type)
        _stagedDocs.value = current
        sessionManager.removeStagedDoc(type)
    }

    fun submitDocuments() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                Log.d("VerificationVM", "Starting submission of ${_stagedDocs.value.size} documents")
                val uploadedDocs = mutableListOf<VerificationDocDto>()
                
                for ((type, path) in _stagedDocs.value) {
                    Log.d("VerificationVM", "Processing $type from path: $path")
                    val file = File(path)
                    if (file.exists()) {
                        Log.d("VerificationVM", "Reading file bytes: ${file.length()} bytes")
                        val bytes = file.readBytes()
                        val base64 = Base64.encodeToString(bytes, Base64.NO_WRAP)
                        val mimeType = if (path.endsWith(".pdf", true)) "application/pdf" else "image/jpeg"
                        
                        Log.d("VerificationVM", "Uploading Base64 to server...")
                        val uploadRes = repository.uploadFile(base64, mimeType, "verifications")
                        if (uploadRes.success && uploadRes.data != null) {
                            Log.d("VerificationVM", "Upload Success for $type. Server path: ${uploadRes.data.url}")
                            uploadedDocs.add(VerificationDocDto(
                                type = type,
                                url = uploadRes.data.url, 
                                status = "PENDING",
                                rejectionReason = null
                            ))
                        } else {
                            Log.e("VerificationVM", "Upload Failed for $type: ${uploadRes.message}")
                            throw Exception("Failed to upload $type: ${uploadRes.message}")
                        }
                    } else {
                        Log.w("VerificationVM", "File not found at $path for $type")
                    }
                }

                if (uploadedDocs.isEmpty()) {
                    Log.w("VerificationVM", "No documents were actually uploaded.")
                    throw Exception("No documents were uploaded. Please select files first.")
                }

                val targetLevel = _requirements.value?.targetLevel ?: "STANDARD"
                Log.d("VerificationVM", "Submitting verification request for level: $targetLevel with ${uploadedDocs.size} docs")
                val response = repository.submitVerification(SubmitVerificationRequest(targetLevel, uploadedDocs))
                
                if (response.success) {
                    Log.d("VerificationVM", "Verification request submitted successfully.")
                    // Cleanup local files
                    _stagedDocs.value.values.forEach { path -> File(path).delete() }
                    sessionManager.clearStagedDocs()
                    _stagedDocs.value = emptyMap()
                    _isSubmitSuccess.value = true
                    loadData()
                } else {
                    Log.e("VerificationVM", "Final Submission Failed: ${response.error?.message}")
                    _error.value = response.error?.message ?: "Submission failed"
                }
            } catch (e: Exception) {
                Log.e("VerificationVM", "Exception in submitDocuments", e)
                _error.value = e.message
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    fun resetState() {
        _isSubmitSuccess.value = false
        _error.value = null
    }
}
