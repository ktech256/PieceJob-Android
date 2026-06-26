package com.piecejob.core.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.piecejob.MainActivity
import com.piecejob.R
import com.piecejob.core.data.repository.UserRepository
import com.piecejob.core.notification.manager.IncomingJob
import com.piecejob.core.notification.manager.NotificationState
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class PieceJobMessagingService : FirebaseMessagingService() {

    @Inject
    lateinit var userRepository: UserRepository
    
    @Inject
    lateinit var notificationState: NotificationState

    @Inject
    lateinit var alertManager: AlertManager

    private val job = SupervisorJob()
    private val scope = CoroutineScope(Dispatchers.IO + job)
    
    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.d("FCM", "New token: $token")
        scope.launch {
            userRepository.updateFcmToken(token)
        }
    }

    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)
        Log.d("FCM", "Message received from: ${message.from}. Data: ${message.data}")

        val type = message.data["type"]
        val jobId = message.data["jobId"]
        
        if (type == "NEW_JOB_BROADCAST" && jobId != null) {
            handleIncomingJob(message.data)
        } else if (type == "BROADCAST_CANCELLED" || type == "JOB_ASSIGNED_ELSEWHERE") {
            handleBroadcastTermination(jobId)
        } else {
            val title = message.notification?.title ?: message.data["title"] ?: "PieceJob"
            val body = message.notification?.body ?: message.data["body"] ?: ""
            showStandardNotification(title, body, message.data)
        }
    }

    private fun handleIncomingJob(data: Map<String, String>) {
        val incomingJob = IncomingJob(
            jobId = data["jobId"] ?: "",
            serviceCode = data["serviceCode"] ?: "Service Request",
            customerName = data["recipientName"],
            address = data["address"],
            distance = data["distance"] ?: "Nearby", 
            earnings = data["earnings"] ?: "Estimated Pay",
            expiresAt = System.currentTimeMillis() + 60000 
        )

        // 1. Update Global UI State (For Banners)
        notificationState.showJobRequest(incomingJob)

        // 2. Play Sound & Vibration
        alertManager.start()

        // 3. Show Heads-Up Notification (For Background/Lockscreen)
        showHeadsUpNotification(incomingJob)
    }

    private fun handleBroadcastTermination(jobId: String?) {
        notificationState.dismissJobRequest()
        alertManager.stop()
        val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        jobId?.hashCode()?.let { nm.cancel(it) }
    }

    private fun showHeadsUpNotification(job: IncomingJob) {
        val intent = Intent(this, MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            putExtra("jobId", job.jobId)
            putExtra("type", "NEW_JOB_BROADCAST")
        }
        
        val pendingIntent = PendingIntent.getActivity(
            this, job.jobId.hashCode(), intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val channelId = "piecejob_broadcasts"
        val builder = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("New ${job.serviceCode} Request")
            .setContentText("Incoming request from ${job.customerName ?: "Customer"}")
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_CALL)
            .setAutoCancel(true)
            .setOngoing(true)
            .setContentIntent(pendingIntent)
            .setFullScreenIntent(pendingIntent, true) 

        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(channelId, "Service Broadcasts", NotificationManager.IMPORTANCE_HIGH).apply {
                description = "Urgent service requests for providers"
                enableLights(true)
                enableVibration(true)
                setSound(null, null) 
            }
            notificationManager.createNotificationChannel(channel)
        }

        notificationManager.notify(job.jobId.hashCode(), builder.build())
    }

    private fun showStandardNotification(title: String, body: String, data: Map<String, String>) {
        val intent = Intent(this, MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
            data.forEach { (key, value) -> putExtra(key, value) }
        }
        val pendingIntent = PendingIntent.getActivity(
            this, System.currentTimeMillis().toInt(), intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val channelId = "piecejob_notifications"
        val builder = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setContentText(body)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)

        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(channelId, "PieceJob Notifications", NotificationManager.IMPORTANCE_HIGH)
            notificationManager.createNotificationChannel(channel)
        }

        notificationManager.notify(System.currentTimeMillis().toInt(), builder.build())
    }

    override fun onDestroy() {
        super.onDestroy()
        job.cancel()
    }
}
