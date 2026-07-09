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

    // FORENSIC GUARD: Prevent re-navigating to the same job/status in the same session
    companion object {
        private var lastHandledJobId: String? = null
        private var lastHandledStatus: String? = null
        private var initialCheckDone: Boolean = false
    }

    init {
        // Only perform the automatic startup check once per app session
        // to prevent the Dashboard -> Negotiation -> Rating -> Dashboard loop.
        if (!initialCheckDone) {
            checkForActiveJob()
            initialCheckDone = true
        }
    }

    private fun checkForActiveJob() {
        viewModelScope.launch {
            val response = jobRepository.getActiveJob()
            if (response.success && response.data != null) {
                val job = response.data
                
                // Only navigate if this is a NEW job or a CHANGE in status from what we last handled
                if (job.id == lastHandledJobId && job.status == lastHandledStatus) {
                    android.util.Log.d("FORENSIC", "MainVM | Active job state unchanged ($lastHandledStatus). Skipping auto-nav.")
                    return@launch
                }

                lastHandledJobId = job.id
                lastHandledStatus = job.status

                // PRIORITY 1: Pending Rating (only if newest job is COMPLETED)
                if (job.status == "COMPLETED") {
                    _navigationEvent.send("RATING:${job.id}")
                }
                // PRIORITY 2: Active Job Tracking (EN_ROUTE and beyond)
                else if (listOf("EN_ROUTE", "ARRIVED", "STARTED", "IN_PROGRESS").contains(job.status)) {
                    _navigationEvent.send(job.id)
                }
                // NOTE: PROVIDER_ACCEPTED and ACCEPTED (Negotiation) are handled PASSIVELY via Dashboard cards
                // to allow recovery without forced navigation loops on app restart.
            } else {
                // Clear state if no active job found
                lastHandledJobId = null
                lastHandledStatus = null
            }
        }
    }
}
