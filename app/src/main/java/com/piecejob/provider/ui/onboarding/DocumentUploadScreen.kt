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

// Provider Theme Colors (Aligned with Section 2.1)
val ForestGreen = Color(0xFF006400)

@Composable
fun DocumentUploadScreen(
    onUploadComplete: () -> Unit
) {
    val documents = listOf(
        "Government ID / Passport",
        "Criminal Background Check",
        "Professional Certification",
        "Trade License (Optional)",
        "Proof of Equipment"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Provider Verification",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = ForestGreen
        )
        Text(
            text = "Upload the required documents for your services",
            fontSize = 14.sp,
            color = Color.Gray,
            modifier = Modifier.padding(top = 8.dp)
        )

        Spacer(modifier = Modifier.height(32.dp))

        documents.forEach { docType ->
            DocumentItem(docName = docType)
            Spacer(modifier = Modifier.height(16.dp))
        }

        Spacer(modifier = Modifier.weight(1f))

        Button(
            onClick = onUploadComplete,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            colors = ButtonDefaults.buttonColors(containerColor = ForestGreen),
            shape = RoundedCornerShape(8.dp)
        ) {
            Text("Submit for Verification", fontWeight = FontWeight.Bold)
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
                Text(text = docName, fontWeight = FontWeight.Medium)
                Text(
                    text = if (isUploaded) "Uploaded" else "Pending",
                    fontSize = 12.sp,
                    color = if (isUploaded) Color(0xFF2E7D32) else Color.Red
                )
            }
            
            Button(
                onClick = { isUploaded = true },
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isUploaded) Color.Gray else ForestGreen
                ),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp)
            ) {
                Text(if (isUploaded) "Edit" else "Upload", fontSize = 12.sp)
            }
        }
    }
}
