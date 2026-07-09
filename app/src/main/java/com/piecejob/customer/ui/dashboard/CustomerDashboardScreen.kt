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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.material.icons.filled.Handyman
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material.icons.filled.RateReview
import androidx.compose.material.icons.filled.OpenInNew
import com.piecejob.core.data.remote.ServiceDto

import com.piecejob.core.utils.formatDateTimeString

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
    onNavigateToTracking: (String) -> Unit,
    onNavigateToChat: (String, String) -> Unit = { _, _ -> },
    onNavigateToSubScreen: (String) -> Unit = {}
) {
    val services by viewModel.services.collectAsState()
    val categoriesList by viewModel.categories.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val activeJob by viewModel.activeJob.collectAsState()
    val activeJobs by viewModel.activeJobs.collectAsState()
    val dashboardData by viewModel.dashboardData.collectAsState()
    val realtimePromotions by viewModel.realtimePromotions.collectAsState()
    val currencySymbol by viewModel.currencySymbol.collectAsState()
    val currentAddress by viewModel.currentAddress.collectAsState()
    val searchResults by viewModel.searchResults.collectAsState()
    val bookAgainServices by viewModel.bookAgainServices.collectAsState()
    val currentUserId = viewModel.currentUserId

    var searchQuery by remember { mutableStateOf("") }
    var isSearchActive by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.loadActiveJob()
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
                currency = currencySymbol
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
                        is com.piecejob.core.data.remote.ServiceCategoryDto -> { }
                        is com.piecejob.core.data.remote.dto.SavedLocationDto -> {
                            onNavigateToBookingWithLocation(it)
                        }
                    }
                }
            }
        }

        // SECTION 4: PROMOTIONAL BANNER CAROUSEL
        item { 
            PromotionCarousel(
                promotions = realtimePromotions,
                isLoading = isLoading
            ) 
        }

        // SECTION 5: POPULAR CATEGORIES
        item { PopularCategories(categoriesList) }

        // SECTION 11: CURRENT ACTIVE JOBS
        if (activeJobs.isNotEmpty()) {
            item {
                Text(
                    text = "Active PieceJobs",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Black,
                    modifier = Modifier.padding(start = 24.dp, top = 24.dp, bottom = 12.dp)
                )
            }
            items(activeJobs) { job ->
                ActiveJobCard(
                    job = job,
                    onTrack = { onNavigateToTracking(job.id) },
                    onResume = {
                        onNavigateToSubScreen(com.piecejob.core.ui.navigation.Screen.Negotiation.passArgs(job.id, job.providerId ?: ""))
                    }
                )
            }
        }
        item { SectionTitle("Book Again") }
        item {
            LazyRow(
                contentPadding = PaddingValues(horizontal = 24.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                if (bookAgainServices.isEmpty()) {
                    if (isLoading) {
                        items(3) { Box(modifier = Modifier.size(64.dp).clip(CircleShape).background(Color.LightGray.copy(alpha = 0.2f))) }
                    } else {
                        item { Text("No recently booked services.", fontSize = 12.sp, color = Color.Gray, modifier = Modifier.padding(vertical = 12.dp)) }
                    }
                } else {
                    items(bookAgainServices) { service ->
                        RecentServiceAvatar(service.name) { onServiceClick(service) }
                    }
                }
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
                if (recommendations.isEmpty() && isLoading) {
                    items(3) { SkeletonCard(width = 140.dp) }
                } else if (recommendations.isEmpty()) {
                    item { Text("Calculating recommendations...", fontSize = 12.sp, color = Color.Gray) }
                } else {
                    items(recommendations) { service ->
                        ServiceCardSmall(service) { selectedServiceForDetails = it }
                    }
                }
            }
        }

        // SECTION 12: LATEST ACTIVITY
        item { SectionTitle("Latest Activity") }
        val activityList = dashboardData?.latestActivity ?: emptyList()
        val filteredActivity = activityList
            .filter { it.status == "COMPLETED" || it.status == "CANCELLED" }
            .take(5)

        if (filteredActivity.isEmpty()) {
            if (isLoading) {
                items(2) { SkeletonListItem() }
            } else {
                item { Text("No recent activity found.", fontSize = 12.sp, color = Color.Gray, modifier = Modifier.padding(horizontal = 24.dp)) }
            }
        } else {
            items(
                items = filteredActivity,
                key = { it.id }
            ) { act ->
                ActivityItem(act, currencySymbol, currentUserId)
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
                if (providers.isEmpty() && isLoading) {
                    items(3) { SkeletonCard(width = 170.dp) }
                } else if (providers.isEmpty()) {
                    item { Text("No providers found in your area.", fontSize = 12.sp, color = Color.Gray) }
                } else {
                    items(providers) { provider ->
                        TopProviderCard(provider)
                    }
                }
            }
        }

        // SECTION 13: REFERRAL PROGRAM
        item { 
            ReferralDashboardCard(dashboardData?.referralCampaign) {
                onProfileClick()
            } 
        }

        // SECTION 14: CUSTOMER TIPS
        item { CustomerTipsSection() }
        
        item { Spacer(modifier = Modifier.height(100.dp)) }
    }

    // FLOATING ACTION BUTTON
    Box(modifier = Modifier.fillMaxSize()) {
        ExtendedFloatingActionButton(
            onClick = onRequestServiceClick,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
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
        is com.piecejob.core.data.remote.dto.TopProviderDto -> result.name
        else -> "Result"
    }
    val type = when (result) {
        is ServiceDto -> "Service"
        is com.piecejob.core.data.remote.ServiceCategoryDto -> "Category"
        is com.piecejob.core.data.remote.dto.SavedLocationDto -> "Saved Location"
        is com.piecejob.core.data.remote.dto.TopProviderDto -> "Provider"
        else -> "Other"
    }
    val icon = when (result) {
        is com.piecejob.core.data.remote.dto.SavedLocationDto -> Icons.Default.Star
        is com.piecejob.core.data.remote.dto.TopProviderDto -> Icons.Default.Person
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
fun PromotionCarousel(promotions: List<com.piecejob.core.data.remote.dto.PromotionDto>, isLoading: Boolean) {
    if (isLoading && promotions.isEmpty()) {
        Box(modifier = Modifier.padding(horizontal = 24.dp)) {
            SkeletonCard(width = 360.dp)
        }
        return
    }

    val promo = if (promotions.isNotEmpty()) promotions.first() else null
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(130.dp)
            .padding(horizontal = 24.dp),
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFD32F2F))
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            if (promo?.imageUrl != null) {
                coil.compose.AsyncImage(
                    model = promo.imageUrl,
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = androidx.compose.ui.layout.ContentScale.Crop
                )
                Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.3f)))
            } else {
                Icon(
                    Icons.Default.Stars, 
                    contentDescription = null, 
                    tint = Color.White.copy(alpha = 0.2f), 
                    modifier = Modifier.size(100.dp).align(Alignment.CenterEnd)
                )
            }
            
            Column(modifier = Modifier.padding(24.dp)) {
                Text(
                    text = promo?.title ?: "PIECEJOB MAKES FINDING WORKERS EASY", 
                    color = Color.White, 
                    fontWeight = FontWeight.Black, 
                    fontSize = 18.sp,
                    lineHeight = 24.sp
                )
                Text(
                    text = promo?.description ?: "Need a mechanic, plumber, electrician, cleaner, mover, or any skilled worker? PieceJob connects you with trusted professionals near you in minutes — fast, reliable, and hassle-free.", 
                    color = Color.White.copy(alpha = 0.8f), 
                    fontSize = 12.sp, 
                    maxLines = 4
                )
                Spacer(modifier = Modifier.height(8.dp))
                Surface(color = Color.White, shape = RoundedCornerShape(8.dp)) {
                    Text(
                        text = promo?.ctaText ?: "LEARN MORE",
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp), 
                        fontSize = 10.sp, 
                        fontWeight = FontWeight.Black, 
                        color = Color.Black
                    )
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
            val cats = listOf("HDS", "CSS", "HMS", "OPS", "LLS", "TSS")
            cats.forEach { code -> CategoryIcon(code, code) }
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
                    Surface(color = Color(0xFFE8F5E9), shape = RoundedCornerShape(6.dp)) {
                        Text(text = "LIVE", modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp), fontSize = 8.sp, fontWeight = FontWeight.Black, color = Color(0xFF2E7D32))
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
        title = { Text(text = service.name, fontWeight = FontWeight.Black, fontSize = 20.sp) },
        text = {
            Column {
                Text(text = service.description ?: "Professional service on demand.", fontSize = 14.sp)
                if ((service.bookingFee ?: 0.0) > 0) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Surface(
                        color = Color(0xFFF5F5F5),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp).fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Booking Fee", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            Text(String.format("%.2f", service.bookingFee), fontWeight = FontWeight.Black, fontSize = 12.sp)
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = onConfirm, colors = ButtonDefaults.buttonColors(containerColor = confirmColor)) {
                Text("CONFIRM", fontWeight = FontWeight.Black)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("CANCEL", color = Color.Gray) }
        },
        containerColor = Color.White,
        shape = RoundedCornerShape(28.dp)
    )
}

