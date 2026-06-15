package com.piecejob.provider.ui.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
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

    // PULL TO REFRESH logic could be added here
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF4F5F7))
    ) {
        // TOP COMMAND CENTER HEADER
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF121212), RoundedCornerShape(bottomStart = 32.dp, bottomEnd = 32.dp))
                .padding(top = 56.dp, bottom = 32.dp, start = 24.dp, end = 24.dp)
        ) {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(modifier = Modifier.size(48.dp), shape = CircleShape, color = Color.White.copy(alpha = 0.1f)) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(Icons.Default.Person, contentDescription = null, tint = Color.White)
                            }
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        Column {
                            Text("Welcome, Provider", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Black)
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(stats?.tier ?: "BRONZE", color = Color(0xFFFFA000), fontSize = 10.sp, fontWeight = FontWeight.Black)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("•", color = Color.Gray)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(stats?.verificationStatus ?: "PENDING", color = if(stats?.verificationStatus == "APPROVED") Color(0xFF4CAF50) else Color.Gray, fontSize = 10.sp, fontWeight = FontWeight.Black)
                            }
                        }
                    }

                    IconButton(
                        onClick = onSosTrigger,
                        modifier = Modifier.size(44.dp).background(Color(0xFFD32F2F), CircleShape)
                    ) {
                        Text("SOS", color = Color.White, fontWeight = FontWeight.Black, fontSize = 12.sp)
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // STATUS TOGGLE BAR
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    color = Color.White.copy(alpha = 0.05f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.1f))
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier.size(10.dp).background(if (isOnline) Color(0xFF4CAF50) else Color.Gray, CircleShape)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = if (isOnline) "ONLINE" else "OFFLINE",
                                color = Color.White,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Black,
                                letterSpacing = 1.sp
                            )
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (isOnline) {
                                Text("Visible", color = Color(0xFF4CAF50), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            } else {
                                Text("Ghost Mode", color = Color.Gray, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Switch(
                                checked = isOnline,
                                onCheckedChange = { viewModel.toggleOnlineStatus(context) },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = Color.White,
                                    checkedTrackColor = Color(0xFF4CAF50)
                                )
                            )
                        }
                    }
                }
            }
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            if (isShadowBanned) item { ShadowBanNotice() }

            // CARD 1: EARNINGS SUMMARY
            item {
                StatsCard(
                    title = "Earnings Summary",
                    mainValue = "$${String.format("%.2f", stats?.earningsToday ?: 0.0)}",
                    subValues = listOf("Week: $${String.format("%.2f", stats?.earningsWeekly ?: 0.0)}", "Month: $${String.format("%.2f", stats?.earningsMonthly ?: 0.0)}"),
                    icon = Icons.Default.Payments,
                    color = Color(0xFF2E7D32)
                )
            }

            // CARD 2: PERFORMANCE OVERVIEW
            item {
                PerformanceCard(
                    rating = stats?.rating ?: 0.0,
                    acceptance = stats?.acceptanceRate ?: 0.0,
                    arrival = stats?.arrivalRate ?: 0.0
                )
            }

            // CARD 3: TIER PROGRESSION
            item {
                TierCard(tier = stats?.tier ?: "BRONZE", progress = stats?.tierProgress?.toFloat() ?: 0f)
            }

            // CARD 4: JOB ACTIVITY
            item {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    MiniActivityCard("Active Jobs", (stats?.jobsActive ?: 0).toString(), Icons.Default.Sync, Modifier.weight(1f))
                    MiniActivityCard("Completed", (stats?.jobsCompleted ?: 0).toString(), Icons.Default.CheckCircle, Modifier.weight(1f))
                }
            }

            // CARD 5: WALLET SUMMARY
            item {
                StatsCard(
                    title = "Wallet Balance",
                    mainValue = "$0.00", // Will wire wallet later
                    subValues = listOf("Escrow: $0.00", "Pending: $0.00"),
                    icon = Icons.Default.AccountBalanceWallet,
                    color = Color(0xFF1976D2)
                )
            }

            // QUICK ACTIONS
            item {
                Text("Quick Actions", fontWeight = FontWeight.Black, fontSize = 16.sp, modifier = Modifier.padding(top = 8.dp))
            }
            item {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    QuickActionButton("Trades", Icons.Default.Handyman) {}
                    QuickActionButton("Docs", Icons.Default.Description) {}
                    QuickActionButton("Stats", Icons.Default.BarChart) {}
                    QuickActionButton("Support", Icons.Default.HelpCenter) {}
                }
            }

            if (activeJob != null) {
                item {
                    Text("Active Engagement", fontWeight = FontWeight.Black, fontSize = 16.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    ActiveJobCard(
                        job = activeJob!!,
                        onStart = { viewModel.startJob(it) },
                        onComplete = { viewModel.completeJob(it) },
                        onArrive = { viewModel.markArrival(it) }
                    )
                }
            }

            // RECENT ACTIVITY FEED (Placeholder)
            item {
                Text("Recent Activity", fontWeight = FontWeight.Black, fontSize = 16.sp)
            }
            items(3) {
                RecentActivityItem()
            }
        }
    }
}

