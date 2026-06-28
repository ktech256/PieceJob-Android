package com.piecejob.core.socket

import android.util.Log
import com.piecejob.core.data.local.SessionManager
import com.google.android.gms.maps.model.LatLng
import io.socket.client.IO
import io.socket.client.Socket
import org.json.JSONArray
import org.json.JSONObject
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SocketManager @Inject constructor(
    private val sessionManager: SessionManager
) {
    private var socket: Socket? = null
    private val TAG = "SocketManager"

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
        Log.d("FORENSIC", "JOIN_ROOM_REQUESTED | Room: job_$jobId")
    }

    fun joinUser(userId: String) {
        socket?.emit("join_user", userId)
        Log.d("FORENSIC", "JOIN_ROOM_REQUESTED | Room: user_$userId")
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

    fun onStatusUpdated(callback: (String, JSONObject?) -> Unit) {
        socket?.on("status_updated") { args ->
            val data = args[0] as JSONObject
            val status = data.getString("status")
            Log.d("FORENSIC", "CUSTOMER_SOCKET_RECEIVED | Event: status_updated | Status: $status")
            callback(status, data.optJSONObject("providerInfo"))
        }
    }

    fun onJobAccepted(callback: (String, String, JSONObject?) -> Unit) {
        socket?.on("JOB_ACCEPTED") { args ->
            val data = args[0] as JSONObject
            val status = data.getString("status")
            Log.d("FORENSIC", "CUSTOMER_SOCKET_RECEIVED | Event: JOB_ACCEPTED | Status: $status")
            callback(data.getString("jobId"), data.getString("providerId"), data.optJSONObject("providerInfo"))
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

    fun onNewMessage(callback: (JSONObject) -> Unit) {
        socket?.on("new_message") { args ->
            callback(args[0] as JSONObject)
        }
    }

    fun clearListeners() {
        socket?.off("location_updated")
        socket?.off("route_updated")
        socket?.off("status_updated")
        socket?.off("JOB_ACCEPTED")
        socket?.off("NEW_JOB_BROADCAST")
        socket?.off("new_message")
    }

    fun disconnect() {
        clearListeners()
        socket?.disconnect()
        socket = null
    }
}