@Composable
fun RecentServiceAvatar(label: String, onClick: () -> Unit = {}) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.clickable { onClick() }) {
        Box(modifier = Modifier.size(64.dp).clip(CircleShape).background(Color.White).padding(2.dp).border(2.dp, Color(0xFFD32F2F), CircleShape).padding(4.dp)) {
           Surface(modifier = Modifier.fillMaxSize(), shape = CircleShape, color = Color(0xFFF5F5F5)) {
               Box(contentAlignment = Alignment.Center) { Icon(Icons.Default.History, contentDescription = null, tint = Color.Gray) }
           }
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(label, fontSize = 11.sp, fontWeight = FontWeight.Bold, maxLines = 1)
    }
}

@Composable
fun ActiveJobCard(
    job: com.piecejob.core.data.remote.dto.JobDto,
    onTrack: () -> Unit,
    onResume: () -> Unit
) {
    val isNegotiation = job.status == "PROVIDER_ACCEPTED" || job.status == "ACCEPTED"
    
    var statusLabel = ""
    var statusIcon = Icons.Default.Timer
    var statusColor = Color.Gray
    var actionLabel = "OPEN"
    var actionIcon = Icons.Default.ChevronRight

    when (job.status) {
        "PROVIDER_ACCEPTED" -> {
            statusLabel = "Negotiation (Round ${job.negotiationRounds ?: 1} of 4)"
            statusIcon = Icons.Default.Chat
            statusColor = Color(0xFFFFA000)
            actionLabel = "RESUME"
            actionIcon = Icons.Default.OpenInNew
        }
        "ACCEPTED" -> {
            statusLabel = "Price agreed (Wait for dispatch)"
            statusIcon = Icons.Default.CheckCircle
            statusColor = Color(0xFF2E7D32)
            actionLabel = "RESUME"
            actionIcon = Icons.Default.OpenInNew
        }
        "EN_ROUTE" -> {
            statusLabel = "Provider is on the way"
            statusIcon = Icons.Default.DirectionsCar
            statusColor = Color(0xFFE65100)
            actionLabel = "TRACK"
            actionIcon = Icons.Default.Navigation
        }
        "ARRIVED" -> {
            statusLabel = "Provider has arrived"
            statusIcon = Icons.Default.LocationOn
            statusColor = Color(0xFF4CAF50)
            actionLabel = "TRACK"
            actionIcon = Icons.Default.Navigation
        }
        "STARTED", "IN_PROGRESS" -> {
            statusLabel = "Work has started"
            statusIcon = Icons.Default.Handyman
            statusColor = Color(0xFF2E7D32)
            actionLabel = "TRACK"
            actionIcon = Icons.Default.Navigation
        }
        "COMPLETED" -> {
            statusLabel = "Work completed"
            statusIcon = Icons.Default.Verified
            statusColor = Color(0xFF1976D2)
            actionLabel = "OPEN"
            actionIcon = Icons.Default.RateReview
        }
        else -> {
            statusLabel = job.status
            statusIcon = Icons.Default.Timer
            statusColor = Color.Gray
            actionLabel = "OPEN"
            actionIcon = Icons.Default.ChevronRight
        }
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 8.dp),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = statusIcon,
                        contentDescription = null,
                        tint = statusColor,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = statusLabel,
                        fontWeight = FontWeight.Black,
                        color = statusColor,
                        fontSize = 11.sp,
                        letterSpacing = 1.sp
                    )
                }
                
                // Subtle Progress Indicator
                val progress = when(job.status) {
                    "PROVIDER_ACCEPTED" -> 0.1f
                    "ACCEPTED" -> 0.3f
                    "EN_ROUTE" -> 0.5f
                    "ARRIVED" -> 0.7f
                    "STARTED", "IN_PROGRESS" -> 0.9f
                    "COMPLETED" -> 1.0f
                    else -> 0.0f
                }
                LinearProgressIndicator(
                    progress = progress,
                    modifier = Modifier.width(40.dp).height(4.dp).clip(CircleShape),
                    color = statusColor,
                    trackColor = statusColor.copy(alpha = 0.1f)
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    modifier = Modifier.size(44.dp),
                    shape = CircleShape,
                    color = Color(0xFFF8F9FA)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.Person, contentDescription = null, tint = Color.LightGray)
                    }
                }
                Spacer(modifier = Modifier.width(16.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = job.serviceName ?: job.serviceCode ?: "Active Job",
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 16.sp
                    )
                    Text(
                        text = if (job.providerInfo != null) "${job.providerInfo.firstName} ${job.providerInfo.lastName.take(1)}." else "Provider assigned",
                        fontSize = 13.sp,
                        color = Color.Gray,
                        fontWeight = FontWeight.Bold
                    )
                }
                
                Button(
                    onClick = { if (isNegotiation) onResume() else onTrack() },
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF121212)),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Icon(actionIcon, contentDescription = null, modifier = Modifier.size(14.dp), tint = Color.White)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = actionLabel,
                        color = Color.White,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Black
                    )
                }
            }
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
fun ReferralDashboardCard(campaign: com.piecejob.core.data.remote.dto.ReferralCampaignDto?, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(24.dp).clickable { onClick() }, 
        shape = RoundedCornerShape(24.dp), 
        colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF8E1))
    ) {
        Row(modifier = Modifier.padding(24.dp), verticalAlignment = Alignment.CenterVertically) {
            Surface(modifier = Modifier.size(48.dp), shape = CircleShape, color = Color.White) {
                Box(contentAlignment = Alignment.Center) { Icon(Icons.Default.CardGiftcard, contentDescription = null, tint = Color(0xFFFFA000)) }
            }
            Spacer(modifier = Modifier.width(20.dp))
            Column {
                Text(text = campaign?.title ?: "Invite & Earn", fontWeight = FontWeight.Black, fontSize = 16.sp)
                Text(text = campaign?.description ?: "Get rewards for every friend who joins.", fontSize = 12.sp, color = Color.Gray)
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
            Text("Learn how PieceJob Escrow keeps you safe.", fontSize = 12.sp, color = Color.Gray, lineHeight = 18.sp)
        }
    }
}

