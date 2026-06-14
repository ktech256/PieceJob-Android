package com.piecejob.provider.ui.verification

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
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
            launch {
                val response = repository.getVerificationStatus()
                if (response.success) _status.value = response.data
            }
            launch {
                val response = repository.getVerificationRequirements()
                if (response.success) _requirements.value = response.data
            }
            _isLoading.value = false
        }
    }

    private fun loadStagedDocs() {
        val staged = mutableMapOf<String, String>()
        val allDocTypes = listOf(
            "GOVERNMENT_ID", "SELFIE", "CRIMINAL_CHECK",
            "CERTIFICATION", "EXPERIENCE_VERIFICATION",
            "TRADE_LICENSE", "TOOL_VERIFICATION"
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
                    uri?.let { context.contentResolver.getType(it)?.split("/")?.last() } ?: "file"
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
                // In full implementation, we'd upload these files to server first
                // For now, we simulate submission with local paths as URLs
                val docsToSubmit = _stagedDocs.value.map { (type, path) ->
                    VerificationDocDto(type = type, url = "file://$path", status = "PENDING", rejectionReason = null)
                }

                val targetLevel = _requirements.value?.targetLevel ?: "STANDARD"
                val response = repository.submitVerification(SubmitVerificationRequest(targetLevel, docsToSubmit))
                
                if (response.success) {
                    // Cleanup local files
                    _stagedDocs.value.values.forEach { path -> File(path).delete() }
                    sessionManager.clearStagedDocs()
                    _stagedDocs.value = emptyMap()
                    _isSubmitSuccess.value = true
                    loadData()
                } else {
                    _error.value = response.error?.message ?: "Submission failed"
                }
            } catch (e: Exception) {
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
