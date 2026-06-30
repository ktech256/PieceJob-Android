package com.piecejob.customer.ui.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
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
    onRequestServiceClick: () -> Unit,
    onNavigateToBookingWithLocation: (com.piecejob.core.data.remote.dto.SavedLocationDto) -> Unit,
    onProfileClick: () -> Unit,
    onNotificationsClick: () -> Unit,
    onSosClick: () -> Unit,
    onNavigateToTracking: (String) -> Unit
) {
    val services by viewModel.services.collectAsState()
    val categoriesList by viewModel.categories.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val activeJob by viewModel.activeJob.collectAsState()
    val dashboardData by viewModel.dashboardData.collectAsState()
    val currentAddress by viewModel.currentAddress.collectAsState()
    val searchResults by viewModel.searchResults.collectAsState()

    var searchQuery by remember { mutableStateOf("") }
    var isSearchActive by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.loadDashboard()
    }

    var selectedServiceForDetails by remember { mutableStateOf<ServiceDto?>(null) }

    if (selectedServiceForDetails != null) {
        ServiceDetailsDialog(
            service = selectedServiceForDetails!!,
            onConfirm = {
                val service = selectedServiceForDetails!!
                selectedServiceForDetails = null
                onServiceClick(service)
            },
            onDismiss = { selectedServiceForDetails = null }
        )
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF8F9FA))
    ) {
        // SECTION 1: HEADER
        item { DashboardHeader(currentAddress, onNotificationsClick, onSosClick) }

        // SECTION 2: WELCOME AREA
        item { 
            val profile = dashboardData?.profile
            val wallet = dashboardData?.wallet
            WelcomeCard(
                name = profile?.firstName ?: "User",
                balance = wallet?.balanceMain ?: 0.0,
                currency = wallet?.currency ?: "R"
            ) 
        }

        // SECTION 3: GLOBAL SEARCH
        item { 
            SearchBar(
                query = searchQuery,
                onQueryChange = {
                    searchQuery = it
                    viewModel.onSearch(it)
                },
                onActiveChange = { isSearchActive = it }
            ) 
        }

        if (isSearchActive && searchQuery.isNotEmpty()) {
            items(searchResults) { result ->
                SearchResultItem(result) {
                    isSearchActive = false
                    searchQuery = ""
                    when (it) {
                        is ServiceDto -> onServiceClick(it)
                        is com.piecejob.core.data.remote.ServiceCategoryDto -> {
                            // Category click logic (filter or scroll)
                        }
                        is com.piecejob.core.data.remote.dto.SavedLocationDto -> {
                            onNavigateToBookingWithLocation(it)
                        }
                    }
                }
            }
        }

        // SECTION 4: PROMOTIONAL BANNER CAROUSEL
        item { PromotionCarousel(dashboardData?.promotions ?: emptyList()) }

        // SECTION 5: POPULAR CATEGORIES
        item { PopularCategories(categoriesList) }

        // SECTION 11: CURRENT ACTIVE JOB CARD (Conditional)
        if (activeJob != null) {
            item {
                ActiveJobMiniCard(
                    job = activeJob!!,
                    onClick = { onNavigateToTracking(activeJob!!.id) }
                )
            }
        }

        // SECTION 7: RECENTLY USED SERVICES
        item { SectionTitle("Book Again") }
        item {
            LazyRow(
                contentPadding = PaddingValues(horizontal = 24.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                items(3) { RecentServiceAvatar() }
            }
        }

        // SECTION 10: EMERGENCY SERVICES
        item { EmergencyServicesSection() }

        // SECTION 6: ALL SERVICE CATEGORIES
        if (isLoading && services.isEmpty()) {
            item { SkeletonGrid() }
        } else {
            val servicesGrouped = services.groupBy { it.category }
            
            servicesGrouped.forEach { (categoryCode, servicesInCategory) ->
                val categoryName = categoriesList.find { it.code == categoryCode }?.name ?: categoryCode
                item {
                    Text(
                        text = categoryName,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Black,
                        modifier = Modifier.padding(start = 24.dp, top = 24.dp, bottom = 12.dp)
                    )
                }
                item {
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 24.dp),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        items(servicesInCategory) { service ->
                            ServiceCardSmall(service) { selectedServiceForDetails = it }
                        }
                    }
                }
            }
        }

        // SECTION 8: RECOMMENDED SERVICES
        item { SectionTitle("Recommended For You") }
        item {
            LazyRow(
                contentPadding = PaddingValues(horizontal = 24.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                val recommendations = dashboardData?.recommendations ?: emptyList()
                if (recommendations.isEmpty()) {
                    items(3) { RecommendedServiceCard() }
                } else {
                    items(recommendations) { service ->
                        ServiceCardSmall(service) { selectedServiceForDetails = it }
                    }
                }
            }
        }

        // SECTION 12: RECENT BOOKINGS
        item { SectionTitle("Latest Activity") }
        val activityList = dashboardData?.latestActivity ?: emptyList()
        if (activityList.isEmpty()) {
            items(2) { RecentBookingItem() }
        } else {
            items(activityList) { act ->
                ActivityItem(act)
            }
        }

        // SECTION 9: TOP RATED PROVIDERS NEARBY
        item { SectionTitle("Top Rated Nearby") }
        item {
            LazyRow(
                contentPadding = PaddingValues(horizontal = 24.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                val providers = dashboardData?.topRatedNearby ?: emptyList()
                if (providers.isEmpty()) {
                    items(3) { ProviderMiniCard() }
                } else {
                    items(providers) { provider ->
                        TopProviderCard(provider)
                    }
                }
            }
        }

        // SECTION 13: REFERRAL PROGRAM
        item { ReferralDashboardCard() }

        // SECTION 14: CUSTOMER TIPS
        item { CustomerTipsSection() }
        
        item { Spacer(modifier = Modifier.height(100.dp)) }
    }

    // FLOATING ACTION BUTTON: REQUEST SERVICE
    Box(modifier = Modifier.fillMaxSize()) {
        ExtendedFloatingActionButton(
            onClick = onRequestServiceClick,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 24.dp)
                .fillMaxWidth(0.85f)
                .height(64.dp),
            containerColor = Color(0xFFD32F2F),
            contentColor = Color.White,
            shape = RoundedCornerShape(20.dp),
            elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 8.dp)
        ) {
            Icon(Icons.Default.Add, contentDescription = null)
            Spacer(modifier = Modifier.width(12.dp))
            Text("REQUEST A PIECEJOB", fontWeight = FontWeight.Black, letterSpacing = 1.sp)
        }
    }
}

