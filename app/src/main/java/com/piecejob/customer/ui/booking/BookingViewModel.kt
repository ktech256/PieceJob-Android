package com.piecejob.customer.ui.booking

import android.annotation.SuppressLint
import android.content.Context
import android.location.Geocoder
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.piecejob.core.data.remote.ServiceDto
import com.piecejob.core.data.remote.ZoneDto
import com.piecejob.core.data.remote.PriceEstimateDto
import com.piecejob.core.data.remote.dto.JobDto
import com.piecejob.core.data.remote.dto.CreateJobRequest
import com.piecejob.core.data.repository.JobRepository
import com.piecejob.core.data.repository.ServiceRepository
import com.piecejob.core.data.repository.ProviderRepository
import com.piecejob.core.data.repository.SettingsRepository
import com.piecejob.core.data.remote.dto.ProviderDto
import com.piecejob.core.data.remote.dto.RecipientDto
import com.piecejob.core.data.remote.PaymentMethodDto
import com.google.android.libraries.places.api.net.PlacesClient
import com.google.android.libraries.places.api.net.FindAutocompletePredictionsRequest
import com.google.android.libraries.places.api.net.FetchPlaceRequest
import com.google.android.libraries.places.api.model.Place
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.Priority
import com.piecejob.core.socket.SocketManager
import com.piecejob.core.data.local.SessionManager
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

enum class BookingStep {
    ADDRESS_SELECTION,
    RECIPIENT_CHOICE,
    RECIPIENT_SELECTION,
    CATEGORY_SELECTION,
    SERVICE_SELECTION,
    BOOKING_FEE,
    PAYMENT_GATEWAY,
    PAYMENT_WEBVIEW,
    TRACKING
}

data class AddressPrediction(
    val placeId: String,
    val primaryText: String,
    val secondaryText: String,
    val fullText: String
)

