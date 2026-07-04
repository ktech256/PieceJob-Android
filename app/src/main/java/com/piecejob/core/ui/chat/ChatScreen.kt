package com.piecejob.core.ui.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.Sell
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.piecejob.core.data.remote.ServiceDto
import com.piecejob.core.data.remote.dto.MessageDto

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.ui.layout.ContentScale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    jobId: String,
    otherUserId: String,
    viewModel: ChatViewModel = hiltViewModel(),
    onBack: () -> Unit
) {
    val messages by viewModel.messages.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val serviceConfig by viewModel.serviceConfig.collectAsState()
    val jobState by viewModel.jobState.collectAsState()
    var messageText by remember { mutableStateOf("") }
    
    var showPriceDialog by remember { mutableStateOf(false) }
    var priceAmount by remember { mutableStateOf("") }
    var priceNote by remember { mutableStateOf("") }
    
    val isProvider = com.piecejob.BuildConfig.FLAVOR == "provider"

    LaunchedEffect(jobState?.status) {
        if (jobState?.status == "PROVIDER_ACCEPTED") {
            // Strictly enforce the separation requested by the user.
            // If we are in ChatScreen but the job is in negotiation, we should be in NegotiationScreen.
            onBack() // Or navigate to Negotiation.
        }
    }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetMultipleContents()
    ) { uris ->
        if (uris.isNotEmpty()) {
            viewModel.uploadTaskPhotos(uris.take(4))
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
                title = { Text("Chat - Job #$jobId", fontSize = 16.sp, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
            )
        },
        bottomBar = {
            Surface(tonalElevation = 4.dp, shadowElevation = 8.dp) {
                Column {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 12.dp, end = 12.dp, bottom = 12.dp, top = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = messageText,
                            onValueChange = { messageText = it },
                            modifier = Modifier.weight(1f),
                            placeholder = { Text("Type a message...") },
                            shape = RoundedCornerShape(24.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        IconButton(
                            onClick = {
                                if (messageText.isNotBlank()) {
                                    viewModel.sendMessage(otherUserId, messageText)
                                    messageText = ""
                                }
                            },
                            enabled = messageText.isNotBlank(),
                            colors = IconButtonDefaults.iconButtonColors(contentColor = MaterialTheme.colorScheme.primary)
                        ) {
                            Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Send")
                        }
                    }
                }
            }
        }
    ) { padding ->
        Box(modifier = Modifier.padding(padding).fillMaxSize()) {
            if (isLoading && messages.isEmpty()) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    reverseLayout = false
                ) {
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

@Composable
fun ChatBubble(msg: MessageDto, isMe: Boolean, onAction: (String, Map<String, Any>?) -> Unit = { _, _ -> }) {
    val metadata = msg.metadata
    val type = metadata?.get("type") as? String

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalAlignment = if (isMe) Alignment.End else Alignment.Start
    ) {
        if (type != null) {
            StructuredMessageCard(type, metadata, isMe, onAction)
        } else {
            Surface(
                color = if (isMe) MaterialTheme.colorScheme.primary else Color(0xFFF1F0F0),
                contentColor = if (isMe) Color.White else Color.Black,
                shape = RoundedCornerShape(
                    topStart = 16.dp,
                    topEnd = 16.dp,
                    bottomStart = if (isMe) 16.dp else 4.dp,
                    bottomEnd = if (isMe) 4.dp else 16.dp
                )
            ) {
                Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
                    if (msg.mediaUrl != null && msg.mediaType == "IMAGE") {
                        coil.compose.AsyncImage(
                            model = msg.mediaUrl,
                            contentDescription = null,
                            modifier = Modifier.sizeIn(maxWidth = 200.dp, maxHeight = 200.dp).padding(bottom = 4.dp)
                        )
                    }
                    Text(
                        text = msg.text ?: "",
                        fontSize = 14.sp
                    )
                }
            }
        }
        Text(
            text = msg.createdAt.takeLast(5),
            fontSize = 10.sp,
            color = Color.Gray,
            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
        )
    }
}

@Composable
fun StructuredMessageCard(type: String, metadata: Map<String, Any>, isMe: Boolean, onAction: (String, Map<String, Any>?) -> Unit) {
    Card(
        modifier = Modifier.widthIn(max = 280.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFF5F5F5)),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color.LightGray)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            when (type) {
                "PHOTO_REQUEST" -> {
                    Text("📷 Photo Request", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    Text("The provider requested photos for this task.", fontSize = 13.sp)
                    if (!isMe) {
                        Button(
                            onClick = { onAction("UPLOAD_PHOTOS", metadata) },
                            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                            contentPadding = PaddingValues(0.dp)
                        ) {
                            Text("Upload Photos", fontSize = 12.sp)
                        }
                    }
                }
                "PHOTO_UPLOAD" -> {
                    Text("✅ Photos Uploaded", fontWeight = FontWeight.Bold, color = Color(0xFF2E7D32))
                    val photos = (metadata["allPhotos"] as? List<*>)?.filterIsInstance<String>() ?: emptyList()
                    androidx.compose.foundation.lazy.grid.LazyVerticalGrid(
                        columns = androidx.compose.foundation.lazy.grid.GridCells.Fixed(2),
                        modifier = Modifier.height(120.dp).padding(top = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        items(photos.size) { index ->
                            coil.compose.AsyncImage(
                                model = photos[index],
                                contentDescription = null,
                                modifier = Modifier.fillMaxSize().background(Color.Gray),
                                contentScale = androidx.compose.ui.layout.ContentScale.Crop
                            )
                        }
                    }
                    if (!isMe) {
                        Button(
                            onClick = { onAction("MARK_SEEN", metadata) },
                            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color.Black),
                            contentPadding = PaddingValues(0.dp)
                        ) {
                            Text("Mark as Seen", fontSize = 12.sp)
                        }
                    }
                }
                "PRICE_PROPOSAL" -> {
                    val amount = metadata["amount"] as? Double ?: 0.0
                    val round = (metadata["round"] as? Double)?.toInt() ?: 1
                    Text("💰 Price Proposal (Round $round)", fontWeight = FontWeight.Bold)
                    Text("Proposed Amount: R$amount", fontSize = 18.sp, fontWeight = FontWeight.Black)
                    if (metadata["note"] != null) {
                        Text("Note: ${metadata["note"]}", fontSize = 12.sp, color = Color.Gray)
                    }
                    if (!isMe) {
                        Row(modifier = Modifier.padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(
                                onClick = { onAction("REJECT_PROPOSAL", metadata) },
                                modifier = Modifier.weight(1f),
                                contentPadding = PaddingValues(0.dp)
                            ) { Text("Reject", fontSize = 11.sp) }
                            Button(
                                onClick = { onAction("ACCEPT_PROPOSAL", metadata) },
                                modifier = Modifier.weight(1f),
                                contentPadding = PaddingValues(0.dp)
                            ) { Text("Accept", fontSize = 11.sp) }
                        }
                        Button(
                            onClick = { onAction("COUNTER_PROPOSAL", metadata) },
                            modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFA000)),
                            contentPadding = PaddingValues(0.dp)
                        ) { Text("Counter Offer", fontSize = 11.sp) }
                    }
                }
                "PRICE_ACCEPTED" -> {
                    val amount = metadata["amount"] as? Double ?: 0.0
                    Text("🤝 Price Agreed", fontWeight = FontWeight.Bold, color = Color(0xFF2E7D32))
                    Text("Agreed Price: R$amount", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    Text("Negotiation concluded.", fontSize = 12.sp, color = Color.Gray)
                }
            }
        }
    }
}