@Composable
fun DashboardHeader(address: String, onNotify: () -> Unit, onSos: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 56.dp, bottom = 16.dp, start = 24.dp, end = 24.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text("PieceJob", fontWeight = FontWeight.Black, fontSize = 24.sp, color = Color(0xFFD32F2F))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.LocationOn, contentDescription = null, modifier = Modifier.size(12.dp), tint = Color.Gray)
                Text(
                    text = address, 
                    fontSize = 12.sp, 
                    color = Color.Gray, 
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                )
            }
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onNotify) {
                Icon(Icons.Outlined.Notifications, contentDescription = null, modifier = Modifier.size(24.dp))
            }
            Spacer(modifier = Modifier.width(4.dp))
            Surface(
                modifier = Modifier
                    .size(44.dp)
                    .clickable { onSos() },
                shape = CircleShape,
                color = Color(0xFFD32F2F),
                shadowElevation = 4.dp
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text("SOS", color = Color.White, fontWeight = FontWeight.Black, fontSize = 12.sp)
                }
            }
        }
    }
}

@Composable
fun WelcomeCard(name: String, balance: Double, currency: String) {
    val greeting = when (java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY)) {
        in 0..11 -> "Good Morning"
        in 12..16 -> "Good Afternoon"
        else -> "Good Evening"
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 8.dp),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF121212))
    ) {
        Row(modifier = Modifier.padding(24.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text("$greeting, $name", color = Color.White.copy(alpha = 0.6f), fontSize = 14.sp)
                Text("Ready to book?", color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Black)
            }
            Column(horizontalAlignment = Alignment.End) {
                Text("BALANCE", color = Color.White.copy(alpha = 0.4f), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                Text("$currency ${String.format("%.2f", balance)}", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Black)
            }
        }
    }
}

