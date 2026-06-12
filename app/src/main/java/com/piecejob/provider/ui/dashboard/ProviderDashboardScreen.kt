package com.piecejob.provider.ui.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.piecejob.core.data.remote.dto.JobDto

@Composable
fun ProviderDashboardScreen(
    viewModel: ProviderDashboardViewModel = hiltViewModel(),
    onSosTrigger: () -> Unit
) {
    val stats by viewModel.stats.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val isOnline by viewModel.isOnline.collectAsState()
    val isShadowBanned by viewModel.isShadowBanned.collectAsState()
    val availableJobs by viewModel.availableJobs.collectAsState()
    val activeJob by viewModel.activeJob.collectAsState()
    
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF8F9FA))
    ) {
        // Premium Dark Header (Provider Branding)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.primary, RoundedCornerShape(bottomStart = 24.dp, bottomEnd = 24.dp))
                .padding(top = 48.dp, bottom = 24.dp, start = 20.dp, end = 20.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Provider Dashboard",
                        color = Color.White,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                    Text(
                        text = if (isOnline) "You are currently ONLINE" else "You are currently OFFLINE",
                        color = if (isOnline) Color(0xFF4CAF50) else Color.White.copy(alpha = 0.7f),
                        fontSize = 12.sp
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = onSosTrigger,
                        modifier = Modifier.size(40.dp).background(Color(0xFFD32F2F), CircleShape)
                    ) {
                        Text("SOS", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 10.sp)
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Switch(
                        checked = isOnline,
                        onCheckedChange = { viewModel.toggleOnlineStatus(context) },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = Color(0xFF4CAF50),
                            uncheckedThumbColor = Color.Gray,
                            uncheckedTrackColor = Color.DarkGray
                        )
                    )
                }
            }
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            if (isShadowBanned) {
                item { ShadowBanNotice() }
            }

            // Earnings Summary Card
            item {
                EarningsCard(isLoading, stats?.earningsToday ?: 0.0)
            }

            if (activeJob != null) {
                item {
                    Text("Current Active Job", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
                item {
                    ActiveJobCard(
                        job = activeJob!!,
                        onStart = { viewModel.startJob(it) },
                        onComplete = { viewModel.completeJob(it) },
                        onArrive = { viewModel.markArrival(it) }
                    )
                }
            } else if (isOnline) {
                item {
                    Text("Available Broadcasts", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
                
                if (availableJobs.isEmpty()) {
                    item {
                        Box(modifier = Modifier.fillMaxWidth().padding(40.dp), contentAlignment = Alignment.Center) {
                            Text("Searching for nearby jobs...", color = Color.Gray, fontStyle = androidx.compose.ui.text.font.FontStyle.Italic)
                        }
                    }
                } else {
                    items(availableJobs) { job ->
                        BroadcastCard(
                            job = job,
                            onAccept = { viewModel.acceptJob(it) }
                        )
                    }
                }
            } else {
                item {
                    Box(modifier = Modifier.fillMaxWidth().padding(top = 60.dp), contentAlignment = Alignment.Center) {
                        Text("Go online to start receiving jobs", color = Color.Gray)
                    }
                }
            }
        }
    }
}

@Composable
fun ShadowBanNotice() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFFFEBEE))
    ) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("⚠️", fontSize = 24.sp)
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text("Shadow Ban Active", fontWeight = FontWeight.Bold, color = Color.Red, fontSize = 14.sp)
                Text("Account restricted due to abnormal activity.", fontSize = 12.sp)
            }
        }
    }
}

@Composable
fun EarningsCard(isLoading: Boolean, amount: Double) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(text = "Today's Earnings", fontSize = 14.sp, color = Color.Gray)
            if (isLoading) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp))
            } else {
                Text(
                    text = "$${String.format("%.2f", amount)}",
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Black,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

@Composable
fun BroadcastCard(job: JobDto, onAccept: (String) -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(text = "NEW BROADCAST", fontWeight = FontWeight.Bold, color = Color(0xFFD32F2F), fontSize = 12.sp)
                Text(text = "1.2 km away", fontSize = 12.sp, color = Color.Gray)
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = job.serviceCode, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            Text(text = "Booking Fee: $${job.bookingFee}", fontSize = 14.sp, color = Color.Gray)
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Button(
                onClick = { onAccept(job.id) },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32))
            ) {
                Text("ACCEPT JOB")
            }
        }
    }
}

@Composable
fun ActiveJobCard(job: JobDto, onArrive: (String) -> Unit, onStart: (String) -> Unit, onComplete: (String) -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.size(8.dp).background(Color(0xFF4CAF50), CircleShape))
                Spacer(modifier = Modifier.width(8.dp))
                Text(text = "LIVE JOB", fontWeight = FontWeight.Bold, color = Color(0xFF4CAF50), fontSize = 12.sp)
            }
            Spacer(modifier = Modifier.height(12.dp))
            Text(text = job.serviceCode, fontSize = 20.sp, fontWeight = FontWeight.Bold)
            Text(text = "Status: ${job.status}", fontSize = 16.sp, color = Color.Gray)
            
            Spacer(modifier = Modifier.height(20.dp))
            
            when (job.status) {
                "ACCEPTED" -> {
                    Button(
                        onClick = { onArrive(job.id) },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF212121))
                    ) {
                        Text("MARK ARRIVED")
                    }
                }
                "ARRIVED" -> {
                    Button(
                        onClick = { onStart(job.id) },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32))
                    ) {
                        Text("START WORK")
                    }
                }
                "STARTED" -> {
                    Button(
                        onClick = { onComplete(job.id) },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD32F2F))
                    ) {
                        Text("COMPLETE WORK")
                    }
                }
            }
        }
    }
}
