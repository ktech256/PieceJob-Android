package com.piecejob.provider.ui.onboarding

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.piecejob.provider.ui.verification.ProviderVerificationViewModel

@Composable
fun DocumentUploadScreen(
    viewModel: ProviderVerificationViewModel = hiltViewModel(),
    onUploadComplete: () -> Unit
) {
    val isLoading by viewModel.isLoading.collectAsState()
    
    val documents = listOf(
        "GOVERNMENT_ID",
        "CRIMINAL_CHECK",
        "PROFESSIONAL_CERT",
        "TRADE_LICENSE",
        "EQUIPMENT_PROOF"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Verification Documents",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF212121)
        )
        Text(
            text = "Upload the required documents for your services",
            fontSize = 14.sp,
            color = Color.Gray,
            modifier = Modifier.padding(top = 8.dp)
        )

        Spacer(modifier = Modifier.height(32.dp))

        if (isLoading) {
            LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
        }

        documents.forEach { docType ->
            DocumentItem(docName = docType)
            Spacer(modifier = Modifier.height(16.dp))
        }

        Spacer(modifier = Modifier.weight(1f))

        Button(
            onClick = { 
                // In full implementation, we'd send the actual URLs from S3/Firebase Storage
                viewModel.submitVerification("STANDARD", emptyList())
                onUploadComplete() 
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF212121)),
            shape = RoundedCornerShape(12.dp),
            enabled = !isLoading
        ) {
            Text("SUBMIT ALL DOCUMENTS", fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun DocumentItem(docName: String) {
    var isUploaded by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFF5F5F5))
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(text = docName.replace("_", " "), fontWeight = FontWeight.Medium)
                Text(
                    text = if (isUploaded) "Ready" else "Missing",
                    fontSize = 12.sp,
                    color = if (isUploaded) Color(0xFF2E7D32) else Color(0xFFD32F2F)
                )
            }
            
            Button(
                onClick = { isUploaded = true },
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isUploaded) Color.Gray else Color(0xFF212121)
                ),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp)
            ) {
                Text(if (isUploaded) "Edit" else "Capture", fontSize = 12.sp)
            }
        }
    }
}
