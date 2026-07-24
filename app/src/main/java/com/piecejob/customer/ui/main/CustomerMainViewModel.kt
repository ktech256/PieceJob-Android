package com.piecejob.customer.ui.main

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.piecejob.core.data.local.SessionManager
import com.piecejob.core.data.repository.JobRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CustomerMainViewModel @Inject constructor(
    private val jobRepository: JobRepository,
    private val sessionManager: SessionManager
) : ViewModel() {

    private val _navigationEvent = Channel<String>(Channel.BUFFERED)
    val navigationEvent: Flow<String> = _navigationEvent.receiveAsFlow()

    init {
        // Observe True App Resume events from MainActivity
        viewModelScope.launch {
            sessionManager.appResumeEvent.collect {
                android.util.Log.d("FORENSIC", "MainVM | App Resume Event Received. Checking for restoration.")
                checkForActiveJob(isAutoRestore = true)
            }
        }

        // Initial check on cold start
        checkForActiveJob(isAutoRestore = true)
    }

    private fun checkForActiveJob(isAutoRestore: Boolean) {
        viewModelScope.launch {
            // Guard: Only perform automatic navigation if restoration is permitted
            if (isAutoRestore && !sessionManager.shouldPerformRestoration()) {
                android.util.Log.d("FORENSIC", "MainVM | Restoration blocked by guard.")
                return@launch
            }

            val response = jobRepository.getActiveJob()
            if (response.success && response.data != null) {
                val job = response.data
                android.util.Log.d("FORENSIC", "MainVM | Active job found for restoration: ${job.id} | Status: ${job.status}")

                // PRIORITY 1: Pending Rating (only if newest job is COMPLETED)
                if (job.status == "COMPLETED") {
                    _navigationEvent.send("RATING:${job.id}")
                }
                // PRIORITY 2: Active Job Tracking (EN_ROUTE and beyond)
                else if (listOf("EN_ROUTE", "ARRIVED", "STARTED", "IN_PROGRESS").contains(job.status)) {
                    _navigationEvent.send(job.id)
                }
            }
        }
    }
}