@Composable
fun TopProviderCard(provider: com.piecejob.core.data.remote.dto.TopProviderDto) {
    Card(modifier = Modifier.width(170.dp), shape = RoundedCornerShape(24.dp), colors = CardDefaults.cardColors(containerColor = Color.White)) {
        Column(modifier = Modifier.padding(20.dp)) {
            Box(modifier = Modifier.size(44.dp).clip(CircleShape).background(Color.LightGray)) {
                if (provider.photo != null) coil.compose.AsyncImage(model = provider.photo, contentDescription = null, contentScale = androidx.compose.ui.layout.ContentScale.Crop)
            }
            Spacer(modifier = Modifier.height(16.dp))
            Text(provider.name, fontWeight = FontWeight.ExtraBold, fontSize = 15.sp, maxLines = 1)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Star, contentDescription = null, tint = Color(0xFFFFA000), modifier = Modifier.size(12.dp))
                Text(" ${String.format("%.1f", provider.rating)} • ${provider.tier}", fontSize = 11.sp, color = Color(0xFFFFA000), fontWeight = FontWeight.Bold)
            }
            Spacer(modifier = Modifier.height(4.dp))
            val distLabel = if (provider.distance != null) {
                val km = provider.distance / 1000
                if (km < 1) "${String.format("%.0f", provider.distance)}m away" 
                else "${String.format("%.1f", km)}km away"
            } else "Nearby"
            
            Text(distLabel, fontSize = 10.sp, color = Color.Gray, fontWeight = FontWeight.Medium)
            Text(provider.services.firstOrNull() ?: "General Pro", fontSize = 10.sp, color = Color.Gray, maxLines = 1)
        }
    }
}

