package com.piecejob.provider.ui.onboarding

import android.graphics.Bitmap
import android.graphics.Color as AndroidColor
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.InsertDriveFile
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.piecejob.provider.ui.verification.ProviderVerificationViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DocumentUploadScreen(
    docType: String,
    viewModel: ProviderVerificationViewModel = hiltViewModel(),
    onUploadComplete: () -> Unit
) {
    val isLoading by viewModel.isLoading.collectAsState()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    
    var imageUri by remember { mutableStateOf<Uri?>(null) }
    var bitmap by remember { mutableStateOf<Bitmap?>(null) }
    var showPicker by remember { mutableStateOf(false) }

    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicturePreview(),
        onResult = { b -> if (b != null) bitmap = b; imageUri = null }
    )

    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent(),
        onResult = { uri -> imageUri = uri; bitmap = null }
    )

    val fileLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
        onResult = { uri -> imageUri = uri; bitmap = null }
    )

    val isSelfie = docType == "SELFIE"
    val allowPDF = !isSelfie && docType != "TOOL_VERIFICATION"

    if (showPicker) {
        ModalBottomSheet(onDismissRequest = { showPicker = false }) {
            Column(modifier = Modifier.padding(16.dp).padding(bottom = 32.dp)) {
                Text("Select Document Source", fontWeight = FontWeight.Black, modifier = Modifier.padding(bottom = 16.dp))
                
                PickerOption(Icons.Default.CameraAlt, "Take Photo") {
                    cameraLauncher.launch(null)
                    showPicker = false
                }
                PickerOption(Icons.Default.PhotoLibrary, "Choose From Gallery") {
                    galleryLauncher.launch("image/*")
                    showPicker = false
                }
                if (allowPDF) {
                    PickerOption(Icons.Default.InsertDriveFile, "Choose File (PDF)") {
                        fileLauncher.launch(arrayOf("application/pdf"))
                        showPicker = false
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedButton(onClick = { showPicker = false }, modifier = Modifier.fillMaxWidth()) {
                    Text("CANCEL")
                }
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Upload ${docType.replace("_", " ")}",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF212121)
        )
        Text(
            text = "Please ensure the image is clear and well lit.",
            fontSize = 14.sp,
            color = Color.Gray,
            modifier = Modifier.padding(top = 8.dp)
        )

        Spacer(modifier = Modifier.height(48.dp))

        if (isLoading) {
            LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            Spacer(modifier = Modifier.height(16.dp))
        }

        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .height(260.dp)
                .clickable { showPicker = true },
            shape = RoundedCornerShape(24.dp),
            color = Color(0xFFF8F9FA),
            border = androidx.compose.foundation.BorderStroke(2.dp, Color.LightGray.copy(alpha = 0.5f))
        ) {
            Box(contentAlignment = Alignment.Center) {
                if (bitmap != null) {
                    Image(bitmap!!.asImageBitmap(), null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
                } else if (imageUri != null) {
                    Text("File Selected: ${imageUri?.path?.takeLast(20)}")
                } else {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.FileUpload, contentDescription = null, modifier = Modifier.size(48.dp), tint = Color.Gray)
                        Spacer(modifier = Modifier.height(12.dp))
                        Text("Tap to Select Document", color = Color.Gray, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        Button(
            onClick = { 
                if (isSelfie && bitmap != null) {
                    val error = validateSelfie(bitmap!!)
                    if (error != null) {
                        Toast.makeText(context, error, Toast.LENGTH_LONG).show()
                        return@Button
                    }
                }
                // viewModel.uploadDocument(docType, uri or bitmap)
                onUploadComplete() 
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(58.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF006400)),
            shape = RoundedCornerShape(16.dp),
            enabled = !isLoading && (bitmap != null || imageUri != null)
        ) {
            Text("UPLOAD DOCUMENT", fontWeight = FontWeight.Black)
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
