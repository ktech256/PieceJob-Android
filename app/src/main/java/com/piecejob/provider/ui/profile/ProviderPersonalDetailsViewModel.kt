package com.piecejob.provider.ui.profile

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.piecejob.core.data.repository.ProviderRepository
import com.piecejob.core.data.remote.dto.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ProviderPersonalDetailsViewModel @Inject constructor(
    private val repository: ProviderRepository
) : ViewModel() {

    private val _profile = MutableStateFlow<ProviderFullDto?>(null)
    val profile: StateFlow<ProviderFullDto?> = _profile

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    private val _isUpdateSuccess = MutableStateFlow(false)
    val isUpdateSuccess: StateFlow<Boolean> = _isUpdateSuccess

    private val _uploadingPhoto = MutableStateFlow(false)
    val uploadingPhoto: StateFlow<Boolean> = _uploadingPhoto

    init {
        loadProfile()
    }

    fun loadProfile() {
        viewModelScope.launch {
            _isLoading.value = true
            Log.d("PersonalDetails", "Fetching full profile...")
            val response = repository.getProviderFullProfile()
            if (response.success && response.data != null) {
                Log.d("PersonalDetails", "Profile data: ${response.data}")
                _profile.value = response.data
            } else {
                Log.e("PersonalDetails", "Failed to load profile: ${response.message}")
                _error.value = response.message ?: "Failed to load profile"
            }
            _isLoading.value = false
        }
    }

    fun updateProfile(request: UpdateProfileRequest) {
        viewModelScope.launch {
            _isLoading.value = true
            val response = repository.updateProfile(request)
            if (response.success) {
                _profile.value = response.data
                _isUpdateSuccess.value = true
            } else {
                _error.value = response.message ?: "Update failed"
            }
            _isLoading.value = false
        }
    }
    
    fun resetSuccessState() {
        _isUpdateSuccess.value = false
    }

    fun uploadProfilePhoto(uri: android.net.Uri, context: android.content.Context) {
        viewModelScope.launch {
            _uploadingPhoto.value = true
            try {
                val base64 = com.piecejob.core.utils.FileUtils.uriToBase64(uri, context)
                val mimeType = context.contentResolver.getType(uri) ?: "image/jpeg"
                val response = repository.uploadFile(base64, mimeType, "profiles")
                if (response.success && response.data != null) {
                    _profile.value = _profile.value?.let {
                        it.copy(userId = it.userId.copy(profilePhoto = response.data.url))
                    }
                }
            } catch (e: Exception) {
                _error.value = "Photo upload failed: ${e.message}"
            } finally {
                _uploadingPhoto.value = false
            }
        }
    }

    fun uploadProofOfResidence(uri: android.net.Uri, context: android.content.Context) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val base64 = com.piecejob.core.utils.FileUtils.uriToBase64(uri, context)
                val mimeType = context.contentResolver.getType(uri) ?: "application/pdf"
                val response = repository.uploadFile(base64, mimeType, "documents")
                if (response.success && response.data != null) {
                    _profile.value = _profile.value?.let {
                        it.copy(userId = it.userId.copy(pendingAddress = it.userId.pendingAddress?.copy(proofOfResidenceUrl = response.data.url) ?: PendingAddressDto("", "", "", response.data.url, "", "PENDING")))
                    }
                }
            } catch (e: Exception) {
                _error.value = "Document upload failed: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }
}
