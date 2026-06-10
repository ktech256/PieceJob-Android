package com.piecejob.provider.ui.dashboard

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

// Provider Theme Colors
val ForestGreen = Color(0xFF006400)
val OliveDrab = Color(0xFF737000)
val CadetGray = Color(0xFF91A3B0)

@Composable
fun ProviderDashboardScreen(
    onAcceptJob: (String) -> Unit,
    onRejectJob: (String) -> Unit,
    onStartJob: (String) -> Unit,
    onCompleteJob: (String) -> Unit
) {
    // Mocking an incoming job state
    var incomingJob by remember { mutableStateOf<String?>(null) }
    var activeJob by remember { mutableStateOf<String?>(null) }
    var jobStatus by remember { mutableStateOf("ACCEPTED") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Header
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(ForestGreen, RoundedCornerShape(8.dp))
                .padding(16.dp)
        ) {
            Text(
                text = "Provider Dashboard",
                color = Color.White,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Earnings Summary Placeholder
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = CadetGray.copy(alpha = 0.1f))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(text = "Today's Earnings", fontSize = 14.sp, color = Color.Gray)
                Text(text = "$450.00", fontSize = 28.sp, fontWeight = FontWeight.Black, color = ForestGreen)
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Job Interaction Logic
        if (incomingJob != null) {
            BroadcastCard(
                jobId = incomingJob!!,
                onAccept = {
                    onAcceptJob(it)
                    activeJob = it
                    incomingJob = null
                },
                onReject = {
                    onRejectJob(it)
                    incomingJob = null
                }
            )
        } else if (activeJob != null) {
            ActiveJobCard(
                jobId = activeJob!!,
                status = jobStatus,
                onStart = { onStartJob(it); jobStatus = "STARTED" },
                onComplete = { onCompleteJob(it); activeJob = null; jobStatus = "ACCEPTED" }
            )
        } else {
            Text(text = "Waiting for new jobs...", color = Color.Gray, fontStyle = androidx.compose.ui.text.font.FontStyle.Italic)
        }
    }
}

@Composable
fun BroadcastCard(jobId: String, onAccept: (String) -> Unit, onReject: (String) -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(text = "NEW JOB REQUEST", fontWeight = FontWeight.Bold, color = OliveDrab)
            Text(text = "Service: House Cleaning", fontSize = 18.sp)
            Text(text = "Distance: 1.2 km", fontSize = 14.sp, color = Color.Gray)
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = { onAccept(jobId) },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = ForestGreen)
                ) {
                    Text("Accept")
                }
                OutlinedButton(
                    onClick = { onReject(jobId) },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Reject")
                }
            }
        }
    }
}

@Composable
fun ActiveJobCard(jobId: String, status: String, onStart: (String) -> Unit, onComplete: (String) -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(text = "ACTIVE JOB", fontWeight = FontWeight.Bold, color = ForestGreen)
            Text(text = "Status: $status", fontSize = 18.sp)
            
            Spacer(modifier = Modifier.height(16.dp))
            
            if (status == "ARRIVED") {
                Button(
                    onClick = { onStart(jobId) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = ForestGreen)
                ) {
                    Text("Start Job")
                }
            } else if (status == "STARTED") {
                Button(
                    onClick = { onComplete(jobId) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Red)
                ) {
                    Text("Complete Job")
                }
            } else {
                Text(text = "Navigate to Customer...", color = ForestGreen)
            }
        }
    }
}
