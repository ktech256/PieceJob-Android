package com.piecejob.provider.ui.tracking

import android.content.Intent
import android.net.Uri
import android.util.Log
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
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

import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.google.android.libraries.navigation.*
import com.google.android.gms.maps.GoogleMap
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun ProviderTrackingScreen(
    jobId: String,
    viewModel: ProviderTrackingViewModel = hiltViewModel(),
    onBack: () -> Unit,
    onNavigateToRating: (String) -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = androidx.compose.ui.platform.LocalLifecycleOwner.current
    val scope = rememberCoroutineScope()
    
    val job by viewModel.job.collectAsState()
    val providerLocation by viewModel.providerLocation.collectAsState()
    val eta by viewModel.eta.collectAsState()
    val distance by viewModel.distance.collectAsState()
    val showReminder by viewModel.showStartReminder.collectAsState()
    val error by viewModel.error.collectAsState()

    var showCancelDialog by remember { mutableStateOf(false) }

    var navigator by remember { mutableStateOf<Navigator?>(null) }
    var sdkEtaText by remember { mutableStateOf("") }
    var sdkDistanceText by remember { mutableStateOf("") }

    val navigationView = remember {
        NavigationView(context).apply { onCreate(null) }
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_START -> navigationView.onStart()
                Lifecycle.Event.ON_RESUME -> navigationView.onResume()
                Lifecycle.Event.ON_PAUSE -> navigationView.onPause()
                Lifecycle.Event.ON_STOP -> navigationView.onStop()
                Lifecycle.Event.ON_DESTROY -> navigationView.onDestroy()
                else -> {}
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    LaunchedEffect(Unit) {
        val activity = (context as? android.app.Activity)
        if (activity != null) {
            NavigationApi.getNavigator(activity, object : NavigationApi.NavigatorListener {
                override fun onNavigatorReady(nav: Navigator) {
                    navigator = nav
                    nav.setAudioGuidance(Navigator.AudioGuidance.VOICE_ALERTS_AND_GUIDANCE)
                    scope.launch {
                        while (true) {
                            val tad = nav.currentTimeAndDistance
                            if (tad != null) {
                                val mins = (tad.seconds / 60).toInt()
                                sdkEtaText = if (mins < 1) "1 min" else "$mins mins"
                                val km = tad.meters / 1000.0
                                sdkDistanceText = if (km < 1.0) "${tad.meters.toInt()} m" else String.format("%.1f km", km)
                            }
                            delay(2000)
                        }
                    }
                }
                override fun onError(errorCode: Int) { 
                    Log.e("NAV_SDK", "Error: $errorCode") 
                }
            })
        }
    }

    LaunchedEffect(navigator, job) {
        val nav = navigator ?: return@LaunchedEffect
        val j = job ?: return@LaunchedEffect
        val status = j.status
        
        if (status == "COMPLETED" || status == "CANCELLED") {
            nav.stopGuidance()
            nav.clearDestinations()
            return@LaunchedEffect
        }

        val dest = j.location?.coordinates
        if (dest != null && dest.size >= 2) {
            val waypoint = Waypoint.builder()
                .setLatLng(dest[1], dest[0])
                .setTitle("Customer Location")
                .build()
            nav.setDestination(waypoint)
            nav.startGuidance()
        }
    }

    // Prevent accidental exit during active job
    BackHandler {
        onBack() // This will go to Home (Dashboard) while job remains active
    }

    LaunchedEffect(jobId) {
        viewModel.initTracking(jobId)
    }

    LaunchedEffect(job?.status) {
        if (job?.status == "COMPLETED" || job?.status == "CANCELLED") {
            delay(2000)
            if (job?.status == "COMPLETED") {
                onNavigateToRating(jobId)
            } else {
                onBack()
            }
        }
    }

    val customerLatLng = remember(job) {
        job?.location?.coordinates?.let { LatLng(it[1], it[0]) } ?: LatLng(0.0, 0.0)
    }

    if (showCancelDialog) {
        AlertDialog(
            onDismissRequest = { showCancelDialog = false },
            title = { Text("Cancel Job?", fontWeight = FontWeight.Black) },
            text = { Text("Are you sure you want to cancel this job? This may affect your performance rating.") },
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
        AndroidView(
            factory = { navigationView },
            modifier = Modifier.fillMaxSize()
        )

        // Top Status & Navigation Info
        Column(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 56.dp, start = 16.dp, end = 16.dp)
                .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Header with Back Button
            Surface(
                color = Color.White.copy(alpha = 0.9f),
                shape = RoundedCornerShape(12.dp),
                shadowElevation = 4.dp
            ) {
                Row(
                    modifier = Modifier.padding(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onBack, modifier = Modifier.size(40.dp)) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.Black)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = job?.status?.replace("_", " ") ?: "LOADING...",
                        color = when(job?.status) {
                            "ACCEPTED" -> Color(0xFF1976D2)
                            "ARRIVED" -> Color(0xFFFFA000)
                            "STARTED" -> Color(0xFF4CAF50)
                            else -> Color.DarkGray
                        },
                        fontWeight = FontWeight.Black,
                        fontSize = 14.sp
                    )
                }
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
                        val isArrivedOrStarted = job?.status == "ARRIVED" || job?.status == "STARTED"
                        Text(
                            text = if (isArrivedOrStarted) "You have arrived" else "ETA: ${sdkEtaText.ifBlank { eta }}", 
                            fontWeight = FontWeight.Black, 
                            fontSize = 16.sp
                        )
                        Text(
                            text = if (isArrivedOrStarted) "Customer is waiting" else "Distance: ${sdkDistanceText.ifBlank { distance }}", 
                            color = Color.Gray, 
                            fontSize = 12.sp
                        )
                    }
                }
            }
        }

        // FAB to Recenter
        SmallFloatingActionButton(
            onClick = { 
                navigationView.getMapAsync { map -> 
                    map.followMyLocation(GoogleMap.CameraPerspective.TILTED) 
                } 
            },
            modifier = Modifier.padding(16.dp).align(Alignment.CenterEnd).offset(y = (-40).dp),
            containerColor = Color.White,
            contentColor = Color.Black
        ) { 
            Icon(Icons.Default.MyLocation, contentDescription = "Recenter") 
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
                        "ACCEPTED", "ARRIVED" -> {
                            val isArrived = currentJob.status == "ARRIVED"
                            Button(
                                onClick = { 
                                    if (isArrived) {
                                        Log.d("TrackingFlow", "Start Work pressed")
                                        viewModel.startJob() 
                                    } else {
                                        Log.d("TrackingFlow", "Recenter pressed while accepted")
                                        navigationView.getMapAsync { map -> 
                                            map.followMyLocation(GoogleMap.CameraPerspective.TILTED) 
                                        }
                                    }
                                },
                                modifier = Modifier.fillMaxWidth().height(60.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (isArrived) Color(0xFFFFA000) else Color(0xFF4CAF50)
                                ),
                                shape = RoundedCornerShape(16.dp)
                            ) {
                                Text(
                                    text = if (isArrived) "START WORK NOW" else "NAVIGATING TO CUSTOMER", 
                                    fontWeight = FontWeight.Black
                                )
                            }
                        }
                        "STARTED" -> {
                            Button(
                                onClick = { 
                                    Log.d("TrackingFlow", "Complete Work pressed")
                                    viewModel.completeJob() 
                                },
                                modifier = Modifier.fillMaxWidth().height(60.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32)),
                                shape = RoundedCornerShape(16.dp)
                            ) {
                                Text("COMPLETE WORK", fontWeight = FontWeight.Black)
                            }
                        }
                    }

                    if (currentJob.status != "COMPLETED" && currentJob.status != "CANCELLED") {
                        Spacer(modifier = Modifier.height(12.dp))
                        OutlinedButton(
                            onClick = { showCancelDialog = true },
                            modifier = Modifier.fillMaxWidth().height(56.dp),
                            border = BorderStroke(1.dp, Color.LightGray),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Text("CANCEL JOB", color = Color.Gray, fontWeight = FontWeight.Bold)
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
