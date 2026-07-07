package com.piecejob.core.notification

import android.content.Context
import android.media.Ringtone
import android.media.RingtoneManager
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AlertManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private var ringtone: Ringtone? = null
    private val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator

    fun start() {
        android.util.Log.d("ALERT_AUDIT", "Starting Alert (Audio + Haptics)")
        stop() // Ensure clean start
        try {
            val notificationUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)
            ringtone = RingtoneManager.getRingtone(context, notificationUri)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                ringtone?.isLooping = true
            }
            ringtone?.play()

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator.vibrate(VibrationEffect.createWaveform(longArrayOf(0, 500, 200, 500), 0))
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(longArrayOf(0, 500, 200, 500), 0)
            }
        } catch (e: Exception) {
            android.util.Log.e("ALERT_AUDIT", "Alert failed: ${e.message}", e)
        }
    }

    fun stop() {
        android.util.Log.d("ALERT_AUDIT", "Stopping Alert")
        try {
            if (ringtone?.isPlaying == true) {
                ringtone?.stop()
            }
        } catch (e: Exception) {
            android.util.Log.e("ALERT_AUDIT", "Failed to stop ringtone safely: ${e.message}")
        } finally {
            ringtone = null
            vibrator.cancel()
        }
    }
}
