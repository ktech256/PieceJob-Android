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
        stop() // Ensure clean start
        try {
            val notificationUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)
            ringtone = RingtoneManager.getRingtone(context, notificationUri)
            ringtone?.play()

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator.vibrate(VibrationEffect.createWaveform(longArrayOf(0, 500, 200, 500), 0))
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(longArrayOf(0, 500, 200, 500), 0)
            }
        } catch (e: Exception) {
            // Log error
        }
    }

    fun stop() {
        ringtone?.stop()
        vibrator.cancel()
    }
}