@Composable
fun SearchBar(query: String, onQueryChange: (String) -> Unit, onActiveChange: (Boolean) -> Unit) {
    OutlinedCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 16.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Search, contentDescription = null, tint = Color.Gray)
            Spacer(modifier = Modifier.width(12.dp))
            BasicTextField(
                value = query,
                onValueChange = {
                    onQueryChange(it)
                    onActiveChange(true)
                },
                modifier = Modifier.weight(1f),
                decorationBox = { innerTextField ->
                    if (query.isEmpty()) {
                        Text("Search cleaning, plumbers, mechanics...", color = Color.Gray, fontSize = 14.sp)
                    }
                    innerTextField()
                }
            )
            if (query.isNotEmpty()) {
                Icon(
                    Icons.Default.Close, 
                    contentDescription = null, 
                    modifier = Modifier.clickable { 
                        onQueryChange("") 
                        onActiveChange(false)
                    }
                )
            }
        }
    }
}

@Composable
fun SearchResultItem(result: Any, onClick: (Any) -> Unit) {
    val name = when (result) {
        is ServiceDto -> result.name
        is com.piecejob.core.data.remote.ServiceCategoryDto -> result.name
        is com.piecejob.core.data.remote.dto.SavedLocationDto -> result.name
        else -> "Result"
    }
    val type = when (result) {
        is ServiceDto -> "Service"
        is com.piecejob.core.data.remote.ServiceCategoryDto -> "Category"
        is com.piecejob.core.data.remote.dto.SavedLocationDto -> "Saved Location"
        else -> "Other"
    }
    val icon = when (result) {
        is com.piecejob.core.data.remote.dto.SavedLocationDto -> Icons.Default.Star
        else -> Icons.Default.Search
    }

    ListItem(
        headlineContent = { Text(name, fontWeight = FontWeight.Bold) },
        supportingContent = { Text(type, fontSize = 12.sp, color = Color.Gray) },
        leadingContent = { Icon(icon, contentDescription = null, tint = if (result is com.piecejob.core.data.remote.dto.SavedLocationDto) Color(0xFFFFA000) else Color.LightGray) },
        modifier = Modifier.clickable { onClick(result) }.padding(horizontal = 24.dp)
    )
}

@Composable
fun PromotionCarousel(promotions: List<com.piecejob.core.data.remote.dto.PromotionDto>) {
    if (promotions.isEmpty()) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .height(130.dp)
                .padding(horizontal = 24.dp),
            shape = RoundedCornerShape(28.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFFD32F2F))
        ) {
            Box(modifier = Modifier.fillMaxSize().padding(24.dp)) {
                Text("PJ PLUS EXCLUSIVE\nGet 50% OFF all\ntasks this weekend.", color = Color.White, fontWeight = FontWeight.Black, fontSize = 18.sp, lineHeight = 24.sp)
                Icon(Icons.Default.Stars, contentDescription = null, tint = Color.White.copy(alpha = 0.2f), modifier = Modifier.size(100.dp).align(Alignment.CenterEnd))
            }
        }
    } else {
        // Simple Carousel (for now first one)
        val promo = promotions.first()
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .height(130.dp)
                .padding(horizontal = 24.dp),
            shape = RoundedCornerShape(28.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFFD32F2F))
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                if (promo.imageUrl != null) {
                    coil.compose.AsyncImage(
                        model = promo.imageUrl,
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = androidx.compose.ui.layout.ContentScale.Crop
                    )
                    Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.3f)))
                }
                Column(modifier = Modifier.padding(24.dp)) {
                    Text(promo.title, color = Color.White, fontWeight = FontWeight.Black, fontSize = 18.sp)
                    Text(promo.description, color = Color.White.copy(alpha = 0.8f), fontSize = 12.sp, maxLines = 2)
                    Spacer(modifier = Modifier.height(8.dp))
                    Surface(color = Color.White, shape = RoundedCornerShape(8.dp)) {
                        Text(promo.ctaText, modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp), fontSize = 10.sp, fontWeight = FontWeight.Black, color = Color.Black)
                    }
                }
            }
        }
    }
}

