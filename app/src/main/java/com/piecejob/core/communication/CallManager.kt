package com.piecejob.core.communication

import android.app.Application
import android.util.Log
import io.livekit.android.LiveKit
import io.livekit.android.events.RoomEvent
import io.livekit.android.room.Room
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CallManager @Inject constructor(
    private val application: Application
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    
    private var room: Room? = null
    private val url = "wss://piecejob-125so6f8.livekit.cloud"

    private val _isCallActive = MutableStateFlow(false)
    val isCallActive: StateFlow<Boolean> = _isCallActive.asStateFlow()

    private val _connectionStatus = MutableStateFlow("Idle")
    val connectionStatus: StateFlow<String> = _connectionStatus.asStateFlow()

    private val _isMuted = MutableStateFlow(false)
    val isMuted: StateFlow<Boolean> = _isMuted.asStateFlow()

    private val _isSpeakerOn = MutableStateFlow(false)
    val isSpeakerOn: StateFlow<Boolean> = _isSpeakerOn.asStateFlow()

    var activeJobId: String? = null
    var targetUserId: String? = null
    var currentCallId: String? = null

    fun setCallActive(active: Boolean) {
        _isCallActive.value = active
    }

    fun connect(token: String) {
        if (room != null) return
        
        val currentRoom = LiveKit.create(application)
        room = currentRoom
        
        scope.launch {
            currentRoom.events.events.collect { event ->
                when (event) {
                    is RoomEvent.Connected -> {
                        Log.d("CallManager", "LiveKit Connected")
                        _connectionStatus.value = "Connected"
                        scope.launch {
                            currentRoom.localParticipant.setMicrophoneEnabled(!_isMuted.value)
                        }
                    }
                    is RoomEvent.Disconnected -> {
                        Log.d("CallManager", "LiveKit Disconnected")
                        _connectionStatus.value = "Disconnected"
                        cleanup()
                    }
                    is RoomEvent.Reconnecting -> {
                        _connectionStatus.value = "Reconnecting..."
                    }
                    is RoomEvent.Reconnected -> {
                        _connectionStatus.value = "Connected"
                    }
                    else -> {}
                }
            }
        }

        scope.launch {
            try {
                _connectionStatus.value = "Connecting..."
                currentRoom.connect(url, token)
            } catch (e: Exception) {
                Log.e("CallManager", "Connection failed", e)
                _connectionStatus.value = "Failed: ${e.message}"
                cleanup()
            }
        }
    }

    fun toggleMute() {
        val newMute = !_isMuted.value
        _isMuted.value = newMute
        scope.launch {
            room?.localParticipant?.setMicrophoneEnabled(!newMute)
        }
    }

    fun toggleSpeaker() {
        _isSpeakerOn.value = !_isSpeakerOn.value
    }

    fun disconnect() {
        scope.launch {
            room?.disconnect()
            cleanup()
        }
    }

    private fun cleanup() {
        room = null
        _isCallActive.value = false
        _isMuted.value = false
        _isSpeakerOn.value = false
        _connectionStatus.value = "Idle"
        activeJobId = null
        targetUserId = null
        currentCallId = null
    }
}
