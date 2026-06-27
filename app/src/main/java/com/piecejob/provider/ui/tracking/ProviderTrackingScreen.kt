package com.piecejob.provider.ui.tracking

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.maps.android.compose.*
import com.piecejob.core.data.remote.dto.JobDto

import androidx.activity.compose.BackHandler
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.BitmapDescriptorFactory

@Composable
fun ProviderTrackingScreen(
    jobId: String,
    viewModel: ProviderTrackingViewModel = hiltViewModel(),
    onBack: () -> Unit,
    onNavigateToRating: (String) -> Unit
) {
    val context = LocalContext.current
    val job by viewModel.job.collectAsState()
    val providerLocation by viewModel.providerLocation.collectAsState()
    val eta by viewModel.eta.collectAsState()
    val distance by viewModel.distance.collectAsState()
    val showReminder by viewModel.showStartReminder.collectAsState()
    val error by viewModel.error.collectAsState()

    // Prevent accidental exit during active job
    BackHandler {
        onBack() // This will go to Home (Dashboard) while job remains active
    }

    LaunchedEffect(jobId) {
        viewModel.initTracking(jobId)
    }

    LaunchedEffect(job?.status) {
        if (job?.status == "COMPLETED") {
            onNavigateToRating(jobId)
        }
    }

    val customerLatLng = remember(job) {
        job?.location?.coordinates?.let { LatLng(it[1], it[0]) } ?: LatLng(0.0, 0.0)
    }

    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(customerLatLng, 15f)
    }

    // Auto-adjust camera with padding to see both points
    LaunchedEffect(providerLocation, customerLatLng) {
        if (providerLocation != null && customerLatLng.latitude != 0.0) {
            val bounds = LatLngBounds.builder()
                .include(providerLocation!!)
                .include(customerLatLng)
                .build()
            cameraPositionState.animate(
                CameraUpdateFactory.newLatLngBounds(bounds, 300)
            )
        }
    }

    if (showReminder) {
        AlertDialog(
            onDismissRequest = { },
            title = { Text("Forgotten to Start?", fontWeight = FontWeight.Black) },
            text = { Text("You've been at the customer for 25 minutes. Would you like to start the job now? It will auto-start in 15 seconds.") },
            confirmButton = {
                Button(onClick = { viewModel.startJob() }) { Text("START NOW") }
            },
            dismissButton = {
                TextButton(onClick = { /* Dismissed but viewModel might auto-start anyway */ }) { Text("NOT YET") }
            }
        )
    }

    Box(modifier = Modifier.fillMaxSize()) {
        if (customerLatLng.latitude != 0.0) {
            GoogleMap(
                modifier = Modifier.fillMaxSize(),
                cameraPositionState = cameraPositionState,
                uiSettings = MapUiSettings(zoomControlsEnabled = false, myLocationButtonEnabled = true),
                properties = MapProperties(isMyLocationEnabled = true)
            ) {
                // Customer Marker
                Marker(
                    state = MarkerState(position = customerLatLng),
                    title = "Customer",
                    icon = BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED)
                )

                // Provider Live Marker
                providerLocation?.let { loc ->
                    Marker(
                        state = MarkerState(position = loc),
                        title = "You",
                        icon = BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_BLUE)
                    )
                    
                    Polyline(
                        points = listOf(loc, customerLatLng),
                        color = Color(0xFF1976D2),
                        width = 10f
                    )
                }
            }
        } else {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        }

        // Top Status & Navigation Info
        Column(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 56.dp, start = 16.dp, end = 16.dp)
                .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Job Status Chip
            Surface(
                color = when(job?.status) {
                    "ACCEPTED" -> Color(0xFF1976D2)
                    "ARRIVED" -> Color(0xFFFFA000)
                    "STARTED" -> Color(0xFF4CAF50)
                    else -> Color.DarkGray
                },
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    text = job?.status?.replace("_", " ") ?: "LOADING...",
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
                    color = Color.White,
                    fontWeight = FontWeight.Black,
                    fontSize = 12.sp
                )
            }

            // ETA Card
            Card(
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
            ) {
                Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Navigation, contentDescription = null, tint = Color(0xFF1976D2))
                    Spacer(modifier = Modifier.width(16.dp))
                    Column {
                        Text(text = "ETA: $eta", fontWeight = FontWeight.Black, fontSize = 16.sp)
                        Text(text = "Distance: $distance", color = Color.Gray, fontSize = 12.sp)
                    }
                }
            }
        }

        // Bottom Action Panel
        Surface(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth(),
            shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
            color = Color.White,
            shadowElevation = 24.dp
        ) {
            Column(modifier = Modifier.padding(24.dp)) {
                job?.let { currentJob ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(modifier = Modifier.size(40.dp), shape = CircleShape, color = Color(0xFFF5F5F5)) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(Icons.Default.Build, contentDescription = null, modifier = Modifier.size(20.dp), tint = Color.Gray)
                            }
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(text = currentJob.serviceCode, fontWeight = FontWeight.Black, fontSize = 18.sp)
                            Text(text = currentJob.location?.address ?: "Near pickup", color = Color.Gray, fontSize = 12.sp)
                        }
                        Text(text = "R${currentJob.bookingFee}", fontWeight = FontWeight.ExtraBold, fontSize = 20.sp, color = Color(0xFF2E7D32))
                    }

                    Spacer(modifier = Modifier.height(24.dp))
                    
                    when (currentJob.status) {
                        "ACCEPTED" -> {
                            Button(
                                onClick = {
                                    val gmmIntentUri = Uri.parse("google.navigation:q=${customerLatLng.latitude},${customerLatLng.longitude}")
                                    val mapIntent = Intent(Intent.ACTION_VIEW, gmmIntentUri)
                                    mapIntent.setPackage("com.google.android.apps.maps")
                                    context.startActivity(mapIntent)
                                },
                                modifier = Modifier.fillMaxWidth().height(60.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50)),
                                shape = RoundedCornerShape(16.dp)
                            ) {
                                Icon(Icons.Default.Directions, contentDescription = null)
                                Spacer(modifier = Modifier.width(12.dp))
                                Text("OPEN NAVIGATION", fontWeight = FontWeight.Black)
                            }
                        }
                        "ARRIVED" -> {
                            Button(
                                onClick = { viewModel.startJob() },
                                modifier = Modifier.fillMaxWidth().height(60.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFA000)),
                                shape = RoundedCornerShape(16.dp)
                            ) {
                                Text("START WORK NOW", fontWeight = FontWeight.Black)
                            }
                        }
                        "STARTED" -> {
                            Button(
                                onClick = { viewModel.completeJob() },
                                modifier = Modifier.fillMaxWidth().height(60.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD32F2F)),
                                shape = RoundedCornerShape(16.dp)
                            ) {
                                Text("COMPLETE WORK", fontWeight = FontWeight.Black)
                            }
                        }
                    }
                }
            }
        }

        if (error != null) {
            Snackbar(modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 120.dp)) { Text(error!!) }
        }
    }
}

@Composable
fun JobHeader(job: JobDto) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = job.serviceCode, fontWeight = FontWeight.Black, fontSize = 18.sp)
            Text(text = job.location?.address ?: "Nearby", color = Color.Gray, fontSize = 12.sp)
        }
        Text(text = "R${job.bookingFee}", fontWeight = FontWeight.Bold, color = Color(0xFF2E7D32), fontSize = 18.sp)
    }
}
