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

    override fun onCreate() {
        android.util.Log.e("PIECEJOB_FCM", "Messaging service created")
        super.onCreate()
    }

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
        android.util.Log.e("PIECEJOB_FCM", "onNewToken executed")
        android.util.Log.e("PIECEJOB_FCM", "Token = $token")
        android.util.Log.d("FCM_AUDIT", "FCM_GENERATED (onNewToken): Tokenacquired. Len=${token.length}, Thread=${Thread.currentThread().name}")
        scope.launch {
            val res = userRepository.updateFcmToken(token)
            android.util.Log.d("FCM_AUDIT", "FCM_ON_NEW_TOKEN_UPLOAD_RESULT: ${res.success}")
        }
    }

    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)
        android.util.Log.d("FCM_AUDIT", "MESSAGE_RECEIVED: from=${message.from}")
        android.util.Log.d("FCM_AUDIT", "PAYLOAD_DATA: ${message.data}")
        android.util.Log.d("FCM_AUDIT", "NOTIFICATION_BLOCK: ${message.notification?.title} / ${message.notification?.body}")

        val type = message.data["type"]
        val jobId = message.data["jobId"]
        
        if (type == "NEW_JOB_BROADCAST" && jobId != null) {
            Log.d("FCM_AUDIT", "Processing NEW_JOB_BROADCAST for $jobId")
            handleIncomingJob(message.data)
        } else if (type == "INCOMING_CALL") {
            Log.d("FCM_AUDIT", "Processing INCOMING_CALL")
            handleIncomingCall(message.data)
        } else if (type == "BROADCAST_CANCELLED" || type == "JOB_ASSIGNED_ELSEWHERE") {
            Log.d("FCM_AUDIT", "Processing Termination Signal: $type")
            handleBroadcastTermination(jobId)
        } else {
            val title = message.notification?.title ?: message.data["title"] ?: "PieceJob"
            val body = message.notification?.body ?: message.data["body"] ?: ""
            Log.d("FCM_AUDIT", "Showing standard notification: $title")
            showStandardNotification(title, body, message.data)
        }
    }

    private fun handleIncomingJob(data: Map<String, String>) {
        Log.d("FCM_AUDIT", "ENTRY: handleIncomingJob")
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
        Log.d("FCM_AUDIT", "Updating notificationState with Job ${incomingJob.jobId}")
        notificationState.showJobRequest(incomingJob)

        // 2. Play Sound & Vibration
        Log.d("FCM_AUDIT", "Triggering AlertManager")
        alertManager.start()

        // 3. Show Heads-Up Notification (For Background/Lockscreen)
        Log.d("FCM_AUDIT", "Triggering Heads-Up Notification")
        showHeadsUpNotification(incomingJob)
    }

    private fun handleIncomingCall(data: Map<String, String>) {
        val jobId = data["jobId"] ?: ""
        val callerId = data["callerId"] ?: ""
        val callId = data["callId"] ?: ""
        val callerName = data["callerName"] ?: "Someone"
        val callerPhone = data["callerPhone"] ?: ""
        val callerPhoto = data["callerPhoto"] ?: ""

        // 1. Intent to Open Call Screen (Accept flow)
        val acceptIntent = Intent(this, MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            putExtra("type", "INCOMING_CALL")
            putExtra("jobId", jobId)
            putExtra("callerId", callerId)
            putExtra("callId", callId)
            putExtra("callerName", callerName)
            putExtra("callerPhone", callerPhone)
            putExtra("callerPhoto", callerPhoto)
            putExtra("autoAccept", true)
        }
        
        val acceptPendingIntent = PendingIntent.getActivity(
            this, jobId.hashCode() + 1, acceptIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // 2. Intent to Reject (Broadcast flow)
        val rejectIntent = Intent(this, CallNotificationActionReceiver::class.java).apply {
            action = "ACTION_REJECT_CALL"
            putExtra("jobId", jobId)
            putExtra("callerId", callerId)
            putExtra("callId", callId)
        }
        
        val rejectPendingIntent = PendingIntent.getBroadcast(
            this, jobId.hashCode() + 2, rejectIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // 3. General Click Intent
        val contentIntent = Intent(this, MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            putExtra("type", "INCOMING_CALL")
            putExtra("jobId", jobId)
            putExtra("callerId", callerId)
            putExtra("callId", callId)
            putExtra("callerName", callerName)
            putExtra("callerPhone", callerPhone)
            putExtra("callerPhoto", callerPhoto)
        }

        val contentPendingIntent = PendingIntent.getActivity(
            this, jobId.hashCode(), contentIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val channelId = "piecejob_calls"
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(channelId, "Calls", NotificationManager.IMPORTANCE_HIGH).apply {
                description = "Urgent incoming calls"
                enableLights(true)
                enableVibration(true)
                setSound(null, null) 
                lockscreenVisibility = NotificationCompat.VISIBILITY_PUBLIC
            }
            notificationManager.createNotificationChannel(channel)
        }

        val builder = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("Incoming call")
            .setContentText("$callerName is calling you...")
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_CALL)
            .setAutoCancel(true)
            .setOngoing(true)
            .setVibrate(longArrayOf(0, 500, 200, 500))
            .setFullScreenIntent(contentPendingIntent, true) 
            .setContentIntent(contentPendingIntent)
            .addAction(android.R.drawable.ic_menu_call, "Accept", acceptPendingIntent)
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, "Reject", rejectPendingIntent)

        notificationManager.notify(jobId.hashCode(), builder.build())
    }

    private fun handleBroadcastTermination(jobId: String?) {
        Log.d("FCM_AUDIT", "ENTRY: handleBroadcastTermination for $jobId")
        notificationState.dismissJobRequest()
        alertManager.stop()
        val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        jobId?.hashCode()?.let { nm.cancel(it) }
    }

    private fun showHeadsUpNotification(job: IncomingJob) {
        Log.d("FCM_AUDIT", "ENTRY: showHeadsUpNotification")
        val intent = Intent(this, MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            putExtra("jobId", job.jobId)
            putExtra("type", "NEW_JOB_BROADCAST")
        }
        
        val pendingIntent = PendingIntent.getActivity(
            this, job.jobId.hashCode(), intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val channelId = "piecejob_broadcasts_v2"
        Log.d("FCM_AUDIT", "Building notification for channel: $channelId")
        val builder = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(job.serviceCode)
            .setContentText("Job from ${job.customerName ?: "Customer"}")
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_CALL)
            .setAutoCancel(true)
            .setOngoing(true)
            .setVibrate(longArrayOf(0, 500, 200, 500))
            .setContentIntent(pendingIntent)
            .setFullScreenIntent(pendingIntent, true) 

        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Log.d("FCM_AUDIT", "Ensuring channel $channelId exists")
            val channel = NotificationChannel(channelId, "Service Requests", NotificationManager.IMPORTANCE_HIGH).apply {
                description = "Urgent service requests for providers"
                enableLights(true)
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 500, 200, 500)
                setSound(null, null) 
            }
            notificationManager.createNotificationChannel(channel)
        }

        Log.d("FCM_AUDIT", "Executing notify() for ID ${job.jobId.hashCode()}")
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
