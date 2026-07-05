package com.piecejob.customer.ui.tracking

import androidx.compose.animation.*
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
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
    onCallOpen: (String, String, String, String?) -> Unit,
    onSosTrigger: () -> Unit,
    onNavigateToRating: (String) -> Unit,
    onBack: () -> Unit
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val job by viewModel.job.collectAsState()

    DisposableEffect(job?.id, job?.status) {
        val currentJob = job
        if (currentJob != null && isTrackingCapable(currentJob.status)) {
            android.util.Log.d("LOCATION_AUDIT", "Tracking Screen Active. Starting LocationService.")
            com.piecejob.core.location.LocationService.activeJobId = currentJob.id
            com.piecejob.core.location.LocationService.startService(context)
        }

        onDispose {
            android.util.Log.d("LOCATION_AUDIT", "Tracking Screen Disposed or Job changed. Stopping LocationService.")
            com.piecejob.core.location.LocationService.stopService(context)
        }
    }
    
    // FORENSIC: Track recompositions
    LaunchedEffect(job?.status) {
        if (job?.status != null) {
            android.util.Log.d("FORENSIC", "TRACKING_JOB_STATUS_CHANGED | Status: ${job?.status}")
        }
    }
    
    SideEffect {
        android.util.Log.d("FORENSIC", "TRACKING_COMPOSE_RECOMPOSED | Status: ${job?.status}")
    }
    val nearbyProviders by viewModel.nearbyProviders.collectAsState()
    val providerLocation by viewModel.providerLocation.collectAsState()
    val providerHeading by viewModel.providerHeading.collectAsState()
    val animatedHeading by animateFloatAsState(
        targetValue = providerHeading,
        animationSpec = tween(durationMillis = 1000, easing = LinearEasing),
        label = "HeadingAnimation"
    )
    val routePoints by viewModel.routePoints.collectAsState()
    val eta by viewModel.eta.collectAsState()
    val distance by viewModel.distance.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val error by viewModel.error.collectAsState()

    var showCancelDialog by remember { mutableStateOf(false) }
    var hasAutoNavigatedToNegotiation by remember { mutableStateOf(false) }

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

    LaunchedEffect(job?.status, phase) {
        if (isNegotiating && !hasAutoNavigatedToNegotiation) {
            hasAutoNavigatedToNegotiation = true
            onChatOpen(job?.providerId ?: "")
        }

        if (job?.status == "CANCELLED") {
            android.util.Log.d("FORENSIC", "TRACKING_EXIT | Job Cancelled. Returning to dashboard.")
            delay(1000)
            onBack()
        } else if (job?.status == "COMPLETED") {
            android.util.Log.d("FORENSIC", "TRACKING_EXIT | Job Completed. Moving to rating.")
            onNavigateToRating(jobId)
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

    val isTerminalState = job?.status == "COMPLETED" || job?.status == "CANCELLED" || job?.status == "RATED"
    val phase = job?.currentNegotiationPhase ?: "NEUTRAL"
    val isNegotiating = listOf("PHOTO_REQUEST", "WAITING_FOR_PHOTOS", "PHOTOS_UPLOADED", "PRICE_PROPOSAL", "WAITING_FOR_CUSTOMER", "WAITING_FOR_PROVIDER", "PRICE_ACCEPTED").contains(phase)

    Box(modifier = Modifier.fillMaxSize()) {
        // MAP
        GoogleMap(
            modifier = Modifier.fillMaxSize(),
            cameraPositionState = cameraPositionState,
            uiSettings = MapUiSettings(
                zoomControlsEnabled = false, 
                myLocationButtonEnabled = !isTerminalState && !isNegotiating,
                scrollGesturesEnabled = !isTerminalState && !isNegotiating,
                zoomGesturesEnabled = !isTerminalState && !isNegotiating,
                tiltGesturesEnabled = !isTerminalState && !isNegotiating,
                rotationGesturesEnabled = !isTerminalState && !isNegotiating
            ),
            properties = MapProperties(isMyLocationEnabled = !isTerminalState && !isNegotiating)
        ) {
            // Customer Destination Marker
            if (customerLatLng.latitude != 0.0) {
                Marker(
                    state = MarkerState(position = customerLatLng),
                    title = "Your Location",
                    icon = BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED)
                )
            }

            // Provider Live Marker
            if (providerLocation != null) {
                val providerLatLng = LatLng(providerLocation!!.first, providerLocation!!.second)
                Marker(
                    state = MarkerState(position = providerLatLng),
                    title = "Your Provider",
                    rotation = animatedHeading,
                    anchor = androidx.compose.ui.geometry.Offset(0.5f, 0.5f),
                    icon = BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE)
                )
            }

            // Route Polyline
            if (routePoints.isNotEmpty()) {
                Polyline(
                    points = routePoints,
                    color = Color(0xFFD32F2F),
                    width = 12f
                )
            }
        }

        if (isNegotiating) {
            Box(
                modifier = Modifier.fillMaxSize().background(Color.White),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(32.dp)) {
                    Icon(Icons.Default.HourglassEmpty, contentDescription = null, modifier = Modifier.size(64.dp), tint = Color(0xFFFFA000))
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Negotiation Session", fontWeight = FontWeight.Black, fontSize = 20.sp)
                    Text(
                        text = if (job?.status == "ACCEPTED") "Price agreed. Waiting for provider to confirm dispatch." else "Finalize task details and price agreement to proceed with tracking.",
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        color = Color.Gray,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    Button(
                        onClick = { onChatOpen(job?.providerId ?: "") },
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFA000))
                    ) {
                        Text("RESUME NEGOTIATION", fontWeight = FontWeight.Black)
                    }
                }
            }
        }

        // Top Status Bar (Mirroring Provider Layout with Back Arrow inside)
        AnimatedVisibility(
            visible = true,
            enter = slideInVertically() + fadeIn(),
            modifier = Modifier
                .align(Alignment.TopCenter)
                .statusBarsPadding()
                .padding(16.dp)
        ) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = Color.White.copy(alpha = 0.95f),
                shape = RoundedCornerShape(12.dp),
                shadowElevation = 8.dp
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onBack, modifier = Modifier.size(40.dp)) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.Black)
                    }
                    
                    Spacer(modifier = Modifier.width(8.dp))
                    
                    val providerName = job?.providerInfo?.firstName ?: "Provider"
                    val statusText = when (job?.status) {
                        "BROADCASTED", "BROADCASTING" -> "Broadcasting request..."
                        "PROVIDER_ACCEPTED" -> "Negotiating with $providerName..."
                        "ACCEPTED" -> "$providerName accepted your request!"
                        "EN_ROUTE" -> "$providerName is on the way"
                        "ARRIVED" -> "$providerName has arrived"
                        "STARTED", "IN_PROGRESS" -> "$providerName started the work"
                        "COMPLETED" -> "Job Completed!"
                        "CANCELLED" -> "Job Cancelled"
                        else -> "Connecting..."
                    }
                    
                    Column(modifier = Modifier.weight(1f)) {
                        Text(text = statusText, color = Color.Black, fontSize = 14.sp, fontWeight = FontWeight.ExtraBold)
                        if (job?.status == "ACCEPTED" || job?.status == "EN_ROUTE") {
                            Text(text = "ETA: $eta ($distance)", color = Color.Gray, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        } else {
                            Text(text = "Job #${jobId.takeLast(6).uppercase()}", color = Color.Gray, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                    }
                    
                    if (job?.status != "COMPLETED" && job?.status != "CANCELLED") {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color(0xFFD32F2F), strokeWidth = 2.dp)
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
                        SearchingPanel(job?.serviceName ?: job?.serviceCode ?: "Service", nearbyProviders.size)
                    } else {
                        val isTerminalState = job?.status == "COMPLETED" || job?.status == "CANCELLED" || job?.status == "RATED"
    val isNegotiating = job?.status == "PROVIDER_ACCEPTED" || job?.priceStatus == "PENDING"
                        AssignedProviderPanel(
                            job = job!!,
                            eta = eta,
                            isTerminalState = isTerminalState,
                            onChatOpen = onChatOpen,
                            onCallOpen = onCallOpen,
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
            // Snackbar removed for consistency with Provider app.
        }
    }
}

private fun isTrackingCapable(status: String): Boolean {
    return when (status) {
        "ACCEPTED", "EN_ROUTE", "ARRIVED", "STARTED", "IN_PROGRESS" -> true
        else -> false
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
    eta: String,
    isTerminalState: Boolean,
    onChatOpen: (String) -> Unit,
    onCallOpen: (String, String, String, String?) -> Unit,
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
                var lastClickTime by remember { mutableLongStateOf(0L) }
                FilledIconButton(
                    onClick = { 
                        val now = System.currentTimeMillis()
                        if (now - lastClickTime < 1000) return@FilledIconButton
                        lastClickTime = now

                        val targetId = job.providerId
                        val info = job.providerInfo
                        
                        android.util.Log.d("FORENSIC", "CALL_BUTTON_CLICKED | Target: $targetId | Terminal: $isTerminalState")
                        
                        if (!isTerminalState && targetId != null) {
                            val name = info?.let { "${it.firstName} ${it.lastName}" } ?: "Professional"
                            val phone = info?.phoneNumber ?: ""
                            val photo = info?.profilePicture
                            
                            android.util.Log.d("FORENSIC", "CALL_NAVIGATING | To: $name")
                            onCallOpen(targetId, name, phone, photo)
                        } else if (targetId == null) {
                            android.util.Log.e("FORENSIC", "CALL_FAILED | No provider ID available")
                        }
                    },
                    modifier = Modifier.size(48.dp),
                    colors = IconButtonDefaults.filledIconButtonColors(
                        containerColor = if (isTerminalState) Color.LightGray else Color(0xFFE8F5E9)
                    ),
                    enabled = !isTerminalState
                ) {
                    Icon(Icons.Default.Phone, contentDescription = "Call", tint = if (isTerminalState) Color.Gray else Color(0xFF2E7D32))
                }
                
                FilledIconButton(
                    onClick = { if (!isTerminalState) { job?.providerId?.let { onChatOpen(it) } } },
                    modifier = Modifier.size(48.dp),
                    colors = IconButtonDefaults.filledIconButtonColors(
                        containerColor = if (isTerminalState) Color.LightGray else Color(0xFFE3F2FD)
                    ),
                    enabled = !isTerminalState
                ) {
                    Icon(Icons.Default.Email, contentDescription = "Message", tint = if (isTerminalState) Color.Gray else Color(0xFF1976D2))
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
                    val etaDisplay = when (job.status) {
                        "ARRIVED" -> "Provider has arrived"
                        "STARTED", "IN_PROGRESS" -> "Work in Progress"
                        "COMPLETED" -> "Job Completed"
                        "CANCELLED" -> "Job Cancelled"
                        else -> if (eta.contains("min")) "Arriving in $eta" else "ETA: $eta"
                    }
                    Text(text = etaDisplay, fontWeight = FontWeight.Black, color = Color.Black)
                }
            }
        }
    }
}
