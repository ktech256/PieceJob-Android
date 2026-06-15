package com.piecejob.provider.ui.profile

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Gavel
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.piecejob.core.data.remote.dto.DisputeDto

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProviderDisputeScreen(
    viewModel: ProviderDisputeViewModel = hiltViewModel(),
    onBack: () -> Unit
) {
    val disputes by viewModel.disputes.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    
    var showAddDialog by remember { mutableStateOf(false) }
    var jobId by remember { mutableStateOf("") }
    var reason by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var selectedEvidence by remember { mutableStateOf<List<android.net.Uri>>(emptyList()) }

    val context = androidx.compose.ui.platform.LocalContext.current
    val multiLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.GetMultipleContents(),
        onResult = { selectedEvidence = it }
    )

    if (showAddDialog) {
        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = { Text("Raise a Dispute", fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    OutlinedTextField(value = jobId, onValueChange = { jobId = it }, label = { Text("Job ID") }, modifier = Modifier.fillMaxWidth())
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(value = reason, onValueChange = { reason = it }, label = { Text("Reason (e.g. Non-payment)") }, modifier = Modifier.fillMaxWidth())
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(value = description, onValueChange = { description = it }, label = { Text("Description") }, modifier = Modifier.fillMaxWidth(), minLines = 3)
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(text = "Evidence (Photos)", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(onClick = { multiLauncher.launch("image/*") }, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = Color.DarkGray)) {
                        Text(if (selectedEvidence.isEmpty()) "Add Photos" else "${selectedEvidence.size} Photos Selected")
                    }
                }
            },
            confirmButton = {
                Button(onClick = {
                    viewModel.raiseDispute(jobId, reason, description, selectedEvidence, context)
                    showAddDialog = false
                    jobId = ""
                    reason = ""
                    description = ""
                    selectedEvidence = emptyList()
                }, enabled = jobId.isNotEmpty() && reason.isNotEmpty()) { Text("Submit") }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Dispute Center", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showAddDialog = true }) {
                Icon(Icons.Default.Add, contentDescription = "Raise Dispute")
            }
        }
    ) { padding ->
        if (isLoading && disputes.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else if (disputes.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("No active disputes.", color = Color.Gray)
            }
        } else {
            LazyColumn(modifier = Modifier.fillMaxSize().padding(padding).padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                items(disputes) { dispute ->
                    DisputeRow(dispute)
                }
            }
        }
    }
}

@Composable
fun DisputeRow(dispute: DisputeDto) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Gavel, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(text = "Job: ${dispute.jobId.takeLast(6)}", fontWeight = FontWeight.Bold)
                }
                DisputeStatusBadge(dispute.status)
            }
            Spacer(modifier = Modifier.height(12.dp))
            Text(text = dispute.reason, fontWeight = FontWeight.Black, fontSize = 15.sp)
            Text(text = dispute.description, fontSize = 13.sp, color = Color.DarkGray, maxLines = 2)
            
            if (dispute.evidenceUrls.isNotEmpty()) {
                Spacer(modifier = Modifier.height(12.dp))
                Text(text = "${dispute.evidenceUrls.size} Evidence Photos Attached", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            }

            if (dispute.resolution != null) {
                Spacer(modifier = Modifier.height(12.dp))
                Surface(color = Color(0xFFE8F5E9), shape = RoundedCornerShape(8.dp), modifier = Modifier.fillMaxWidth()) {
                    Text(text = "Resolution: ${dispute.resolution}", modifier = Modifier.padding(12.dp), fontSize = 12.sp, color = Color(0xFF2E7D32), fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun DisputeStatusBadge(status: String) {
    Surface(
        color = when(status) {
            "OPEN" -> Color(0xFFE3F2FD)
            "RESOLVED" -> Color(0xFFE8F5E9)
            "CLOSED" -> Color(0xFFF5F5F5)
            else -> Color(0xFFFFF3E0)
        },
        shape = RoundedCornerShape(8.dp)
    ) {
        Text(
            text = status,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            color = when(status) {
                "OPEN" -> Color(0xFF1976D2)
                "RESOLVED" -> Color(0xFF2E7D32)
                "CLOSED" -> Color(0xFF616161)
                else -> Color(0xFFE65100)
            }
        )
    }
}