@Composable
fun PopularCategories(categories: List<com.piecejob.core.data.remote.ServiceCategoryDto>) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(24.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        if (categories.isEmpty()) {
            val cats = listOf(
                "HDS" to "Home & Domestic Services (HDS)",
                "CSS" to "Care & Support Services (CSS)",
                "HMS" to "Handyman & Repairs Services (HMS)",
                "OPS" to "Outdoor & Property Services (OPS)",
                "LLS" to "Convenience & Lifestyle Services (LLS)",
                "TSS" to "Technology & Home Setup Services (TSS)"
            )
            cats.forEach { (code, label) ->
                CategoryIcon(code, label.take(10))
            }
        } else {
            categories.take(6).forEach { cat ->
                CategoryIcon(cat.code, cat.name.split("&").first().trim().take(10))
            }
        }
    }
}

@Composable
fun CategoryIcon(code: String, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Surface(modifier = Modifier.size(52.dp), shape = RoundedCornerShape(16.dp), color = Color.White, shadowElevation = 2.dp) {
            Box(contentAlignment = Alignment.Center) { Text(code, fontWeight = FontWeight.Black, fontSize = 12.sp) }
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(label, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.Gray)
    }
}

@Composable
fun ServiceCardSmall(service: ServiceDto, onClick: (ServiceDto) -> Unit) {
    Card(
        modifier = Modifier.width(140.dp).clickable { onClick(service) },
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Surface(modifier = Modifier.size(40.dp), shape = RoundedCornerShape(12.dp), color = Color(0xFFFDECEA)) {
                    Box(contentAlignment = Alignment.Center) { Text(service.name.take(1), color = Color(0xFFD32F2F), fontWeight = FontWeight.Bold) }
                }
                
                if (!service.onlineCountLabel.isNullOrBlank() && service.onlineCountLabel != "0 Online") {
                    Surface(
                        color = Color(0xFFE8F5E9),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = "LIVE",
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp),
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Black,
                            color = Color(0xFF2E7D32)
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(20.dp))
            Text(service.name, fontWeight = FontWeight.Bold, fontSize = 14.sp, maxLines = 1)
            
            Row(verticalAlignment = Alignment.CenterVertically) {
                val isOnline = !service.onlineCountLabel.isNullOrBlank() && service.onlineCountLabel != "0 Online"
                Box(modifier = Modifier.size(6.dp).background(if(isOnline) Color(0xFF4CAF50) else Color.Gray, CircleShape))
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = service.onlineCountLabel ?: "Offline",
                    color = if(isOnline) Color(0xFF2E7D32) else Color.Gray,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
fun ServiceDetailsDialog(
    service: ServiceDto,
    confirmColor: Color = Color(0xFFD32F2F),
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = service.name,
                fontWeight = FontWeight.Black,
                fontSize = 20.sp
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                Text(
                    text = service.description ?: "No specific details provided for this service.",
                    fontSize = 14.sp,
                    color = Color.DarkGray,
                    lineHeight = 20.sp
                )
            }
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(containerColor = confirmColor),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("CONFIRM", fontWeight = FontWeight.Black)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("CANCEL", color = Color.Gray, fontWeight = FontWeight.Bold)
            }
        },
        containerColor = Color.White,
        shape = RoundedCornerShape(28.dp)
    )
}

