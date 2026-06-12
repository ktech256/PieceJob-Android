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
    val isLoading by viewModel.isLoading.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
            .padding(16.dp)
    ) {
        Text("Performance & Insights", fontSize = 24.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(24.dp))

        if (isLoading) {
            LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
        }

        LazyColumn(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            item {
                StatGrid(
                    listOf(
                        "Jobs" to (analytics?.totalJobsCompleted?.toString() ?: "0"),
                        "Acceptance" to "${((analytics?.acceptanceRate ?: 0f) * 100).toInt()}%",
                        "Completion" to "${((analytics?.completionRate ?: 0f) * 100).toInt()}%"
                    )
                )
            }
            
            item {
                TierProgressionCard(progress = analytics?.tierProgression ?: 0)
            }
        }
    }
}

@Composable
fun CustomerAnalyticsScreen(
    viewModel: CustomerAnalyticsViewModel = hiltViewModel()
) {
    val analytics by viewModel.analytics.collectAsState()
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
                        "Spending" to String.format(Locale.getDefault(), "$%.2f", analytics?.totalSpending ?: 0.0)
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
                        Text(String.format(Locale.getDefault(), "$%.2f", cat.amount), fontWeight = FontWeight.Bold)
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
