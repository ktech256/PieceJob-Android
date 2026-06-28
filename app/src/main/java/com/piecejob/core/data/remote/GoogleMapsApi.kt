package com.piecejob.core.data.remote

import com.google.gson.annotations.SerializedName
import retrofit2.http.GET
import retrofit2.http.Query

interface GoogleMapsApi {
    @GET("maps/api/directions/json")
    suspend fun getDirections(
        @Query("origin") origin: String,
        @Query("destination") destination: String,
        @Query("key") apiKey: String,
        @Query("mode") mode: String = "driving"
    ): DirectionsResponse
}

data class DirectionsResponse(
    @SerializedName("routes") val routes: List<RouteDto>,
    @SerializedName("status") val status: String
)

data class RouteDto(
    @SerializedName("overview_polyline") val overviewPolyline: PolylineDto,
    @SerializedName("legs") val legs: List<LegDto>
)

data class LegDto(
    @SerializedName("distance") val distance: TextValueDto,
    @SerializedName("duration") val duration: TextValueDto
)

data class TextValueDto(
    @SerializedName("text") val text: String,
    @SerializedName("value") val value: Int
)

data class PolylineDto(
    @SerializedName("points") val points: String
)
