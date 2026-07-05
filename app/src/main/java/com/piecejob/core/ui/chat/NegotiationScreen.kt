package com.piecejob.core.ui.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
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
import android.net.Uri
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.layout.ContentScale
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.material.icons.filled.AddAPhoto
import androidx.compose.material.icons.filled.Delete
import androidx.compose.foundation.Image
import androidx.compose.ui.draw.clip
import coil.compose.rememberAsyncImagePainter

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
    val uploadProgress by viewModel.uploadProgress.collectAsState()
    val uploadError by viewModel.uploadError.collectAsState()
    val uploadSuccess by viewModel.uploadSuccess.collectAsState()
    val serviceConfig by viewModel.serviceConfig.collectAsState()
    val jobState by viewModel.jobState.collectAsState()
    val phase = jobState?.currentNegotiationPhase ?: "NEUTRAL"
    
    var showPriceDialog by remember { mutableStateOf(false) }
    var priceAmount by remember { mutableStateOf("") }

    var selectedPhotosForGallery by remember { mutableStateOf<List<String>?>(null) }
    var initialPhotoIndex by remember { mutableIntStateOf(0) }
    
    var showPhotoPicker by remember { mutableStateOf(false) }
    var stagingUris by remember { mutableStateOf<List<Uri>>(emptyList()) }
    var tempCameraUri by remember { mutableStateOf<Uri?>(null) }
    val context = LocalContext.current
    val isProvider = com.piecejob.BuildConfig.FLAVOR == "provider"

    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetMultipleContents()
    ) { uris ->
        stagingUris = (stagingUris + uris).take(4)
    }

    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        if (success && tempCameraUri != null) {
            stagingUris = (stagingUris + tempCameraUri!!).take(4)
            tempCameraUri = null
        }
    }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            try {
                val uri = createTempUri(context)
                tempCameraUri = uri
                cameraLauncher.launch(uri)
            } catch (e: Exception) {
                android.util.Log.e("CAMERA_ERROR", "Failed to create temp uri", e)
            }
        }
    }

    if (showPhotoPicker) {
        ModalBottomSheet(onDismissRequest = { showPhotoPicker = false }) {
            Column(modifier = Modifier.padding(16.dp).padding(bottom = 32.dp)) {
                Text("Select Photo Source", fontWeight = FontWeight.Black, modifier = Modifier.padding(bottom = 16.dp))
                PickerOption(androidx.compose.material.icons.Icons.Default.CameraAlt, "Take Photo") {
                    showPhotoPicker = false
                    cameraPermissionLauncher.launch(android.Manifest.permission.CAMERA)
                }
                PickerOption(androidx.compose.material.icons.Icons.Default.PhotoLibrary, "Choose From Gallery") {
                    showPhotoPicker = false
                    galleryLauncher.launch("image/*")
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
        if (jobState?.status == "EN_ROUTE" || jobState?.status == "ARRIVED" || jobState?.status == "STARTED") {
            onNegotiationComplete(jobId, otherUserId)
        }
    }

    LaunchedEffect(phase) {
        if (isProvider && phase == "PRICE_PROPOSAL" && jobState?.activeProposal == null) {
            showPriceDialog = true
        }
    }

    if (showPriceDialog) {
        AlertDialog(
            onDismissRequest = { showPriceDialog = false },
            title = { Text("What is your price?") },
            text = {
                Column {
                    OutlinedTextField(
                        value = priceAmount,
                        onValueChange = { if (it.all { char -> char.isDigit() || char == '.' }) priceAmount = it },
                        label = { Text("Amount") },
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Decimal)
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val amount = priceAmount.toDoubleOrNull()
                        if (amount != null) {
                            viewModel.proposePrice(amount)
                            showPriceDialog = false
                            priceAmount = ""
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

    if (selectedPhotosForGallery != null) {
        FullscreenPhotoGallery(
            photos = selectedPhotosForGallery!!,
            initialIndex = initialPhotoIndex,
            onDismiss = { selectedPhotosForGallery = null }
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
                    val phase = jobState?.currentNegotiationPhase ?: "NEUTRAL"
                    
                    if (isProvider) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            when (phase) {
                                "PHOTO_REQUEST" -> {
                                    Button(
                                        onClick = { viewModel.requestPhotos() },
                                        modifier = Modifier.weight(1f),
                                        shape = RoundedCornerShape(12.dp)
                                    ) {
                                        Icon(Icons.Default.PhotoCamera, null, modifier = Modifier.size(16.dp))
                                        Spacer(Modifier.width(4.dp))
                                        Text("Request Task Photos", fontSize = 11.sp)
                                    }
                                }
                                "WAITING_FOR_PHOTOS" -> {
                                    Button(
                                        onClick = { },
                                        modifier = Modifier.weight(1f),
                                        shape = RoundedCornerShape(12.dp),
                                        enabled = false,
                                        colors = ButtonDefaults.buttonColors(containerColor = Color.Gray)
                                    ) {
                                        Text("Waiting for Photos...", fontSize = 11.sp)
                                    }
                                }
                                "PHOTOS_UPLOADED" -> {
                                    Button(
                                        onClick = { viewModel.markPhotosSeen() },
                                        modifier = Modifier.weight(1f),
                                        shape = RoundedCornerShape(12.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = Color.Black)
                                    ) {
                                        Icon(Icons.Default.Check, null, modifier = Modifier.size(16.dp))
                                        Spacer(Modifier.width(4.dp))
                                        Text("CONTINUE", fontSize = 11.sp)
                                    }
                                }
                                "PRICE_PROPOSAL", "WAITING_FOR_PROVIDER" -> {
                                    Button(
                                        onClick = { showPriceDialog = true },
                                        modifier = Modifier.weight(1f),
                                        shape = RoundedCornerShape(12.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFA000))
                                    ) {
                                        Icon(Icons.Default.Sell, null, modifier = Modifier.size(16.dp))
                                        Spacer(Modifier.width(4.dp))
                                        Text(if (phase == "WAITING_FOR_PROVIDER") "Send Counter Offer" else "What is your price?", fontSize = 11.sp)
                                    }
                                }
                                "WAITING_FOR_CUSTOMER" -> {
                                    Button(
                                        onClick = { },
                                        modifier = Modifier.weight(1f),
                                        shape = RoundedCornerShape(12.dp),
                                        enabled = false,
                                        colors = ButtonDefaults.buttonColors(containerColor = Color.Gray)
                                    ) {
                                        Text("Waiting for Customer...", fontSize = 11.sp)
                                    }
                                }
                                "PRICE_ACCEPTED" -> {
                                    Button(
                                        onClick = { viewModel.confirmDispatch() },
                                        modifier = Modifier.weight(1f),
                                        shape = RoundedCornerShape(12.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32))
                                    ) {
                                        Icon(Icons.Default.LocalShipping, null, modifier = Modifier.size(16.dp))
                                        Spacer(Modifier.width(4.dp))
                                        Text("START JOURNEY", fontSize = 11.sp)
                                    }
                                }
                            }
                        }
                        
                        // Status Text
                        val statusHint = when (phase) {
                            "PHOTO_REQUEST" -> "Ask for photos to see the task detail."
                            "WAITING_FOR_PHOTOS" -> "Customer is currently selecting/uploading photos."
                            "PHOTOS_UPLOADED" -> "Review the photos above then tap CONTINUE."
                            "PRICE_PROPOSAL" -> "Ready to propose a price agreement."
                            "WAITING_FOR_CUSTOMER" -> "Provider sent proposal. Waiting for customer response."
                            "WAITING_FOR_PROVIDER" -> "Review the counter-offer then respond."
                            "PRICE_ACCEPTED" -> "Agreement complete! Tap dispatch to start."
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
                        // Customer View
                        val customerHint = when (phase) {
                            "PHOTO_REQUEST" -> "Provider is reviewing your request."
                            "WAITING_FOR_PHOTOS" -> "Provider requested photos. Use SELECT button below."
                            "PHOTOS_UPLOADED" -> "Photos sent. Waiting for provider review."
                            "PRICE_PROPOSAL" -> "Provider is preparing a price proposal."
                            "WAITING_FOR_CUSTOMER" -> "Proposal received. Please review and respond."
                            "WAITING_FOR_PROVIDER" -> "Counter-offer sent. Waiting for provider."
                            "PRICE_ACCEPTED" -> "Agreement complete! Waiting for provider dispatch."
                            else -> ""
                        }
                        if (customerHint.isNotEmpty()) {
                            Text(
                                customerHint,
                                textAlign = TextAlign.Center,
                                fontSize = 12.sp,
                                color = Color.Gray,
                                modifier = Modifier.fillMaxWidth().padding(bottom = if (phase == "WAITING_FOR_PHOTOS" && stagingUris.isEmpty()) 8.dp else 0.dp)
                            )
                        }
                        
                        if (phase == "WAITING_FOR_PHOTOS" && stagingUris.isEmpty()) {
                            Button(
                                onClick = { showPhotoPicker = true },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(Icons.Default.PhotoCamera, null)
                                Spacer(Modifier.width(8.dp))
                                Text("SELECT PHOTOS")
                            }
                        }
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

                    if (!jobState?.taskPhotos.isNullOrEmpty()) {
                        item {
                            TaskPhotosRow(
                                photos = jobState?.taskPhotos!!,
                                label = if (isProvider) "Inspect Task Photos" else "Photos Sent"
                            ) { index ->
                                selectedPhotosForGallery = jobState?.taskPhotos
                                initialPhotoIndex = index
                            }
                        }
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

                    if (stagingUris.isNotEmpty() && !isProvider) {
                        item {
                            PhotoStagingCard(
                                uris = stagingUris,
                                onRemove = { uri -> stagingUris = stagingUris.filter { it != uri } },
                                onAddMore = { showPhotoPicker = true },
                                onSend = {
                                    viewModel.uploadTaskPhotos(stagingUris)
                                },
                                onClear = { stagingUris = emptyList() },
                                isLoading = isLoading,
                                progressText = uploadProgress,
                                error = uploadError,
                                isSuccess = uploadSuccess
                            )
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
    
    val phase = job.currentNegotiationPhase ?: "NEUTRAL"
    val photoRequired = job.photoSharingRequired == true
    val negRequired = job.priceNegotiationRequired == true
    
    val steps = remember(job.status, phase, photoRequired, negRequired) {
        mutableListOf<NegotiationStep>().apply {
            // Step 1: Request Accepted
            add(NegotiationStep("Request Accepted", true))
            
            // Step 2: Photos Shared (only if required)
            if (photoRequired) {
                val photosDone = !listOf("PHOTO_REQUEST", "WAITING_FOR_PHOTOS", "PHOTOS_UPLOADED").contains(phase)
                add(NegotiationStep("Photos Shared", photosDone))
            }
            
            // Step 3: Price Agreed (only if required)
            if (negRequired) {
                val priceDone = phase == "PRICE_ACCEPTED" || phase == "DISPATCHED"
                add(NegotiationStep("Price Agreed", priceDone))
            }
            
            // Step 4: Provider Dispatched
            add(NegotiationStep("Provider Dispatched", phase == "DISPATCHED"))
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
            
            Spacer(modifier = Modifier.height(16.dp))
            
            if (isMe) {
                Text("Waiting for counterparty to respond...", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.Gray)
            } else {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    val currentRound = proposal.round
                    val isFinalRound = currentRound >= 4

                    if (isFinalRound) {
                        OutlinedButton(
                            onClick = { onAction("REJECT") },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(0.dp)
                        ) { Text("REJECT", fontSize = 11.sp) }
                    }
                    
                    Button(
                        onClick = { onAction("COUNTER") },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFA000)),
                        contentPadding = PaddingValues(0.dp)
                    ) { Text("BARGAIN", fontSize = 11.sp) }
                    
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

@Composable
fun PhotoStagingCard(
    uris: List<Uri>,
    onRemove: (Uri) -> Unit,
    onAddMore: () -> Unit,
    onSend: () -> Unit,
    onClear: () -> Unit,
    isLoading: Boolean,
    progressText: String,
    error: String?,
    isSuccess: Boolean
) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("Photos to Upload (${uris.size}/4)", fontWeight = FontWeight.Black, fontSize = 14.sp)
                if (!isLoading && !isSuccess) {
                    IconButton(onClick = onClear) {
                        Icon(Icons.Default.Delete, contentDescription = "Clear all", tint = Color.Gray)
                    }
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
            
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                modifier = Modifier.height(240.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(uris.size) { index ->
                    val uri = uris[index]
                    Box(modifier = Modifier.fillMaxSize().aspectRatio(1f).clip(RoundedCornerShape(12.dp))) {
                        Image(
                            painter = rememberAsyncImagePainter(uri),
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                        if (!isLoading && !isSuccess) {
                            IconButton(
                                onClick = { onRemove(uri) },
                                modifier = Modifier.align(Alignment.TopEnd).padding(4.dp).background(Color.Black.copy(alpha = 0.5f), CircleShape).size(24.dp)
                            ) {
                                Icon(Icons.Default.Close, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                }
                
                if (uris.size < 4 && !isLoading && !isSuccess) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .aspectRatio(1f)
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color(0xFFF5F5F5))
                                .clickable { onAddMore() },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.AddAPhoto, contentDescription = null, tint = Color.Gray)
                        }
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))

            if (isSuccess) {
                Row(
                    modifier = Modifier.fillMaxWidth().background(Color(0xFFE8F5E9), RoundedCornerShape(8.dp)).padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Check, contentDescription = null, tint = Color(0xFF2E7D32))
                    Spacer(Modifier.width(8.dp))
                    Text(progressText, color = Color(0xFF2E7D32), fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
                LaunchedEffect(Unit) {
                    kotlinx.coroutines.delay(2000)
                    onClear()
                }
            } else if (error != null) {
                Column(modifier = Modifier.fillMaxWidth().background(Color(0xFFFFEBEE), RoundedCornerShape(8.dp)).padding(12.dp)) {
                    Text("Upload Failed", color = Color(0xFFD32F2F), fontWeight = FontWeight.Black, fontSize = 12.sp)
                    Text(error, color = Color(0xFFD32F2F), fontSize = 11.sp)
                    Button(
                        onClick = onSend,
                        modifier = Modifier.padding(top = 8.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD32F2F))
                    ) {
                        Text("RETRY", fontSize = 10.sp)
                    }
                }
            } else if (isLoading) {
                Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth().clip(CircleShape))
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(progressText, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                }
            } else {
                Button(
                    onClick = onSend,
                    modifier = Modifier.fillMaxWidth(),
                    enabled = uris.isNotEmpty(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("SEND ${uris.size} PHOTOS", fontWeight = FontWeight.Black)
                }
            }
        }
    }
}

fun createTempUri(context: android.content.Context): Uri {
    val tempFile = java.io.File.createTempFile("captured_image_", ".jpg", context.cacheDir).apply {
        createNewFile()
        deleteOnExit()
    }
    return androidx.core.content.FileProvider.getUriForFile(
        context,
        "${context.packageName}.fileprovider",
        tempFile
    )
}

@Composable
fun TaskPhotosRow(photos: List<String>, label: String, onClick: (Int) -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, Color(0xFFEEEEEE))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(label, fontWeight = FontWeight.Black, fontSize = 12.sp, color = Color.Gray)
            Spacer(modifier = Modifier.height(12.dp))
            androidx.compose.foundation.lazy.LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(photos.size) { index ->
                    coil.compose.AsyncImage(
                        model = photos[index],
                        contentDescription = null,
                        modifier = Modifier
                            .size(100.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color.LightGray)
                            .clickable { onClick(index) },
                        contentScale = ContentScale.Crop
                    )
                }
            }
        }
    }
}
