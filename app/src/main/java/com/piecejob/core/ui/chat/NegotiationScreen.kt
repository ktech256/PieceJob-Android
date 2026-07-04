package com.piecejob.core.ui.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.Sell
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.piecejob.core.data.remote.ServiceDto
import com.piecejob.core.data.remote.dto.MessageDto

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NegotiationScreen(
    jobId: String,
    otherUserId: String,
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
    
    val isProvider = com.piecejob.BuildConfig.FLAVOR == "provider"

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetMultipleContents()
    ) { uris ->
        if (uris.isNotEmpty()) {
            viewModel.uploadTaskPhotos(uris.take(4))
        }
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
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            if (serviceConfig?.photoSharingRequired == true) {
                                Button(
                                    onClick = { viewModel.requestPhotos() },
                                    modifier = Modifier.weight(1f),
                                    enabled = jobState?.taskPhotosRequested != true,
                                    shape = RoundedCornerShape(12.dp),
                                    contentPadding = PaddingValues(0.dp)
                                ) {
                                    Icon(Icons.Default.PhotoCamera, null, modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(4.dp))
                                    Text("Request Photos", fontSize = 11.sp)
                                }
                            }
                            
                            val canProposePrice = if (serviceConfig?.photoSharingRequired == true) {
                                jobState?.taskPhotosSeen == true
                            } else {
                                true
                            }

                            if (serviceConfig?.priceNegotiationRequired == true) {
                                Button(
                                    onClick = { showPriceDialog = true },
                                    modifier = Modifier.weight(1f),
                                    enabled = canProposePrice,
                                    shape = RoundedCornerShape(12.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFA000)),
                                    contentPadding = PaddingValues(0.dp)
                                ) {
                                    Icon(Icons.Default.Sell, null, modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(4.dp))
                                    Text("Propose Price", fontSize = 11.sp)
                                }
                            } else if (jobState?.status == "PROVIDER_ACCEPTED") {
                                Button(
                                    onClick = { viewModel.confirmDispatch() },
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(12.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32)),
                                    contentPadding = PaddingValues(0.dp)
                                ) {
                                    Icon(Icons.Default.LocalShipping, null, modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(4.dp))
                                    Text("Confirm Dispatch", fontSize = 11.sp)
                                }
                            }
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
                    
                    items(messages) { msg ->
                        ChatBubble(
                            msg = msg,
                            isMe = msg.senderId._id != otherUserId,
                            onAction = { action, meta ->
                                when (action) {
                                    "UPLOAD_PHOTOS" -> photoPickerLauncher.launch("image/*")
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
