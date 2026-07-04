package com.piecejob.provider.ui.tracking

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
import com.google.android.libraries.navigation.Navigator.RouteStatus
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
    onChatOpen: (String) -> Unit,
    onCallOpen: (String, String, String, String?) -> Unit,
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
                                
                                // Send initial status to trigger customer refresh
                                viewModel.updateStatus("ACCEPTED")

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
        val pricePending = j.priceStatus == "PENDING"
        
        if (status == "COMPLETED" || status == "CANCELLED" || status == "PROVIDER_ACCEPTED" || pricePending) {
            nav.stopGuidance()
            nav.clearDestinations()
            return@LaunchedEffect
        }

        // AUTO-START NAVIGATION TO CUSTOMER
        val dest = j.location?.coordinates
        if (dest != null && dest.size >= 2 && dest[0] != 0.0) {
            val waypoint = Waypoint.builder()
                .setLatLng(dest[1], dest[0])
                .setTitle("Customer Location")
                .build()
            
            android.util.Log.d("ForensicLog", "NAV_START | Setting destination to ${dest[1]}, ${dest[0]}")
            // Note: If guidance doesn't start, it's usually because terms weren't accepted.
            // Some versions of the SDK prompt automatically on getNavigator or setDestination.
            nav.setDestination(waypoint)
            nav.startGuidance()
        }
    }

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
        if (job?.status == "COMPLETED" || job?.status == "CANCELLED" || job?.status == "RATED") {
            if (job?.status == "COMPLETED") {
                android.util.Log.d("ForensicLog", "TRACKING_EXIT | Job Completed. Moving to rating.")
                onNavigateToRating(jobId)
            } else {
                android.util.Log.d("ForensicLog", "TRACKING_EXIT | Job Cancelled. Returning.")
                delay(1000)
                onBack()
            }
        }
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

    val isNegotiating = job?.status == "PROVIDER_ACCEPTED" || job?.priceStatus == "PENDING"

    Box(modifier = Modifier.fillMaxSize()) {
        AndroidView(
            factory = { navigationView },
            modifier = Modifier.fillMaxSize()
        )
        
        if (isNegotiating) {
            Box(
                modifier = Modifier.fillMaxSize().background(Color.White),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(32.dp)) {
                    Icon(Icons.Default.HourglassEmpty, contentDescription = null, modifier = Modifier.size(64.dp), tint = Color(0xFFFFA000))
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Negotiation Session Active", fontWeight = FontWeight.Black, fontSize = 20.sp)
                    Text(
                        text = if (job?.status == "ACCEPTED") "Price agreed. Please confirm dispatch to start navigation." else "Navigation and exact location are locked until task details and price are agreed.",
                        textAlign = TextAlign.Center,
                        color = Color.Gray,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    Button(
                        onClick = { onChatOpen(job?.customerId ?: "") },
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFA000))
                    ) {
                        Text("RESUME NEGOTIATION", fontWeight = FontWeight.Black)
                    }
                }
            }
        }

        // Overlay: Top Header
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
                    val statusText = remember(job?.status) {
                        job?.status?.replace("_", " ") ?: "INITIALIZING..."
                    }
                    Text(
                        text = statusText,
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

        // Overlay: Recenter FAB
        SmallFloatingActionButton(
            onClick = { 
                if (navigator != null) {
                    navigationView.getMapAsync { map -> 
                        map.followMyLocation(GoogleMap.CameraPerspective.TILTED) 
                    }
                }
            },
            modifier = Modifier.padding(16.dp).align(Alignment.CenterEnd).offset(y = (-40).dp),
            containerColor = Color.White,
            contentColor = Color.Black
        ) { 
            Icon(Icons.Default.MyLocation, contentDescription = "Recenter") 
        }

        // Overlay: Bottom Action Panel (Copy of Customer Style)
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
                    val isTerminalState = job?.status == "COMPLETED" || job?.status == "CANCELLED" || job?.status == "RATED"
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
                                        text = currentJob.serviceName ?: currentJob.serviceCode ?: "Unknown Service",
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

                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    var lastClickTime by remember { mutableLongStateOf(0L) }
                                    FilledIconButton(
                                        onClick = { 
                                            val now = System.currentTimeMillis()
                                            if (now - lastClickTime < 1000) return@FilledIconButton
                                            lastClickTime = now

                                            val targetId = job?.customerId
                                            val info = job?.customerInfo

                                            android.util.Log.d("FORENSIC", "CALL_BUTTON_CLICKED | Target: $targetId | Terminal: $isTerminalState")
                                            
                                            if (!isTerminalState && targetId != null) {
                                                val name = info?.let { "${it.firstName} ${it.lastName}" } ?: "Customer"
                                                val phone = info?.phoneNumber ?: ""
                                                val photo = info?.profilePicture
                                                
                                                android.util.Log.d("FORENSIC", "CALL_NAVIGATING | To: $name")
                                                onCallOpen(targetId, name, phone, photo)
                                            } else if (targetId == null) {
                                                android.util.Log.e("FORENSIC", "CALL_FAILED | No customer ID available")
                                            }
                                        },
                                        modifier = Modifier.size(44.dp),
                                        colors = IconButtonDefaults.filledIconButtonColors(
                                            containerColor = if (isTerminalState) Color.LightGray else Color(0xFFE8F5E9)
                                        ),
                                        enabled = !isTerminalState
                                    ) {
                                        Icon(Icons.Default.Phone, contentDescription = "Call", tint = if (isTerminalState) Color.Gray else Color(0xFF2E7D32))
                                    }
                                    
                                    FilledIconButton(
                                        onClick = { if (!isTerminalState && currentJob.customerId != null) onChatOpen(currentJob.customerId) },
                                        modifier = Modifier.size(44.dp),
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
            // Snackbar removed as per Issue 1. Legitimate errors are logged and visible via UI state changes.
        }
    }
}
