package com.piecejob.provider.ui.profile

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.piecejob.core.data.remote.dto.DisputeDto
import com.piecejob.core.data.remote.dto.RaiseDisputeRequest
import com.piecejob.core.data.repository.ProviderRepository
import com.piecejob.core.utils.FileUtils
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ProviderDisputeViewModel @Inject constructor(
    private val repository: ProviderRepository
) : ViewModel() {

    private val _disputes = MutableStateFlow<List<DisputeDto>>(emptyList())
    val disputes: StateFlow<List<DisputeDto>> = _disputes

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    init {
        loadDisputes()
    }

    fun loadDisputes() {
        viewModelScope.launch {
            _isLoading.value = true
            val response = repository.getMyDisputes()
            if (response.success) {
                _disputes.value = response.data ?: emptyList()
            }
            _isLoading.value = false
        }
    }

    fun raiseDispute(jobId: String, reason: String, description: String, evidenceUris: List<Uri>, context: Context) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val evidenceUrls = mutableListOf<String>()
                for (uri in evidenceUris) {
                    val base64 = FileUtils.uriToBase64(uri, context)
                    val mimeType = context.contentResolver.getType(uri) ?: "image/jpeg"
                    val uploadRes = repository.uploadFile(base64, mimeType, "disputes")
                    if (uploadRes.success && uploadRes.data != null) {
                        evidenceUrls.add(uploadRes.data.url)
                    }
                }
                
                val response = repository.raiseDispute(RaiseDisputeRequest(jobId, reason, description, evidenceUrls))
                if (response.success) {
                    loadDisputes()
                }
            } catch (e: Exception) {
                // Handle error
            } finally {
                _isLoading.value = false
            }
        }
    }
}
