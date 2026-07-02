package com.piecejob.core.notification.manager

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject
import javax.inject.Singleton

data class IncomingJob(
    val jobId: String,
    val serviceCode: String,
    val serviceName: String?,
    val customerName: String?,
    val address: String?,
    val distance: String?,
    val expiresAt: Long
)

@Singleton
class NotificationState @Inject constructor() {
    private val _activeJobRequest = MutableStateFlow<IncomingJob?>(null)
    val activeJobRequest: StateFlow<IncomingJob?> = _activeJobRequest

    private val _isAccepting = MutableStateFlow(false)
    val isAccepting: StateFlow<Boolean> = _isAccepting

    fun showJobRequest(job: IncomingJob) {
        _activeJobRequest.value = job
        _isAccepting.value = false
    }

    fun setAccepting(accepting: Boolean) {
        _isAccepting.value = accepting
    }

    fun dismissJobRequest() {
        _activeJobRequest.value = null
        _isAccepting.value = false
    }
}
