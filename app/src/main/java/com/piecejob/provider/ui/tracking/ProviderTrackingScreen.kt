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
import com.piecejob.core.ui.components.PieceJobButton
import com.piecejob.core.ui.components.PieceJobOutlinedButton

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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
    val revealRecipient by viewModel.revealRecipientInfo.collectAsState()
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
        
        val terminalStates = listOf("COMPLETED", "CANCELLED", "CUSTOMER_CANCELLED", "PROVIDER_CANCELLED", "EXPIRED", "FAILED", "TIMED_OUT", "NO_PROVIDER_FOUND", "RATED")
        
        if (terminalStates.contains(status) || pricePending || status == "PROVIDER_ACCEPTED") {
            android.util.Log.d("ForensicLog", "NAV_CLEANUP | Job Terminal Status: $status. Terminating all navigation.")
            try {
                nav.stopGuidance()
                nav.clearDestinations()
                nav.setAudioGuidance(Navigator.AudioGuidance.SILENT)
                // Note: We don't call cleanup() here because it's a singleton and 
                // might affect other parts if the user hasn't fully exited.
                // But stopGuidance + SILENT + clearDestinations should kill the directions.
            } catch (e: Exception) {
                android.util.Log.e("ForensicLog", "NAV_CLEANUP_ERROR | ${e.message}")
            }
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
        val status = job?.status
        if (status != null) {
            android.util.Log.d("ForensicLog", "JOB_STATE_CHANGED | Job: $jobId | New Status: $status")
        }
        
        val terminalStates = listOf("COMPLETED", "CANCELLED", "CUSTOMER_CANCELLED", "PROVIDER_CANCELLED", "EXPIRED", "FAILED", "TIMED_OUT", "NO_PROVIDER_FOUND", "RATED")
        
        if (terminalStates.contains(status)) {
            android.util.Log.d("ForensicLog", "TRACKING_EXIT | Terminal State $status. Cleaning up navigation.")
            try {
                navigator?.stopGuidance()
                navigator?.clearDestinations()
                navigator?.setAudioGuidance(Navigator.AudioGuidance.SILENT)
                // Calling cleanup() is the most effective way to kill the background service and notification card
                navigator?.cleanup()
            } catch (e: Exception) {
                android.util.Log.e("ForensicLog", "NAV_CLEANUP_ERROR | ${e.message}")
            }

            if (status == "COMPLETED") {
                android.util.Log.d("ForensicLog", "TRACKING_EXIT | Moving to rating.")
                onNavigateToRating(jobId)
            } else {
                android.util.Log.d("ForensicLog", "TRACKING_EXIT | Returning.")
                delay(1000)
                onBack()
            }
        }
    }

    if (showCancelDialog) {
        AlertDialog(
            onDismissRequest = { showCancelDialog = false },
            title = { Text("Cancel this job?", fontWeight = FontWeight.Black) },
            text = { 
                Text(
                    "Cancelling after accepting a customer request negatively affects your reliability score.\n\n" +
                    "Repeated cancellations may reduce future job opportunities and can result in temporary account suspension for up to 24 hours.\n\n" +
                    "Only cancel if you genuinely cannot complete this job.",
                    fontSize = 13.sp
                ) 
            },
            confirmButton = {
                Button(
                    onClick = {
                        showCancelDialog = false
                        viewModel.cancelJob()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD32F2F))
                ) { Text("CANCEL JOB") }
            },
            dismissButton = {
                TextButton(onClick = { showCancelDialog = false }) { Text("KEEP JOB") }
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

    val phase = job?.currentNegotiationPhase ?: "NEUTRAL"
    val isNegotiating = false // Removed RESUME NEGOTIATION overlay as per Issue 2 requirement

    Box(modifier = Modifier.fillMaxSize()) {
        val isWorkInProgress = job?.status == "STARTED" || job?.status == "IN_PROGRESS"

        if (isWorkInProgress && job != null) {
            ProviderWorkInProgressDashboard(
                job = job!!,
                onChatOpen = onChatOpen,
                onCallOpen = onCallOpen,
                onBack = onBack,
                onCompleteJob = { viewModel.completeJob() },
                onSosTrigger = { /* SOS trigger */ }
            )
        } else {
            AndroidView(
                factory = { navigationView },
                modifier = Modifier.fillMaxSize()
            )

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
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
        ) {
            Card(
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
                                        Icon(Icons.Default.Phone, contentDescription = "Call Customer", tint = if (isTerminalState) Color.Gray else Color(0xFF2E7D32))
                                    }
                                    
                                    FilledIconButton(
                                        onClick = { if (!isTerminalState && currentJob.customerId != null) onChatOpen(currentJob.customerId) },
                                        modifier = Modifier.size(44.dp),
                                        colors = IconButtonDefaults.filledIconButtonColors(
                                            containerColor = if (isTerminalState) Color.LightGray else Color(0xFFE3F2FD)
                                        ),
                                        enabled = !isTerminalState
                                    ) {
                                        Icon(Icons.Default.Email, contentDescription = "Message Customer", tint = if (isTerminalState) Color.Gray else Color(0xFF1976D2))
                                    }
                                }
                            }

                        if (currentJob.isForSomeoneElse == true) {
                            Spacer(modifier = Modifier.height(16.dp))
                            Surface(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(16.dp),
                                color = Color(0xFFFFF8E1),
                                border = BorderStroke(1.dp, Color(0xFFFFA000).copy(alpha = 0.5f))
                            ) {
                                Row(
                                    modifier = Modifier.padding(16.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Surface(
                                        modifier = Modifier.size(40.dp),
                                        shape = CircleShape,
                                        color = Color.White
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Icon(Icons.Default.People, contentDescription = null, tint = Color(0xFFFFA000))
                                        }
                                    }
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text("RECIPIENT", fontSize = 10.sp, fontWeight = FontWeight.Black, color = Color(0xFFE65100))
                                        Text(currentJob.recipientName ?: "Someone Else", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                                        if (revealRecipient && !currentJob.recipientPhone.isNullOrBlank()) {
                                            Text(currentJob.recipientPhone!!, fontSize = 13.sp, color = Color.DarkGray)
                                        } else if (!currentJob.recipientPhone.isNullOrBlank()) {
                                            Text("Phone hidden until arrival", fontSize = 11.sp, color = Color.Gray, fontStyle = androidx.compose.ui.text.font.FontStyle.Italic)
                                        }
                                    }
                                    
                                    if (revealRecipient && !currentJob.recipientPhone.isNullOrBlank()) {
                                        IconButton(
                                            onClick = {
                                                val intent = android.content.Intent(android.content.Intent.ACTION_DIAL).apply {
                                                    data = android.net.Uri.parse("tel:${currentJob.recipientPhone}")
                                                }
                                                context.startActivity(intent)
                                            },
                                            modifier = Modifier.background(Color(0xFFFFA000), CircleShape).size(36.dp)
                                        ) {
                                            Icon(Icons.Default.PhoneEnabled, contentDescription = "Call Recipient", tint = Color.White, modifier = Modifier.size(18.dp))
                                        }
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(24.dp))
                        
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            val status = currentJob.status
                            val isJourneyStarted = status == "EN_ROUTE" || status == "ARRIVED" || status == "STARTED" || status == "IN_PROGRESS"

                            when (status) {
                                "ACCEPTED" -> {
                                    PieceJobButton(
                                        text = "START JOURNEY",
                                        onClick = { viewModel.confirmDispatch() },
                                        modifier = Modifier.weight(1f),
                                        containerColor = Color(0xFF2E7D32),
                                        isLoading = viewModel.isLoading.collectAsState().value,
                                        height = 56.dp,
                                        shape = RoundedCornerShape(16.dp)
                                    )
                                }
                                "EN_ROUTE", "ARRIVED" -> {
                                    val isArrived = status == "ARRIVED"
                                    PieceJobButton(
                                        text = if (isArrived) "START WORK" else "DRIVING TO CUSTOMER", 
                                        onClick = { 
                                            if (isArrived) viewModel.startJob() 
                                        },
                                        modifier = Modifier.weight(1f),
                                        containerColor = if (isArrived) Color(0xFF4CAF50) else Color(0xFFF5F5F5),
                                        contentColor = if (isArrived) Color.White else Color.Gray,
                                        enabled = isArrived,
                                        isLoading = viewModel.isLoading.collectAsState().value,
                                        height = 56.dp,
                                        shape = RoundedCornerShape(16.dp)
                                    )
                                }
                                "STARTED" -> {
                                    PieceJobButton(
                                        text = "COMPLETE JOB",
                                        onClick = { viewModel.completeJob() },
                                        modifier = Modifier.weight(1f),
                                        containerColor = Color(0xFF2E7D32),
                                        isLoading = viewModel.isLoading.collectAsState().value,
                                        height = 56.dp,
                                        shape = RoundedCornerShape(16.dp)
                                    )
                                }
                            }
                            
                            if (!isJourneyStarted && status != "COMPLETED" && status != "CANCELLED") {
                                PieceJobOutlinedButton(
                                    text = "CANCEL",
                                    onClick = { viewModel.cancelJob() }, // Standardized
                                    modifier = Modifier.weight(0.6f),
                                    contentColor = Color.Red,
                                    isLoading = viewModel.isLoading.collectAsState().value,
                                    height = 56.dp,
                                    shape = RoundedCornerShape(16.dp)
                                )
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
    }

    if (error != null) {
            // Snackbar removed as per Issue 1. Legitimate errors are logged and visible via UI state changes.
        }
    }
}

@Composable
fun ProviderWorkInProgressDashboard(
    job: com.piecejob.core.data.remote.dto.JobDto,
    onChatOpen: (String) -> Unit,
    onCallOpen: (String, String, String, String?) -> Unit,
    onBack: () -> Unit,
    onCompleteJob: () -> Unit,
    onSosTrigger: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF8F9FA))
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        // Top Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Back")
            }
            Spacer(modifier = Modifier.width(8.dp))
            Column {
                Text(text = "Work in Progress", fontWeight = FontWeight.Black, fontSize = 20.sp)
                Text(text = "Job #${job.id.takeLast(6).uppercase()}", color = Color.Gray, fontSize = 12.sp)
            }
            Spacer(modifier = Modifier.weight(1f))
            Surface(
                color = Color(0xFFE8F5E9),
                shape = RoundedCornerShape(8.dp)
            ) {
                Row(modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.size(8.dp).background(Color(0xFF2E7D32), CircleShape))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(text = "LIVE", color = Color(0xFF2E7D32), fontWeight = FontWeight.Black, fontSize = 10.sp)
                }
            }
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 24.dp)
                .verticalScroll(androidx.compose.foundation.rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(32.dp))
            
            // Timer
            JobTimer(job.startedAtUtc ?: job.startedAt)
            Text(text = "ELAPSED TIME", color = Color.Gray, fontSize = 12.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)

            Spacer(modifier = Modifier.height(48.dp))

            // Customer Info
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(24.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            modifier = Modifier.size(64.dp),
                            shape = CircleShape,
                            color = Color(0xFFFDECEA)
                        ) {
                            if (job.customerInfo?.profilePicture != null) {
                                coil.compose.AsyncImage(
                                    model = job.customerInfo.profilePicture,
                                    contentDescription = null,
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = androidx.compose.ui.layout.ContentScale.Crop
                                )
                            } else {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(Icons.Default.Person, contentDescription = null, tint = Color(0xFFD32F2F), modifier = Modifier.size(32.dp))
                                }
                            }
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(text = job.customerInfo?.let { "${it.firstName} ${it.lastName}" } ?: job.recipientName ?: "Customer", fontWeight = FontWeight.Black, fontSize = 18.sp)
                            Text(text = "Service: ${job.serviceName ?: "General"}", color = Color.Gray, fontSize = 13.sp)
                        }
                        
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            FilledIconButton(
                                onClick = { 
                                    job.customerId?.let { id -> 
                                        onCallOpen(id, "${job.customerInfo?.firstName} ${job.customerInfo?.lastName}", job.customerInfo?.phoneNumber ?: job.recipientPhone ?: "", job.customerInfo?.profilePicture)
                                    }
                                },
                                colors = IconButtonDefaults.filledIconButtonColors(containerColor = Color(0xFFE8F5E9))
                            ) {
                                Icon(Icons.Default.Phone, null, tint = Color(0xFF2E7D32))
                            }
                            FilledIconButton(
                                onClick = { job.customerId?.let { onChatOpen(it) } },
                                colors = IconButtonDefaults.filledIconButtonColors(containerColor = Color(0xFFE3F2FD))
                            ) {
                                Icon(Icons.Default.Message, null, tint = Color(0xFF1976D2))
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Financial Info
            val gross = (job.agreedPrice ?: 0.0)
            val fee = (job.serviceFee ?: 0.0)
            val net = gross - fee

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                Column(modifier = Modifier.padding(24.dp)) {
                    Text(text = "Earnings Summary", fontWeight = FontWeight.Black, fontSize = 14.sp)
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(text = "Gross Payment", color = Color.Gray)
                        Text(text = "${job.currency ?: ""} ${String.format(java.util.Locale.US, "%.2f", gross)}", fontWeight = FontWeight.Bold)
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(text = "PieceJob Service Fee", color = Color.Gray)
                        Text(text = "- ${job.currency ?: ""} ${String.format(java.util.Locale.US, "%.2f", fee)}", color = Color(0xFFD32F2F), fontWeight = FontWeight.Bold)
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    HorizontalDivider(color = Color(0xFFF8F9FA))
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(text = "Your Net Earnings", fontWeight = FontWeight.Black)
                        Text(text = "${job.currency ?: ""} ${String.format(java.util.Locale.US, "%.2f", Math.max(0.0, net))}", fontWeight = FontWeight.Black, fontSize = 18.sp, color = Color(0xFF2E7D32))
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Timeline
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                Column(modifier = Modifier.padding(24.dp)) {
                    Text(text = "Job Timeline", fontWeight = FontWeight.Black, fontSize = 14.sp)
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    TimelineItem("Requested", job.createdAt ?: "", isLast = false)
                    TimelineItem("Started Work", job.startedAt ?: "", isLast = true)
                }
            }
            
            Spacer(modifier = Modifier.height(32.dp))
            
            PieceJobButton(
                text = "COMPLETE PIECEJOB",
                onClick = onCompleteJob,
                modifier = Modifier.fillMaxWidth().height(64.dp),
                containerColor = Color(0xFF2E7D32),
                shape = RoundedCornerShape(20.dp)
            )
            
            Spacer(modifier = Modifier.height(16.dp))

            TextButton(onClick = onSosTrigger) {
                Text(text = "EMERGENCY SOS", color = Color(0xFFD32F2F), fontWeight = FontWeight.Black)
            }
            
            Spacer(modifier = Modifier.height(48.dp))
        }
    }
}

