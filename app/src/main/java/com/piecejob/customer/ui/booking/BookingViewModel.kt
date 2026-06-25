package com.piecejob.customer.ui.booking

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
import com.piecejob.core.data.remote.PaymentMethodDto
import com.google.android.libraries.places.api.net.PlacesClient
import com.google.android.libraries.places.api.net.FindAutocompletePredictionsRequest
import com.google.android.libraries.places.api.net.FetchPlaceRequest
import com.google.android.libraries.places.api.model.Place
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

enum class BookingStep {
    ADDRESS_SELECTION,
    RECIPIENT_SELECTION,
    CATEGORY_SELECTION,
    SERVICE_SELECTION,
    BOOKING_FEE,
    PAYMENT_GATEWAY,
    PAYMENT_WEBVIEW,
    MATCHING,
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
    private val placesClient: PlacesClient
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

    // Step 3 & 4: Service
    val categories = MutableStateFlow<List<com.piecejob.core.data.remote.ServiceCategoryDto>>(emptyList())
    val services = MutableStateFlow<List<ServiceDto>>(emptyList())
    val selectedService = MutableStateFlow<ServiceDto?>(null)

    // Step 5: Pricing
    val priceEstimate = MutableStateFlow<PriceEstimateDto?>(null)
    val availablePaymentMethods = MutableStateFlow<List<PaymentMethodDto>>(emptyList())
    val selectedPaymentMethod = MutableStateFlow<PaymentMethodDto?>(null)

    // Step 6: Created Job
    val createdJob = MutableStateFlow<JobDto?>(null)
    val paymentUrl = MutableStateFlow<String?>(null)
    val paymentReference = MutableStateFlow<String?>(null)

    init {
        android.util.Log.d("BOOKING_VM", "BookingViewModel Initialized")
        loadCategories()
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
            _currentStep.value = BookingStep.CATEGORY_SELECTION
        } else {
            _currentStep.value = BookingStep.RECIPIENT_SELECTION
        }
    }

    fun confirmRecipient() {
        _currentStep.value = BookingStep.CATEGORY_SELECTION
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
            val res = jobRepository.getPriceEstimate(service.code, resolvedZone.value?.id, false)
            if (res.success) {
                priceEstimate.value = res.data
                _currentStep.value = BookingStep.BOOKING_FEE
            } else {
                _error.value = res.error?.message
            }
            _isLoading.value = false
        }
    }

    fun createJob() {
        val service = selectedService.value ?: return
        val coords = selectedCoordinates.value ?: return
        
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
            val res = jobRepository.createJob(request)
            if (res.success && res.data != null) {
                createdJob.value = res.data
                loadPaymentMethods()
                _currentStep.value = BookingStep.PAYMENT_GATEWAY
            } else {
                _error.value = res.error?.message
            }
            _isLoading.value = false
        }
    }

    private fun loadPaymentMethods() {
        viewModelScope.launch {
            val res = settingsRepository.getPaymentMethods()
            if (res.success) {
                availablePaymentMethods.value = res.data ?: emptyList()
                selectedPaymentMethod.value = availablePaymentMethods.value.firstOrNull()
            }
        }
    }

    fun payBookingFee() {
        val jobId = createdJob.value?.id ?: return
        viewModelScope.launch {
            _isLoading.value = true
            val res = jobRepository.payBookingFee(jobId)
            if (res.success && res.data != null) {
                if (res.data.paymentUrl != null) {
                    paymentUrl.value = res.data.paymentUrl
                    paymentReference.value = res.data.reference
                    _currentStep.value = BookingStep.PAYMENT_WEBVIEW
                } else {
                    // Fallback for simulated success
                    _currentStep.value = BookingStep.MATCHING
                    startTracking()
                }
            } else {
                _error.value = res.error?.message ?: "Failed to initialize payment"
            }
            _isLoading.value = false
        }
    }

    fun verifyPayment() {
        val reference = paymentReference.value ?: return
        viewModelScope.launch {
            _isLoading.value = true
            val res = jobRepository.verifyPayment(reference)
            if (res.success && res.data != null) {
                createdJob.value = res.data
                _currentStep.value = BookingStep.MATCHING
                startTracking()
            } else {
                _error.value = res.error?.message ?: "Verification failed"
            }
            _isLoading.value = false
        }
    }

    private fun startTracking() {
        // Here we would navigate to the tracking screen or start polling job status
    }

    fun previousStep() {
        when (_currentStep.value) {
            BookingStep.RECIPIENT_SELECTION -> _currentStep.value = BookingStep.ADDRESS_SELECTION
            BookingStep.CATEGORY_SELECTION -> _currentStep.value = if (isForSomeoneElse.value) BookingStep.RECIPIENT_SELECTION else BookingStep.ADDRESS_SELECTION
            BookingStep.SERVICE_SELECTION -> _currentStep.value = BookingStep.CATEGORY_SELECTION
            BookingStep.BOOKING_FEE -> _currentStep.value = BookingStep.SERVICE_SELECTION
            BookingStep.PAYMENT_WEBVIEW -> _currentStep.value = BookingStep.BOOKING_FEE
            else -> {}
        }
    }
}
