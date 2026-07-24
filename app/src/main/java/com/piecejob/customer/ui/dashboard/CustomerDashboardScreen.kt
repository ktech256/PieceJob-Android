package com.piecejob.customer.ui.dashboard

import androidx.compose.animation.*
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
import com.piecejob.core.ui.components.PieceJobButton
import com.piecejob.core.utils.formatDateTimeString
import com.piecejob.core.utils.formatRating

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
    val isReferralEnabled by viewModel.isReferralEnabled.collectAsState()
    val currentAddress by viewModel.currentAddress.collectAsState()
    val searchResults by viewModel.searchResults.collectAsState()
    val bookAgainServices by viewModel.bookAgainServices.collectAsState()
    val currentUserId = viewModel.currentUserId

    var searchQuery by remember { mutableStateOf("") }
    var isSearchActive by remember { mutableStateOf(false) }

    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            if (event == androidx.lifecycle.Lifecycle.Event.ON_RESUME) {
                android.util.Log.d("FORENSIC", "DASHBOARD_RESUMED | Refreshing data")
                viewModel.refresh()
                viewModel.loadActiveJob()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

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
                    },
                    onNavigateToChat = onNavigateToChat
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
        if (isReferralEnabled) {
            item { 
                ReferralDashboardCard(dashboardData?.referralCampaign) {
                    onNavigateToSubScreen(com.piecejob.core.ui.navigation.Screen.CustomerReferrals.route)
                } 
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
        supportingContent = { 
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(type, fontSize = 12.sp, color = Color.Gray)
                if (result is com.piecejob.core.data.remote.dto.TopProviderDto) {
                    Spacer(modifier = Modifier.width(8.dp))
                    val isNew = result.ratingCount <= 5
                    if (isNew) {
                        Text("⭐ New Provider", fontSize = 11.sp, color = Color(0xFF1976D2), fontWeight = FontWeight.Bold)
                    } else {
                        Icon(Icons.Default.Star, null, tint = Color(0xFFFFA000), modifier = Modifier.size(12.dp))
                        Text(" ${formatRating(result.rating)}", fontSize = 12.sp, color = Color(0xFFFFA000), fontWeight = FontWeight.Bold)
                    }
                }
            }
        },
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
fun ActiveJobChip(status: String, color: Color) {
    Surface(
        color = color.copy(alpha = 0.1f),
        shape = RoundedCornerShape(8.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, color.copy(alpha = 0.2f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Box(modifier = Modifier.size(6.dp).background(color, CircleShape))
            Text(
                text = status.uppercase(),
                fontSize = 10.sp,
                fontWeight = FontWeight.Black,
                color = color,
                letterSpacing = 0.5.sp
            )
        }
    }
}

@Composable
fun ActiveJobCard(
    job: com.piecejob.core.data.remote.dto.JobDto,
    onTrack: () -> Unit,
    onResume: () -> Unit,
    onNavigateToChat: (String, String) -> Unit = { _, _ -> }
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    
    // Status Logic
    val (statusLabel, statusColor) = when (job.status) {
        "BROADCASTED", "BROADCASTING", "REQUEST_CREATED" -> "Searching" to Color.Gray
        "PROVIDER_ACCEPTED" -> "Negotiation" to Color(0xFFFFA000)
        "ACCEPTED" -> "Accepted" to Color(0xFFE65100)
        "EN_ROUTE" -> "En Route" to Color(0xFF1976D2)
        "ARRIVED" -> "Arrived" to Color(0xFF2E7D32)
        "STARTED", "IN_PROGRESS" -> "In Progress" to Color(0xFF2E7D32)
        "COMPLETED", "RATED" -> "Completed" to Color(0xFF2E7D32)
        "CANCELLED" -> "Cancelled" to Color(0xFFD32F2F)
        else -> job.status to Color.Gray
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 8.dp)
            .animateContentSize(),
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            // HEADER: Status Chip + Job ID
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                ActiveJobChip(statusLabel, statusColor)
                Text(
                    text = "#${job.id.takeLast(6).uppercase()}",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.LightGray
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            // PROVIDER INFO: Horizontal Layout
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    modifier = Modifier.size(56.dp),
                    shape = RoundedCornerShape(18.dp),
                    color = Color(0xFFF8F9FA)
                ) {
                    if (job.providerInfo?.profilePicture != null) {
                        coil.compose.AsyncImage(
                            model = job.providerInfo.profilePicture,
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = androidx.compose.ui.layout.ContentScale.Crop
                        )
                    } else {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(Icons.Default.Person, contentDescription = null, tint = Color.LightGray)
                        }
                    }
                }

                Spacer(modifier = Modifier.width(16.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = job.providerInfo?.let { "${it.firstName} ${it.lastName}" } ?: "Searching for Pro...",
                        fontWeight = FontWeight.Black,
                        fontSize = 17.sp,
                        maxLines = 1,
                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = job.serviceName ?: job.serviceCode ?: "Active PieceJob",
                            fontSize = 12.sp,
                            color = Color.Gray,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1
                        )
                        if (job.providerInfo != null) {
                            Text(" • ", color = Color.LightGray)
                            Icon(Icons.Default.Star, contentDescription = null, tint = Color(0xFFFFA000), modifier = Modifier.size(12.dp))
                            val isNew = (job.providerInfo.jobsCompleted ?: 0) <= 5
                            Text(
                                text = if (isNew) " New Provider" else " ${formatRating(job.providerInfo.ratingAvg)}",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Black,
                                color = if (isNew) Color(0xFF1976D2) else Color(0xFFFFA000)
                            )
                        }
                    }
                }
                
                if (job.providerId != null && (job.status == "EN_ROUTE" || job.status == "ARRIVED")) {
                    FilledIconButton(
                        onClick = {
                            val intent = android.content.Intent(android.content.Intent.ACTION_DIAL).apply {
                                data = android.net.Uri.parse("tel:${job.providerInfo?.phoneNumber}")
                            }
                            context.startActivity(intent)
                        },
                        modifier = Modifier.size(44.dp),
                        colors = IconButtonDefaults.filledIconButtonColors(containerColor = Color(0xFFFDECEA))
                    ) {
                        Icon(Icons.Default.Phone, contentDescription = "Call", tint = Color(0xFFD32F2F), modifier = Modifier.size(20.dp))
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // DYNAMIC BUTTONS
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                when (job.status) {
                    "BROADCASTED", "BROADCASTING", "REQUEST_CREATED" -> {
                        PieceJobButton(
                            text = "SEARCHING...",
                            onClick = {},
                            enabled = false,
                            modifier = Modifier.weight(1f),
                            height = 48.dp,
                            fontSize = 12.sp
                        )
                    }
                    "PROVIDER_ACCEPTED", "ACCEPTED" -> {
                        PieceJobButton(
                            text = "RESUME",
                            onClick = onResume,
                            modifier = Modifier.weight(1f),
                            height = 48.dp,
                            fontSize = 12.sp,
                            containerColor = Color(0xFFFFA000),
                            icon = Icons.Default.Chat
                        )
                    }
                    "EN_ROUTE", "ARRIVED" -> {
                        PieceJobButton(
                            text = "TRACK LIVE",
                            onClick = onTrack,
                            modifier = Modifier.weight(1f),
                            height = 48.dp,
                            fontSize = 12.sp,
                            containerColor = Color(0xFF121212),
                            icon = Icons.Default.Navigation
                        )
                    }
                    "STARTED", "IN_PROGRESS" -> {
                        PieceJobButton(
                            text = "VIEW PROGRESS",
                            onClick = onTrack,
                            modifier = Modifier.weight(1f),
                            height = 48.dp,
                            fontSize = 12.sp,
                            containerColor = Color(0xFF121212),
                            icon = Icons.Default.Handyman
                        )
                    }
                    "COMPLETED" -> {
                        PieceJobButton(
                            text = "RATE PRO",
                            onClick = onTrack,
                            modifier = Modifier.weight(1f),
                            height = 48.dp,
                            fontSize = 12.sp,
                            containerColor = Color(0xFF2E7D32),
                            icon = Icons.Default.Star
                        )
                    }
                    "CANCELLED" -> {
                        PieceJobButton(
                            text = "BOOK AGAIN",
                            onClick = {}, // TODO: Implement re-book
                            modifier = Modifier.weight(1f),
                            height = 48.dp,
                            fontSize = 12.sp,
                            containerColor = Color(0xFF121212)
                        )
                    }
                }
                
                if (job.providerId != null && job.status != "COMPLETED" && job.status != "CANCELLED") {
                    androidx.compose.material3.OutlinedButton(
                        onClick = { onNavigateToChat(job.id, job.providerId) },
                        modifier = Modifier.height(48.dp),
                        shape = RoundedCornerShape(16.dp),
                        contentPadding = PaddingValues(horizontal = 16.dp)
                    ) {
                        Icon(Icons.Default.Message, contentDescription = null, modifier = Modifier.size(18.dp))
                    }
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
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 16.dp)
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
                        text = campaign?.description ?: "Invite friends to PieceJob and earn rewards for every successful job.", 
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
            val isNew = provider.ratingCount <= 5
            if (isNew) {
                Text("⭐⭐⭐⭐⭐", fontSize = 11.sp, color = Color(0xFFFFA000))
                Text("New Provider", fontSize = 11.sp, color = Color(0xFF1976D2), fontWeight = FontWeight.Bold)
            } else {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Star, contentDescription = null, tint = Color(0xFFFFA000), modifier = Modifier.size(12.dp))
                    Text(" ${formatRating(provider.rating)} • ${provider.tier}", fontSize = 11.sp, color = Color(0xFFFFA000), fontWeight = FontWeight.Bold)
                }
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
                    val amount = act.amount
                    Text(
                        text = if (amount != null) "$currency ${String.format("%.2f", amount)}" else "N/A",
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
                val actor = if (act.cancelledBy == currentUserId) "You" else (act.cancelledByName ?: "Provider")
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
                
                if ((act.amount ?: 0.0) <= 0) {
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
