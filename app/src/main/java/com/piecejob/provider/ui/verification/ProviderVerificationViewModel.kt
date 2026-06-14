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
            _error.value = null
            try {
                Log.d("UPLOAD_TRACE", "1. submitDocuments() triggered. Staged items: ${_stagedDocs.value.size}")
                val uploadedDocs = mutableListOf<VerificationDocDto>()
                
                for ((type, path) in _stagedDocs.value) {
                    Log.d("UPLOAD_TRACE", "2. Processing $type. Path: $path")
                    val file = File(path)
                    if (file.exists()) {
                        val bytes = file.readBytes()
                        Log.d("UPLOAD_TRACE", "3. File size: ${bytes.size} bytes")
                        
                        val base64 = Base64.encodeToString(bytes, Base64.NO_WRAP)
                        Log.d("UPLOAD_TRACE", "4. Base64 encoded. Length: ${base64.length}")
                        
                        val mimeType = if (path.endsWith(".pdf", true)) "application/pdf" else "image/jpeg"
                        
                        Log.d("UPLOAD_TRACE", "5. Starting API call to providers/upload-file")
                        val uploadRes = repository.uploadFile(base64, mimeType, "verifications")
                        
                        Log.d("UPLOAD_TRACE", "6. API response: success=${uploadRes.success}, hasData=${uploadRes.data != null}")
                        if (uploadRes.success && uploadRes.data != null) {
                            Log.d("UPLOAD_TRACE", "7. PASS: Uploaded $type. Bucket Path: ${uploadRes.data.url}")
                            uploadedDocs.add(VerificationDocDto(
                                type = type,
                                url = uploadRes.data.url, 
                                status = "PENDING",
                                rejectionReason = null
                            ))
                        } else {
                            val msg = uploadRes.message ?: uploadRes.error?.message ?: "Unknown error"
                            Log.e("UPLOAD_TRACE", "7. FAIL: Upload $type failed. Message: $msg")
                            throw Exception("Failed to upload $type: $msg")
                        }
                    } else {
                        Log.e("UPLOAD_TRACE", "2. FAIL: File not found at $path")
                    }
                }

                if (uploadedDocs.isEmpty()) {
                    Log.e("UPLOAD_TRACE", "8. FAIL: uploadedDocs is empty")
                    throw Exception("No documents were uploaded. Please select files first.")
                }

                val targetLevel = _requirements.value?.targetLevel ?: "STANDARD"
                Log.d("UPLOAD_TRACE", "9. Submitting verification transaction. Level: $targetLevel, Docs count: ${uploadedDocs.size}")
                val response = repository.submitVerification(SubmitVerificationRequest(targetLevel, uploadedDocs))
                
                if (response.success) {
                    Log.d("UPLOAD_TRACE", "10. PASS: Verification submitted successfully.")
                    // Cleanup local files
                    _stagedDocs.value.values.forEach { path -> File(path).delete() }
                    sessionManager.clearStagedDocs()
                    _stagedDocs.value = emptyMap()
                    _isSubmitSuccess.value = true
                    loadData()
                } else {
                    Log.e("UPLOAD_TRACE", "10. FAIL: Transaction failed. Error: ${response.error?.message}")
                    _error.value = response.error?.message ?: "Submission failed"
                }
            } catch (e: Exception) {
                Log.e("UPLOAD_TRACE", "ERROR in pipeline: ${e.message}", e)
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
