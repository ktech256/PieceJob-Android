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
import com.piecejob.core.ui.components.PieceJobButton
import com.piecejob.core.ui.components.PieceJobOutlinedButton

import com.piecejob.core.utils.formatPrivacyAddress
import com.piecejob.core.utils.formatDateTimeString

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
    val referralCampaign by viewModel.referralCampaign.collectAsState()
    val isReferralEnabled by viewModel.isReferralEnabled.collectAsState()
    val currencySymbol by viewModel.currencySymbol.collectAsState()
    val error by viewModel.error.collectAsState()
    val currentUserId = viewModel.currentUserId
    
    val context = LocalContext.current

    // ✅ Refresh data every time we return to dashboard to catch active job state
    LaunchedEffect(Unit) {
        viewModel.refresh()
    }

    LaunchedEffect(Unit) {
        viewModel.navigationEvent.collect { event ->
            if (event.startsWith("NEGOTIATION:")) {
                val parts = event.split(":")
                if (parts.size >= 3) {
                    onNavigateToSubScreen(com.piecejob.core.ui.navigation.Screen.Negotiation.passArgs(parts[1], parts[2]))
                }
            } else if (event.startsWith("TRACKING:")) {
                val jobId = event.removePrefix("TRACKING:")
                onNavigateToTracking(jobId)
            } else {
                onNavigateToTracking(event)
            }
        }
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
        } else if (currentJob != null && (currentJob.status == "PROVIDER_ACCEPTED" || currentJob.priceStatus == "PENDING")) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .clickable { onNavigateToSubScreen(com.piecejob.core.ui.navigation.Screen.Negotiation.passArgs(currentJob.id, currentJob.customerId ?: "")) },
                color = Color(0xFFFFF8E1),
                shape = RoundedCornerShape(12.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFFA000))
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.PendingActions, contentDescription = null, tint = Color(0xFFFFA000))
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text("RESUME NEGOTIATION", fontSize = 10.sp, fontWeight = FontWeight.Black, color = Color(0xFFE65100))
                        Text(
                            text = if (currentJob.priceStatus == "ACCEPTED") "Price agreed. Please confirm dispatch." else "Propose price or request photos for ${currentJob.serviceName ?: currentJob.serviceCode}",
                            fontSize = 13.sp, 
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Text("OPEN", color = Color(0xFFE65100), fontWeight = FontWeight.Black, fontSize = 12.sp)
                    Icon(Icons.Default.ChevronRight, contentDescription = null, tint = Color(0xFFFFA000))
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
                        onArrive = { id ->
                            if (currentJob.status == "PROVIDER_ACCEPTED" || currentJob.priceStatus == "PENDING") {
                                onNavigateToSubScreen(com.piecejob.core.ui.navigation.Screen.Negotiation.passArgs(currentJob.id, currentJob.customerId ?: ""))
                            } else {
                                viewModel.markArrival(id)
                            }
                        },
                        onNavigateToSubScreen = onNavigateToSubScreen,
                        onClick = { 
                            if (currentJob.status == "PROVIDER_ACCEPTED" || currentJob.priceStatus == "PENDING") {
                                onNavigateToSubScreen(com.piecejob.core.ui.navigation.Screen.Negotiation.passArgs(currentJob.id, currentJob.customerId ?: ""))
                            } else {
                                onNavigateToTracking(currentJob.id) 
                            }
                        }
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
            
            val filteredActivity = recentActivity
                .filter { it.status == "COMPLETED" || it.status == "CANCELLED" }
                .take(5)

            if (filteredActivity.isEmpty()) {
                item {
                    Text(
                        text = "No recent activity found.",
                        fontSize = 12.sp,
                        color = Color.Gray,
                        modifier = Modifier.padding(vertical = 12.dp)
                    )
                }
            } else {
                items(
                    items = filteredActivity,
                    key = { it.id }
                ) { activity ->
                    RecentActivityItem(activity, currencySymbol, currentUserId)
                }
            }

            // REFERRAL CAMPAIGN
            if (isReferralEnabled) {
                item {
                    ReferralDashboardCard(referralCampaign) {
                        onNavigateToSubScreen(com.piecejob.core.ui.navigation.Screen.Referral.route)
                    }
                }
            }
        }
    }
}

