package com.piecejob.provider.ui.main

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.piecejob.core.data.repository.JobRepository
import com.piecejob.core.notification.AlertManager
import com.piecejob.core.notification.manager.IncomingJob
import com.piecejob.core.notification.manager.NotificationState
import com.piecejob.core.socket.SocketManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ProviderMainViewModel @Inject constructor(
    private val jobRepository: JobRepository,
    private val socketManager: SocketManager,
    private val alertManager: AlertManager,
    val notificationState: NotificationState
) : ViewModel() {

    init {
        setupSocketListeners()
    }

    private fun setupSocketListeners() {
        socketManager.onNewBroadcast { data ->
            android.util.Log.d("SOCKET_AUDIT", "NEW_JOB_BROADCAST received via Socket: $data")
            val incomingJob = IncomingJob(
                jobId = data.optString("jobId"),
                serviceCode = data.optString("serviceCode", "Service Request"),
                customerName = data.optString("recipientName"),
                address = data.optString("address") ?: data.optJSONObject("location")?.optString("address"),
                distance = "Nearby",
                earnings = "R 150.00",
                expiresAt = System.currentTimeMillis() + 60000
            )
            notificationState.showJobRequest(incomingJob)
            alertManager.start()
        }
    }

    fun acceptJob(jobId: String) {
        viewModelScope.launch {
            val res = jobRepository.acceptJob(jobId)
            if (res.success) {
                notificationState.dismissJobRequest()
                alertManager.stop()
            }
        }
    }

    fun declineJob(jobId: String) {
        notificationState.dismissJobRequest()
        alertManager.stop()
    }
}
