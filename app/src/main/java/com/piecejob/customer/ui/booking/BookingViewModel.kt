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
import com.piecejob.core.data.remote.dto.ProviderDto
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class BookingStep {
    ADDRESS_SELECTION,
    RECIPIENT_SELECTION,
    CATEGORY_SELECTION,
    SERVICE_SELECTION,
    BOOKING_FEE,
    PAYMENT_GATEWAY,
    MATCHING,
    TRACKING
}

@HiltViewModel
class BookingViewModel @Inject constructor(
    private val jobRepository: JobRepository,
    private val serviceRepository: ServiceRepository,
    private val providerRepository: ProviderRepository
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

    // Step 6: Created Job
    val createdJob = MutableStateFlow<JobDto?>(null)

    init {
        loadCategories()
    }

    private fun loadCategories() {
        viewModelScope.launch {
            val res = serviceRepository.getCategories()
            if (res.success) {
                categories.value = res.data ?: emptyList()
            }
        }
    }

    fun setAddress(address: String, coordinates: List<Double>) {
        selectedAddress.value = address
        selectedCoordinates.value = coordinates
        validateZone(coordinates)
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
            // Fetch online providers for map markers
            // We use a generic heartbeat or similar endpoint if available, 
            // or we filter the providers list by coordinates.
            // For now, let's assume we can fetch online providers by country.
            val res = providerRepository.getOnlineProviders()
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
            val res = serviceRepository.getServices()
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
                _currentStep.value = BookingStep.PAYMENT_GATEWAY
            } else {
                _error.value = res.error?.message
            }
            _isLoading.value = false
        }
    }

    fun payBookingFee() {
        val jobId = createdJob.value?.id ?: return
        viewModelScope.launch {
            _isLoading.value = true
            val res = jobRepository.payBookingFee(jobId)
            if (res.success) {
                _currentStep.value = BookingStep.MATCHING
                startTracking()
            } else {
                _error.value = res.error?.message
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
            else -> {}
        }
    }
}
