package com.piecejob.core.ui.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Sell
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.piecejob.core.data.remote.ServiceDto
import com.piecejob.core.data.remote.dto.MessageDto
import com.piecejob.core.data.remote.dto.JobDto
import com.piecejob.core.data.remote.dto.PriceProposalDto

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.clickable
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.filled.Close
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NegotiationScreen(
    jobId: String,
    otherUserId: String,
    currentUserId: String = "",
    viewModel: ChatViewModel = hiltViewModel(),
    onBack: () -> Unit,
    onNegotiationComplete: (String, String) -> Unit
) {
    val messages by viewModel.messages.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val serviceConfig by viewModel.serviceConfig.collectAsState()
    val jobState by viewModel.jobState.collectAsState()
    
    var showPriceDialog by remember { mutableStateOf(false) }
    var priceAmount by remember { mutableStateOf("") }
    var priceNote by remember { mutableStateOf("") }

    var selectedPhotosForGallery by remember { mutableStateOf<List<String>?>(null) }
    var initialPhotoIndex by remember { mutableIntStateOf(0) }
    
    var showPhotoPicker by remember { mutableStateOf(false) }
    val isProvider = com.piecejob.BuildConfig.FLAVOR == "provider"

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetMultipleContents()
    ) { uris ->
        if (uris.isNotEmpty()) {
            viewModel.uploadTaskPhotos(uris.take(4))
        }
    }

    if (showPhotoPicker) {
        ModalBottomSheet(onDismissRequest = { showPhotoPicker = false }) {
            Column(modifier = Modifier.padding(16.dp).padding(bottom = 32.dp)) {
                Text("Select Photo Source", fontWeight = FontWeight.Black, modifier = Modifier.padding(bottom = 16.dp))
                PickerOption(androidx.compose.material.icons.Icons.Default.CameraAlt, "Take Photo") {
                    showPhotoPicker = false
                    photoPickerLauncher.launch("image/*")
                }
                PickerOption(androidx.compose.material.icons.Icons.Default.PhotoLibrary, "Choose From Gallery") {
                    showPhotoPicker = false
                    photoPickerLauncher.launch("image/*")
                }
                Spacer(modifier = Modifier.height(16.dp))
                OutlinedButton(onClick = { showPhotoPicker = false }, modifier = Modifier.fillMaxWidth()) {
                    Text("CANCEL")
                }
            }
        }
    }

    LaunchedEffect(jobId) {
        viewModel.initChat(jobId)
    }

    LaunchedEffect(jobState?.status) {
        if (jobState?.status == "ACCEPTED" || jobState?.status == "EN_ROUTE") {
            onNegotiationComplete(jobId, otherUserId)
        }
    }

    if (showPriceDialog) {
        AlertDialog(
            onDismissRequest = { showPriceDialog = false },
            title = { Text("Propose Price") },
            text = {
                Column {
                    OutlinedTextField(
                        value = priceAmount,
                        onValueChange = { if (it.all { char -> char.isDigit() || char == '.' }) priceAmount = it },
                        label = { Text("Amount") },
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Decimal)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = priceNote,
                        onValueChange = { priceNote = it },
                        label = { Text("Optional Note") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val amount = priceAmount.toDoubleOrNull()
                        if (amount != null) {
                            viewModel.proposePrice(amount, priceNote)
                            showPriceDialog = false
                            priceAmount = ""
                            priceNote = ""
                        }
                    },
                    enabled = priceAmount.isNotBlank()
                ) { Text("Send Proposal") }
            },
            dismissButton = {
                TextButton(onClick = { showPriceDialog = false }) { Text("Cancel") }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Column {
                        Text("Negotiation Session", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                        Text("Job #$jobId", fontSize = 10.sp, color = Color.Gray)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
            )
        },
        bottomBar = {
            Surface(tonalElevation = 8.dp, shadowElevation = 12.dp) {
                Column(modifier = Modifier.padding(16.dp)) {
                    if (isProvider) {
                        val hasPhotos = !jobState?.taskPhotos.isNullOrEmpty()
                        val photosRequested = jobState?.taskPhotosRequested == true
                        val photosSeen = jobState?.taskPhotosSeen == true
                        val negRequired = serviceConfig?.priceNegotiationRequired == true
                        val photoRequired = serviceConfig?.photoSharingRequired == true

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            if (photoRequired && !photosRequested) {
                                Button(
                                    onClick = { viewModel.requestPhotos() },
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Icon(Icons.Default.PhotoCamera, null, modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(4.dp))
                                    Text("Request Photos", fontSize = 11.sp)
                                }
                            } else if (photoRequired && hasPhotos && !photosSeen) {
                                Button(
                                    onClick = { viewModel.markPhotosSeen() },
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(12.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = Color.Black)
                                ) {
                                    Icon(Icons.Default.Visibility, null, modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(4.dp))
                                    Text("Photos Reviewed", fontSize = 11.sp)
                                }
                            } else if (negRequired && jobState?.priceStatus != "ACCEPTED") {
                                val canPropose = if (photoRequired) photosSeen else true
                                Button(
                                    onClick = { showPriceDialog = true },
                                    modifier = Modifier.weight(1f),
                                    enabled = canPropose,
                                    shape = RoundedCornerShape(12.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFA000))
                                ) {
                                    Icon(Icons.Default.Sell, null, modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(4.dp))
                                    Text("Propose Price", fontSize = 11.sp)
                                }
                            } else if (jobState?.status == "PROVIDER_ACCEPTED") {
                                // No neg required, but in PROVIDER_ACCEPTED (likely waiting for photos)
                                val canDispatch = if (photoRequired) photosSeen else true
                                Button(
                                    onClick = { viewModel.confirmDispatch() },
                                    modifier = Modifier.weight(1f),
                                    enabled = canDispatch,
                                    shape = RoundedCornerShape(12.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32))
                                ) {
                                    Icon(Icons.Default.LocalShipping, null, modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(4.dp))
                                    Text("Confirm Dispatch", fontSize = 11.sp)
                                }
                            }
                        }
                        
                        // Status Text
                        val statusHint = when {
                            photoRequired && !photosRequested -> "Ask for photos to see the task detail."
                            photoRequired && photosRequested && !hasPhotos -> "Waiting for customer to upload photos..."
                            photoRequired && hasPhotos && !photosSeen -> "Review the photos above then mark as reviewed."
                            negRequired && jobState?.priceStatus == "PENDING" -> "Waiting for counter-party to respond..."
                            else -> ""
                        }
                        if (statusHint.isNotEmpty()) {
                            Text(
                                text = statusHint,
                                fontSize = 10.sp,
                                color = Color.Gray,
                                modifier = Modifier.padding(top = 8.dp).fillMaxWidth(),
                                textAlign = TextAlign.Center
                            )
                        }
                    } else {
                        // Customer View - Just informative text
                        Text(
                            "Waiting for provider actions. You can upload photos if requested.",
                            textAlign = TextAlign.Center,
                            fontSize = 12.sp,
                            color = Color.Gray,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        }
    ) { padding ->
        Box(modifier = Modifier.padding(padding).fillMaxSize().background(Color(0xFFF4F5F7))) {
            if (isLoading && messages.isEmpty()) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    reverseLayout = false
                ) {
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF9C4)),
                            border = BorderStroke(1.dp, Color(0xFFFBC02D))
                        ) {
                            Text(
                                "Locked Session: Normal messaging is disabled until the price is agreed upon and the job is dispatched.",
                                modifier = Modifier.padding(12.dp),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF827717),
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                    
                    item {
                        NegotiationProgressTracker(jobState, serviceConfig)
                    }

                    item {
                        NegotiationInfoCard(jobState, serviceConfig)
                    }

                    jobState?.activeProposal?.let { proposal ->
                        item {
                            ActiveProposalHeader(proposal, currentUserId) { action ->
                                if (action == "ACCEPT") viewModel.respondToProposal(proposal.id, "ACCEPT")
                                else if (action == "REJECT") viewModel.respondToProposal(proposal.id, "REJECT")
                                else if (action == "COUNTER") showPriceDialog = true
                            }
                        }
                    }
                    
                    items(messages) { msg ->
                        ChatBubble(
                            msg = msg,
                            isMe = msg.senderId._id != otherUserId,
                            onAction = { action, meta ->
                                when (action) {
                                    "UPLOAD_PHOTOS" -> showPhotoPicker = true
                                    "VIEW_PHOTOS" -> {
                                        selectedPhotosForGallery = meta?.get("photos") as? List<String>
                                        initialPhotoIndex = meta?.get("index") as? Int ?: 0
                                    }
                                    "MARK_SEEN" -> viewModel.markPhotosSeen()
                                    "ACCEPT_PROPOSAL" -> viewModel.respondToProposal(meta?.get("proposalId") as? String ?: "", "ACCEPT")
                                    "REJECT_PROPOSAL" -> viewModel.respondToProposal(meta?.get("proposalId") as? String ?: "", "REJECT")
                                    "COUNTER_PROPOSAL" -> showPriceDialog = true
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun FullscreenPhotoGallery(
    photos: List<String>,
    initialIndex: Int,
    onDismiss: () -> Unit
) {
    val pagerState = rememberPagerState(initialPage = initialIndex) { photos.size }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxSize()
            ) { index ->
                var scale by remember { mutableFloatStateOf(1f) }
                var offset by remember { mutableStateOf(androidx.compose.ui.geometry.Offset.Zero) }
                val state = rememberTransformableState { zoomChange, offsetChange, _ ->
                    scale *= zoomChange
                    offset += offsetChange
                }

                coil.compose.AsyncImage(
                    model = photos[index],
                    contentDescription = null,
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer(
                            scaleX = scale.coerceIn(1f, 5f),
                            scaleY = scale.coerceIn(1f, 5f),
                            translationX = offset.x,
                            translationY = offset.y
                        )
                        .transformable(state = state),
                    contentScale = androidx.compose.ui.layout.ContentScale.Fit
                )
            }

            IconButton(
                onClick = onDismiss,
                modifier = Modifier.align(Alignment.TopEnd).padding(16.dp)
            ) {
                Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White)
            }

            Text(
                text = "${pagerState.currentPage + 1} / ${photos.size}",
                color = Color.White,
                modifier = Modifier.align(Alignment.BottomCenter).padding(32.dp),
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
fun PickerOption(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
        Spacer(modifier = Modifier.width(16.dp))
        Text(label, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun NegotiationInfoCard(job: JobDto?, service: ServiceDto?) {
    if (job == null) return
    
    Card(
        modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, Color.LightGray)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Session Intelligence", fontWeight = FontWeight.Black, fontSize = 12.sp, color = Color.Gray)
            Spacer(modifier = Modifier.height(12.dp))
            
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                InfoBadge("Round: ${job.negotiationRounds ?: 0} / 4")
                if (job.taskPhotosRequested == true) {
                    val photoStatus = if (job.taskPhotosSeen == true) "Photos Reviewed" else if (!job.taskPhotos.isNullOrEmpty()) "Photos Uploaded" else "Awaiting Photos"
                    InfoBadge(photoStatus)
                }
            }
            
            if (job.agreedPrice != null) {
                Spacer(modifier = Modifier.height(12.dp))
                Text("Price Agreed: R${job.agreedPrice}", fontWeight = FontWeight.Black, color = Color(0xFF2E7D32), fontSize = 16.sp)
            }
        }
    }
}

@Composable
fun NegotiationProgressTracker(job: JobDto?, service: ServiceDto?) {
    if (job == null) return
    
    val photoRequired = service?.photoSharingRequired == true
    val negRequired = service?.priceNegotiationRequired == true
    
    val steps = remember(job.status, job.priceStatus, job.taskPhotosSeen, photoRequired, negRequired) {
        mutableListOf<NegotiationStep>().apply {
            add(NegotiationStep("Request Accepted", true))
            
            val photosStepCompleted = if (photoRequired) job.taskPhotosSeen == true else true
            add(NegotiationStep("Photos Shared", photosStepCompleted))
            
            val priceStepCompleted = if (negRequired) job.priceStatus == "ACCEPTED" else true
            add(NegotiationStep("Price Agreed", priceStepCompleted))
            
            add(NegotiationStep("Provider Dispatched", job.status != "PROVIDER_ACCEPTED"))
        }
    }

    Card(
        modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, Color(0xFFEEEEEE))
    ) {
        Row(
            modifier = Modifier.padding(16.dp).fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            steps.forEachIndexed { index, step ->
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .background(
                                color = if (step.isCompleted) Color(0xFF2E7D32) else Color(0xFFEEEEEE),
                                shape = CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        if (step.isCompleted) {
                            Icon(Icons.Default.Check, null, modifier = Modifier.size(14.dp), tint = Color.White)
                        } else {
                            Text("${index + 1}", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.Gray)
                        }
                    }
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = step.label,
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (step.isCompleted) Color.Black else Color.Gray,
                        textAlign = TextAlign.Center,
                        lineHeight = 10.sp
                    )
                }
                
                if (index < steps.size - 1) {
                    Box(
                        modifier = Modifier
                            .height(1.dp)
                            .weight(0.5f)
                            .background(if (step.isCompleted && steps[index+1].isCompleted) Color(0xFF2E7D32) else Color(0xFFEEEEEE))
                            .offset(y = (-8).dp)
                    )
                }
            }
        }
    }
}

data class NegotiationStep(val label: String, val isCompleted: Boolean)

@Composable
fun InfoBadge(text: String) {
    Surface(
        color = Color(0xFFF5F5F5),
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(0.5.dp, Color.LightGray)
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
fun ActiveProposalHeader(proposal: PriceProposalDto, currentUserId: String, onAction: (String) -> Unit) {
    val isMe = proposal.senderId == currentUserId
    
    Card(
        modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isMe) Color(0xFFF1F8E9) else Color(0xFFE3F2FD)
        ),
        border = BorderStroke(1.dp, (if (isMe) Color(0xFF4CAF50) else Color(0xFF1976D2)).copy(alpha = 0.5f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.Sell, 
                    contentDescription = null, 
                    tint = if (isMe) Color(0xFF2E7D32) else Color(0xFF1976D2), 
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (isMe) "Your Proposal" else "Incoming Proposal", 
                    fontWeight = FontWeight.Black, 
                    color = if (isMe) Color(0xFF2E7D32) else Color(0xFF1976D2)
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            Text("Amount: R${proposal.amount}", fontSize = 24.sp, fontWeight = FontWeight.Black)
            if (!proposal.note.isNullOrBlank()) {
                Text("Note: ${proposal.note}", fontSize = 12.sp, color = Color.DarkGray, modifier = Modifier.padding(top = 4.dp))
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            if (isMe) {
                Text("Waiting for counterparty to respond...", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.Gray)
            } else {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        onClick = { onAction("REJECT") },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(0.dp)
                    ) { Text("REJECT", fontSize = 11.sp) }
                    
                    Button(
                        onClick = { onAction("COUNTER") },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFA000)),
                        contentPadding = PaddingValues(0.dp)
                    ) { Text("COUNTER", fontSize = 11.sp) }
                    
                    Button(
                        onClick = { onAction("ACCEPT") },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32)),
                        contentPadding = PaddingValues(0.dp)
                    ) { Text("ACCEPT", fontSize = 11.sp) }
                }
            }
        }
    }
}