@Composable
fun ActivityItem(act: com.piecejob.core.data.remote.dto.ActivityDto, currency: String, currentUserId: String) {
    val isCancelled = act.status == "CANCELLED"
    val statusColor = if (isCancelled) Color(0xFFD32F2F) else Color(0xFF2E7D32)
    val bgColor = if (isCancelled) Color(0xFFFFEBEE) else Color(0xFFE8F5E9)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 8.dp),
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
                        text = act.serviceName ?: "General Service",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.Black
                    )
                }
                
                Column(horizontalAlignment = Alignment.End) {
                    if (!isCancelled && act.isNegotiated == false) {
                        Text(
                            text = "Booking Fee",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF1976D2)
                        )
                    }
                    Text(
                        text = "$currency ${String.format("%.2f", act.amount)}",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Black,
                        color = Color.Black
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(verticalAlignment = Alignment.Top) {
                Icon(Icons.Default.LocationOn, null, modifier = Modifier.size(12.dp).padding(top = 2.dp), tint = Color.Gray)
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = act.address ?: "Service Address",
                    fontSize = 11.sp,
                    color = Color.Gray,
                    fontWeight = FontWeight.Medium,
                    lineHeight = 14.sp
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            if (isCancelled) {
                val actor = if (act.cancelledBy == currentUserId) "You" else "Provider"
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Column {
                        Text("Cancelled:", fontSize = 9.sp, fontWeight = FontWeight.Black, color = Color.Gray)
                        Text(formatDateTimeString(act.cancelledAt ?: act.createdAt), fontSize = 10.sp, fontWeight = FontWeight.Bold)
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
                        Text(formatDateTimeString(act.startedAt), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text("Completed:", fontSize = 9.sp, fontWeight = FontWeight.Black, color = Color.Gray)
                        Text(formatDateTimeString(act.completedAt), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }
                }
                
                if (act.amount <= 0) {
                    // This shouldn't happen based on requirements but handle just in case
                    Text(
                        "Total Paid: $currency 0.00", 
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

@Composable
fun SkeletonCard(width: androidx.compose.ui.unit.Dp) {
    Box(modifier = Modifier.size(width, 160.dp).background(Color.LightGray.copy(alpha = 0.2f), RoundedCornerShape(24.dp)))
}

@Composable
fun SkeletonListItem() {
    Row(modifier = Modifier.fillMaxWidth().padding(24.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(modifier = Modifier.size(40.dp).background(Color.LightGray.copy(alpha = 0.2f), RoundedCornerShape(10.dp)))
        Spacer(modifier = Modifier.width(16.dp))
        Column {
            Box(modifier = Modifier.size(120.dp, 12.dp).background(Color.LightGray.copy(alpha = 0.2f), RoundedCornerShape(4.dp)))
            Spacer(modifier = Modifier.height(8.dp))
            Box(modifier = Modifier.size(80.dp, 8.dp).background(Color.LightGray.copy(alpha = 0.2f), RoundedCornerShape(4.dp)))
        }
    }
}