@Composable
fun StatsCard(title: String, mainValue: String, subValues: List<String>, icon: ImageVector, color: Color) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Row(modifier = Modifier.padding(24.dp), verticalAlignment = Alignment.CenterVertically) {
            Surface(modifier = Modifier.size(56.dp), shape = RoundedCornerShape(16.dp), color = color.copy(alpha = 0.1f)) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(icon, contentDescription = null, tint = color)
                }
            }
            Spacer(modifier = Modifier.width(20.dp))
            Column {
                Text(title, fontSize = 12.sp, color = Color.Gray, fontWeight = FontWeight.Bold)
                Text(mainValue, fontSize = 28.sp, fontWeight = FontWeight.Black)
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    subValues.forEach { 
                        Text(it, fontSize = 10.sp, color = Color.Gray, fontWeight = FontWeight.Medium)
                    }
                }
            }
        }
    }
}

@Composable
fun PerformanceCard(rating: Double, acceptance: Double, arrival: Double) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text("Performance Overview", fontSize = 14.sp, fontWeight = FontWeight.Black)
            Spacer(modifier = Modifier.height(16.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                MetricItem("Rating", String.format("%.1f", rating), Icons.Default.Star, Color(0xFFFFA000))
                MetricItem("Acceptance", "${(acceptance * 100).toInt()}%", Icons.Default.ThumbUp, Color(0xFF1976D2))
                MetricItem("Arrival", "${(arrival * 100).toInt()}%", Icons.Default.Timer, Color(0xFF4CAF50))
            }
        }
    }
}

@Composable
fun MetricItem(label: String, value: String, icon: ImageVector, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(20.dp))
        Text(value, fontWeight = FontWeight.Black, fontSize = 16.sp)
        Text(label, fontSize = 10.sp, color = Color.Gray)
    }
}

@Composable
fun TierCard(tier: String, progress: Float) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF121212))
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Tier: $tier", color = Color(0xFFFFA000), fontWeight = FontWeight.Black)
                Text("${(progress * 100).toInt()}% to Next Tier", color = Color.White, fontSize = 10.sp)
            }
            Spacer(modifier = Modifier.height(12.dp))
            LinearProgressIndicator(
                progress = progress,
                modifier = Modifier.fillMaxWidth().height(8.dp).clip(CircleShape),
                color = Color(0xFFFFA000),
                trackColor = Color.White.copy(alpha = 0.1f)
            )
        }
    }
}

@Composable
fun MiniActivityCard(title: String, value: String, icon: ImageVector, modifier: Modifier) {
    Card(modifier = modifier, shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = Color.White)) {
        Column(modifier = Modifier.padding(16.dp)) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(18.dp), tint = Color.Gray)
            Spacer(modifier = Modifier.height(8.dp))
            Text(value, fontSize = 20.sp, fontWeight = FontWeight.Black)
            Text(title, fontSize = 10.sp, color = Color.Gray)
        }
    }
}

@Composable
fun QuickActionButton(label: String, icon: ImageVector, onClick: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Surface(
            modifier = Modifier.size(56.dp).clickable { onClick() },
            shape = RoundedCornerShape(16.dp),
            color = Color.White,
            shadowElevation = 2.dp
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(icon, contentDescription = null, tint = Color.DarkGray)
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(label, fontSize = 10.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun RecentActivityItem() {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(modifier = Modifier.size(40.dp), shape = RoundedCornerShape(10.dp), color = Color.White) {
            Box(contentAlignment = Alignment.Center) {
                Icon(Icons.Default.History, contentDescription = null, modifier = Modifier.size(20.dp), tint = Color.Gray)
            }
        }
        Spacer(modifier = Modifier.width(16.dp))
        Column {
            Text("Job Completed: House Cleaning", fontSize = 13.sp, fontWeight = FontWeight.Bold)
            Text("2 hours ago • +$45.00", fontSize = 11.sp, color = Color.Gray)
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
fun ActiveJobCard(job: JobDto, onArrive: (String) -> Unit, onStart: (String) -> Unit, onComplete: (String) -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.size(8.dp).background(Color(0xFF4CAF50), CircleShape))
                Spacer(modifier = Modifier.width(8.dp))
                Text(text = "LIVE JOB", fontWeight = FontWeight.Black, color = Color(0xFF4CAF50), fontSize = 12.sp, letterSpacing = 1.sp)
            }
            Spacer(modifier = Modifier.height(12.dp))
            Text(text = job.serviceCode, fontSize = 20.sp, fontWeight = FontWeight.Black)
            Text(text = "Status: ${job.status}", fontSize = 14.sp, color = Color.Gray)
            
            if (job.isForSomeoneElse) {
                Spacer(modifier = Modifier.height(12.dp))
                Surface(color = Color(0xFFF5F5F5), shape = RoundedCornerShape(8.dp)) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text("RECIPIENT INFO", fontSize = 9.sp, fontWeight = FontWeight.Black, color = Color.Gray)
                        Text(text = job.recipientName ?: "Unknown", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        job.recipientPhone?.let { Text(text = it, fontSize = 12.sp, color = Color.Gray) }
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(20.dp))
            
            when (job.status) {
                "ACCEPTED" -> Button(onClick = { onArrive(job.id) }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp)) { Text("MARK ARRIVED") }
                "ARRIVED" -> Button(onClick = { onStart(job.id) }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp), colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32))) { Text("START WORK") }
                "STARTED" -> Button(onClick = { onComplete(job.id) }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp), colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD32F2F))) { Text("COMPLETE WORK") }
            }
        }
    }
}
