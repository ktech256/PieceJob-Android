package com.piecejob.customer.ui.tracking

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.piecejob.core.ui.components.LiveTrackingMap

@Composable
fun CustomerTrackingScreen(
    jobId: String,
    viewModel: JobTrackingViewModel = hiltViewModel(),
    onChatOpen: (String) -> Unit,
    onSosTrigger: () -> Unit,
    onBack: () -> Unit
) {
    val job by viewModel.job.collectAsState()
    val providerLocation by viewModel.providerLocation.collectAsState()
    
    // Hardcoded for demo, in real app extract from job object
    val customerLocation = 0.0 to 0.0 

    LaunchedEffect(jobId) {
        viewModel.initTracking(jobId)
    }

    Box(modifier = Modifier.fillMaxSize()) {
        LiveTrackingMap(
            providerLocation = providerLocation,
            customerLocation = customerLocation,
            modifier = Modifier.fillMaxSize()
        )

        // Top Status Bar (Customer Red)
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 48.dp, start = 16.dp, end = 16.dp)
                .align(Alignment.TopCenter),
            colors = CardDefaults.cardColors(containerColor = Color(0xFFD32F2F)),
            shape = RoundedCornerShape(16.dp)
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = "Status: ${job?.status ?: "Connecting..."}", color = Color.White, fontSize = 12.sp)
                    Text(text = "Job #${jobId.takeLast(6)}", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                }
                if (job?.status == "ACCEPTED" || job?.status == "ARRIVED") {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp), color = Color.White, strokeWidth = 2.dp)
                }
            }
        }

        // Bottom Provider Info Card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter),
            shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 16.dp)
        ) {
            Column(modifier = Modifier.padding(24.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFFDECEA)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(text = "P", color = Color(0xFFD32F2F), fontSize = 24.sp, fontWeight = FontWeight.Bold)
                    }
                    
                    Spacer(modifier = Modifier.width(16.dp))
                    
                    Column(modifier = Modifier.weight(1f)) {
                        Text(text = "Provider assigned", fontWeight = FontWeight.ExtraBold, fontSize = 18.sp)
                        Text(text = "⭐ 4.8 • Top Rated", color = Color.Gray, fontSize = 14.sp)
                    }
                    
                    IconButton(
                        onClick = { job?.providerId?.let { onChatOpen(it) } },
                        modifier = Modifier.size(48.dp).background(Color(0xFFF5F5F5), CircleShape)
                    ) {
                        Text("💬")
                    }
                }
                
                Spacer(modifier = Modifier.height(24.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedButton(
                        onClick = onSosTrigger,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color.Red)
                    ) {
                        Text("SOS", color = Color.Red)
                    }
                    
                    Button(
                        onClick = { viewModel.cancelJob(); onBack() },
                        modifier = Modifier.weight(2f),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF5F5F5)),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(text = "Cancel Job", color = Color.DarkGray)
                    }
                }
            }
        }
    }
}
