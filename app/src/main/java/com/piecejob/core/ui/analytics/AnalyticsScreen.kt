package com.piecejob.core.ui.analytics

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
import androidx.hilt.navigation.compose.hiltViewModel
import com.piecejob.core.data.remote.dto.*
import java.util.Locale

@Composable
fun ProviderAnalyticsScreen(
    viewModel: ProviderAnalyticsViewModel = hiltViewModel()
) {
    val analytics by viewModel.analytics.collectAsState()
    val currencySymbol by viewModel.currencySymbol.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF4F5F7))
    ) {
        // Custom Header
        Box(modifier = Modifier.fillMaxWidth().background(Color(0xFF121212)).padding(top = 56.dp, bottom = 24.dp, start = 24.dp, end = 24.dp)) {
            Column {
                Text("Performance & Insights", fontSize = 24.sp, fontWeight = FontWeight.Black, color = Color.White)
                Text("Enterprise Quality Management", fontSize = 12.sp, color = Color.Gray, fontWeight = FontWeight.Bold)
            }
        }

        if (isLoading) {
            LinearProgressIndicator(modifier = Modifier.fillMaxWidth(), color = Color(0xFFD32F2F))
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Health Score Focus
            item {
                HealthScoreCard(
                    score = analytics?.healthScore ?: 100.0,
                    status = analytics?.healthStatus ?: "Excellent"
                )
            }

            // Core Operations
            item {
                StatGrid(
                    listOf(
                        "Accepted" to (analytics?.totalJobsAccepted?.toString() ?: "0"),
                        "Completed" to (analytics?.totalJobsCompleted?.toString() ?: "0"),
                        "Cancelled" to (analytics?.totalJobsCancelled?.toString() ?: "0")
                    )
                )
            }

            // Efficiency
            item {
                StatGrid(
                    listOf(
                        "Acceptance" to "${(analytics?.acceptanceRate ?: 0.0).toInt()}%",
                        "Completion" to "${(analytics?.completionRate ?: 0.0).toInt()}%",
                        "Arrival" to "${(analytics?.arrivalRate ?: 0.0).toInt()}%"
                    )
                )
            }

            // Rankings
            item {
                RankingCard(
                    national = analytics?.currentRank ?: 0,
                    city = analytics?.cityRank ?: 0,
                    province = analytics?.provinceRank ?: 0
                )
            }

            // Timeline Info
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Operational Metrics", fontWeight = FontWeight.Black, fontSize = 14.sp)
                        Spacer(modifier = Modifier.height(12.dp))
                        MetricRow("Avg Arrival Time", analytics?.averageArrivalTime ?: "N/A")
                        MetricRow("Avg Job Duration", analytics?.averageJobDuration ?: "N/A")
                        MetricRow("Most Requested", analytics?.mostRequestedService ?: "N/A")
                        MetricRow("Active Since", analytics?.activeSince ?: "N/A")
                    }
                }
            }
            
            item {
                TierProgressionCard(progress = analytics?.tierProgression ?: 0)
            }
        }
    }
}

@Composable
fun MetricRow(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, fontSize = 12.sp, color = Color.Gray, fontWeight = FontWeight.Medium)
        Text(value, fontSize = 12.sp, fontWeight = FontWeight.Black)
    }
}

@Composable
fun RankingCard(national: Int, city: Int, province: Int) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Row(modifier = Modifier.padding(20.dp), horizontalArrangement = Arrangement.SpaceBetween) {
            RankingItem("National", national)
            RankingItem("Province", province)
            RankingItem("City", city)
        }
    }
}

@Composable
fun RankingItem(label: String, rank: Int) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, fontSize = 10.sp, color = Color.Gray, fontWeight = FontWeight.Bold)
        Text("#$rank", fontSize = 20.sp, fontWeight = FontWeight.Black, color = Color(0xFF1976D2))
    }
}

@Composable
fun HealthScoreCard(score: Double, status: String) {
    val color = when {
        score >= 90 -> Color(0xFF4CAF50)
        score >= 80 -> Color(0xFF1976D2)
        score >= 70 -> Color(0xFFFFA000)
        else -> Color(0xFFD32F2F)
    }
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = color.copy(alpha = 0.1f)),
        border = androidx.compose.foundation.BorderStroke(1.dp, color.copy(alpha = 0.3f))
    ) {
        Row(modifier = Modifier.padding(20.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
            Column {
                Text("Health Score", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = color)
                Text(status, fontSize = 24.sp, fontWeight = FontWeight.Black, color = color)
            }
            Text("${score.toInt()}%", fontSize = 32.sp, fontWeight = FontWeight.Black, color = color)
        }
    }
}

@Composable
fun CustomerAnalyticsScreen(
    viewModel: CustomerAnalyticsViewModel = hiltViewModel()
) {
    val analytics by viewModel.analytics.collectAsState()
    val currencySymbol by viewModel.currencySymbol.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
            .padding(16.dp)
    ) {
        Text("My Activity Insights", fontSize = 24.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(24.dp))

        if (isLoading) {
            LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
        }

        LazyColumn(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            item {
                StatGrid(
                    listOf(
                        "Bookings" to (analytics?.totalBookings?.toString() ?: "0"),
                        "Spending" to String.format(Locale.getDefault(), "%s%.2f", currencySymbol, analytics?.totalSpending ?: 0.0)
                    )
                )
            }
            
            item {
                Text("Spending by Category", fontWeight = FontWeight.Bold)
            }

            items(analytics?.topCategories ?: emptyList()) { cat ->
                Card(modifier = Modifier.fillMaxWidth()) {
                    Row(modifier = Modifier.padding(16.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(cat.categoryName)
                        Text(String.format(Locale.getDefault(), "%s%.2f", currencySymbol, cat.amount), fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun TierProgressionCard(progress: Int) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF212121))
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text("Tier Progression", color = Color.White, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(12.dp))
            LinearProgressIndicator(
                progress = { progress / 100f },
                modifier = Modifier.fillMaxWidth(),
                color = Color(0xFFD32F2F),
                trackColor = Color.White.copy(alpha = 0.2f)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text("$progress% to next tier", color = Color.White.copy(alpha = 0.7f), fontSize = 12.sp)
        }
    }
}

@Composable
fun StatGrid(stats: List<Pair<String, String>>) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        stats.forEach { (label, value) ->
            Card(
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFF5F5F5))
            ) {
                Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(text = label, fontSize = 10.sp, color = Color.Gray, fontWeight = FontWeight.Bold)
                    Text(text = value, fontSize = 18.sp, fontWeight = FontWeight.Black)
                }
            }
        }
    }
}
