package com.piecejob.provider.ui.verification

import android.graphics.Bitmap
import android.graphics.Color as AndroidColor
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.ui.platform.LocalContext
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
    onBack: () -> Unit
) {
    val status by viewModel.status.collectAsState()
    val requirements by viewModel.requirements.collectAsState()
    val stagedDocs by viewModel.stagedDocs.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val isSubmitSuccess by viewModel.isSubmitSuccess.collectAsState()
    val error by viewModel.error.collectAsState()
    val context = LocalContext.current

    var currentPickingType by remember { mutableStateOf<String?>(null) }
    var showSourcePicker by remember { mutableStateOf(false) }

    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicturePreview(),
        onResult = { bitmap ->
            if (bitmap != null && currentPickingType != null) {
                if (currentPickingType == "SELFIE") {
                    val valError = validateSelfie(bitmap)
                    if (valError != null) {
                        Toast.makeText(context, valError, Toast.LENGTH_LONG).show()
                        return@rememberLauncherForActivityResult
                    }
                }
                viewModel.stageDocument(currentPickingType!!, null, bitmap)
                currentPickingType = null
            }
        }
    )

    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent(),
        onResult = { uri ->
            if (uri != null && currentPickingType != null) {
                if (currentPickingType == "SELFIE") {
                    try {
                        val bitmap = if (android.os.Build.VERSION.SDK_INT < 28) {
                            android.provider.MediaStore.Images.Media.getBitmap(context.contentResolver, uri)
                        } else {
                            val source = android.graphics.ImageDecoder.createSource(context.contentResolver, uri)
                            android.graphics.ImageDecoder.decodeBitmap(source)
                        }
                        val valError = validateSelfie(bitmap)
                        if (valError != null) {
                            Toast.makeText(context, valError, Toast.LENGTH_LONG).show()
                            return@rememberLauncherForActivityResult
                        }
                    } catch (e: Exception) {
                        // If we can't decode for validation, we might skip or show error
                    }
                }
                viewModel.stageDocument(currentPickingType!!, uri, null)
                currentPickingType = null
            }
        }
    )

    val fileLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
        onResult = { uri ->
            if (uri != null && currentPickingType != null) {
                viewModel.stageDocument(currentPickingType!!, uri, null)
                currentPickingType = null
            }
        }
    )

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

    if (showSourcePicker) {
        ModalBottomSheet(onDismissRequest = { showSourcePicker = false }) {
            Column(modifier = Modifier.padding(16.dp).padding(bottom = 32.dp)) {
                val label = currentPickingType?.replace("_", " ") ?: ""
                Text("Select source for $label", fontWeight = FontWeight.Black, modifier = Modifier.padding(bottom = 16.dp))
                
                PickerOption(Icons.Default.CameraAlt, "Take Photo") {
                    cameraLauncher.launch(null)
                    showSourcePicker = false
                }
                PickerOption(Icons.Default.PhotoLibrary, "Choose From Gallery") {
                    galleryLauncher.launch("image/*")
                    showSourcePicker = false
                }
                
                val allowPDF = currentPickingType != "SELFIE" && currentPickingType != "TOOL_VERIFICATION"
                if (allowPDF) {
                    PickerOption(Icons.Default.InsertDriveFile, "Choose File (PDF)") {
                        fileLauncher.launch(arrayOf("application/pdf"))
                        showSourcePicker = false
                    }
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedButton(onClick = { showSourcePicker = false }, modifier = Modifier.fillMaxWidth()) {
                    Text("CANCEL")
                }
            }
        }
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
                            // Use status and rejectionReason from backend requirements DTO
                            DocumentStagingCard(
                                label = docReq.label,
                                status = if (stagedDocs.containsKey(docReq.type)) "STAGED" else docReq.status,
                                imagePath = stagedDocs[docReq.type] ?: status?.latestRequest?.documents?.find { it.type == docReq.type }?.url,
                                isRequired = docReq.isRequired,
                                rejectionReason = docReq.rejectionReason,
                                onAction = { 
                                    currentPickingType = docReq.type
                                    showSourcePicker = true
                                },
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
    rejectionReason: String? = null,
    onAction: () -> Unit,
    onDelete: () -> Unit
) {
    val isPdf = imagePath?.endsWith(".pdf", ignoreCase = true) == true || imagePath?.contains("application/pdf") == true
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(190.dp), // Slightly taller for reason
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            if (imagePath != null) {
                if (isPdf) {
                    Column(
                        modifier = Modifier.fillMaxSize().padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(Icons.Default.PictureAsPdf, null, modifier = Modifier.size(48.dp), tint = Color.Red)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("PDF Document", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        Text(imagePath.split("/").last(), fontSize = 8.sp, color = Color.Gray, maxLines = 1)
                    }
                } else {
                    AsyncImage(
                        model = imagePath,
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                    
                    // Filename overlay for images
                    Box(
                        modifier = Modifier.align(Alignment.TopStart).padding(8.dp).background(Color.Black.copy(alpha = 0.5f), RoundedCornerShape(4.dp))
                    ) {
                        Text(
                            text = imagePath.split("/").last(),
                            color = Color.White,
                            fontSize = 8.sp,
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                        )
                    }
                }
                
                // Overlay for Actions
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.2f))
                )
                
                if (status == "STAGED") {
                    Row(
                        modifier = Modifier.align(Alignment.TopEnd).padding(8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Replace button (Edit)
                        IconButton(
                            onClick = onAction,
                            modifier = Modifier.size(32.dp).background(Color.White.copy(alpha = 0.8f), CircleShape)
                        ) {
                            Icon(Icons.Default.Edit, contentDescription = "Replace", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                        }
                        
                        // Remove button
                        IconButton(
                            onClick = onDelete,
                            modifier = Modifier.size(32.dp).background(Color.White.copy(alpha = 0.8f), CircleShape)
                        ) {
                            Icon(Icons.Default.Delete, contentDescription = "Remove", tint = Color.Red, modifier = Modifier.size(16.dp))
                        }
                    }
                }
            } else {
                Column(
                    modifier = Modifier.fillMaxSize().padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = if (status == "VERIFIED") Icons.Default.CheckCircle else Icons.Default.FileUpload,
                        contentDescription = null,
                        tint = if (status == "VERIFIED") Color(0xFF2E7D32) else Color.LightGray,
                        modifier = Modifier.size(32.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(text = label, fontWeight = FontWeight.Bold, fontSize = 12.sp, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                    
                    if (status == "REJECTED" && rejectionReason != null) {
                        Text(
                            text = "Reason: $rejectionReason",
                            color = Color.Red,
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(top = 4.dp),
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                }
            }

            // Bottom Status bar
            Surface(
                modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth(),
                color = when(status) {
                    "VERIFIED" -> Color(0xFF2E7D32)
                    "PENDING REVIEW" -> Color(0xFFEF6C00)
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
                    if (status == "REJECTED") {
                         Icon(Icons.Default.Refresh, null, tint = Color.White, modifier = Modifier.size(12.dp).clickable { onAction() })
                    }
                }
            }
            
            // Full card clickable if not verified/pending review/staged
            if (imagePath == null && status != "VERIFIED" && status != "PENDING REVIEW") {
                 Box(modifier = Modifier.fillMaxSize().clickable { onAction() })
            }
        }
    }
}

@Composable
fun PickerOption(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, null, tint = MaterialTheme.colorScheme.primary)
            Spacer(modifier = Modifier.width(16.dp))
            Text(label, fontWeight = FontWeight.Bold)
        }
    }
}

fun validateSelfie(bitmap: Bitmap): String? {
    val width = bitmap.width
    val height = bitmap.height
    var totalLuminance = 0L
    val sampleSize = 100
    
    // Simple luminance check
    for (i in 0 until sampleSize) {
        val x = (Math.random() * width).toInt()
        val y = (Math.random() * height).toInt()
        val pixel = bitmap.getPixel(x, y)
        val r = AndroidColor.red(pixel)
        val g = AndroidColor.green(pixel)
        val b = AndroidColor.blue(pixel)
        totalLuminance += (0.299 * r + 0.587 * g + 0.114 * b).toLong()
    }
    
    val avgLuminance = totalLuminance / sampleSize
    
    return when {
        avgLuminance < 30 -> "Image is too dark. Please use better lighting."
        avgLuminance > 225 -> "Image is too bright. Please avoid direct glare."
        else -> null
    }
}
