package com.piecejob.core.socket

import android.util.Log
import com.piecejob.core.data.local.SessionManager
import com.google.android.gms.maps.model.LatLng
import io.socket.client.IO
import io.socket.client.Socket
import org.json.JSONArray
import org.json.JSONObject
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SocketManager @Inject constructor(
    private val sessionManager: SessionManager
) {
    private var socket: Socket? = null
    private val TAG = "SocketManager"
    private var currentActiveJobId: String? = null

    private val _statusEventFlow = MutableSharedFlow<StatusEvent>(extraBufferCapacity = 10)
    val statusEventFlow: SharedFlow<StatusEvent> = _statusEventFlow

    private val _broadcastEventFlow = MutableSharedFlow<JSONObject>(extraBufferCapacity = 10)
    val broadcastEventFlow: SharedFlow<JSONObject> = _broadcastEventFlow

    private val _messageEventFlow = MutableSharedFlow<JSONObject>(extraBufferCapacity = 10)
    val messageEventFlow: SharedFlow<JSONObject> = _messageEventFlow

    private val _callEventFlow = MutableSharedFlow<JSONObject>(extraBufferCapacity = 10)
    val callEventFlow: SharedFlow<JSONObject> = _callEventFlow

    private val _callSignalFlow = MutableSharedFlow<JSONObject>(extraBufferCapacity = 10)
    val callSignalFlow: SharedFlow<JSONObject> = _callSignalFlow

    fun connect(baseUrl: String) {
        if (socket?.connected() == true) return

        val token = sessionManager.getAuthToken() ?: return
        
        try {
            val options = IO.Options.builder()
                .setAuth(mapOf("token" to token))
                .build()
            
            socket = IO.socket(baseUrl, options)
            
            socket?.on(Socket.EVENT_CONNECT) {
                Log.d(TAG, "Socket connected")
                // 1. Join User Room (Global)
                sessionManager.getUserId()?.let { joinUser(it) }
                // 2. Join Workspace Room (Isolation)
                sessionManager.getCountryCode()?.let { joinWorkspace(it) }
                // 3. Re-join Active Job Room if session exists
                currentActiveJobId?.let { joinJob(it) }
            }
            
            socket?.on(Socket.EVENT_DISCONNECT) {
                Log.d(TAG, "Socket disconnected")
            }
            
            socket?.on(Socket.EVENT_CONNECT_ERROR) { args ->
                Log.e(TAG, "Socket connect error: ${args[0]}")
            }

            // PERMANENT GLOBAL LISTENERS (Don't use .off() on these)
            socket?.on("PROMOTIONS_UPDATED") { args ->
                try {
                    val data = args[0] as JSONObject
                    Log.d("FORENSIC", "GLOBAL_SOCKET_RECEIVED | PROMOTIONS_UPDATED | Workspace: ${data.optString("workspace")}")
                    // Reuse status flow but with a special jobId/status to trigger refresh
                    _statusEventFlow.tryEmit(StatusEvent("INTERNAL", "PROMOTIONS_REFRESH"))
                } catch (e: Exception) {
                    Log.e(TAG, "Error in PROMOTIONS_UPDATED listener", e)
                }
            }

            socket?.on("status_updated") { args ->
                try {
                    val data = args[0] as JSONObject
                    val jobId = data.getString("jobId")
                    val status = data.getString("status")
                    Log.d("FORENSIC", "GLOBAL_SOCKET_RECEIVED | status_updated | Job: $jobId | Status: $status")
                    _statusEventFlow.tryEmit(StatusEvent(jobId, status, data.optJSONObject("providerInfo")))
                } catch (e: Exception) {
                    Log.e(TAG, "Error in status_updated listener", e)
                }
            }

            socket?.on("NEW_JOB_BROADCAST") { args ->
                try {
                    val data = args[0] as JSONObject
                    Log.d("FORENSIC", "GLOBAL_SOCKET_RECEIVED | NEW_JOB_BROADCAST | Job: ${data.optString("jobId")}")
                    _broadcastEventFlow.tryEmit(data)
                } catch (e: Exception) {
                    Log.e(TAG, "Error in NEW_JOB_BROADCAST listener", e)
                }
            }

            socket?.on("JOB_ACCEPTED") { args ->
                try {
                    val data = args[0] as JSONObject
                    val jobId = data.getString("jobId")
                    Log.d("FORENSIC", "GLOBAL_SOCKET_RECEIVED | JOB_ACCEPTED | Job: $jobId")
                    _statusEventFlow.tryEmit(StatusEvent(jobId, "ACCEPTED", data.optJSONObject("providerInfo")))
                } catch (e: Exception) {
                    Log.e(TAG, "Error in JOB_ACCEPTED listener", e)
                }
            }

            socket?.on("new_message") { args ->
                try {
                    val data = args[0] as JSONObject
                    Log.d("FORENSIC", "CHAT_SOCKET_RECEIVED | Job: ${data.optString("jobId")}")
                    _messageEventFlow.tryEmit(data)
                } catch (e: Exception) {
                    Log.e(TAG, "Error in new_message listener", e)
                }
            }

            socket?.on("incoming_call_intent") { args ->
                try {
                    val data = args[0] as JSONObject
                    Log.d("FORENSIC", "CALL_SOCKET_RECEIVED | From: ${data.optString("callerId")}")
                    _callEventFlow.tryEmit(data)
                } catch (e: Exception) {
                    Log.e(TAG, "Error in incoming_call_intent listener", e)
                }
            }

            socket?.on("call_signal_received") { args ->
                try {
                    val data = args[0] as JSONObject
                    Log.d("FORENSIC", "CALL_SIGNAL_RECEIVED | Signal: ${data.optString("signal")}")
                    _callSignalFlow.tryEmit(data)
                } catch (e: Exception) {
                    Log.e(TAG, "Error in call_signal_received listener", e)
                }
            }

            socket?.connect()
        } catch (e: Exception) {
            Log.e(TAG, "Socket connection failed", e)
        }
    }

    fun joinJob(jobId: String) {
        currentActiveJobId = jobId
        socket?.emit("join_job", jobId)
        Log.d("FORENSIC", "JOIN_ROOM_REQUESTED | Room: job_$jobId")
    }

    fun leaveJob(jobId: String) {
        if (currentActiveJobId == jobId) currentActiveJobId = null
        // Server handles leaving room on emit or disconnect
    }

    fun joinUser(userId: String) {
        socket?.emit("join_user", userId)
        Log.d("FORENSIC", "JOIN_ROOM_REQUESTED | Room: user_$userId")
    }

    fun joinWorkspace(countryCode: String) {
        socket?.emit("join_workspace", countryCode)
        Log.d("FORENSIC", "JOIN_WORKSPACE_REQUESTED | Workspace: $countryCode")
    }

    fun sendHeartbeat(lat: Double, lng: Double, hardwareId: String, isMock: Boolean) {
        val userId = sessionManager.getUserId() ?: return
        val data = JSONObject().apply {
            put("userId", userId)
            val coords = JSONArray().apply {
                put(lng)
                put(lat)
            }
            put("coordinates", coords)
            put("hardwareId", hardwareId)
            put("isMockLocation", isMock)
        }
        socket?.emit("heartbeat", data)
    }

    fun updateLocation(jobId: String, lat: Double, lng: Double, heading: Float = 0f) {
        val userId = sessionManager.getUserId() ?: return
        val role = sessionManager.getRole() ?: "customer"
        val data = JSONObject().apply {
            put("jobId", jobId)
            val coords = JSONArray().apply {
                put(lng)
                put(lat)
            }
            put("coordinates", coords)
            put("heading", heading)
            put("userId", userId)
            put("role", role)
        }
        socket?.emit("update_location", data)
    }

    fun sendRoute(jobId: String, points: List<LatLng>) {
        val data = JSONObject().apply {
            put("jobId", jobId)
            val path = JSONArray()
            points.forEach { 
                val p = JSONObject()
                p.put("lat", it.latitude)
                p.put("lng", it.longitude)
                path.put(p)
            }
            put("points", path)
        }
        socket?.emit("route_updated", data)
    }

    fun sendSosGpsPing(incidentId: String, lat: Double, lng: Double) {
        val data = JSONObject().apply {
            put("incidentId", incidentId)
            val coords = JSONArray().apply {
                put(lng)
                put(lat)
            }
            put("coordinates", coords)
        }
        socket?.emit("sos_gps_ping", data)
    }

    fun sendCallSignal(jobId: String, receiverId: String, signal: String) {
        val data = JSONObject().apply {
            put("jobId", jobId)
            put("receiverId", receiverId)
            put("signal", signal)
            put("senderId", sessionManager.getUserId())
        }
        socket?.emit("call_signal", data)
    }

    fun onLocationUpdated(callback: (Double, Double, Float) -> Unit) {
        socket?.on("location_updated") { args ->
            val data = args[0] as JSONObject
            val coords = data.getJSONArray("coordinates")
            val heading = data.optDouble("heading", 0.0).toFloat()
            callback(coords.getDouble(1), coords.getDouble(0), heading)
        }
    }

    fun onRouteUpdated(callback: (List<LatLng>) -> Unit) {
        socket?.on("route_updated") { args ->
            val data = args[0] as JSONObject
            val path = data.getJSONArray("points")
            val points = mutableListOf<LatLng>()
            for (i in 0 until path.length()) {
                val p = path.getJSONObject(i)
                points.add(LatLng(p.getDouble("lat"), p.getDouble("lng")))
            }
            callback(points)
        }
    }

    fun onNewBroadcast(callback: (JSONObject) -> Unit) {
        socket?.on("NEW_JOB_BROADCAST") { args ->
            try {
                val data = args[0] as JSONObject
                callback(data)
            } catch (e: Exception) {
                Log.e(TAG, "Error parsing broadcast job", e)
            }
        }
    }

    fun clearListeners() {
        // We only clear ephemeral listeners, NOT global status/message/call listeners
        socket?.off("location_updated")
        socket?.off("route_updated")
    }

    fun disconnect() {
        clearListeners()
        socket?.disconnect()
        socket = null
    }
}

data class StatusEvent(val jobId: String, val status: String, val providerInfo: JSONObject? = null)
