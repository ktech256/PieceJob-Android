package com.piecejob.provider.ui.profile

import android.content.Context
import android.net.Uri
import android.util.Base64
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.piecejob.core.data.repository.ProviderRepository
import com.piecejob.core.data.repository.VerificationRepository
import com.piecejob.core.data.remote.dto.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ProviderBankDetailsViewModel @Inject constructor(
    private val repository: ProviderRepository,
    private val verificationRepository: VerificationRepository
) : ViewModel() {

    private val _bankDetails = MutableStateFlow<BankDetailsDto?>(null)
    val bankDetails: StateFlow<BankDetailsDto?> = _bankDetails

    private val _profile = MutableStateFlow<ProviderFullDto?>(null)
    val profile: StateFlow<ProviderFullDto?> = _profile

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _isUpdateSuccess = MutableStateFlow(false)
    val isUpdateSuccess: StateFlow<Boolean> = _isUpdateSuccess

    private val _uploadUrl = MutableStateFlow<String?>(null)
    val uploadUrl: StateFlow<String?> = _uploadUrl

    init {
        loadData()
    }

    fun loadData() {
        viewModelScope.launch {
            _isLoading.value = true
            launch {
                val response = repository.getBankDetails()
                if (response.success) _bankDetails.value = response.data
            }
            launch {
                val response = repository.getProviderFullProfile()
                if (response.success) _profile.value = response.data
            }
            _isLoading.value = false
        }
    }

    fun updateBankDetails(bank: String, holder: String, number: String, branch: String, type: String, confirmationUrl: String?) {
        viewModelScope.launch {
            _isLoading.value = true
            val request = UpdateBankDetailsRequest(bank, holder, number, branch, type, confirmationUrl ?: _uploadUrl.value)
            val response = repository.updateBankDetails(request)
            if (response.success) {
                _bankDetails.value = response.data
                _isUpdateSuccess.value = true
            }
            _isLoading.value = false
        }
    }

    fun uploadBankConfirmation(uri: Uri, context: Context) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
                if (bytes != null) {
                    val base64 = Base64.encodeToString(bytes, Base64.NO_WRAP)
                    val mimeType = context.contentResolver.getType(uri) ?: "application/pdf"
                    
                    val response = verificationRepository.uploadFile(base64, mimeType, "banking")
                    if (response.success && response.data != null) {
                        _uploadUrl.value = response.data.url
                    }
                }
            } catch (e: Exception) {
                _uploadUrl.value = null
            } finally {
                _isLoading.value = false
            }
        }
    }
}
