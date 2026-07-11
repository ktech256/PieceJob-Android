package com.piecejob.customer.ui.account

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.piecejob.core.data.repository.UserRepository
import com.piecejob.core.data.remote.dto.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CustomerAccountViewModel @Inject constructor(
    private val userRepository: UserRepository,
    private val configRepository: com.piecejob.core.data.repository.ConfigRepository
) : ViewModel() {

    private val _user = MutableStateFlow<UserDto?>(null)
    val user: StateFlow<UserDto?> = _user

    val isReferralEnabled = MutableStateFlow(configRepository.isReferralEnabled())
    val referralBaseUrl = MutableStateFlow(configRepository.getReferralBaseUrl())
    val qrBrandingType = MutableStateFlow(configRepository.getQrBrandingType())

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    private val _addresses = MutableStateFlow<List<AddressDto>>(emptyList())
    val addresses: StateFlow<List<AddressDto>> = _addresses

    private val _savedLocations = MutableStateFlow<List<SavedLocationDto>>(emptyList())
    val savedLocations: StateFlow<List<SavedLocationDto>> = _savedLocations

    private val _paymentMethods = MutableStateFlow<List<UserCardDto>>(emptyList())
    val paymentMethods: StateFlow<List<UserCardDto>> = _paymentMethods

    private val _emergencyContacts = MutableStateFlow<List<EmergencyContactDto>>(emptyList())
    val emergencyContacts: StateFlow<List<EmergencyContactDto>> = _emergencyContacts

    private val _referralStats = MutableStateFlow<ReferralStatsDto?>(null)
    val referralStats: StateFlow<ReferralStatsDto?> = _referralStats

    private val _subscription = MutableStateFlow<SubscriptionDto?>(null)
    val subscription: StateFlow<SubscriptionDto?> = _subscription

    init {
        loadProfile()
    }

    fun loadProfile() {
        viewModelScope.launch {
            _isLoading.value = true
            android.util.Log.d("FORENSIC", "VM | loadProfile() triggered")
            val response = userRepository.getProfile()
            if (response.success && response.data != null) {
                android.util.Log.d("FORENSIC", "VM | Profile Loaded: ${response.data.firstName} ${response.data.lastName}")
                android.util.Log.d("FORENSIC", "VM | Gender: ${response.data.gender} | DOB: ${response.data.dob}")
                _user.value = response.data
                _addresses.value = response.data.addresses ?: emptyList()
                _savedLocations.value = response.data.savedLocations ?: emptyList()
                _paymentMethods.value = response.data.paymentMethods ?: emptyList()
                _emergencyContacts.value = response.data.emergencyContacts ?: emptyList()
                _subscription.value = response.data.subscription
            } else {
                android.util.Log.e("FORENSIC", "VM | Profile Load Failed: ${response.message}")
            }
            _isLoading.value = false
        }
    }

    fun updateProfile(firstName: String, lastName: String, email: String, gender: String?, dob: String?, profilePhoto: String?) {
        viewModelScope.launch {
            _isLoading.value = true
            val response = userRepository.updateProfile(firstName, lastName, email, gender, dob, profilePhoto)
            if (response.success) {
                loadProfile()
            } else {
                _error.value = response.message
            }
            _isLoading.value = false
        }
    }

    fun addAddress(label: String, address: String, coordinates: List<Double>, isDefault: Boolean) {
        viewModelScope.launch {
            _isLoading.value = true
            android.util.Log.d("FORENSIC", "VM | addAddress: $label | $address")
            val response = userRepository.addAddress(label, address, coordinates, isDefault)
            if (response.success) {
                android.util.Log.d("FORENSIC", "VM | Address Added. Count: ${response.data?.size}")
                _addresses.value = response.data ?: emptyList()
            } else {
                android.util.Log.e("FORENSIC", "VM | addAddress Failed: ${response.message}")
                _error.value = response.message
            }
            _isLoading.value = false
        }
    }

    fun updateAddress(addressId: String, label: String, address: String, coordinates: List<Double>, isDefault: Boolean) {
        viewModelScope.launch {
            _isLoading.value = true
            val response = userRepository.updateAddress(addressId, label, address, coordinates, isDefault)
            if (response.success) {
                _addresses.value = response.data ?: emptyList()
            } else {
                _error.value = response.message
            }
            _isLoading.value = false
        }
    }

    fun deleteAddress(addressId: String) {
        viewModelScope.launch {
            _isLoading.value = true
            val response = userRepository.deleteAddress(addressId)
            if (response.success) {
                _addresses.value = response.data ?: emptyList()
            } else {
                _error.value = response.message
            }
            _isLoading.value = false
        }
    }

    fun addSavedLocation(name: String, address: String, coordinates: List<Double>) {
        viewModelScope.launch {
            _isLoading.value = true
            val response = userRepository.addSavedLocation(name, address, coordinates)
            if (response.success) {
                _savedLocations.value = response.data ?: emptyList()
            } else {
                _error.value = response.message
            }
            _isLoading.value = false
        }
    }

    fun updateSavedLocation(locationId: String, name: String, address: String, coordinates: List<Double>) {
        viewModelScope.launch {
            _isLoading.value = true
            val response = userRepository.updateSavedLocation(locationId, name, address, coordinates)
            if (response.success) {
                _savedLocations.value = response.data ?: emptyList()
            } else {
                _error.value = response.message
            }
            _isLoading.value = false
        }
    }

    fun deleteSavedLocation(locationId: String) {
        viewModelScope.launch {
            _isLoading.value = true
            val response = userRepository.deleteSavedLocation(locationId)
            if (response.success) {
                _savedLocations.value = response.data ?: emptyList()
            } else {
                _error.value = response.message
            }
            _isLoading.value = false
        }
    }

    fun addPaymentMethod(brand: String, last4: String, expMonth: Int, expYear: Int, token: String, isDefault: Boolean) {
        viewModelScope.launch {
            _isLoading.value = true
            val response = userRepository.addPaymentMethod(brand, last4, expMonth, expYear, token, isDefault)
            if (response.success) {
                _paymentMethods.value = response.data ?: emptyList()
            } else {
                _error.value = response.message
            }
            _isLoading.value = false
        }
    }

    fun deletePaymentMethod(cardId: String) {
        viewModelScope.launch {
            _isLoading.value = true
            val response = userRepository.deletePaymentMethod(cardId)
            if (response.success) {
                _paymentMethods.value = response.data ?: emptyList()
            } else {
                _error.value = response.message
            }
            _isLoading.value = false
        }
    }

    fun addEmergencyContact(name: String, phone: String, relationship: String) {
        viewModelScope.launch {
            _isLoading.value = true
            val response = userRepository.addEmergencyContact(name, phone, relationship)
            if (response.success) {
                _emergencyContacts.value = response.data ?: emptyList()
            } else {
                _error.value = response.message
            }
            _isLoading.value = false
        }
    }

    fun updateEmergencyContact(contactId: String, name: String, phone: String, relationship: String) {
        viewModelScope.launch {
            _isLoading.value = true
            val response = userRepository.updateEmergencyContact(contactId, name, phone, relationship)
            if (response.success) {
                _emergencyContacts.value = response.data ?: emptyList()
            } else {
                _error.value = response.message
            }
            _isLoading.value = false
        }
    }

    fun deleteEmergencyContact(contactId: String) {
        viewModelScope.launch {
            _isLoading.value = true
            val response = userRepository.deleteEmergencyContact(contactId)
            if (response.success) {
                _emergencyContacts.value = response.data ?: emptyList()
            } else {
                _error.value = response.message
            }
            _isLoading.value = false
        }
    }

    fun updatePreferences(language: String?, country: String?) {
        viewModelScope.launch {
            _isLoading.value = true
            val response = userRepository.updatePreferences(language, country)
            if (response.success) {
                loadProfile()
            }
            _isLoading.value = false
        }
    }

    fun loadReferralStats() {
        viewModelScope.launch {
            _isLoading.value = true
            // Refresh config to get latest referral settings
            configRepository.refreshWorkspaceConfig()
            isReferralEnabled.value = configRepository.isReferralEnabled()
            referralBaseUrl.value = configRepository.getReferralBaseUrl()
            qrBrandingType.value = configRepository.getQrBrandingType()

            val response = userRepository.getReferralStats()
            if (response.success) {
                _referralStats.value = response.data
            }
            _isLoading.value = false
        }
    }

    fun attachReferral(code: String) {
        viewModelScope.launch {
            _isLoading.value = true
            val response = userRepository.attachReferral(code)
            if (response.success) {
                loadProfile()
            } else {
                _error.value = response.message
            }
            _isLoading.value = false
        }
    }

    fun updatePrivacy(profileVisibility: String, shareLocation: Boolean, dataSharing: Boolean, marketingPreferences: Boolean) {
        viewModelScope.launch {
            _isLoading.value = true
            val response = userRepository.updatePrivacy(profileVisibility, shareLocation, dataSharing, marketingPreferences)
            if (response.success) {
                loadProfile()
            }
            _isLoading.value = false
        }
    }

    fun upgradeSubscription(plan: String) {
        viewModelScope.launch {
            _isLoading.value = true
            val response = userRepository.upgradeSubscription(plan)
            if (response.success) {
                _subscription.value = response.data
            }
            _isLoading.value = false
        }
    }

    fun cancelSubscription() {
        viewModelScope.launch {
            _isLoading.value = true
            val response = userRepository.cancelSubscription()
            if (response.success) {
                _subscription.value = response.data
            }
            _isLoading.value = false
        }
    }
    
    fun clearError() {
        _error.value = null
    }
}
