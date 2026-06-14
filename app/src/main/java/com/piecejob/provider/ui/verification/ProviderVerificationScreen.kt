package com.piecejob.provider.ui.verification

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.piecejob.core.data.remote.VerificationStatusDto
import com.piecejob.core.data.remote.VerificationDocDto

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProviderVerificationScreen(
    viewModel: ProviderVerificationViewModel = hiltViewModel(),
    onUploadClick: (String) -> Unit,
    onBack: () -> Unit
) {
    val status by viewModel.status.collectAsState()
    val requirements by viewModel.requirements.collectAsState()
    val stagedDocs by viewModel.stagedDocs.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val isSubmitSuccess by viewModel.isSubmitSuccess.collectAsState()
    val error by viewModel.error.collectAsState()

    if (isSubmitSuccess) {
        AlertDialog(
            onDismissRequest = { viewModel.resetState() },
            title = { Text("Success") },
            text = { Text("Documents submitted successfully for review.") },
            confirmButton = {
                Button(onClick = { viewModel.resetState() }) {
                    Text("OK")
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Verification Hub", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        bottomBar = {
            if (stagedDocs.isNotEmpty()) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shadowElevation = 8.dp,
                    color = Color.White
                ) {
                    Button(
                        onClick = { viewModel.submitDocuments() },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp)
                            .height(56.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF006400)),
                        enabled = !isLoading
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(modifier = Modifier.size(24.dp), color = Color.White)
                        } else {
                            Text("SUBMIT DOCUMENTS", fontWeight = FontWeight.Black)
                        }
                    }
                }
            }
        }
    ) { padding ->
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(24.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Status Banner
            item(span = { GridItemSpan(2) }) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = when(status?.currentStatus) {
                            "APPROVED" -> Color(0xFFE8F5E9)
                            "PENDING" -> Color(0xFFFFF3E0)
                            "REJECTED" -> Color(0xFFFFEBEE)
                            else -> Color(0xFFF5F5F5)
                        }
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(20.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(text = "Current Level", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.Gray)
                            Text(text = status?.currentLevel ?: "NONE", fontWeight = FontWeight.Black, fontSize = 18.sp)
                        }
                        Surface(
                            color = when(status?.currentStatus) {
                                "APPROVED" -> Color(0xFF2E7D32)
                                "PENDING" -> Color(0xFFEF6C00)
                                "REJECTED" -> Color(0xFFC62828)
                                else -> Color.Gray
                            },
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = status?.currentStatus ?: "NOT STARTED",
                                fontWeight = FontWeight.Black,
                                fontSize = 10.sp,
                                color = Color.White,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                            )
                        }
                    }
                }
            }

            if (requirements == null) {
                item(span = { GridItemSpan(2) }) {
                    Box(modifier = Modifier.fillMaxWidth().height(200.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }
            } else {
                val groups = requirements?.requirements?.groupBy { it.group } ?: emptyMap()
                val groupOrder = listOf("STANDARD", "PROFESSIONAL", "TRADE", "HIGH_VETTING")

                groupOrder.forEach { groupKey ->
                    val docs = groups[groupKey]
                    if (docs != null) {
                        item(span = { GridItemSpan(2) }) {
                            Column(modifier = Modifier.padding(top = 8.dp)) {
                                Text(
                                    text = groupKey,
                                    fontWeight = FontWeight.Black,
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.primary,
                                    letterSpacing = 1.sp
                                )
                                HorizontalDivider(modifier = Modifier.padding(top = 4.dp))
                            }
                        }

                        items(docs) { docReq ->
                            val existingDoc = status?.latestRequest?.documents?.find { it.type == docReq.type }
                            val stagedPath = stagedDocs[docReq.type]
                            
                            DocumentStagingCard(
                                label = docReq.label,
                                status = existingDoc?.status ?: if (stagedPath != null) "STAGED" else "MISSING",
                                imagePath = stagedPath ?: existingDoc?.url,
                                isRequired = docReq.isRequired,
                                onAction = { onUploadClick(docReq.type) },
                                onDelete = { viewModel.removeStagedDocument(docReq.type) }
                            )
                        }
                    }
                }
            }

            item(span = { GridItemSpan(2) }) {
                Spacer(modifier = Modifier.height(100.dp))
            }
        }
    }
}

@Composable
fun DocumentStagingCard(
    label: String,
    status: String,
    imagePath: String?,
    isRequired: Boolean,
    onAction: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(180.dp),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            if (imagePath != null) {
                AsyncImage(
                    model = imagePath,
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
                
                // Overlay for Actions
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.3f))
                )
                
                if (status == "STAGED") {
                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier.align(Alignment.TopEnd).padding(4.dp).background(Color.White.copy(alpha = 0.8f), CircleShape)
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = null, tint = Color.Red, modifier = Modifier.size(16.dp))
                    }
                }
            } else {
                Column(
                    modifier = Modifier.fillMaxSize().padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = if (status == "APPROVED") Icons.Default.CheckCircle else Icons.Default.FileUpload,
                        contentDescription = null,
                        tint = if (status == "APPROVED") Color(0xFF2E7D32) else Color.LightGray,
                        modifier = Modifier.size(32.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(text = label, fontWeight = FontWeight.Bold, fontSize = 12.sp, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                }
            }

            // Bottom Status bar
            Surface(
                modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth(),
                color = when(status) {
                    "APPROVED" -> Color(0xFF2E7D32)
                    "PENDING" -> Color(0xFFEF6C00)
                    "STAGED" -> MaterialTheme.colorScheme.primary
                    "REJECTED" -> Color.Red
                    else -> Color.Gray.copy(alpha = 0.8f)
                }
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(text = status, color = Color.White, fontWeight = FontWeight.Black, fontSize = 9.sp)
                    if (status != "APPROVED" && status != "PENDING") {
                        Icon(
                            Icons.Default.Edit, 
                            null, 
                            tint = Color.White, 
                            modifier = Modifier.size(12.dp).clickable { onAction() }
                        )
                    }
                }
            }
            
            if (imagePath == null && status != "APPROVED" && status != "PENDING") {
                 Box(modifier = Modifier.fillMaxSize().clickable { onAction() })
            }
        }
    }
}