@Composable
fun TimelineItem(label: String, time: String, isLast: Boolean) {
    Row(modifier = Modifier.height(IntrinsicSize.Min)) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(modifier = Modifier.size(12.dp).background(Color(0xFFD32F2F), CircleShape))
            if (!isLast) {
                Box(modifier = Modifier.width(2.dp).weight(1f).background(Color(0xFFEEEEEE)))
            }
        }
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.padding(bottom = if (isLast) 0.dp else 24.dp)) {
            Text(text = label, fontWeight = FontWeight.Bold, fontSize = 13.sp)
            Text(text = com.piecejob.core.utils.formatDateTimeString(time), color = Color.Gray, fontSize = 12.sp)
        }
    }
}

@Composable
fun JobTimer(startedAtIso: String?) {
    var elapsedMillis by remember { mutableLongStateOf(0L) }
    
    LaunchedEffect(startedAtIso) {
        if (startedAtIso == null) return@LaunchedEffect
        
        val startedAt = try {
            val df = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", java.util.Locale.US)
            df.timeZone = java.util.TimeZone.getTimeZone("UTC")
            df.parse(startedAtIso)?.time ?: System.currentTimeMillis()
        } catch (e: Exception) {
            System.currentTimeMillis()
        }
        
        while (true) {
            elapsedMillis = System.currentTimeMillis() - startedAt
            delay(1000)
        }
    }
    
    val totalSeconds = Math.max(0L, elapsedMillis / 1000)
    val seconds = totalSeconds % 60
    val minutes = (totalSeconds / 60) % 60
    val hours = totalSeconds / 3600
    
    Text(
        text = String.format(java.util.Locale.US, "%02d:%02d:%02d", hours, minutes, seconds),
        fontSize = 48.sp,
        fontWeight = FontWeight.Black,
        color = Color.Black
    )
}
