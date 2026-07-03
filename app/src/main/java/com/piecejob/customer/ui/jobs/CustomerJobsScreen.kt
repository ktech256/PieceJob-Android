package com.piecejob.customer.ui.jobs

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
import com.piecejob.core.data.remote.dto.JobDto

@Composable
fun CustomerJobsScreen(
    viewModel: CustomerJobsViewModel = hiltViewModel(),
    onNavigateToJob: (String) -> Unit
) {
    var selectedTabIndex by remember { mutableIntStateOf(0) }
    val tabs = listOf("Active", "Completed", "Cancelled")
    
    val activeJobs by viewModel.activeJobs.collectAsState()
    val completedJobs by viewModel.completedJobs.collectAsState()
    val cancelledJobs by viewModel.cancelledJobs.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.navigationEvent.collect { (jobId, route) ->
            onNavigateToJob(route)
        }
    }

    Column(modifier = Modifier.fillMaxSize().background(Color(0xFFF4F5F7))) {
        TabRow(
            selectedTabIndex = selectedTabIndex,
            containerColor = Color.White,
            contentColor = MaterialTheme.colorScheme.primary
        ) {
            tabs.forEachIndexed { index, title ->
                Tab(
                    selected = selectedTabIndex == index,
                    onClick = { selectedTabIndex = index },
                    text = { Text(title, fontSize = 14.sp, fontWeight = FontWeight.Bold) }
                )
            }
        }

        Box(modifier = Modifier.fillMaxSize()) {
            if (isLoading && activeJobs.isEmpty() && completedJobs.isEmpty() && cancelledJobs.isEmpty()) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            } else {
                val currentList = when (selectedTabIndex) {
                    0 -> activeJobs
                    1 -> completedJobs
                    2 -> cancelledJobs
                    else -> emptyList()
                }

                if (currentList.isEmpty()) {
                    EmptyState(tabs[selectedTabIndex])
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        items(currentList) { job ->
                            CustomerJobCard(job) { viewModel.openJob(job) }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun CustomerJobCard(job: JobDto, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        onClick = onClick
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Column {
                    Text(text = job.serviceName ?: job.serviceCode, fontWeight = FontWeight.Black, fontSize = 18.sp)
                    Text(text = "Job ID: #${job.id.takeLast(6).uppercase()}", fontSize = 10.sp, color = Color.Gray)
                }
                Surface(
                    color = getStatusColor(job.status).copy(alpha = 0.1f),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = job.status.replace("_", " "),
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        color = getStatusColor(job.status),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Black
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            val providerName = if (job.providerInfo != null) "${job.providerInfo?.firstName} ${job.providerInfo?.lastName}" else "Searching for Provider..."
            JobInfoRow(label = "Provider", value = providerName)
            JobInfoRow(label = "Date/Time", value = job.createdAt.take(16).replace("T", " "))
            JobInfoRow(label = "Location", value = job.location?.address ?: "Location Shared")
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text(text = "Total Amount", fontSize = 12.sp, color = Color.Gray)
                Text(text = "${job.currency} ${String.format("%.2f", (job.serviceFee ?: 0.0) + job.bookingFee)}", fontWeight = FontWeight.Black, color = Color.Black)
            }
        }
    }
}

@Composable
fun JobInfoRow(label: String, value: String) {
    Row(modifier = Modifier.padding(vertical = 2.dp)) {
        Text(text = "$label: ", fontSize = 12.sp, color = Color.Gray, fontWeight = FontWeight.Bold)
        Text(text = value, fontSize = 12.sp, color = Color.DarkGray, maxLines = 1)
    }
}

@Composable
fun EmptyState(tabName: String) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(text = "No $tabName Bookings", color = Color.Gray, fontWeight = FontWeight.Bold)
            Text(text = "Your booking history will appear here.", color = Color.LightGray, fontSize = 12.sp)
        }
    }
}

fun getStatusColor(status: String): Color {
    return when (status) {
        "COMPLETED" -> Color(0xFF4CAF50)
        "CANCELLED" -> Color(0xFFD32F2F)
        "STARTED", "IN_PROGRESS" -> Color(0xFF1976D2)
        "ACCEPTED", "ARRIVED" -> Color(0xFFFFA000)
        else -> Color.Gray
    }
}
