package com.piecejob.customer.ui.main

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.piecejob.core.data.repository.JobRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CustomerMainViewModel @Inject constructor(
    private val jobRepository: JobRepository
) : ViewModel() {

    private val _navigationEvent = Channel<String>(Channel.BUFFERED)
    val navigationEvent: Flow<String> = _navigationEvent.receiveAsFlow()

    private var lastHandledJobId: String? = null
    private var lastHandledStatus: String? = null
    private var isRestorationHandled: Boolean = false

    init {
        // Initial restoration on startup
        refreshActiveJob()
    }

    fun refreshActiveJob() {
        android.util.Log.d("FORENSIC", "MainVM | Explicit refresh requested. Resetting handled flag.")
        isRestorationHandled = false
        checkForActiveJob()
    }

    private fun checkForActiveJob() {
        viewModelScope.launch {
            val response = jobRepository.getActiveJob()
            if (response.success && response.data != null) {
                val job = response.data
                
                // If we've already navigated to this EXACT state in this "foreground session", stop.
                if (isRestorationHandled && job.id == lastHandledJobId && job.status == lastHandledStatus) {
                    android.util.Log.d("FORENSIC", "MainVM | Active job state unchanged and already handled. Skipping auto-nav.")
                    return@launch
                }

                lastHandledJobId = job.id
                lastHandledStatus = job.status
                isRestorationHandled = true

                // PRIORITY 1: Pending Rating (only if newest job is COMPLETED)
                if (job.status == "COMPLETED") {
                    _navigationEvent.send("RATING:${job.id}")
                }
                // PRIORITY 2: Active Job Tracking (EN_ROUTE and beyond)
                else if (listOf("EN_ROUTE", "ARRIVED", "STARTED", "IN_PROGRESS").contains(job.status)) {
                    _navigationEvent.send(job.id)
                }
            } else {
                // Clear state if no active job found
                lastHandledJobId = null
                lastHandledStatus = null
                isRestorationHandled = true // Consider "handled" if nothing to restore
            }
        }
    }
}