@Composable
fun ReferralDashboardCard(campaign: com.piecejob.core.data.remote.dto.ReferralCampaignDto?, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 16.dp)
            .clickable { onClick() }, 
        shape = RoundedCornerShape(24.dp), 
        colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF8E1)),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            if (campaign?.bannerUrl != null) {
                coil.compose.AsyncImage(
                    model = campaign.bannerUrl,
                    contentDescription = null,
                    modifier = Modifier.matchParentSize(),
                    contentScale = androidx.compose.ui.layout.ContentScale.Crop
                )
                Box(modifier = Modifier.matchParentSize().background(Color.Black.copy(alpha = 0.4f)))
            }
            
            Row(modifier = Modifier.padding(24.dp), verticalAlignment = Alignment.CenterVertically) {
                val isDark = campaign?.bannerUrl != null
                Surface(
                    modifier = Modifier.size(56.dp), 
                    shape = CircleShape, 
                    color = if (isDark) Color.White.copy(alpha = 0.2f) else Color.White
                ) {
                    Box(contentAlignment = Alignment.Center) { 
                        Icon(
                            imageVector = Icons.Default.CardGiftcard, 
                            contentDescription = null, 
                            tint = if (isDark) Color.White else Color(0xFFFFA000), 
                            modifier = Modifier.size(28.dp)
                        ) 
                    }
                }
                Spacer(modifier = Modifier.width(20.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = campaign?.title ?: "Refer & Earn Rewards", 
                        fontWeight = FontWeight.Black, 
                        fontSize = 16.sp, 
                        color = if (isDark) Color.White else Color(0xFFE65100)
                    )
                    Text(
                        text = campaign?.description ?: "Get rewards when your referrals join and complete their first PieceJob.", 
                        fontSize = 12.sp, 
                        color = if (isDark) Color.White.copy(alpha = 0.8f) else Color.DarkGray, 
                        lineHeight = 18.sp,
                        maxLines = 2,
                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                    )
                    
                    if (campaign != null) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Surface(
                            color = if (isDark) Color.White else Color(0xFFFFE082),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = "Earn ${campaign.currency} ${String.format("%.2f", campaign.rewardAmount)}",
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Black,
                                color = Color(0xFFE65100)
                            )
                        }
                    }
                }
                Icon(
                    imageVector = Icons.Default.ChevronRight, 
                    contentDescription = null, 
                    tint = if (isDark) Color.White else Color(0xFFFFA000)
                )
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
fun RecentActivityItem(activity: com.piecejob.core.data.remote.dto.ActivityDto, currency: String, currentUserId: String) {
    val isCancelled = activity.status == "CANCELLED"
    val statusColor = if (isCancelled) Color(0xFFD32F2F) else Color(0xFF2E7D32)
    val bgColor = if (isCancelled) Color(0xFFFFEBEE) else Color(0xFFE8F5E9)

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(modifier = Modifier.size(32.dp), shape = CircleShape, color = bgColor) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = if (isCancelled) Icons.Default.Cancel else Icons.Default.CheckCircle,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = statusColor
                        )
                    }
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (isCancelled) "Cancelled" else "Completed",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Black,
                        color = statusColor
                    )
                    Text(
                        text = activity.serviceName ?: "General Service",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.Black
                    )
                }
                if (!isCancelled) {
                    val amount = activity.amount
                    Text(
                        text = if (amount != null && amount > 0) "$currency ${String.format("%.2f", amount)}" else "N/A",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Black,
                        color = Color(0xFF2E7D32)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.LocationOn, null, modifier = Modifier.size(12.dp), tint = Color.Gray)
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = formatPrivacyAddress(activity.address),
                    fontSize = 11.sp,
                    color = Color.Gray,
                    fontWeight = FontWeight.Medium
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            if (isCancelled) {
                val actor = if (activity.cancelledBy == currentUserId) "You" else "Customer"
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Column {
                        Text("Cancelled:", fontSize = 9.sp, fontWeight = FontWeight.Black, color = Color.Gray)
                        Text(formatDateTimeString(activity.cancelledAt ?: activity.createdAt), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }
                    Text(
                        text = "Cancelled by $actor",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Black,
                        color = Color(0xFFD32F2F),
                        modifier = Modifier.align(Alignment.Bottom)
                    )
                }
            } else {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Column {
                        Text("Started:", fontSize = 9.sp, fontWeight = FontWeight.Black, color = Color.Gray)
                        Text(formatDateTimeString(activity.startedAt), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text("Completed:", fontSize = 9.sp, fontWeight = FontWeight.Black, color = Color.Gray)
                        Text(formatDateTimeString(activity.completedAt), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }
                }
                
                if ((activity.amount ?: 0.0) <= 0) {
                   Text(
                       "Gross: N/A • Net: N/A", 
                       fontSize = 9.sp, 
                       color = Color.Gray, 
                       modifier = Modifier.padding(top = 4.dp).align(Alignment.End)
                   )
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
fun ActiveJobCard(job: JobDto, isLoading: Boolean, onArrive: (String) -> Unit, onStart: (String) -> Unit, onComplete: (String) -> Unit, onNavigateToSubScreen: (String) -> Unit, onClick: () -> Unit) {
    val isCompleted = job.status == "COMPLETED"
    val isNegotiationPending = job.status == "PROVIDER_ACCEPTED" || job.priceStatus == "PENDING"
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { 
                if (isNegotiationPending) {
                    onNavigateToSubScreen(com.piecejob.core.ui.navigation.Screen.Negotiation.passArgs(job.id, job.customerId ?: ""))
                } else {
                    onClick() 
                }
            },
        shape = RoundedCornerShape(24.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = androidx.compose.foundation.BorderStroke(2.dp, 
            if (isCompleted) Color(0xFF1976D2).copy(alpha = 0.5f) 
            else if (isNegotiationPending) Color(0xFFFFA000)
            else Color(0xFF4CAF50))
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.size(8.dp).background(
                        if (isCompleted) Color(0xFF1976D2) 
                        else if (isNegotiationPending) Color(0xFFFFA000)
                        else Color(0xFF4CAF50), CircleShape))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = if (isCompleted) "PENDING RATING" else if (isNegotiationPending) "NEGOTIATION SESSION" else "CURRENT ACTIVE JOB", fontWeight = FontWeight.Black, color = if (isCompleted) Color(0xFF1976D2) else if (isNegotiationPending) Color(0xFFE65100) else Color(0xFF4CAF50), fontSize = 11.sp, letterSpacing = 1.sp)
                }
                
                Surface(
                    color = (if (isCompleted) Color(0xFF1976D2) else if (isNegotiationPending) Color(0xFFFFA000) else Color(0xFF4CAF50)).copy(alpha = 0.1f),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = job.status.replace("_", " "),
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        color = if (isCompleted) Color(0xFF1565C0) else if (isNegotiationPending) Color(0xFFBF360C) else Color(0xFF2E7D32),
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
                        "PROVIDER_ACCEPTED", "ACCEPTED" -> {
                            if (isNegotiationPending) {
                                PieceJobButton(
                                    text = "RESUME NEGOTIATION",
                                    onClick = { 
                                        onNavigateToSubScreen(com.piecejob.core.ui.navigation.Screen.Negotiation.passArgs(job.id, job.customerId ?: ""))
                                    },
                                    containerColor = Color(0xFFFFA000),
                                    height = 48.dp,
                                    fontSize = 12.sp
                                )
                            } else {
                                PieceJobButton(
                                    text = "MARK ARRIVED",
                                    onClick = { onArrive(job.id) },
                                    isLoading = isLoading,
                                    containerColor = Color(0xFF1976D2),
                                    height = 48.dp,
                                    fontSize = 12.sp
                                )
                            }
                        }
                        
                        "ARRIVED" -> PieceJobButton(
                            text = "START WORK",
                            onClick = { onStart(job.id) },
                            isLoading = isLoading,
                            containerColor = Color(0xFF4CAF50),
                            height = 48.dp,
                            fontSize = 12.sp
                        )
                        
                        "STARTED", "IN_PROGRESS" -> {
                            var isCompleting by remember { mutableStateOf(false) }
                            PieceJobButton(
                                text = "COMPLETE WORK",
                                onClick = { 
                                    isCompleting = true
                                    onComplete(job.id) 
                                },
                                isLoading = isLoading || isCompleting,
                                containerColor = Color(0xFFD32F2F),
                                height = 48.dp,
                                fontSize = 12.sp
                            )
                        }

                        "COMPLETED" -> PieceJobButton(
                            text = "RATE CUSTOMER",
                            onClick = onClick,
                            containerColor = Color(0xFFFFA000),
                            height = 48.dp,
                            fontSize = 12.sp
                        )
                    }
                }
                
                if (!isCompleted) {
                    // Resume Tracking Button
                    PieceJobOutlinedButton(
                        text = "RESUME",
                        onClick = onClick,
                        modifier = Modifier.weight(0.8f),
                        icon = Icons.Default.Navigation,
                        height = 48.dp,
                        fontSize = 12.sp,
                        contentColor = Color.Gray
                    )
                }
            }
        }
    }
}