@Composable
fun RecentServiceAvatar() {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(modifier = Modifier.size(64.dp).clip(CircleShape).background(Color.White).padding(2.dp).border(2.dp, Color(0xFFD32F2F), CircleShape).padding(4.dp)) {
           Surface(modifier = Modifier.fillMaxSize(), shape = CircleShape, color = Color(0xFFF5F5F5)) {
               Box(contentAlignment = Alignment.Center) { Icon(Icons.Default.History, contentDescription = null, tint = Color.Gray) }
           }
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text("Cleaning", fontSize = 11.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun RecommendedServiceCard() {
    Card(modifier = Modifier.width(200.dp), shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = Color.White)) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Surface(modifier = Modifier.size(32.dp), shape = RoundedCornerShape(8.dp), color = Color(0xFFE3F2FD)) {
                Box(contentAlignment = Alignment.Center) { Icon(Icons.Default.Bolt, contentDescription = null, tint = Color(0xFF1976D2), modifier = Modifier.size(16.dp)) }
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text("Electrician", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                Text("Nearby Pros", fontSize = 10.sp, color = Color.Gray)
            }
        }
    }
}

@Composable
fun ActiveJobMiniCard(job: com.piecejob.core.data.remote.dto.JobDto, onClick: () -> Unit) {
    val isCompleted = job.status == "COMPLETED"
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 8.dp)
            .clickable { onClick() },
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isCompleted) Color(0xFFE3F2FD) else Color(0xFFE8F5E9)
        ),
        border = androidx.compose.foundation.BorderStroke(1.dp, if (isCompleted) Color(0xFFBBDEFB) else Color(0xFFC8E6C9))
    ) {
        Row(modifier = Modifier.padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = if (isCompleted) Icons.Default.Grade else Icons.Default.Timer,
                contentDescription = null,
                tint = if (isCompleted) Color(0xFF1976D2) else Color(0xFF2E7D32)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    text = if (isCompleted) "Rate Your Experience" else "Active Job: ${job.serviceCode}",
                    fontWeight = FontWeight.Black,
                    fontSize = 15.sp,
                    color = if (isCompleted) Color(0xFF1565C0) else Color.Black
                )
                Text(
                    text = if (isCompleted) "Tap here to submit your review" else "Status: ${job.status.replace("_", " ")}",
                    fontSize = 12.sp,
                    color = Color.Gray,
                    fontWeight = FontWeight.Medium
                )
            }
            Spacer(modifier = Modifier.weight(1f))
            Icon(Icons.Default.ChevronRight, contentDescription = null, tint = Color.Gray)
        }
    }
}

@Composable
fun EmergencyServicesSection() {
    Column(modifier = Modifier.padding(horizontal = 24.dp, vertical = 16.dp)) {
        Text("Safety & Emergency", fontWeight = FontWeight.Black, color = Color(0xFFD32F2F), fontSize = 16.sp)
        Spacer(modifier = Modifier.height(16.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            EmergencyCard("Urgent Plumber", Modifier.weight(1f))
            EmergencyCard("Locksmith", Modifier.weight(1f))
        }
    }
}

@Composable
fun EmergencyCard(title: String, modifier: Modifier) {
    Card(modifier = modifier, colors = CardDefaults.cardColors(containerColor = Color(0xFFFDECEA))) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Warning, contentDescription = null, tint = Color(0xFFD32F2F), modifier = Modifier.size(14.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text(title, fontWeight = FontWeight.Bold, color = Color(0xFFD32F2F), fontSize = 12.sp)
        }
    }
}

@Composable
fun RecentBookingItem() {
    ListItem(
        headlineContent = { Text("Garden Maintenance", fontWeight = FontWeight.Bold) },
        supportingContent = { Text("Completed • 12 Jun 2026", fontSize = 12.sp, color = Color.Gray) },
        leadingContent = { 
            Surface(modifier = Modifier.size(40.dp), shape = RoundedCornerShape(10.dp), color = Color(0xFFF5F5F5)) {
                Box(contentAlignment = Alignment.Center) { Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF4CAF50), modifier = Modifier.size(20.dp)) }
            }
        },
        trailingContent = { Text("$120.00", fontWeight = FontWeight.Black, fontSize = 14.sp) },
        colors = ListItemDefaults.colors(containerColor = Color.Transparent)
    )
}

@Composable
fun ProviderMiniCard() {
    Card(modifier = Modifier.width(170.dp), shape = RoundedCornerShape(24.dp), colors = CardDefaults.cardColors(containerColor = Color.White)) {
        Column(modifier = Modifier.padding(20.dp)) {
            Box(modifier = Modifier.size(44.dp).clip(CircleShape).background(Color.LightGray))
            Spacer(modifier = Modifier.height(16.dp))
            Text("John Smith", fontWeight = FontWeight.ExtraBold, fontSize = 15.sp)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Star, contentDescription = null, tint = Color(0xFFFFA000), modifier = Modifier.size(12.dp))
                Text(" 4.9 • Elite", fontSize = 11.sp, color = Color(0xFFFFA000), fontWeight = FontWeight.Bold)
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text("0.8 km away", fontSize = 10.sp, color = Color.Gray)
        }
    }
}

