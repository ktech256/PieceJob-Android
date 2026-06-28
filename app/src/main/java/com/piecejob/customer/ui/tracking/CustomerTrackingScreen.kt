package com.piecejob.customer.ui.tracking

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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.maps.android.compose.*
import com.piecejob.core.ui.components.LiveTrackingMap
import kotlinx.coroutines.delay

@Composable
fun CustomerTrackingScreen(
    jobId: String,
    viewModel: JobTrackingViewModel = hiltViewModel(),
    onChatOpen: (String) -> Unit,
    onSosTrigger: () -> Unit,
    onBack: () -> Unit
) {
    val job by viewModel.job.collectAsState()
    val nearbyProviders by viewModel.nearbyProviders.collectAsState()
    val providerLocation by viewModel.providerLocation.collectAsState()
    val providerHeading by viewModel.providerHeading.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val error by viewModel.error.collectAsState()

    var showCancelDialog by remember { mutableStateOf(false) }

    val customerLatLng = remember(job) {
        job?.location?.coordinates?.let { LatLng(it[1], it[0]) } ?: LatLng(0.0, 0.0)
    }

    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(customerLatLng, 15f)
    }

    // Auto-adjust camera to fit both customer and provider when assigned
    LaunchedEffect(customerLatLng, providerLocation) {
        if (providerLocation != null) {
            val providerLatLng = LatLng(providerLocation!!.first, providerLocation!!.second)
            val bounds = LatLngBounds.builder()
                .include(customerLatLng)
                .include(providerLatLng)
                .build()
            cameraPositionState.animate(
                com.google.android.gms.maps.CameraUpdateFactory.newLatLngBounds(bounds, 150)
            )
        } else {
            cameraPositionState.animate(
                com.google.android.gms.maps.CameraUpdateFactory.newLatLngZoom(customerLatLng, 15f)
            )
        }
    }

    LaunchedEffect(jobId) {
        viewModel.initTracking(jobId)
    }

    LaunchedEffect(job?.status) {
        if (job?.status == "CANCELLED" || job?.status == "COMPLETED") {
            delay(2000)
            onBack()
        }
    }

    if (showCancelDialog) {
        AlertDialog(
            onDismissRequest = { showCancelDialog = false },
            title = { Text("Cancel Request?", fontWeight = FontWeight.Black) },
            text = { Text("Are you sure you want to cancel this request? This action cannot be undone.") },
            confirmButton = {
                Button(
                    onClick = {
                        showCancelDialog = false
                        viewModel.cancelJob()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD32F2F))
                ) { Text("YES, CANCEL") }
            },
            dismissButton = {
                TextButton(onClick = { showCancelDialog = false }) { Text("NO, KEEP IT") }
            }
        )
    }

    Box(modifier = Modifier.fillMaxSize()) {
        // MAP
        GoogleMap(
            modifier = Modifier.fillMaxSize(),
            cameraPositionState = cameraPositionState,
            uiSettings = MapUiSettings(zoomControlsEnabled = false, myLocationButtonEnabled = true),
            properties = MapProperties(isMyLocationEnabled = true)
        ) {
            // Customer Marker
            Marker(
                state = MarkerState(position = customerLatLng),
                title = "Your Location",
                icon = com.google.android.gms.maps.model.BitmapDescriptorFactory.defaultMarker(com.google.android.gms.maps.model.BitmapDescriptorFactory.HUE_RED)
            )

            // Nearby Providers (Only shown when searching)
            if (providerLocation == null) {
                nearbyProviders.forEach { p ->
                    Marker(
                        state = MarkerState(position = LatLng(p.location.coordinates[1], p.location.coordinates[0])),
                        title = "${p.firstName}",
                        alpha = 0.6f,
                        icon = com.google.android.gms.maps.model.BitmapDescriptorFactory.defaultMarker(com.google.android.gms.maps.model.BitmapDescriptorFactory.HUE_GREEN)
                    )
                }
            }

            // Assigned Provider Marker
            providerLocation?.let { loc ->
                Marker(
                    state = MarkerState(position = LatLng(loc.first, loc.second)),
                    title = "Your Professional",
                    rotation = providerHeading,
                    icon = com.google.android.gms.maps.model.BitmapDescriptorFactory.defaultMarker(com.google.android.gms.maps.model.BitmapDescriptorFactory.HUE_AZURE)
                )
                
                // Draw Route Polyline (Simplified straight line for now, or use actual route API if available)
                Polyline(
                    points = listOf(customerLatLng, LatLng(loc.first, loc.second)),
                    color = Color(0xFFD32F2F),
                    width = 8f
                )
            }
        }

        // Top Status Bar
        AnimatedVisibility(
            visible = true,
            enter = slideInVertically() + fadeIn(),
            modifier = Modifier.align(Alignment.TopCenter)
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 48.dp, start = 16.dp, end = 16.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFD32F2F)),
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val providerName = job?.providerInfo?.firstName ?: "Provider"
                    val statusText = when (job?.status) {
                        "BROADCASTED", "BROADCASTING" -> "Broadcasting request..."
                        "ACCEPTED" -> "$providerName accepted your request!"
                        "EN_ROUTE" -> "$providerName is on the way"
                        "ARRIVED" -> "$providerName has arrived"
                        "STARTED" -> "$providerName started the job"
                        "COMPLETED" -> "Job Completed!"
                        "CANCELLED" -> "Job Cancelled"
                        else -> "Connecting..."
                    }
                    
                    if (job?.status != null) {
                        android.util.Log.d("ForensicLog", "UI_STATUS_UPDATE | Status: ${job?.status} | Text: $statusText")
                    }
                    
                    Column(modifier = Modifier.weight(1f)) {
                        Text(text = statusText, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Black)
                        Text(text = "Job #${jobId.takeLast(6).uppercase()}", color = Color.White.copy(alpha = 0.8f), fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    }
                    
                    if (job?.status != "COMPLETED" && job?.status != "CANCELLED") {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White, strokeWidth = 2.dp)
                    }
                }
            }
        }

        // Bottom Info Panel
        AnimatedVisibility(
            visible = job != null,
            enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
            modifier = Modifier.align(Alignment.BottomCenter)
        ) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 24.dp)
            ) {
                Column(modifier = Modifier.padding(24.dp)) {
                    val isAssigned = job?.status != null && 
                                   job?.status != "BROADCASTED" && 
                                   job?.status != "BROADCASTING" && 
                                   job?.status != "PAYMENT_PENDING" && 
                                   job?.status != "BOOKING_FEE_PAID" && 
                                   job?.status != "DRAFT"

                    if (!isAssigned) {
                        // Searching UI
                        SearchingPanel(job?.serviceCode ?: "Service", nearbyProviders.size)
                    } else {
                        // Assigned Provider UI
                        AssignedProviderPanel(
                            job = job!!,
                            onChatOpen = onChatOpen,
                            onSosTrigger = onSosTrigger
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(24.dp))
                    
                    if (job?.status != "COMPLETED" && job?.status != "STARTED" && job?.status != "CANCELLED") {
                        Button(
                            onClick = { showCancelDialog = true },
                            modifier = Modifier.fillMaxWidth().height(56.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF5F5F5)),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Text(text = "Cancel Request", color = Color.DarkGray, fontWeight = FontWeight.Black)
                        }
                    } else if (job?.status == "STARTED") {
                        Text(
                            text = "Job is in progress and cannot be cancelled.",
                            modifier = Modifier.fillMaxWidth(),
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                            fontSize = 12.sp,
                            color = Color.Gray
                        )
                    }
                }
            }
        }
        
        if (error != null) {
            Snackbar(
                modifier = Modifier.align(Alignment.BottomCenter).padding(16.dp),
                action = { TextButton(onClick = { onBack() }) { Text("DISMISS", color = Color.White) } }
            ) { Text(error!!) }
        }
    }
}