@HiltViewModel
class BookingViewModel @Inject constructor(
    private val jobRepository: JobRepository,
    private val serviceRepository: ServiceRepository,
    private val providerRepository: ProviderRepository,
    private val settingsRepository: SettingsRepository,
    private val userRepository: com.piecejob.core.data.repository.UserRepository,
    private val walletRepository: com.piecejob.core.data.repository.WalletRepository,
    private val configRepository: com.piecejob.core.data.repository.ConfigRepository,
    private val socketManager: SocketManager,
    private val sessionManager: SessionManager,
    private val placesClient: PlacesClient,
    private val fusedLocationClient: FusedLocationProviderClient,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _currentStep = MutableStateFlow(BookingStep.ADDRESS_SELECTION)
    val currentStep: StateFlow<BookingStep> = _currentStep

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    // Step 1: Address
    val selectedAddress = MutableStateFlow("")
    val selectedCoordinates = MutableStateFlow<List<Double>?>(null)
    val resolvedZone = MutableStateFlow<ZoneDto?>(null)
    val nearbyProviders = MutableStateFlow<List<ProviderDto>>(emptyList())
    val addressPredictions = MutableStateFlow<List<AddressPrediction>>(emptyList())

    // Step 2: Recipient
    val isForSomeoneElse = MutableStateFlow(false)
    val recipientName = MutableStateFlow("")
    val recipientPhone = MutableStateFlow("")
    val savedRecipients = MutableStateFlow<List<RecipientDto>>(emptyList())

    // Step 3 & 4: Service
    val categories = MutableStateFlow<List<com.piecejob.core.data.remote.ServiceCategoryDto>>(emptyList())
    val services = MutableStateFlow<List<ServiceDto>>(emptyList())
    val selectedService = MutableStateFlow<ServiceDto?>(null)

    // Step 5: Pricing
    val priceEstimate = MutableStateFlow<PriceEstimateDto?>(null)
    val wallet = MutableStateFlow<com.piecejob.core.data.remote.dto.WalletDto?>(null)
    val availablePaymentMethods = MutableStateFlow<List<PaymentMethodDto>>(emptyList())
    val selectedPaymentMethod = MutableStateFlow<PaymentMethodDto?>(null)

    // Step 6: Created Job
    val createdJob = MutableStateFlow<JobDto?>(null)
    val paymentUrl = MutableStateFlow<String?>(null)
    val paymentReference = MutableStateFlow<String?>(null)

    private val _currentGpsCoordinates = MutableStateFlow<List<Double>?>(null)
    val currentGpsCoordinates: StateFlow<List<Double>?> = _currentGpsCoordinates

    init {
        android.util.Log.d("BOOKING_VM", "BookingViewModel Initialized")
        loadCategories()
        setupPaymentSocket()
    }

    @SuppressLint("MissingPermission")
    fun fetchCurrentLocation(isManualSelection: Boolean = false) {
        viewModelScope.launch {
            if (isManualSelection) _isLoading.value = true
            try {
                val location = fusedLocationClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, null).await()
                if (location != null) {
                    val lat = location.latitude
                    val lng = location.longitude
                    _currentGpsCoordinates.value = listOf(lng, lat)
                    
                    if (isManualSelection) {
                        reverseGeocode(lat, lng)
                    }
                } else if (isManualSelection) {
                    _error.value = "Unable to obtain live GPS coordinates. Please ensure GPS is enabled."
                }
            } catch (e: Exception) {
                android.util.Log.e("BOOKING_VM", "Error fetching location", e)
                if (isManualSelection) _error.value = "Failed to fetch current location. Please search manually."
            } finally {
                if (isManualSelection) _isLoading.value = false
            }
        }
    }

    fun onLocationSelected(lat: Double, lng: Double) {
        // Trigger reverse geocoding to show a human-readable address instead of raw coordinates
        reverseGeocode(lat, lng)
    }

    private fun reverseGeocode(lat: Double, lng: Double) {
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            _isLoading.value = true
            try {
                val geocoder = Geocoder(context)
                @Suppress("DEPRECATION")
                val addresses = geocoder.getFromLocation(lat, lng, 1)
                if (!addresses.isNullOrEmpty()) {
                    val addr = addresses[0]
                    val fullAddress = addr.getAddressLine(0) ?: "Lat: $lat, Lng: $lng"
                    setAddress(fullAddress, listOf(lng, lat))
                } else {
                    setAddress("Lat: $lat, Lng: $lng", listOf(lng, lat))
                }
            } catch (e: Exception) {
                android.util.Log.e("BOOKING_VM", "Geocoding error", e)
                setAddress("Lat: $lat, Lng: $lng", listOf(lng, lat))
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun initializeWithArgs(serviceCode: String?, address: String?, lat: Double?, lng: Double?) {
        if (address != null && lat != null && lng != null) {
            setAddress(address, listOf(lng, lat))
            if (serviceCode != null) {
                // If we have both, we might want to skip steps. 
                // For now, just set them so they are pre-selected.
                viewModelScope.launch {
                    val res = serviceRepository.getServices(lat = lat, lng = lng)
                    if (res.success && res.data != null) {
                        val service = res.data.services.find { it.code == serviceCode }
                        if (service != null) {
                            selectedService.value = service
                            _currentStep.value = BookingStep.BOOKING_FEE
                            fetchPriceEstimate()
                        } else {
                            _currentStep.value = BookingStep.CATEGORY_SELECTION
                        }
                    }
                }
            } else {
                _currentStep.value = BookingStep.CATEGORY_SELECTION
            }
        } else if (serviceCode != null) {
            // Find service and pre-select, but still need address
            viewModelScope.launch {
                val res = serviceRepository.getServices()
                if (res.success && res.data != null) {
                    val service = res.data.services.find { it.code == serviceCode }
                    selectedService.value = service
                }
            }
        }
    }

    private fun setupPaymentSocket() {
        socketManager.connect(com.piecejob.core.utils.Constants.SOCKET_URL)
        sessionManager.getUserId()?.let { socketManager.joinUser(it) }
        
        viewModelScope.launch {
            socketManager.statusEventFlow.collect { event ->
                if (event.status == "BROADCASTED" && _currentStep.value == BookingStep.PAYMENT_WEBVIEW) {
                    android.util.Log.d("TowMechSecurity", "SOCKET_SIGNAL: Payment confirmed via Webhook. Navigating to Matching.")
                    refreshCreatedJob()
                    _currentStep.value = BookingStep.TRACKING
                }
            }
        }
    }

    private fun refreshCreatedJob() {
        createdJob.value?.id?.let { jobId ->
            viewModelScope.launch {
                val res = jobRepository.getJobById(jobId)
                if (res.success) createdJob.value = res.data
            }
        }
    }

    private fun loadCategories() {
        viewModelScope.launch {
            android.util.Log.d("BOOKING_VM", "Loading Categories...")
            val res = serviceRepository.getCategories()
            if (res.success) {
                categories.value = res.data ?: emptyList()
                android.util.Log.d("BOOKING_VM", "Categories Loaded: ${categories.value.size}")
            } else {
                android.util.Log.e("BOOKING_VM", "Failed to load categories: ${res.error?.message}")
            }
        }
    }

    fun setAddress(address: String, coordinates: List<Double>) {
        selectedAddress.value = address
        selectedCoordinates.value = coordinates
        addressPredictions.value = emptyList()
        validateZone(coordinates)
    }

    fun searchAddress(query: String) {
        selectedAddress.value = query
        if (query.length < 3) {
            addressPredictions.value = emptyList()
            return
        }

        // Diagnostic log to verify the key being used by the system
        android.util.Log.d("MAPS_DEBUG", "Executing search with BuildConfig Key: ${com.piecejob.BuildConfig.GOOGLE_MAPS_API_KEY.take(5)}...${com.piecejob.BuildConfig.GOOGLE_MAPS_API_KEY.takeLast(5)}")

        viewModelScope.launch {
            try {
                val request = FindAutocompletePredictionsRequest.builder()
                    .setQuery(query)
                    .build()

                placesClient.findAutocompletePredictions(request)
                    .addOnSuccessListener { response ->
                        addressPredictions.value = response.autocompletePredictions.map {
                            AddressPrediction(
                                placeId = it.placeId,
                                primaryText = it.getPrimaryText(null).toString(),
                                secondaryText = it.getSecondaryText(null).toString(),
                                fullText = it.getFullText(null).toString()
                            )
                        }
                    }
                    .addOnFailureListener { exception ->
                        val message = if (exception is com.google.android.gms.common.api.ApiException) {
                            "Prediction fetch failed (${exception.statusCode}): ${exception.statusMessage ?: exception.message}. " +
                            "Error 9011 usually means the 'Places API' is not enabled in Google Cloud Console or the API key is restricted."
                        } else {
                            "Prediction fetch failed: ${exception.message}"
                        }
                        android.util.Log.e("PLACES_SEARCH", message, exception)
                        _error.value = "Unable to fetch address suggestions. Please check your internet connection or API configuration."
                    }
            } catch (e: Exception) {
                android.util.Log.e("PLACES_SEARCH", "Error searching address", e)
            }
        }
    }

    fun onPredictionSelected(prediction: AddressPrediction) {
        selectedAddress.value = prediction.fullText
        addressPredictions.value = emptyList()
        
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val placeFields = listOf(Place.Field.LAT_LNG)
                val request = FetchPlaceRequest.newInstance(prediction.placeId, placeFields)
                
                placesClient.fetchPlace(request)
                    .addOnSuccessListener { response ->
                        val latLng = response.place.latLng
                        if (latLng != null) {
                            setAddress(prediction.fullText, listOf(latLng.longitude, latLng.latitude))
                        }
                        _isLoading.value = false
                    }
                    .addOnFailureListener { exception ->
                        android.util.Log.e("PLACES_DETAILS", "Place fetch failed", exception)
                        _isLoading.value = false
                    }
            } catch (e: Exception) {
                android.util.Log.e("PLACES_DETAILS", "Error fetching place details", e)
                _isLoading.value = false
            }
        }
    }

    private fun validateZone(coordinates: List<Double>) {
        viewModelScope.launch {
            _isLoading.value = true
            val res = jobRepository.resolveZone(coordinates[1], coordinates[0])
            if (res.success && res.data != null) {
                resolvedZone.value = res.data
                _error.value = null
                loadNearbyProviders(coordinates)
            } else {
                resolvedZone.value = null
                _error.value = "Sorry, this area is currently outside our service coverage zone."
            }
            _isLoading.value = false
        }
    }

    private fun loadNearbyProviders(coordinates: List<Double>) {
        viewModelScope.launch {
            // Fetch online providers for map markers within radius
            val res = providerRepository.getOnlineProviders(lat = coordinates[1], lng = coordinates[0])
            if (res.success) {
                nearbyProviders.value = res.data ?: emptyList()
            }
        }
    }

    fun selectRecipient(someoneElse: Boolean) {
        isForSomeoneElse.value = someoneElse
        if (!someoneElse) {
            recipientName.value = ""
            recipientPhone.value = ""
            _currentStep.value = BookingStep.CATEGORY_SELECTION
        } else {
            loadSavedRecipients()
            _currentStep.value = BookingStep.RECIPIENT_SELECTION
        }
    }

    fun confirmRecipient() {
        if (_currentStep.value == BookingStep.RECIPIENT_SELECTION) {
            _currentStep.value = BookingStep.CATEGORY_SELECTION
            return
        }

        val currentGps = _currentGpsCoordinates.value
        val selectedCoords = selectedCoordinates.value

        if (currentGps != null && selectedCoords != null) {
            val results = FloatArray(1)
            android.location.Location.distanceBetween(
                currentGps[1], currentGps[0],
                selectedCoords[1], selectedCoords[0],
                results
            )
            val distance = results[0]

            if (distance > 50) {
                // Smart Detection: Distance > 50m, ask who it's for
                _currentStep.value = BookingStep.RECIPIENT_CHOICE
            } else {
                // Within 50m, proceed to Category Selection
                _currentStep.value = BookingStep.CATEGORY_SELECTION
            }
        } else {
            // Fallback if GPS unavailable
            _currentStep.value = BookingStep.CATEGORY_SELECTION
        }
    }

    private fun loadSavedRecipients() {
        viewModelScope.launch {
            val res = userRepository.getSavedRecipients()
            if (res.success) {
                savedRecipients.value = res.data ?: emptyList()
            }
        }
    }

    fun deleteRecipient(recipientId: String) {
        viewModelScope.launch {
            _isLoading.value = true
            val res = userRepository.deleteRecipient(recipientId)
            if (res.success) {
                savedRecipients.value = res.data ?: emptyList()
            }
            _isLoading.value = false
        }
    }

    fun selectCategory(categoryCode: String) {
        viewModelScope.launch {
            _isLoading.value = true
            val coords = selectedCoordinates.value
            val res = serviceRepository.getServices(lat = coords?.get(1), lng = coords?.get(0))
            if (res.success && res.data != null) {
                services.value = res.data.services.filter { it.category == categoryCode }
                _currentStep.value = BookingStep.SERVICE_SELECTION
            }
            _isLoading.value = false
        }
    }

    fun selectService(service: ServiceDto) {
        selectedService.value = service
        fetchPriceEstimate()
    }

    private fun fetchPriceEstimate() {
        val service = selectedService.value ?: return
        viewModelScope.launch {
            _isLoading.value = true
            
            // Load Wallet for referral balance check
            val walletRes = walletRepository.getWalletBalance()
            if (walletRes.success) {
                wallet.value = walletRes.data
            }

            // Refresh workspace config to ensure correct currency
            configRepository.refreshWorkspaceConfig()

            val res = jobRepository.getPriceEstimate(service.code, resolvedZone.value?.id, false)
            if (res.success) {
                priceEstimate.value = res.data
                if ((res.data?.bookingFee ?: 0.0) > 0.0) {
                    _currentStep.value = BookingStep.BOOKING_FEE
                } else {
                    // Requirement: Skip Review & Pay if fee is 0 or null
                    android.util.Log.d("TowMechSecurity", "ZERO_FEE_DETECTED: Skipping review step.")
                    createJob()
                }
            } else {
                _error.value = res.error?.message
            }
            _isLoading.value = false
        }
    }

    fun createJob(useReferral: Boolean = false) {
        android.util.Log.d("TowMechSecurity", "PAYMENT_INIT_STARTED: createJob triggered, useReferral=$useReferral")
        val service = selectedService.value ?: run {
            android.util.Log.e("TowMechSecurity", "PAYMENT_INIT_FAILED: selectedService is null")
            return
        }
        val coords = selectedCoordinates.value ?: run {
            android.util.Log.e("TowMechSecurity", "PAYMENT_INIT_FAILED: selectedCoordinates is null")
            return
        }
        
        viewModelScope.launch {
            _isLoading.value = true
            val request = CreateJobRequest(
                serviceCode = service.code,
                coordinates = coords,
                address = selectedAddress.value,
                zoneId = resolvedZone.value?.id,
                isEmergency = false,
                isForSomeoneElse = isForSomeoneElse.value,
                recipientName = recipientName.value,
                recipientPhone = recipientPhone.value
            )
            android.util.Log.d("NAV_AUDIT", "CREATE_JOB_STARTED")
            val res = jobRepository.createJob(request)
            android.util.Log.d("NAV_AUDIT", "CREATE_JOB_RESPONSE | Success: ${res.success} | JobId: ${res.data?.id}")
            if (res.success && res.data != null) {
                createdJob.value = res.data
                socketManager.joinJob(res.data.id)
                
                if (useReferral) {
                    android.util.Log.d("NAV_AUDIT", "PROCEEDING_WITH_REFERRAL_PAYMENT")
                    payWithReferralBalance()
                } else {
                    android.util.Log.d("NAV_AUDIT", "PROCEEDING_WITH_BOOKING_FEE_PAYMENT")
                    payBookingFee(res.data.id)
                }
            } else {
                val errorMsg = res.error?.message ?: "Job creation failed"
                android.util.Log.e("NAV_AUDIT", "CREATE_JOB_FAILED | Error: $errorMsg")
                _error.value = errorMsg
                _isLoading.value = false
            }
        }
    }

    private fun loadPaymentMethods() {
        // Obsolete - selecting automatically on backend
    }

    fun payBookingFee(explicitJobId: String? = null) {
        val jobId = explicitJobId ?: createdJob.value?.id ?: run {
            android.util.Log.e("TowMechSecurity", "PAYMENT_STEP2_FAILED: jobId is null")
            return
        }
        android.util.Log.d("NAV_AUDIT", "PAY_BOOKING_FEE_STARTED | JobId: $jobId")
        viewModelScope.launch {
            _isLoading.value = true
            val res = jobRepository.payBookingFee(jobId)
            android.util.Log.d("NAV_AUDIT", "PAY_BOOKING_FEE_RESPONSE | Success: ${res.success} | HasData: ${res.data != null}")
            if (res.success && res.data != null) {
                if (res.data.paymentUrl != null) {
                    android.util.Log.d("NAV_AUDIT", "PAY_BOOKING_FEE_URL_RECEIVED | URL: ${res.data.paymentUrl}")
                    paymentUrl.value = res.data.paymentUrl
                    paymentReference.value = res.data.reference
                    _currentStep.value = BookingStep.PAYMENT_WEBVIEW
                } else {
                    android.util.Log.d("NAV_AUDIT", "PAY_BOOKING_FEE_SKIP_WEBVIEW | Navigating to TRACKING")
                    _currentStep.value = BookingStep.TRACKING
                }
            } else {
                val errorMsg = res.error?.message ?: "Payment data payload is null"
                android.util.Log.e("NAV_AUDIT", "PAY_BOOKING_FEE_FAILED | Error: $errorMsg")
                _error.value = errorMsg
            }
            _isLoading.value = false
        }
    }

    fun verifyPayment() {
        val reference = paymentReference.value ?: run {
            android.util.Log.e("TowMechSecurity", "PAYMENT_VERIFY_FAILED: Reference is null")
            return
        }
        android.util.Log.d("TowMechSecurity", "PAYMENT_VERIFY_STARTED: Verifying reference $reference")
        viewModelScope.launch {
            _isLoading.value = true
            android.util.Log.d("NAV_AUDIT", "PAYMENT_VERIFY_STARTED | Ref: $reference")
            val res = jobRepository.verifyPayment(reference)
            android.util.Log.d("NAV_AUDIT", "PAYMENT_VERIFY_RESPONSE | Success: ${res.success} | HasData: ${res.data != null}")
            if (res.success && res.data != null) {
                createdJob.value = res.data
                android.util.Log.d("NAV_AUDIT", "PAYMENT_VERIFY_SUCCESS | Moving to TRACKING step | Job: ${res.data.id}")
                // Immediately navigate to Tracking screen
                _currentStep.value = BookingStep.TRACKING
            } else {
                val errorMsg = res.error?.message ?: "Verification data is null"
                android.util.Log.e("NAV_AUDIT", "PAYMENT_VERIFY_FAILED | Error: $errorMsg")
                _error.value = errorMsg
            }
            _isLoading.value = false
        }
    }

    fun payWithReferralBalance() {
        val jobId = createdJob.value?.id ?: run {
            _error.value = "Active job not found"
            return
        }
        
        viewModelScope.launch {
            _isLoading.value = true
            android.util.Log.d("REFERRAL_AUDIT", "PAYING_WITH_REFERRAL: JobID $jobId")
            
            val res = jobRepository.payBookingFee(jobId) 
            if (res.success && res.data != null) {
                val job = res.data.job
                if (job.status == "BROADCASTED" || job.status == "PENDING_MATCH") {
                    _currentStep.value = BookingStep.TRACKING
                } else if (res.data.paymentUrl != null) {
                    paymentUrl.value = res.data.paymentUrl
                    paymentReference.value = res.data.reference
                    _currentStep.value = BookingStep.PAYMENT_WEBVIEW
                } else {
                    createdJob.value = job
                    _currentStep.value = BookingStep.TRACKING
                }
            } else {
                _error.value = res.error?.message ?: "Payment failed"
            }
            _isLoading.value = false
        }
    }

    fun previousStep() {
        when (_currentStep.value) {
            BookingStep.RECIPIENT_CHOICE -> _currentStep.value = BookingStep.ADDRESS_SELECTION
            BookingStep.RECIPIENT_SELECTION -> _currentStep.value = BookingStep.RECIPIENT_CHOICE
            BookingStep.CATEGORY_SELECTION -> _currentStep.value = if (isForSomeoneElse.value) BookingStep.RECIPIENT_SELECTION else BookingStep.ADDRESS_SELECTION
            BookingStep.SERVICE_SELECTION -> _currentStep.value = BookingStep.CATEGORY_SELECTION
            BookingStep.BOOKING_FEE -> _currentStep.value = BookingStep.SERVICE_SELECTION
            BookingStep.PAYMENT_WEBVIEW -> _currentStep.value = BookingStep.BOOKING_FEE
            else -> {}
        }
    }
}