@Composable
fun ReferralDashboardCard() {
    Card(modifier = Modifier.fillMaxWidth().padding(24.dp), shape = RoundedCornerShape(24.dp), colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF8E1))) {
        Row(modifier = Modifier.padding(24.dp), verticalAlignment = Alignment.CenterVertically) {
            Surface(modifier = Modifier.size(48.dp), shape = CircleShape, color = Color.White) {
                Box(contentAlignment = Alignment.Center) { Icon(Icons.Default.CardGiftcard, contentDescription = null, tint = Color(0xFFFFA000)) }
            }
            Spacer(modifier = Modifier.width(20.dp))
            Column {
                Text("Invite & Earn $50", fontWeight = FontWeight.Black, fontSize = 16.sp)
                Text("Get rewards for every friend who joins.", fontSize = 12.sp, color = Color.Gray)
            }
        }
    }
}

@Composable
fun CustomerTipsSection() {
    Card(modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp), shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = Color.White)) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text("Safety Task Tips", fontWeight = FontWeight.Black, fontSize = 14.sp)
            Spacer(modifier = Modifier.height(8.dp))
            Text("Learn how PieceJob Escrow and Verification keeps you safe on every booking.", fontSize = 12.sp, color = Color.Gray, lineHeight = 18.sp)
        }
    }
}

@Composable
fun TopProviderCard(provider: com.piecejob.core.data.remote.dto.TopProviderDto) {
    Card(modifier = Modifier.width(170.dp), shape = RoundedCornerShape(24.dp), colors = CardDefaults.cardColors(containerColor = Color.White)) {
        Column(modifier = Modifier.padding(20.dp)) {
            Box(modifier = Modifier.size(44.dp).clip(CircleShape).background(Color.LightGray)) {
                if (provider.photo != null) {
                    coil.compose.AsyncImage(model = provider.photo, contentDescription = null, contentScale = androidx.compose.ui.layout.ContentScale.Crop)
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
            Text(provider.name, fontWeight = FontWeight.ExtraBold, fontSize = 15.sp, maxLines = 1)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Star, contentDescription = null, tint = Color(0xFFFFA000), modifier = Modifier.size(12.dp))
                Text(" ${String.format("%.1f", provider.rating)} • ${provider.tier}", fontSize = 11.sp, color = Color(0xFFFFA000), fontWeight = FontWeight.Bold)
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(provider.services.firstOrNull() ?: "", fontSize = 10.sp, color = Color.Gray, maxLines = 1)
        }
    }
}

@Composable
fun ActivityItem(act: com.piecejob.core.data.remote.dto.ActivityDto) {
    ListItem(
        headlineContent = { Text(act.serviceCode, fontWeight = FontWeight.Bold) },
        supportingContent = { Text("${act.status} • ${act.createdAt.take(10)}", fontSize = 12.sp, color = Color.Gray) },
        leadingContent = { 
            Surface(modifier = Modifier.size(40.dp), shape = RoundedCornerShape(10.dp), color = Color(0xFFF5F5F5)) {
                Box(contentAlignment = Alignment.Center) { Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF4CAF50), modifier = Modifier.size(20.dp)) }
            }
        },
        trailingContent = { Text("$${String.format("%.2f", act.amount)}", fontWeight = FontWeight.Black, fontSize = 14.sp) },
        colors = ListItemDefaults.colors(containerColor = Color.Transparent)
    )
}

@Composable
fun SectionTitle(title: String) {
    Text(text = title, fontSize = 18.sp, fontWeight = FontWeight.Black, modifier = Modifier.padding(start = 24.dp, top = 32.dp, bottom = 12.dp))
}

@Composable
fun SkeletonGrid() {
    Column(modifier = Modifier.padding(24.dp)) {
        Box(modifier = Modifier.size(200.dp, 20.dp).background(Color.LightGray.copy(alpha = 0.3f), RoundedCornerShape(4.dp)))
        Spacer(modifier = Modifier.height(16.dp))
        Row {
            repeat(2) {
                Box(modifier = Modifier.size(140.dp, 160.dp).background(Color.LightGray.copy(alpha = 0.3f), RoundedCornerShape(24.dp)))
                Spacer(modifier = Modifier.width(16.dp))
            }
        }
    }
}
