package com.piecejob.customer.ui.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.piecejob.core.data.remote.ServiceDto

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomerDashboardScreen(
    viewModel: CustomerDashboardViewModel = hiltViewModel(),
    onServiceClick: (ServiceDto) -> Unit,
    onProfileClick: () -> Unit,
    onNotificationsClick: () -> Unit
) {
    val services by viewModel.services.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val error by viewModel.error.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text("PieceJob", fontWeight = FontWeight.Black, fontSize = 24.sp) 
                },
                actions = {
                    IconButton(onClick = onNotificationsClick) {
                        Icon(Icons.Default.Notifications, contentDescription = "Notifications")
                    }
                    IconButton(onClick = onProfileClick) {
                        Icon(Icons.Default.Person, contentDescription = "Profile")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.White,
                    titleContentColor = Color(0xFFD32F2F)
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(Color(0xFFFBFBFB))
        ) {
            // Search Bar
            SearchBarPlaceholder()

            if (isLoading && services.isEmpty()) {
                SkeletonDashboard()
            } else if (error != null && services.isEmpty()) {
                ErrorView(error!!, onRetry = { viewModel.loadServices() })
            } else {
                val categories = services.groupBy { it.category }
                
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 24.dp)
                ) {
                    item {
                        PromotionBanner()
                    }

                    categories.forEach { (category, servicesInCategory) ->
                        item {
                            CategoryHeader(category)
                        }
                        item {
                            LazyRow(
                                contentPadding = PaddingValues(horizontal = 16.dp),
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                modifier = Modifier.padding(bottom = 24.dp)
                            ) {
                                items(servicesInCategory) { service ->
                                    ServiceCardPremium(service = service, onClick = { onServiceClick(service) })
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SearchBarPlaceholder() {
    OutlinedCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.Search, contentDescription = "Search", tint = Color.Gray)
            Spacer(modifier = Modifier.width(12.dp))
            Text("Search for services (e.g. Cleaning)", color = Color.Gray, fontSize = 14.sp)
        }
    }
}

@Composable
fun PromotionBanner() {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(140.dp)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFD32F2F))
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Column(modifier = Modifier.padding(20.dp).align(Alignment.CenterStart)) {
                Text("Get 20% OFF", color = Color.White, fontWeight = FontWeight.Black, fontSize = 22.sp)
                Text("On your first home cleaning", color = Color.White.copy(alpha = 0.8f), fontSize = 12.sp)
                Spacer(modifier = Modifier.height(12.dp))
                Button(
                    onClick = { },
                    colors = ButtonDefaults.buttonColors(containerColor = Color.White),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 0.dp),
                    modifier = Modifier.height(32.dp)
                ) {
                    Text("Book Now", color = Color(0xFFD32F2F), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun CategoryHeader(title: String) {
    Text(
        text = title,
        fontSize = 18.sp,
        fontWeight = FontWeight.ExtraBold,
        modifier = Modifier.padding(start = 16.dp, top = 8.dp, bottom = 12.dp)
    )
}

@Composable
fun ServiceCardPremium(service: ServiceDto, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .width(140.dp)
            .height(160.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(16.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Surface(
                modifier = Modifier.size(40.dp),
                shape = RoundedCornerShape(10.dp),
                color = Color(0xFFFDECEA)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(service.name.take(1), color = Color(0xFFD32F2F), fontWeight = FontWeight.Bold)
                }
            }
            
            Column {
                Text(service.name, fontWeight = FontWeight.Bold, fontSize = 14.sp, maxLines = 2, lineHeight = 18.sp)
                Spacer(modifier = Modifier.height(4.dp))
                Text("Available", color = Color(0xFF4CAF50), fontSize = 10.sp, fontWeight = FontWeight.Medium)
            }
        }
    }
}

@Composable
fun SkeletonDashboard() {
    Column(modifier = Modifier.padding(16.dp)) {
        repeat(3) {
            Box(modifier = Modifier.fillMaxWidth().height(20.dp).background(Color.LightGray.copy(alpha = 0.3f), RoundedCornerShape(4.dp)))
            Spacer(modifier = Modifier.height(12.dp))
            Row {
                repeat(3) {
                    Box(modifier = Modifier.size(120.dp).background(Color.LightGray.copy(alpha = 0.3f), RoundedCornerShape(16.dp)))
                    Spacer(modifier = Modifier.width(12.dp))
                }
            }
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
fun ErrorView(message: String, onRetry: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(message, color = Color.Red)
        Spacer(modifier = Modifier.height(16.dp))
        Button(onClick = onRetry) { Text("Retry") }
    }
}
