package com.piecejob.provider.ui.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
    onSosTrigger: () -> Unit,
    onNavigateToTracking: (String) -> Unit,
    onNavigateToSubScreen: (String) -> Unit
) {
    val stats by viewModel.stats.collectAsState()
    val userProfile by viewModel.userProfile.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val isOnline by viewModel.isOnline.collectAsState()
    val isShadowBanned by viewModel.isShadowBanned.collectAsState()
    val availableJobs by viewModel.availableJobs.collectAsState()
    val activeJob by viewModel.activeJob.collectAsState()
    val recentActivity by viewModel.recentActivity.collectAsState()
    val currencySymbol by viewModel.currencySymbol.collectAsState()
    val error by viewModel.error.collectAsState()
    
    val context = LocalContext.current

    // ✅ Refresh data every time we return to dashboard to catch active job state
    LaunchedEffect(Unit) {
        viewModel.refresh()
    }
    
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
                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                if (userProfile?.photo != null) {
                                    coil.compose.AsyncImage(
                                        model = userProfile!!.photo,
                                        contentDescription = null,
                                        modifier = Modifier.fillMaxSize().clip(CircleShape),
                                        contentScale = androidx.compose.ui.layout.ContentScale.Crop
                                    )
                                } else {
                                    Icon(Icons.Default.Person, contentDescription = null, tint = Color.White)
                                }
                            }
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        Column {
                            Text("Welcome, ${userProfile?.firstName ?: "Provider"}", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Black)
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

                if (error != null) {
                    Text(
                        text = error!!,
                        color = Color(0xFFEF5350),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                }

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

        // ✅ NEW: RESUME JOB OVERLAY (If job is started but screen is closed)
        val currentJob = activeJob
        if (currentJob != null && (currentJob.status == "STARTED" || currentJob.status == "IN_PROGRESS")) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .clickable { onNavigateToTracking(currentJob.id) },
                color = Color(0xFFE8F5E9),
                shape = RoundedCornerShape(12.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF4CAF50))
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.PlayArrow, contentDescription = null, tint = Color(0xFF2E7D32))
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text("JOB IN PROGRESS", fontSize = 10.sp, fontWeight = FontWeight.Black, color = Color(0xFF2E7D32))
                        Text("Resume tracking for ${currentJob.serviceName ?: currentJob.serviceCode ?: "Unknown Service"}", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    }
                    Text("RESUME", color = Color(0xFF2E7D32), fontWeight = FontWeight.Black, fontSize = 12.sp)
                    Icon(Icons.Default.ChevronRight, contentDescription = null, tint = Color(0xFF2E7D32))
                }
            }
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            if (isShadowBanned) item { ShadowBanNotice() }

            // ✅ SECTION: ACTIVE ENGAGEMENT (Moved to top for visibility)
            if (currentJob != null) {
                item {
                    Text(
                        "Ongoing Engagement",
                        fontWeight = FontWeight.Black,
                        fontSize = 18.sp,
                        color = Color(0xFF2E7D32),
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    ActiveJobCard(
                        job = currentJob,
                        isLoading = isLoading,
                        onStart = { viewModel.startJob(it) },
                        onComplete = { viewModel.completeJob(it) },
                        onArrive = { viewModel.markArrival(it) },
                        onClick = { onNavigateToTracking(currentJob.id) }
                    )
                }
            }

            // CARD 1: EARNINGS SUMMARY
            item {
                StatsCard(
                    title = "Earnings Summary",
                    mainValue = "$currencySymbol ${String.format("%.2f", stats?.earningsToday ?: 0.0)}",
                    subValues = listOf("Week: $currencySymbol ${String.format("%.2f", stats?.earningsWeekly ?: 0.0)}", "Month: $currencySymbol ${String.format("%.2f", stats?.earningsMonthly ?: 0.0)}"),
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
                    mainValue = "$currencySymbol 0.00", // Will wire wallet later
                    subValues = listOf("Escrow: $currencySymbol 0.00", "Pending: $currencySymbol 0.00"),
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
                    QuickActionButton("Trades", Icons.Default.Handyman) { onNavigateToSubScreen(com.piecejob.core.ui.navigation.Screen.MyServices.route) }
                    QuickActionButton("Docs", Icons.Default.Description) { onNavigateToSubScreen(com.piecejob.core.ui.navigation.Screen.VerificationDocs.route) }
                    QuickActionButton("Stats", Icons.Default.BarChart) { onNavigateToSubScreen(com.piecejob.core.ui.navigation.Screen.ProviderAnalytics.route) }
                    QuickActionButton("Support", Icons.Default.HelpCenter) { onNavigateToSubScreen(com.piecejob.core.ui.navigation.Screen.Support.route) }
                }
            }

            // RECENT ACTIVITY FEED
            item {
                Text("Recent Activity", fontWeight = FontWeight.Black, fontSize = 16.sp)
            }
            if (recentActivity.isEmpty()) {
                item {
                    Text(
                        text = "No recent activity found.",
                        fontSize = 12.sp,
                        color = Color.Gray,
                        modifier = Modifier.padding(vertical = 12.dp)
                    )
                }
            } else {
                items(recentActivity) { activity ->
                    RecentActivityItem(activity, currencySymbol)
                }
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
fun RecentActivityItem(activity: com.piecejob.core.data.remote.dto.ActivityDto, currency: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(modifier = Modifier.size(40.dp), shape = RoundedCornerShape(10.dp), color = Color.White) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = if (activity.type == "JOB") Icons.Default.Work else Icons.Default.Payments,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                    tint = Color.Gray
                )
            }
        }
        Spacer(modifier = Modifier.width(16.dp))
        Column {
            Text(activity.title ?: activity.type, fontSize = 13.sp, fontWeight = FontWeight.Bold)
            Text(
                text = "${activity.createdAt.take(10)} • ${if(activity.amount > 0) "+" else ""}$currency ${String.format("%.2f", activity.amount)}",
                fontSize = 11.sp,
                color = Color.Gray
            )
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
fun ActiveJobCard(job: JobDto, isLoading: Boolean, onArrive: (String) -> Unit, onStart: (String) -> Unit, onComplete: (String) -> Unit, onClick: () -> Unit) {
    val isCompleted = job.status == "COMPLETED"
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(24.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = androidx.compose.foundation.BorderStroke(2.dp, if (isCompleted) Color(0xFF1976D2).copy(alpha = 0.5f) else Color(0xFF4CAF50))
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.size(8.dp).background(if (isCompleted) Color(0xFF1976D2) else Color(0xFF4CAF50), CircleShape))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = if (isCompleted) "PENDING RATING" else "CURRENT ACTIVE JOB", fontWeight = FontWeight.Black, color = if (isCompleted) Color(0xFF1976D2) else Color(0xFF4CAF50), fontSize = 11.sp, letterSpacing = 1.sp)
                }
                
                Surface(
                    color = (if (isCompleted) Color(0xFF1976D2) else Color(0xFF4CAF50)).copy(alpha = 0.1f),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = job.status.replace("_", " "),
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        color = if (isCompleted) Color(0xFF1565C0) else Color(0xFF2E7D32),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Black
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(modifier = Modifier.size(48.dp), shape = CircleShape, color = Color(0xFFF4F5F7)) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.Person, contentDescription = null, tint = Color.Gray)
                    }
                }
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    Text(text = job.serviceName ?: job.serviceCode ?: "Unknown Service", fontSize = 18.sp, fontWeight = FontWeight.Black)
                    Text(text = job.location?.address ?: "Client Location", fontSize = 13.sp, color = Color.Gray, maxLines = 1)
                }
            }
            
            Spacer(modifier = Modifier.height(20.dp))
            
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                // Main Control Button
                Box(modifier = Modifier.weight(1f)) {
                    val isActionLoading = isLoading
                    when (job.status) {
                        "ACCEPTED" -> Button(
                            onClick = { onArrive(job.id) },
                            modifier = Modifier.fillMaxWidth(),
                            enabled = !isActionLoading,
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1976D2))
                        ) { 
                            if (isActionLoading) CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White)
                            else Text("MARK ARRIVED", fontWeight = FontWeight.Bold) 
                        }
                        
                        "ARRIVED" -> Button(
                            onClick = { onStart(job.id) },
                            modifier = Modifier.fillMaxWidth(),
                            enabled = !isActionLoading,
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50))
                        ) { 
                            if (isActionLoading) CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White)
                            else Text("START WORK", fontWeight = FontWeight.Bold) 
                        }
                        
                        "STARTED", "IN_PROGRESS" -> Button(
                            onClick = { onComplete(job.id) },
                            modifier = Modifier.fillMaxWidth(),
                            enabled = !isActionLoading,
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD32F2F))
                        ) { 
                            if (isActionLoading) CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White)
                            else Text("COMPLETE WORK", fontWeight = FontWeight.Bold) 
                        }

                        "COMPLETED" -> Button(
                            onClick = onClick,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFA000))
                        ) { Text("RATE CUSTOMER", fontWeight = FontWeight.Bold) }
                    }
                }
                
                if (!isCompleted) {
                    // Resume Tracking Button
                    OutlinedButton(
                        onClick = onClick,
                        modifier = Modifier.weight(0.8f),
                        shape = RoundedCornerShape(12.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE0E0E0))
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Navigation, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("RESUME", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}
