package com.piecejob.customer.ui.main

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.piecejob.core.data.repository.JobRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CustomerMainViewModel @Inject constructor(
    private val jobRepository: JobRepository
) : ViewModel() {

    private val _navigationEvent = MutableSharedFlow<String>()
    val navigationEvent: SharedFlow<String> = _navigationEvent

    init {
        checkForActiveJob()
    }

    private fun checkForActiveJob() {
        viewModelScope.launch {
            val response = jobRepository.getActiveJob()
            if (response.success && response.data != null) {
                val job = response.data
                if (job.status == "PROVIDER_ACCEPTED" || job.priceNegotiationRequired == true || job.photoSharingRequired == true) {
                    _navigationEvent.emit("NEGOTIATION:${job.id}:${job.providerId}")
                } else {
                    _navigationEvent.emit(job.id)
                }
            }
        }
    }
}
