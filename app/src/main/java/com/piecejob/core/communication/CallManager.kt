package com.piecejob.core.communication

import android.app.Application
import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.os.Build
import android.util.Log
import io.livekit.android.LiveKit
import io.livekit.android.events.RoomEvent
import io.livekit.android.room.Room
import io.livekit.android.room.track.RemoteAudioTrack
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CallManager @Inject constructor(
    private val application: Application
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    
    private val room: Room? get() = _room
    private var _room: Room? = null
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

    private val audioManager = application.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    private var originalMode: Int = AudioManager.MODE_NORMAL
    private var focusRequest: AudioFocusRequest? = null

    fun setCallActive(active: Boolean) {
        _isCallActive.value = active
        if (active) {
            _connectionStatus.value = "Calling..."
            Log.d("FORENSIC", "CALL_MANAGER | setCallActive(true) -> Status: Calling...")
        }
    }

    private val _isRemoteJoined = MutableStateFlow(false)
    val isRemoteJoined: StateFlow<Boolean> = _isRemoteJoined.asStateFlow()

    fun connect(token: String) {
        scope.launch {
            if (_room != null) {
                Log.d("FORENSIC", "CALL_MANAGER | Disconnecting existing room before new connection")
                _room?.disconnect()
            }
            _room = null
            _isRemoteJoined.value = false
            
            // Initial UI state for media connection phase
            _connectionStatus.value = "Connecting..."
            Log.d("FORENSIC", "CALL_MANAGER | connect() started -> Status: Connecting...")
            
            Log.d("FORENSIC", "CALL_MANAGER | Room Joining. URL: $url | Token Len: ${token.length}")
            val currentRoom = LiveKit.create(application)
            _room = currentRoom
            
            setupAudioSession()
            
            launch {
                currentRoom.events.events.collect { event ->
                    Log.d("FORENSIC", "CALL_MANAGER | RoomEvent: ${event.javaClass.simpleName}")
                    when (event) {
                        is RoomEvent.Connected -> {
                            Log.d("FORENSIC", "CALL_MANAGER | Room Connected. Remote participants: ${currentRoom.remoteParticipants.size}")
                            if (currentRoom.remoteParticipants.isNotEmpty()) {
                                _isRemoteJoined.value = true
                                _connectionStatus.value = "Connected"
                                Log.d("FORENSIC", "CALL_MANAGER | Remote already present -> Status: Connected")
                            } else {
                                _connectionStatus.value = "Calling..."
                                Log.d("FORENSIC", "CALL_MANAGER | Waiting for remote -> Status: Calling...")
                            }
                            
                            launch {
                                val hasPermission = androidx.core.content.ContextCompat.checkSelfPermission(
                                    application,
                                    android.Manifest.permission.RECORD_AUDIO
                                ) == android.content.pm.PackageManager.PERMISSION_GRANTED
                                
                                Log.d("FORENSIC", "CALL_MANAGER | Microphone Permission: $hasPermission")
                                if (hasPermission) {
                                    Log.d("FORENSIC", "CALL_MANAGER | Publishing Local Audio. Muted: ${_isMuted.value}")
                                    currentRoom.localParticipant.setMicrophoneEnabled(!_isMuted.value)
                                } else {
                                    Log.e("FORENSIC", "CALL_MANAGER | Permission Denied - Cannot publish audio")
                                }
                            }
                        }
                        is RoomEvent.Disconnected -> {
                            Log.d("FORENSIC", "CALL_MANAGER | Room Disconnected")
                            _connectionStatus.value = "Ended"
                            cleanup()
                        }
                        is RoomEvent.Reconnecting -> {
                            Log.d("FORENSIC", "CALL_MANAGER | Room Reconnecting")
                            _connectionStatus.value = "Reconnecting..."
                        }
                        is RoomEvent.Reconnected -> {
                            Log.d("FORENSIC", "CALL_MANAGER | Room Reconnected")
                            if (currentRoom.remoteParticipants.isNotEmpty()) {
                                _isRemoteJoined.value = true
                                _connectionStatus.value = "Connected"
                            } else {
                                _connectionStatus.value = "Calling..."
                            }
                        }
                        is RoomEvent.ParticipantConnected -> {
                            Log.d("FORENSIC", "CALL_MANAGER | Remote Joined: ${event.participant.identity}")
                            _isRemoteJoined.value = true
                            // Don't transition to Connected until we actually see their audio or they joined
                            // Actually, in WhatsApp, it's Connected when they join.
                            _connectionStatus.value = "Connected"
                            Log.d("FORENSIC", "CALL_MANAGER | Status -> Connected")
                        }
                        is RoomEvent.ParticipantDisconnected -> {
                            Log.d("FORENSIC", "CALL_MANAGER | Remote Left: ${event.participant.identity}")
                            if (currentRoom.remoteParticipants.isEmpty()) {
                                _isRemoteJoined.value = false
                                _connectionStatus.value = "Ended"
                                disconnect("Ended")
                            }
                        }
                        is RoomEvent.TrackSubscribed -> {
                            val track = event.track
                            Log.d("FORENSIC", "CALL_MANAGER | Track Subscribed: ${track.sid} | Type: ${track.kind} | Participant: ${event.participant?.identity}")
                            if (track is RemoteAudioTrack) {
                                Log.d("FORENSIC", "CALL_MANAGER | Remote Audio Subscribed -> Connected")
                                _connectionStatus.value = "Connected"
                            }
                        }
                        is RoomEvent.FailedToConnect -> {
                            Log.e("FORENSIC", "CALL_MANAGER | Connection Failed: ${event.error}")
                            _connectionStatus.value = "Failed"
                            cleanup(isError = true)
                        }
                        else -> {}
                    }
                }
            }

            try {
                currentRoom.connect(url, token)
                Log.d("FORENSIC", "CALL_MANAGER | Room.connect() returned")
            } catch (e: Exception) {
                Log.e("FORENSIC", "CALL_MANAGER | connect() crash: ${e.message}", e)
                _connectionStatus.value = "Failed"
                cleanup(isError = true)
            }
        }
    }

    private fun setupAudioSession() {
        Log.d("FORENSIC", "CALL_MANAGER | setupAudioSession()")
        originalMode = audioManager.mode
        audioManager.mode = AudioManager.MODE_IN_COMMUNICATION
        @Suppress("DEPRECATION")
        audioManager.isSpeakerphoneOn = _isSpeakerOn.value
        
        requestAudioFocus()
    }

    private fun requestAudioFocus() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val playbackAttributes = AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_VOICE_COMMUNICATION)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                    .build()
                val request = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT)
                    .setAudioAttributes(playbackAttributes)
                    .setAcceptsDelayedFocusGain(true)
                    .build()
                focusRequest = request
                audioManager.requestAudioFocus(request)
            } else {
                @Suppress("DEPRECATION")
                audioManager.requestAudioFocus(null, AudioManager.STREAM_VOICE_CALL, AudioManager.AUDIOFOCUS_GAIN_TRANSIENT)
            }
        } catch (e: Exception) {
            Log.e("FORENSIC", "CALL_MANAGER | requestAudioFocus failed", e)
        }
    }

    private fun releaseAudioFocus() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                focusRequest?.let { audioManager.abandonAudioFocusRequest(it) }
                focusRequest = null
            } else {
                @Suppress("DEPRECATION")
                audioManager.abandonAudioFocus(null)
            }
        } catch (e: Exception) {
            Log.e("FORENSIC", "CALL_MANAGER | releaseAudioFocus failed", e)
        }
    }

    fun toggleMute() {
        val newMute = !_isMuted.value
        _isMuted.value = newMute
        Log.d("FORENSIC", "CALL_MANAGER | toggleMute($newMute)")
        scope.launch {
            _room?.localParticipant?.setMicrophoneEnabled(!newMute)
        }
    }

    fun toggleSpeaker() {
        val newSpeaker = !_isSpeakerOn.value
        _isSpeakerOn.value = newSpeaker
        Log.d("FORENSIC", "CALL_MANAGER | toggleSpeaker($newSpeaker)")
        @Suppress("DEPRECATION")
        audioManager.isSpeakerphoneOn = newSpeaker
    }

    fun disconnect(terminalStatus: String? = null) {
        Log.d("FORENSIC", "CALL_MANAGER | disconnect(status=$terminalStatus)")
        scope.launch {
            _room?.disconnect()
            cleanup(terminalStatus = terminalStatus)
        }
    }

    private fun cleanup(isError: Boolean = false, terminalStatus: String? = null) {
        Log.d("FORENSIC", "CALL_MANAGER | cleanup(isError=$isError, status=$terminalStatus)")
        _room = null
        _isCallActive.value = false
        _isMuted.value = false
        _isSpeakerOn.value = false
        _isRemoteJoined.value = false
        
        if (terminalStatus != null) {
            _connectionStatus.value = terminalStatus
        } else if (isError) {
            _connectionStatus.value = "Failed"
        } else {
            _connectionStatus.value = "Idle"
        }
        
        activeJobId = null
        targetUserId = null
        currentCallId = null
        
        audioManager.mode = originalMode
        @Suppress("DEPRECATION")
        audioManager.isSpeakerphoneOn = false
        releaseAudioFocus()
    }
}