@Composable
fun SearchingPanel(serviceName: String, nearbyCount: Int) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
        LinearProgressIndicator(
            modifier = Modifier.fillMaxWidth().clip(CircleShape),
            color = Color(0xFFD32F2F),
            trackColor = Color(0xFFFDECEA)
        )
        Spacer(modifier = Modifier.height(24.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Build, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text(text = serviceName, fontWeight = FontWeight.Black, fontSize = 18.sp)
        }
        Text(
            text = if (nearbyCount > 0) "$nearbyCount providers notified nearby" else "Broadcasting request to best professionals",
            color = Color.Gray,
            fontSize = 14.sp
        )
    }
}

@Composable
fun AssignedProviderPanel(
    job: com.piecejob.core.data.remote.dto.JobDto,
    onChatOpen: (String) -> Unit,
    onSosTrigger: () -> Unit
) {
    Column {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier.size(64.dp).clip(CircleShape).background(Color(0xFFFDECEA)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Person, contentDescription = null, tint = Color(0xFFD32F2F), modifier = Modifier.size(32.dp))
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = job.providerInfo?.let { "${it.firstName} ${it.lastName}" } ?: "Provider assigned",
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 20.sp,
                    lineHeight = 22.sp
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Star, contentDescription = null, tint = Color(0xFFFFA000), modifier = Modifier.size(14.dp))
                    Text(
                        text = " ${job.providerInfo?.ratingAvg ?: "4.9"} • ${job.providerInfo?.jobsCompleted ?: "12"} Jobs",
                        color = Color.Gray,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilledIconButton(
                    onClick = { /* Call logic */ },
                    modifier = Modifier.size(48.dp),
                    colors = IconButtonDefaults.filledIconButtonColors(containerColor = Color(0xFFE8F5E9))
                ) {
                    Icon(Icons.Default.Phone, contentDescription = "Call", tint = Color(0xFF2E7D32))
                }
                
                FilledIconButton(
                    onClick = { job.providerId?.let { onChatOpen(it) } },
                    modifier = Modifier.size(48.dp),
                    colors = IconButtonDefaults.filledIconButtonColors(containerColor = Color(0xFFE3F2FD))
                ) {
                    Icon(Icons.Default.Email, contentDescription = "Message", tint = Color(0xFF1976D2))
                }
            }
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Button(
                onClick = onSosTrigger,
                modifier = Modifier.weight(1f).height(56.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD32F2F)),
                shape = RoundedCornerShape(16.dp)
            ) {
                Text("SOS", fontWeight = FontWeight.Black)
            }
            
            Surface(
                modifier = Modifier.weight(2f).height(56.dp),
                shape = RoundedCornerShape(16.dp),
                color = Color(0xFFF5F5F5)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    val etaDisplay = when (job?.status) {
                        "ARRIVED", "STARTED", "IN_PROGRESS" -> "Provider has arrived"
                        "COMPLETED" -> "Job Completed"
                        "CANCELLED" -> "Job Cancelled"
                        else -> "ETA: 8 mins" // Default or calculate from live location
                    }
                    Text(text = etaDisplay, fontWeight = FontWeight.Black, color = Color.Black)
                }
            }
        }
    }
}
