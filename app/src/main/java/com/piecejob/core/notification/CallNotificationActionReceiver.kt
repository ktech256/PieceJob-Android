package com.piecejob.core.notification

import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.piecejob.core.communication.CallManager
import com.piecejob.core.socket.SocketManager
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class CallNotificationActionReceiver : BroadcastReceiver() {

    @Inject
    lateinit var socketManager: SocketManager

    @Inject
    lateinit var callManager: CallManager

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action
        val jobId = intent.getStringExtra("jobId") ?: return
        val callerId = intent.getStringExtra("callerId") ?: return
        Log.d("FORENSIC", "CALL_NOTIFICATION_RECEIVER | Action: $action | Job: $jobId")

        when (action) {
            "ACTION_REJECT_CALL" -> {
                // 1. Signal Reject via Socket
                socketManager.sendCallSignal(jobId, callerId, "REJECTED")
                
                // 2. Disconnect locally if active (unlikely but safe)
                if (callManager.activeJobId == jobId) {
                    callManager.disconnect("Declined")
                }

                // 3. Dismiss Notification
                val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                notificationManager.cancel(jobId.hashCode())
            }
        }
    }
}
