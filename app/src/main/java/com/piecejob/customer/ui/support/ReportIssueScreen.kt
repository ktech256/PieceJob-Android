package com.piecejob.customer.ui.support

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun ReportIssueScreen(
    jobId: String?,
    onSubmit: (String, String, String) -> Unit
) {
    var subject by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var type by remember { mutableStateOf("SERVICE_QUALITY") }

    val types = listOf("PAYMENT_DISPUTE", "SERVICE_QUALITY", "PROVIDER_BEHAVIOR", "NO_SHOW", "OTHER")

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
            .padding(16.dp)
    ) {
        Text(
            text = "Report an Issue",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF410200)
        )
        Text(
            text = jobId?.let { "Related to Job: $it" } ?: "General platform issue",
            fontSize = 12.sp,
            color = Color.Gray
        )

        Spacer(modifier = Modifier.height(32.dp))

        Text(text = "Issue Category", fontWeight = FontWeight.Bold, fontSize = 14.sp)
        // Simple selection logic
        Row(modifier = Modifier.padding(vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            types.take(2).forEach { t ->
                FilterChip(
                    selected = type == t,
                    onClick = { type = t },
                    label = { Text(t.replace("_", " "), fontSize = 10.sp) }
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = subject,
            onValueChange = { subject = it },
            label = { Text("Subject") },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp)
        )

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = description,
            onValueChange = { description = it },
            label = { Text("Describe what happened...") },
            modifier = Modifier.fillMaxWidth().height(150.dp),
            shape = RoundedCornerShape(12.dp)
        )

        Spacer(modifier = Modifier.weight(1f))

        Button(
            onClick = { onSubmit(type, subject, description) },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF410200)),
            shape = RoundedCornerShape(12.dp),
            enabled = subject.isNotBlank() && description.isNotBlank()
        ) {
            Text("Submit Report", fontWeight = FontWeight.Bold)
        }
    }
}
