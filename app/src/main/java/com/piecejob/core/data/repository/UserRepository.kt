package com.piecejob.core.data.repository

import com.piecejob.core.data.remote.PieceJobApi
import com.piecejob.core.data.remote.dto.*
import com.piecejob.core.data.remote.ApiResponse
import com.piecejob.core.data.remote.ApiError
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UserRepository @Inject constructor(
    private val api: PieceJobApi
) {
    suspend fun updateFcmToken(fcmToken: String): ApiResponse<Unit> {
        return try {
            android.util.Log.d("FCM_AUDIT", "FCM_UPLOAD_START: Token=${fcmToken.take(15)}...")
            val response = api.updateFcmToken(FcmTokenRequest(fcmToken))
            if (response.success) {
                android.util.Log.d("FCM_AUDIT", "FCM_UPLOAD_SUCCESS")
            } else {
                android.util.Log.e("FCM_AUDIT", "FCM_UPLOAD_FAILED: ${response.message}")
            }
            response
        } catch (e: Exception) {
            android.util.Log.e("FCM_AUDIT", "FCM_UPLOAD_CRASH: ${e.message}")
            ApiResponse(false, e.message, null, ApiError("500", e.message ?: "Unknown error"))
        }
    }

    suspend fun getProfile(): ApiResponse<UserDto> {
        return try {
            android.util.Log.d("FORENSIC", "REPO | getProfile() calling API")
            val response = api.getProfile()
            android.util.Log.d("FORENSIC", "REPO | getProfile() response success: ${response.success}")
            if (response.data == null) {
                android.util.Log.e("FORENSIC", "REPO | getProfile() DATA IS NULL! Check JSON keys.")
            }
            response
        } catch (e: Exception) {
            android.util.Log.e("FORENSIC", "REPO | getProfile() CRASH: ${e.message}")
            ApiResponse(false, e.message, null, ApiError("500", e.message ?: "Unknown error"))
        }
    }

    suspend fun getReferralStats(): ApiResponse<ReferralStatsDto> {
        return try {
            api.getReferralStats()
        } catch (e: Exception) {
            ApiResponse(false, e.message, null, ApiError("500", e.message ?: "Unknown error"))
        }
    }

    suspend fun updateProfile(firstName: String, lastName: String, email: String, gender: String?, dob: String?, profilePhoto: String?): ApiResponse<UserDto> {
        return try {
            api.updateProfile(UpdateProfileRequest(firstName, lastName, email, gender, dob, profilePhoto))
        } catch (e: Exception) {
            ApiResponse(false, e.message, null, ApiError("500", e.message ?: "Unknown error"))
        }
    }

    suspend fun getAddresses(): ApiResponse<List<AddressDto>> {
        return try {
            api.getAddresses()
        } catch (e: Exception) {
            ApiResponse(false, e.message, null, ApiError("500", e.message ?: "Unknown error"))
        }
    }

    suspend fun addAddress(label: String, address: String, coordinates: List<Double>, isDefault: Boolean): ApiResponse<List<AddressDto>> {
        return try {
            api.addAddress(AddressDto(label = label, address = address, coordinates = coordinates, isDefault = isDefault))
        } catch (e: Exception) {
            ApiResponse(false, e.message, null, ApiError("500", e.message ?: "Unknown error"))
        }
    }

    suspend fun updateAddress(addressId: String, label: String, address: String, coordinates: List<Double>, isDefault: Boolean): ApiResponse<List<AddressDto>> {
        return try {
            api.updateAddress(addressId, AddressDto(label = label, address = address, coordinates = coordinates, isDefault = isDefault))
        } catch (e: Exception) {
            ApiResponse(false, e.message, null, ApiError("500", e.message ?: "Unknown error"))
        }
    }

    suspend fun deleteAddress(addressId: String): ApiResponse<List<AddressDto>> {
        return try {
            api.deleteAddress(addressId)
        } catch (e: Exception) {
            ApiResponse(false, e.message, null, ApiError("500", e.message ?: "Unknown error"))
        }
    }

    suspend fun getSavedLocations(): ApiResponse<List<SavedLocationDto>> {
        return try {
            api.getSavedLocations()
        } catch (e: Exception) {
            ApiResponse(false, e.message, null, ApiError("500", e.message ?: "Unknown error"))
        }
    }

    suspend fun addSavedLocation(name: String, address: String, coordinates: List<Double>): ApiResponse<List<SavedLocationDto>> {
        return try {
            api.addSavedLocation(SavedLocationDto(name = name, address = address, coordinates = coordinates))
        } catch (e: Exception) {
            ApiResponse(false, e.message, null, ApiError("500", e.message ?: "Unknown error"))
        }
    }

    suspend fun updateSavedLocation(locationId: String, name: String, address: String, coordinates: List<Double>): ApiResponse<List<SavedLocationDto>> {
        return try {
            api.updateSavedLocation(locationId, SavedLocationDto(name = name, address = address, coordinates = coordinates))
        } catch (e: Exception) {
            ApiResponse(false, e.message, null, ApiError("500", e.message ?: "Unknown error"))
        }
    }

    suspend fun deleteSavedLocation(locationId: String): ApiResponse<List<SavedLocationDto>> {
        return try {
            api.deleteSavedLocation(locationId)
        } catch (e: Exception) {
            ApiResponse(false, e.message, null, ApiError("500", e.message ?: "Unknown error"))
        }
    }

    suspend fun getPaymentMethods(): ApiResponse<List<UserCardDto>> {
        return try {
            api.getUserPaymentMethods()
        } catch (e: Exception) {
            ApiResponse(false, e.message, null, ApiError("500", e.message ?: "Unknown error"))
        }
    }

    suspend fun addPaymentMethod(brand: String, last4: String, expMonth: Int, expYear: Int, token: String, isDefault: Boolean): ApiResponse<List<UserCardDto>> {
        return try {
            api.addUserPaymentMethod(UserCardDto(brand = brand, last4 = last4, expMonth = expMonth, expYear = expYear, token = token, isDefault = isDefault))
        } catch (e: Exception) {
            ApiResponse(false, e.message, null, ApiError("500", e.message ?: "Unknown error"))
        }
    }

    suspend fun deletePaymentMethod(cardId: String): ApiResponse<List<UserCardDto>> {
        return try {
            api.deleteUserPaymentMethod(cardId)
        } catch (e: Exception) {
            ApiResponse(false, e.message, null, ApiError("500", e.message ?: "Unknown error"))
        }
    }

    suspend fun getEmergencyContacts(): ApiResponse<List<EmergencyContactDto>> {
        return try {
            api.getEmergencyContacts()
        } catch (e: Exception) {
            ApiResponse(false, e.message, null, ApiError("500", e.message ?: "Unknown error"))
        }
    }

    suspend fun addEmergencyContact(name: String, phone: String, relationship: String): ApiResponse<List<EmergencyContactDto>> {
        return try {
            api.addEmergencyContact(EmergencyContactDto(name = name, phone = phone, relationship = relationship))
        } catch (e: Exception) {
            ApiResponse(false, e.message, null, ApiError("500", e.message ?: "Unknown error"))
        }
    }

    suspend fun updateEmergencyContact(contactId: String, name: String, phone: String, relationship: String): ApiResponse<List<EmergencyContactDto>> {
        return try {
            api.updateEmergencyContact(contactId, EmergencyContactDto(name = name, phone = phone, relationship = relationship))
        } catch (e: Exception) {
            ApiResponse(false, e.message, null, ApiError("500", e.message ?: "Unknown error"))
        }
    }

    suspend fun deleteEmergencyContact(contactId: String): ApiResponse<List<EmergencyContactDto>> {
        return try {
            api.deleteEmergencyContact(contactId)
        } catch (e: Exception) {
            ApiResponse(false, e.message, null, ApiError("500", e.message ?: "Unknown error"))
        }
    }

    suspend fun updatePreferences(language: String?, country: String?): ApiResponse<UserDto> {
        return try {
            api.updatePreferences(mapOf("language" to language, "country" to country))
        } catch (e: Exception) {
            ApiResponse(false, e.message, null, ApiError("500", e.message ?: "Unknown error"))
        }
    }

    suspend fun updatePrivacy(profileVisibility: String, shareLocation: Boolean, dataSharing: Boolean, marketingPreferences: Boolean): ApiResponse<PrivacySettingsDto> {
        return try {
            api.updatePrivacy(PrivacySettingsDto(profileVisibility, shareLocation, dataSharing, marketingPreferences))
        } catch (e: Exception) {
            ApiResponse(false, e.message, null, ApiError("500", e.message ?: "Unknown error"))
        }
    }

    suspend fun getSubscription(): ApiResponse<SubscriptionDto> {
        return try {
            api.getSubscription()
        } catch (e: Exception) {
            ApiResponse(false, e.message, null, ApiError("500", e.message ?: "Unknown error"))
        }
    }

    suspend fun upgradeSubscription(plan: String): ApiResponse<SubscriptionDto> {
        return try {
            api.upgradeSubscription(mapOf("plan" to plan))
        } catch (e: Exception) {
            ApiResponse(false, e.message, null, ApiError("500", e.message ?: "Unknown error"))
        }
    }

    suspend fun cancelSubscription(): ApiResponse<SubscriptionDto> {
        return try {
            api.cancelSubscription()
        } catch (e: Exception) {
            ApiResponse(false, e.message, null, ApiError("500", e.message ?: "Unknown error"))
        }
    }
}
