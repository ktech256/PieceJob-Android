package com.piecejob.core.ui.components

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.*

@Composable
fun LiveTrackingMap(
    providerLocation: Pair<Double, Double>?,
    customerLocation: Pair<Double, Double>,
    modifier: Modifier = Modifier
) {
    val customerLatLng = LatLng(customerLocation.first, customerLocation.second)
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(customerLatLng, 15f)
    }

    GoogleMap(
        modifier = modifier.fillMaxSize(),
        cameraPositionState = cameraPositionState,
        properties = MapProperties(isMyLocationEnabled = false), // We provide markers
        uiSettings = MapUiSettings(zoomControlsEnabled = false)
    ) {
        Marker(
            state = MarkerState(position = customerLatLng),
            title = "Your Location"
        )
        
        providerLocation?.let {
            val providerLatLng = LatLng(it.first, it.second)
            Marker(
                state = MarkerState(position = providerLatLng),
                title = "Provider",
                icon = com.google.android.gms.maps.model.BitmapDescriptorFactory.defaultMarker(com.google.android.gms.maps.model.BitmapDescriptorFactory.HUE_AZURE)
            )

            Polyline(
                points = listOf(customerLatLng, providerLatLng),
                color = Color(0xFFD32F2F),
                width = 5f
            )

            // Auto-zoom to fit both markers
            LaunchedEffect(it) {
                val bounds = com.google.android.gms.maps.model.LatLngBounds.builder()
                    .include(customerLatLng)
                    .include(providerLatLng)
                    .build()
                cameraPositionState.animate(
                    com.google.android.gms.maps.CameraUpdateFactory.newLatLngBounds(bounds, 100)
                )
            }
        }
    }
}
