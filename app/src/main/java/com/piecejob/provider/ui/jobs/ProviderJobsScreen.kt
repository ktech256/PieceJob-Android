package com.piecejob.provider.ui.jobs

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
fun ProviderJobsScreen(
    viewModel: ProviderJobsViewModel = hiltViewModel(),
    onNavigateToTracking: (String) -> Unit
) {
    var selectedTabIndex by remember { mutableIntStateOf(0) }
    val tabs = listOf("Available", "Active", "Scheduled", "Completed", "Cancelled", "Disputed")
    
    val availableJobs by viewModel.availableJobs.collectAsState()
    val activeJobs by viewModel.activeJobs.collectAsState()
    val completedJobs by viewModel.completedJobs.collectAsState()
    val cancelledJobs by viewModel.cancelledJobs.collectAsState()
    val disputedJobs by viewModel.disputedJobs.collectAsState()
    val scheduledJobs by viewModel.scheduledJobs.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()

    // Refresh jobs when screen is opened
    LaunchedEffect(Unit) {
        viewModel.loadJobs()
    }

    LaunchedEffect(Unit) {
        viewModel.navigationEvent.collect { jobId ->
            onNavigateToTracking(jobId)
        }
    }

    Column(modifier = Modifier.fillMaxSize().background(Color(0xFFF4F5F7))) {
        ScrollableTabRow(
            selectedTabIndex = selectedTabIndex,
            edgePadding = 16.dp,
            containerColor = Color(0xFF121212),
            contentColor = Color.White
        ) {
            tabs.forEachIndexed { index, title ->
                Tab(
                    selected = selectedTabIndex == index,
                    onClick = { selectedTabIndex = index },
                    text = { Text(title, fontSize = 12.sp, fontWeight = FontWeight.Bold) }
                )
            }
        }

        Box(modifier = Modifier.fillMaxSize()) {
            if (isLoading && availableJobs.isEmpty() && activeJobs.isEmpty() && completedJobs.isEmpty()) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            } else {
                when (selectedTabIndex) {
                    0 -> JobsList(availableJobs, isLoading) { viewModel.acceptJob(it) }
                    1 -> JobsList(activeJobs, isLoading) { onNavigateToTracking(it) }
                    2 -> JobsList(scheduledJobs, isLoading) { /* Detail */ }
                    3 -> JobsList(completedJobs, isLoading) { onNavigateToTracking(it) }
                    4 -> JobsList(cancelledJobs, isLoading) { onNavigateToTracking(it) }
                    5 -> JobsList(disputedJobs, isLoading) { onNavigateToTracking(it) }
                    else -> EmptyState(tabs[selectedTabIndex])
                }
            }
        }
    }
}

@Composable
fun JobsList(jobs: List<JobDto>, isLoading: Boolean, onAction: (String) -> Unit) {
    if (jobs.isEmpty()) {
        EmptyState("Jobs")
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            items(jobs) { job ->
                JobCard(job, isLoading, onAction)
            }
        }
    }
}

@Composable
fun JobCard(job: JobDto, isLoading: Boolean, onAction: (String) -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Column {
                    Text(text = job.serviceName ?: job.serviceCode, fontWeight = FontWeight.Black, fontSize = 18.sp)
                    Text(text = "Job ID: #${job.id.takeLast(6).uppercase()}", fontSize = 10.sp, color = Color.Gray)
                }
                Text(text = "${job.currency} ${job.serviceFee ?: job.bookingFee}", fontWeight = FontWeight.Black, color = Color(0xFF2E7D32))
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            val customerName = if (job.status == "BROADCASTED") "Hidden until accepted" else "${job.customerInfo?.firstName} ${job.customerInfo?.lastName}"
            InfoRow(label = "Customer", value = customerName)
            InfoRow(label = "Date/Time", value = job.createdAt.take(16).replace("T", " "))
            InfoRow(label = "Location", value = job.location?.address ?: "Client Location")
            
            Spacer(modifier = Modifier.height(16.dp))
            
            if (job.status == "BROADCASTED") {
                Button(
                    onClick = { onAction(job.id) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    enabled = !isLoading
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White)
                    } else {
                        Text("ACCEPT JOB")
                    }
                }
            } else {
                val statusColor = when (job.status) {
                    "COMPLETED" -> Color(0xFF4CAF50)
                    "CANCELLED" -> Color(0xFFD32F2F)
                    "STARTED", "IN_PROGRESS" -> Color(0xFF1976D2)
                    else -> Color(0xFFFFA000)
                }
                
                Button(
                    onClick = { onAction(job.id) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = statusColor)
                ) {
                    Text(text = if(job.status in listOf("ACCEPTED", "ARRIVED", "STARTED", "IN_PROGRESS")) "OPEN TRACKING" else "VIEW STATUS: ${job.status}")
                }
            }
        }
    }
}

@Composable
fun InfoRow(label: String, value: String) {
    Row(modifier = Modifier.padding(vertical = 2.dp)) {
        Text(text = "$label: ", fontSize = 12.sp, color = Color.Gray, fontWeight = FontWeight.Bold)
        Text(text = value, fontSize = 12.sp, color = Color.DarkGray, maxLines = 1)
    }
}
@Composable
fun EmptyState(tabName: String) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(text = "No $tabName Found", color = Color.Gray, fontWeight = FontWeight.Bold)
            Text(text = "We'll notify you when new jobs arrive.", color = Color.LightGray, fontSize = 12.sp)
        }
    }
}
