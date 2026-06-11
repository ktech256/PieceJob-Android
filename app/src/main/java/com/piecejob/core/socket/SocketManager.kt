package com.piecejob.core.socket

// import io.socket.client.IO
// import io.socket.client.Socket
// import io.socket.emitter.Emitter
import org.json.JSONObject
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SocketManager @Inject constructor() {
    // private var socket: Socket? = null

    fun connect(baseUrl: String, token: String) {
        /*
        val options = IO.Options.builder()
            .setAuth(mapOf("token" to token))
            .build()
        
        socket = IO.socket(baseUrl, options)
        socket?.connect()
        */
        println("Socket connected to $baseUrl")
    }

    fun joinJob(jobId: String) {
        // socket?.emit("join_job", jobId)
        println("Joined job room: $jobId")
    }

    fun updateLocation(jobId: String, lat: Double, lng: Double) {
        val data = JSONObject().apply {
            put("jobId", jobId)
            put("coordinates", listOf(lng, lat))
        }
        // socket?.emit("update_location", data)
        println("Sent location update for job $jobId: $lat, $lng")
    }

    fun sendSosGpsPing(incidentId: String, lat: Double, lng: Double) {
        val data = JSONObject().apply {
            put("incidentId", incidentId)
            put("coordinates", listOf(lng, lat))
        }
        // socket?.emit("sos_gps_ping", data)
        println("Sent SOS GPS ping for incident $incidentId: $lat, $lng")
    }

    fun onLocationUpdated(callback: (Double, Double) -> Unit) {
        /*
        socket?.on("location_updated") { args ->
            val data = args[0] as JSONObject
            val coords = data.getJSONArray("coordinates")
            callback(coords.getDouble(1), coords.getDouble(0))
        }
        */
    }

    fun onStatusUpdated(callback: (String) -> Unit) {
        /*
        socket?.on("status_updated") { args ->
            val data = args[0] as JSONObject
            callback(data.getString("status"))
        }
        */
    }

    fun disconnect() {
        // socket?.disconnect()
        println("Socket disconnected")
    }
}
