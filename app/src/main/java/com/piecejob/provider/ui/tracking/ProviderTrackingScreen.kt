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
import android.content.Context
import android.content.ContextWrapper
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.isActive

fun Context.getActivity(): android.app.Activity? = when (this) {
    is android.app.Activity -> this
    is ContextWrapper -> baseContext.getActivity()
    else -> null
}

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
        val activity = context.getActivity()
        NavigationView(activity ?: context).apply { 
            try {
                onCreate(null)
                android.util.Log.d("ForensicLog", "TRACKING_MAP | NavigationView onCreate SUCCESS")
            } catch (e: Exception) {
                android.util.Log.e("ForensicLog", "TRACKING_CRASH | NavigationView.onCreate Error: ${e.message}", e)
            }
        }
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            try {
                when (event) {
                    Lifecycle.Event.ON_START -> navigationView.onStart()
                    Lifecycle.Event.ON_RESUME -> {
                        navigationView.onResume()
                        android.util.Log.d("ForensicLog", "TRACKING_RESUMED | Screen focused")
                    }
                    Lifecycle.Event.ON_PAUSE -> navigationView.onPause()
                    Lifecycle.Event.ON_STOP -> navigationView.onStop()
                    Lifecycle.Event.ON_DESTROY -> navigationView.onDestroy()
                    else -> {}
                }
            } catch (e: Exception) {
                android.util.Log.e("ForensicLog", "TRACKING_CRASH | Lifecycle Error: ${e.message}", e)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    LaunchedEffect(Unit) {
        val activity = context.getActivity()
        if (activity != null) {
            android.util.Log.d("ForensicLog", "TRACKING_INIT | Activity context found. Requesting navigator...")
            NavigationApi.getNavigator(activity, object : NavigationApi.NavigatorListener {
                override fun onNavigatorReady(nav: Navigator) {
                    navigator = nav
                    try {
                        nav.setAudioGuidance(Navigator.AudioGuidance.VOICE_ALERTS_AND_GUIDANCE)
                        
                        // Set 3D perspective and follow mode
                        navigationView.getMapAsync { map ->
                            android.util.Log.d("ForensicLog", "TRACKING_INIT | Map READY. Setting camera follow...")
                            if (navigator != null) {
                                map.followMyLocation(GoogleMap.CameraPerspective.TILTED)
                            }
                        }

                        scope.launch {
                            while (isActive) {
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
                    } catch (e: Exception) {
                        android.util.Log.e("ForensicLog", "TRACKING_CRASH | Navigator Ready Logic Error: ${e.message}", e)
                    }
                }
                override fun onError(errorCode: Int) { 
                    android.util.Log.e("ForensicLog", "TRACKING_INIT | Navigator Error: $errorCode")
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

        // AUTO-START NAVIGATION TO CUSTOMER
        val dest = j.location?.coordinates
        if (dest != null && dest.size >= 2) {
            val waypoint = Waypoint.builder()
                .setLatLng(dest[1], dest[0])
                .setTitle("Customer Location")
                .build()
            
            nav.setDestination(waypoint)
            nav.startGuidance()
            android.util.Log.d("ForensicLog", "NAV_START | Automated turn-by-turn started to ${dest[1]}, ${dest[0]}")
        }
    }

    // Prevent accidental exit during active job
    BackHandler {
        onBack() 
    }

    LaunchedEffect(jobId) {
        viewModel.initTracking(jobId)
    }

    LaunchedEffect(job?.status) {
        if (job?.status != null) {
            android.util.Log.d("ForensicLog", "JOB_STATE_CHANGED | Job: $jobId | New Status: ${job?.status}")
        }
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
                TextButton(onClick = { /* Dismissed */ }) { Text("NOT YET") }
            }
        )
    }

    Box(modifier = Modifier.fillMaxSize()) {
        // Core Navigation UI
        AndroidView(
            factory = { navigationView },
            modifier = Modifier.fillMaxSize()
        )

        // Overlay 1: Top Navigation Stats
        Column(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .statusBarsPadding()
                .padding(16.dp)
                .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Surface(
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
                    Text(
                        text = job?.status?.replace("_", " ") ?: "INITIALIZING...",
                        color = when(job?.status) {
                            "ACCEPTED" -> Color(0xFF1976D2)
                            "ARRIVED" -> Color(0xFFFFA000)
                            "STARTED" -> Color(0xFF4CAF50)
                            else -> Color.DarkGray
                        },
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 14.sp
                    )
                }
            }

            // Real-time Navigation Stats Card
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
                            fontSize = 18.sp
                        )
                        Text(
                            text = if (isArrivedOrStarted) "Customer is waiting" else "Distance: ${sdkDistanceText.ifBlank { distance }}", 
                            color = Color.Gray, 
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        // Overlay 2: Recenter Button
        SmallFloatingActionButton(
            onClick = { 
                if (navigator != null) {
                    navigationView.getMapAsync { map -> 
                        map.followMyLocation(GoogleMap.CameraPerspective.TILTED) 
                    }
                }
            },
            modifier = Modifier
                .padding(16.dp)
                .align(Alignment.CenterEnd)
                .offset(y = (-40).dp),
            containerColor = Color.White,
            contentColor = Color.Black
        ) { 
            Icon(Icons.Default.MyLocation, contentDescription = "Recenter") 
        }

        // Overlay 3: Bottom Action Panel (Copy of Customer Style)
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
                    job?.let { currentJob ->
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
                                    text = currentJob.serviceCode,
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 20.sp,
                                    lineHeight = 22.sp
                                )
                                Text(
                                    text = currentJob.location?.address ?: "Customer Location",
                                    color = Color.Gray,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1
                                )
                            }

                            Text(
                                text = "R${currentJob.bookingFee}",
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 20.sp,
                                color = Color(0xFF2E7D32)
                            )
                        }

                        Spacer(modifier = Modifier.height(24.dp))
                        
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            when (currentJob.status) {
                                "ACCEPTED", "ARRIVED" -> {
                                    val isArrived = currentJob.status == "ARRIVED"
                                    Button(
                                        onClick = { 
                                            if (isArrived) viewModel.startJob() 
                                        },
                                        modifier = Modifier.weight(1f).height(56.dp),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = if (isArrived) Color(0xFF4CAF50) else Color(0xFFF5F5F5),
                                            disabledContainerColor = Color(0xFFF5F5F5)
                                        ),
                                        shape = RoundedCornerShape(16.dp),
                                        enabled = isArrived
                                    ) {
                                        Text(
                                            text = if (isArrived) "START JOB" else "DRIVING TO CUSTOMER", 
                                            fontWeight = FontWeight.Black,
                                            color = if (isArrived) Color.White else Color.Gray
                                        )
                                    }
                                }
                                "STARTED" -> {
                                    Button(
                                        onClick = { viewModel.completeJob() },
                                        modifier = Modifier.weight(1f).height(56.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32)),
                                        shape = RoundedCornerShape(16.dp)
                                    ) {
                                        Text("COMPLETE JOB", fontWeight = FontWeight.Black)
                                    }
                                }
                            }
                            
                            if (currentJob.status != "COMPLETED" && currentJob.status != "CANCELLED") {
                                OutlinedButton(
                                    onClick = { showCancelDialog = true },
                                    modifier = Modifier.weight(0.6f).height(56.dp),
                                    border = BorderStroke(1.dp, Color(0xFFEEEEEE)),
                                    shape = RoundedCornerShape(16.dp),
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.Red)
                                ) {
                                    Text("CANCEL", fontWeight = FontWeight.Black)
                                }
                            }
                        }
                        
                        if (currentJob.status == "STARTED") {
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "Work in progress. Tap complete when finished.",
                                modifier = Modifier.fillMaxWidth(),
                                textAlign = TextAlign.Center,
                                fontSize = 12.sp,
                                color = Color.Gray
                            )
                        }
                    }
                }
            }
        }

        if (error != null) {
            Snackbar(
                modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 120.dp),
                containerColor = Color(0xFF323232),
                contentColor = Color.White
            ) { Text(error!!) }
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
