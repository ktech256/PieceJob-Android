package com.piecejob.core.ui.components

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
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
                // In full implementation, use a custom car/provider icon here
            )
        }
    }
}
