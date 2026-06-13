package com.piecejob.core.socket

import android.util.Log
import com.google.gson.Gson
import com.piecejob.core.data.local.SessionManager
import com.piecejob.core.data.remote.dto.JobDto
import io.socket.client.IO
import io.socket.client.Socket
import org.json.JSONObject
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SocketManager @Inject constructor(
    private val sessionManager: SessionManager
) {
    private var socket: Socket? = null
    private val TAG = "SocketManager"
    private val gson = Gson()

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
            }
            
            socket?.on(Socket.EVENT_DISCONNECT) {
                Log.d(TAG, "Socket disconnected")
            }
            
            socket?.on(Socket.EVENT_CONNECT_ERROR) { args ->
                Log.e(TAG, "Socket connect error: ${args[0]}")
            }

            socket?.connect()
        } catch (e: Exception) {
            Log.e(TAG, "Socket connection failed", e)
        }
    }

    fun joinJob(jobId: String) {
        socket?.emit("join_job", jobId)
        Log.d(TAG, "Joined job room: $jobId")
    }

    fun sendHeartbeat(lat: Double, lng: Double, hardwareId: String, isMock: Boolean) {
        val userId = sessionManager.getUserId() ?: return
        val data = JSONObject().apply {
            put("userId", userId)
            put("coordinates", listOf(lng, lat))
            put("hardwareId", hardwareId)
            put("isMockLocation", isMock)
        }
        socket?.emit("heartbeat", data)
    }

    fun updateLocation(jobId: String, lat: Double, lng: Double) {
        val userId = sessionManager.getUserId() ?: return
        val role = sessionManager.getRole() ?: "customer"
        val data = JSONObject().apply {
            put("jobId", jobId)
            put("coordinates", listOf(lng, lat))
            put("userId", userId)
            put("role", role)
        }
        socket?.emit("update_location", data)
    }

    fun sendSosGpsPing(incidentId: String, lat: Double, lng: Double) {
        val data = JSONObject().apply {
            put("incidentId", incidentId)
            put("coordinates", listOf(lng, lat))
        }
        socket?.emit("sos_gps_ping", data)
    }

    fun onLocationUpdated(callback: (Double, Double) -> Unit) {
        socket?.on("location_updated") { args ->
            val data = args[0] as JSONObject
            val coords = data.getJSONArray("coordinates")
            callback(coords.getDouble(1), coords.getDouble(0))
        }
    }

    fun onStatusUpdated(callback: (String) -> Unit) {
        socket?.on("status_updated") { args ->
            val data = args[0] as JSONObject
            callback(data.getString("status"))
        }
    }

    fun onNewBroadcast(callback: (JobDto) -> Unit) {
        socket?.on("new_job_broadcast") { args ->
            try {
                val data = args[0] as JSONObject
                val job = gson.fromJson(data.toString(), JobDto::class.java)
                callback(job)
            } catch (e: Exception) {
                Log.e(TAG, "Error parsing broadcast job", e)
            }
        }
    }

    fun onNewMessage(callback: (JSONObject) -> Unit) {
        socket?.on("new_message") { args ->
            callback(args[0] as JSONObject)
        }
    }

    fun disconnect() {
        socket?.disconnect()
        socket = null
    }
}
