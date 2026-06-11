package com.piecejob.provider.ui.verification

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.piecejob.core.data.remote.VerificationStatusDto
import com.piecejob.core.data.remote.VerificationDocDto

@Composable
fun ProviderVerificationScreen(
    status: VerificationStatusDto?,
    onUploadClick: (String) -> Unit,
    onSubmitRequest: (String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
            .padding(16.dp)
    ) {
        Text(
            text = "Verification Status",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF006400) // Forest Green
        )
        
        Spacer(modifier = Modifier.height(24.dp))

        // Status Banner
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
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(text = "Current Level", fontSize = 12.sp, color = Color.Gray)
                    Text(text = status?.currentLevel ?: "NONE", fontWeight = FontWeight.Black, fontSize = 18.sp)
                }
                Text(
                    text = status?.currentStatus ?: "NOT STARTED",
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    color = when(status?.currentStatus) {
                        "APPROVED" -> Color(0xFF2E7D32)
                        "PENDING" -> Color(0xFFEF6C00)
                        "REJECTED" -> Color(0xFFC62828)
                        else -> Color.Gray
                    }
                )
            }
        }

        if (status?.latestRequest?.rejectionReason != null) {
            Spacer(modifier = Modifier.height(16.dp))
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFFFEBEE))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(text = "Rejection Reason", fontWeight = FontWeight.Bold, color = Color(0xFFC62828), fontSize = 12.sp)
                    Text(text = status.latestRequest.rejectionReason, fontSize = 14.sp)
                }
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        Text(text = "Verification Requirements", fontWeight = FontWeight.Bold, fontSize = 16.sp)
        
        Spacer(modifier = Modifier.height(16.dp))

        // List of items based on level
        val requirements = listOf("GOVERNMENT_ID", "SELFIE", "CRIMINAL_CHECK")
        
        LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            items(requirements) { req ->
                val doc = status?.latestRequest?.documents?.find { it.type == req }
                RequirementRow(
                    label = req.replace('_', ' '),
                    status = doc?.status ?: "MISSING",
                    onUpload = { onUploadClick(req) }
                )
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        Button(
            onClick = { onSubmitRequest("STANDARD") },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF006400)),
            shape = RoundedCornerShape(12.dp),
            enabled = status?.currentStatus != "PENDING"
        ) {
            Text("Submit for Standard Review", fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun RequirementRow(label: String, status: String, onUpload: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFFF9F9F9), RoundedCornerShape(12.dp))
            .padding(16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(text = label, fontWeight = FontWeight.Bold, fontSize = 14.sp)
            Text(text = status, fontSize = 10.sp, color = when(status) {
                "APPROVED" -> Color(0xFF2E7D32)
                "REJECTED" -> Color.Red
                "PENDING" -> Color(0xFFEF6C00)
                else -> Color.Gray
            })
        }
        
        if (status != "APPROVED" && status != "PENDING") {
            TextButton(onClick = onUpload) {
                Text("Upload", fontWeight = FontWeight.Bold, color = Color(0xFF006400))
            }
        }
    }
}
