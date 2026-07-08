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
                
                // PRIORITY 1: Active Negotiation
                if (job.status == "PROVIDER_ACCEPTED") {
                    _navigationEvent.emit("NEGOTIATION:${job.id}:${job.providerId}")
                } 
                // PRIORITY 2: Pending Rating (only if newest job is COMPLETED)
                else if (job.status == "COMPLETED") {
                    _navigationEvent.emit("RATING:${job.id}")
                }
                // PRIORITY 3: Active Job Tracking
                else if (listOf("ACCEPTED", "EN_ROUTE", "ARRIVED", "STARTED", "IN_PROGRESS").contains(job.status)) {
                    _navigationEvent.emit(job.id)
                }
            }
        }
    }
}
