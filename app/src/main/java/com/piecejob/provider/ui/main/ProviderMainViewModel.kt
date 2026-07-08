package com.piecejob.provider.ui.main

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.piecejob.core.data.repository.JobRepository
import com.piecejob.core.notification.AlertManager
import com.piecejob.core.notification.manager.IncomingJob
import com.piecejob.core.notification.manager.NotificationState
import com.piecejob.core.socket.SocketManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ProviderMainViewModel @Inject constructor(
    private val jobRepository: JobRepository,
    private val socketManager: SocketManager,
    private val alertManager: AlertManager,
    val notificationState: NotificationState
) : ViewModel() {

    private val _navigationEvent = Channel<String>(Channel.BUFFERED)
    val navigationEvent: Flow<String> = _navigationEvent.receiveAsFlow()

    companion object {
        private var lastHandledJobId: String? = null
        private var lastHandledStatus: String? = null
        private var initialCheckDone: Boolean = false
    }

    init {
        setupSocketListeners()
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
                
                if (job.id == lastHandledJobId && job.status == lastHandledStatus) {
                    android.util.Log.d("FORENSIC", "ProviderMainVM | State unchanged. Skipping nav.")
                    return@launch
                }

                lastHandledJobId = job.id
                lastHandledStatus = job.status

                // PRIORITY 1: Active Job Tracking
                if (listOf("ACCEPTED", "EN_ROUTE", "ARRIVED", "STARTED", "IN_PROGRESS").contains(job.status)) {
                    _navigationEvent.send("TRACKING:${job.id}")
                }
                // PRIORITY 2: Active Negotiation
                else if (job.status == "PROVIDER_ACCEPTED") {
                    _navigationEvent.send("NEGOTIATION:${job.id}:${job.customerId}")
                }
                // PRIORITY 3: Pending Rating
                else if (job.status == "COMPLETED") {
                    _navigationEvent.send("RATING:${job.id}")
                }
            } else {
                lastHandledJobId = null
                lastHandledStatus = null
            }
        }
    }

    private fun setupSocketListeners() {
        socketManager.onNewBroadcast { data ->
            android.util.Log.d("FCM_AUDIT", "ENTRY: setupSocketListeners -> onNewBroadcast")
            android.util.Log.d("SOCKET_AUDIT", "NEW_JOB_BROADCAST received via Socket: $data")
            val incomingJob = IncomingJob(
                jobId = data.optString("jobId"),
                serviceCode = data.optString("serviceCode", "Service Request"),
                serviceName = data.optString("serviceName"),
                customerName = data.optString("recipientName"),
                address = data.optString("address").takeIf { it.isNotEmpty() }
                    ?: data.optJSONObject("location")?.optString("address").takeIf { it?.isNotEmpty() == true }
                    ?: "Nearby Location",
                distance = data.optString("distance").takeIf { it.isNotEmpty() } ?: "Nearby",
                expiresAt = System.currentTimeMillis() + 60000
            )
            android.util.Log.d("FCM_AUDIT", "Triggering banner display for Job ${incomingJob.jobId}")
            notificationState.showJobRequest(incomingJob)
            android.util.Log.d("FCM_AUDIT", "Triggering AlertManager via Socket")
            alertManager.start()
        }
    }

    fun acceptJob(jobId: String) {
        viewModelScope.launch {
            notificationState.setAccepting(true)
            val res = jobRepository.acceptJob(jobId)
            if (res.success && res.data != null) {
                notificationState.dismissJobRequest()
                alertManager.stop()
                
                val job = res.data
                if (job.status == "PROVIDER_ACCEPTED" || job.priceStatus == "PENDING") {
                    // Navigate to Negotiation Session
                    _navigationEvent.send("NEGOTIATION:${jobId}:${job.customerId}")
                } else {
                    // Navigate to Tracking for Dispatch
                    _navigationEvent.send("TRACKING:${jobId}")
                }
            } else {
                notificationState.setAccepting(false)
            }
        }
    }

    fun declineJob(jobId: String) {
        notificationState.dismissJobRequest()
        alertManager.stop()
    }
}
