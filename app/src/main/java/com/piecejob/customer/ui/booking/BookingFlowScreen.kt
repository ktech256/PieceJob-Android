package com.piecejob.customer.ui.booking

import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.compose.ui.viewinterop.AndroidView
import android.webkit.WebView
import android.webkit.WebViewClient
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.*
import com.piecejob.core.data.remote.ServiceDto
import com.piecejob.core.data.remote.dto.ProviderDto
import com.piecejob.core.ui.components.PieceJobButton
import com.piecejob.core.ui.components.PieceJobOutlinedButton
import com.piecejob.customer.ui.dashboard.ServiceDetailsDialog

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BookingFlowScreen(
    viewModel: BookingViewModel = hiltViewModel(),
    initialServiceCode: String? = null,
    initialAddress: String? = null,
    initialLat: Double? = null,
    initialLng: Double? = null,
    onTrackingStart: (String) -> Unit,
    onBack: () -> Unit
) {
    val currentStep by viewModel.currentStep.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.initializeWithArgs(initialServiceCode, initialAddress, initialLat, initialLng)
    }

    BackHandler {
        if (currentStep == BookingStep.ADDRESS_SELECTION) {
            onBack()
        } else {
            viewModel.previousStep()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(getStepTitle(currentStep), fontWeight = FontWeight.Black, fontSize = 18.sp) },
                navigationIcon = {
                    IconButton(onClick = { if (currentStep == BookingStep.ADDRESS_SELECTION) onBack() else viewModel.previousStep() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .navigationBarsPadding()
        ) {
            when (currentStep) {
                BookingStep.ADDRESS_SELECTION -> AddressSelectionStep(viewModel, initialLat, initialLng)
                BookingStep.RECIPIENT_SELECTION -> RecipientSelectionStep(viewModel)
                BookingStep.CATEGORY_SELECTION -> CategorySelectionStep(viewModel)
                BookingStep.SERVICE_SELECTION -> ServiceSelectionStep(viewModel)
                BookingStep.BOOKING_FEE -> BookingFeeStep(viewModel)
                BookingStep.PAYMENT_GATEWAY -> { /* Skipped - handled automatically by backend routing */ }
                BookingStep.PAYMENT_WEBVIEW -> PaymentWebViewStep(viewModel)
                BookingStep.TRACKING -> {
                    val job by viewModel.createdJob.collectAsState()
                    LaunchedEffect(job) {
                        job?.id?.let { onTrackingStart(it) }
                    }
                }
            }

            if (isLoading) {
                Box(modifier = Modifier.fillMaxSize().background(Color.White.copy(alpha = 0.5f)), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = Color(0xFFD32F2F))
                }
            }
        }
    }
}

@Composable
fun AddressSelectionStep(viewModel: BookingViewModel, initialLat: Double?, initialLng: Double?) {
    val nearbyProviders by viewModel.nearbyProviders.collectAsState()
    val error by viewModel.error.collectAsState()
    val selectedCoords by viewModel.selectedCoordinates.collectAsState()
    val currentGps by viewModel.currentGpsCoordinates.collectAsState()
    val addressPredictions by viewModel.addressPredictions.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val context = androidx.compose.ui.platform.LocalContext.current

    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(LatLng(0.0, 0.0), 2f)
    }

    val permissionLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        if (permissions[android.Manifest.permission.ACCESS_FINE_LOCATION] == true ||
            permissions[android.Manifest.permission.ACCESS_COARSE_LOCATION] == true) {
            viewModel.fetchCurrentLocation(isManualSelection = false)
        }
    }

    fun checkAndRequestLocation() {
        val hasFine = androidx.core.content.ContextCompat.checkSelfPermission(context, android.Manifest.permission.ACCESS_FINE_LOCATION) == android.content.pm.PackageManager.PERMISSION_GRANTED
        val hasCoarse = androidx.core.content.ContextCompat.checkSelfPermission(context, android.Manifest.permission.ACCESS_COARSE_LOCATION) == android.content.pm.PackageManager.PERMISSION_GRANTED
        
        if (hasFine || hasCoarse) {
            viewModel.fetchCurrentLocation(isManualSelection = false)
        } else {
            permissionLauncher.launch(arrayOf(
                android.Manifest.permission.ACCESS_FINE_LOCATION,
                android.Manifest.permission.ACCESS_COARSE_LOCATION
            ))
        }
    }

    // Effect to center map when current GPS is found (Initial Entry)
    LaunchedEffect(currentGps) {
        if (selectedCoords == null && currentGps != null) {
            cameraPositionState.animate(
                com.google.android.gms.maps.CameraUpdateFactory.newLatLngZoom(
                    LatLng(currentGps!![1], currentGps!![0]), 12.5f // ~8km radius view
                )
            )
        }
    }

    // Effect to center map when a specific location is selected (Marker)
    LaunchedEffect(selectedCoords) {
        selectedCoords?.let { coords ->
            cameraPositionState.animate(
                com.google.android.gms.maps.CameraUpdateFactory.newLatLngZoom(
                    LatLng(coords[1], coords[0]), 15f
                )
            )
        }
    }

    LaunchedEffect(Unit) {
        if (initialLat == null || initialLng == null) {
            checkAndRequestLocation()
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // MAP (75%)
        Box(modifier = Modifier.weight(0.75f)) {
            var isMapLoaded by remember { mutableStateOf(false) }
            
            LaunchedEffect(Unit) {
                android.util.Log.d("MAP_INIT", "Starting GoogleMap initialization. API Key present: ${com.piecejob.BuildConfig.GOOGLE_MAPS_API_KEY.isNotBlank()}")
                kotlinx.coroutines.delay(10000)
                if (!isMapLoaded) {
                    isMapLoaded = true
                }
            }

            GoogleMap(
                modifier = Modifier.fillMaxSize(),
                cameraPositionState = cameraPositionState,
                onMapLoaded = { 
                    isMapLoaded = true 
                },
                onMapClick = { latLng ->
                    val formatted = "Selected: ${String.format(java.util.Locale.US, "%.4f, %.4f", latLng.latitude, latLng.longitude)}"
                    viewModel.setAddress(formatted, listOf(latLng.longitude, latLng.latitude))
                },
                onPOIClick = { poi ->
                    viewModel.setAddress(poi.name, listOf(poi.latLng.longitude, poi.latLng.latitude))
                },
                uiSettings = MapUiSettings(
                    zoomControlsEnabled = false,
                    myLocationButtonEnabled = false
                ),
                properties = MapProperties(
                    isMyLocationEnabled = true
                )
            ) {
                nearbyProviders.forEach { provider ->
                    provider.location.coordinates.let { coords ->
                        Marker(
                            state = MarkerState(position = LatLng(coords[1], coords[0])),
                            title = "${provider.firstName} ${provider.lastName}",
                            icon = com.google.android.gms.maps.model.BitmapDescriptorFactory.defaultMarker(com.google.android.gms.maps.model.BitmapDescriptorFactory.HUE_GREEN)
                        )
                    }
                }

                selectedCoords?.let { coords ->
                    Marker(
                        state = MarkerState(position = LatLng(coords[1], coords[0])),
                        title = "Service Location",
                        icon = com.google.android.gms.maps.model.BitmapDescriptorFactory.defaultMarker(com.google.android.gms.maps.model.BitmapDescriptorFactory.HUE_RED)
                    )
                }
            }

            if (!isMapLoaded) {
                Box(
                    modifier = Modifier.fillMaxSize().background(Color.White),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = Color(0xFFD32F2F))
                }
            }
            
            if (error != null) {
                Surface(
                    color = Color.Red,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.align(Alignment.TopCenter).padding(16.dp)
                ) {
                    Text(error!!, color = Color.White, modifier = Modifier.padding(12.dp), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        // SELECTION UI (25%)
        Card(
            modifier = Modifier.weight(if (addressPredictions.isNotEmpty()) 0.55f else 0.35f).fillMaxWidth(),
            shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 16.dp)
        ) {
            Column(modifier = Modifier.padding(24.dp)) {
                val focusManager = LocalFocusManager.current
                val addressText by viewModel.selectedAddress.collectAsState()
                
                OutlinedTextField(
                    value = addressText,
                    onValueChange = { 
                        viewModel.searchAddress(it)
                    },
                    placeholder = { Text("Search for address...") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    trailingIcon = {
                        if (addressText.isNotBlank()) {
                            IconButton(onClick = { viewModel.searchAddress("") }) {
                                Icon(Icons.Default.Close, contentDescription = "Clear")
                            }
                        }
                    },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Text,
                        imeAction = ImeAction.Search
                    ),
                    keyboardActions = KeyboardActions(
                        onSearch = {
                            focusManager.clearFocus()
                        }
                    ),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = Color.White,
                        unfocusedContainerColor = Color.White,
                        focusedBorderColor = Color(0xFFD32F2F)
                    )
                )

                if (addressPredictions.isNotEmpty()) {
                    LazyColumn(
                        modifier = Modifier.fillMaxWidth().heightIn(max = 200.dp).padding(top = 8.dp)
                    ) {
                        items(addressPredictions) { prediction ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { 
                                        viewModel.onPredictionSelected(prediction)
                                        focusManager.clearFocus()
                                    }
                                    .padding(vertical = 12.dp, horizontal = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.LocationOn, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(prediction.primaryText, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                    Text(prediction.secondaryText, color = Color.Gray, fontSize = 12.sp)
                                }
                            }
                            HorizontalDivider(color = Color(0xFFEEEEEE), thickness = 0.5.dp)
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // "Your Location" Button (35%)
                    PieceJobButton(
                        text = "YOUR LOCATION",
                        onClick = { viewModel.fetchCurrentLocation(isManualSelection = true) },
                        modifier = Modifier.weight(0.35f),
                        containerColor = Color(0xFF25D366), // WhatsApp Green
                        icon = Icons.Default.MyLocation,
                        fontSize = 10.sp,
                        height = 56.dp,
                        isLoading = isLoading
                    )

                    // "Continue" Button (65%)
                    PieceJobButton(
                        text = "CONTINUE",
                        onClick = { viewModel.confirmRecipient() },
                        enabled = (addressText.isNotBlank() && selectedCoords != null),
                        modifier = Modifier.weight(0.65f),
                        containerColor = Color(0xFFD32F2F),
                        height = 56.dp,
                        isLoading = isLoading
                    )
                }
            }
        }
    }
}

@Composable
fun RecipientSelectionStep(viewModel: BookingViewModel) {
    var recipientName by remember { mutableStateOf("") }
    var recipientPhone by remember { mutableStateOf("") }
    val isLoading by viewModel.isLoading.collectAsState()

    Column(modifier = Modifier.fillMaxSize().padding(24.dp)) {
        Text("Who is this service for?", fontWeight = FontWeight.Black, fontSize = 22.sp)
        Spacer(modifier = Modifier.height(32.dp))
        
        OutlinedTextField(
            value = recipientName,
            onValueChange = { recipientName = it },
            label = { Text("Recipient Name") },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp)
        )
        Spacer(modifier = Modifier.height(16.dp))
        OutlinedTextField(
            value = recipientPhone,
            onValueChange = { recipientPhone = it },
            label = { Text("Recipient Phone (Optional)") },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp)
        )
        
        Spacer(modifier = Modifier.weight(1f))
        
        PieceJobButton(
            text = "CONTINUE",
            onClick = { 
                viewModel.recipientName.value = recipientName
                viewModel.recipientPhone.value = recipientPhone
                viewModel.confirmRecipient()
            },
            enabled = recipientName.isNotBlank(),
            height = 56.dp,
            isLoading = isLoading
        )
    }
}

@Composable
fun CategorySelectionStep(viewModel: BookingViewModel) {
    val categories by viewModel.categories.collectAsState()

    LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        item { Text("What do you need help with?", fontWeight = FontWeight.Black, fontSize = 22.sp, modifier = Modifier.padding(bottom = 16.dp)) }
        items(categories) { category ->
            Card(
                onClick = { viewModel.selectCategory(category.code) },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFEEEEEE))
            ) {
                Row(modifier = Modifier.padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
                    Surface(modifier = Modifier.size(48.dp), shape = RoundedCornerShape(12.dp), color = Color(0xFFFDECEA)) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(category.code, fontWeight = FontWeight.Black, fontSize = 12.sp, color = Color(0xFFD32F2F))
                        }
                    }
                    Spacer(modifier = Modifier.width(20.dp))
                    Column {
                        Text(category.name, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        Text(category.description ?: "Select from available services", fontSize = 12.sp, color = Color.Gray)
                    }
                    Spacer(modifier = Modifier.weight(1f))
                    Icon(Icons.Default.ChevronRight, contentDescription = null, tint = Color.LightGray)
                }
            }
        }
    }
}

@Composable
fun ServiceSelectionStep(viewModel: BookingViewModel) {
    val services by viewModel.services.collectAsState()
    var selectedServiceForDetails by remember { mutableStateOf<ServiceDto?>(null) }

    if (selectedServiceForDetails != null) {
        ServiceDetailsDialog(
            service = selectedServiceForDetails!!,
            onConfirm = {
                val s = selectedServiceForDetails!!
                selectedServiceForDetails = null
                viewModel.selectService(s)
            },
            onDismiss = { selectedServiceForDetails = null }
        )
    }

    LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        item { Text("Select specific service", fontWeight = FontWeight.Black, fontSize = 22.sp, modifier = Modifier.padding(bottom = 16.dp)) }
        items(services) { service ->
            Card(
                onClick = { selectedServiceForDetails = service },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFEEEEEE))
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(service.name, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        Spacer(modifier = Modifier.weight(1f))
                        Surface(
                            color = if (service.onlineCountLabel == "0" || service.onlineCountLabel == null) Color.LightGray else Color(0xFFE8F5E9),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                "ONLINE: ${service.onlineCountLabel ?: "0"}", 
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Black,
                                color = if (service.onlineCountLabel == "0" || service.onlineCountLabel == null) Color.DarkGray else Color(0xFF2E7D32)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(service.description ?: "Professional service provider", fontSize = 13.sp, color = Color.Gray)
                }
            }
        }
    }
}

@Composable
fun BookingFeeStep(viewModel: BookingViewModel) {
    val estimate by viewModel.priceEstimate.collectAsState()
    val selectedService by viewModel.selectedService.collectAsState()
    val wallet by viewModel.wallet.collectAsState()
    val error by viewModel.error.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    var showPaymentWarning by remember { mutableStateOf(false) }
    var showReferralSuccess by remember { mutableStateOf(false) }

    val bookingFee = estimate?.bookingFee ?: 0.0
    val referralBalance = wallet?.balanceReferral ?: 0.0
    val canUseReferral = referralBalance >= bookingFee && bookingFee > 0

    if (showPaymentWarning) {
        AlertDialog(
            onDismissRequest = { showPaymentWarning = false },
            title = { Text("Booking Fee Payment", fontWeight = FontWeight.Black) },
            text = { Text("This booking fee confirms your booking. The final service price will be negotiated directly between you and the provider.") },
            confirmButton = {
                PieceJobButton(
                    text = "CONFIRM",
                    onClick = { 
                        android.util.Log.d("TowMechSecurity", "PAYMENT_CONFIRM_CLICKED: Review & Pay Confirm Button")
                        showPaymentWarning = false
                        viewModel.createJob()
                    },
                    isLoading = isLoading,
                    fullWidth = false,
                    height = 40.dp,
                    fontSize = 14.sp
                )
            },
            dismissButton = {
                TextButton(onClick = { showPaymentWarning = false }) { Text("CANCEL") }
            }
        )
    }

    Column(modifier = Modifier.fillMaxSize().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        if (error != null) {
            Surface(
                color = Color.Red,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.padding(bottom = 16.dp)
            ) {
                Text(error!!, color = Color.White, modifier = Modifier.padding(12.dp), fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        }

        Spacer(modifier = Modifier.height(32.dp))
        Surface(modifier = Modifier.size(80.dp), shape = CircleShape, color = Color(0xFFFDECEA)) {
            Box(contentAlignment = Alignment.Center) {
                Icon(Icons.Default.AccountBalanceWallet, contentDescription = null, tint = Color(0xFFD32F2F), modifier = Modifier.size(40.dp))
            }
        }
        Spacer(modifier = Modifier.height(24.dp))
        Text(selectedService?.name ?: "Service", fontSize = 18.sp, fontWeight = FontWeight.Black, color = Color.Black)
        Text("Booking Fee", fontSize = 14.sp, color = Color.Gray, fontWeight = FontWeight.Bold)
        Text(String.format(java.util.Locale.getDefault(), "%s %.2f", estimate?.currencySymbol ?: estimate?.currency ?: "", bookingFee), fontSize = 42.sp, fontWeight = FontWeight.Black)

        Spacer(modifier = Modifier.height(24.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFFF8F9FA))
        ) {
            val symbol = estimate?.currencySymbol ?: estimate?.currency ?: ""
            Column(modifier = Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text(
                    text = "This booking fee confirms your booking. The final service price will be negotiated directly between you and the provider.",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color.DarkGray,
                    lineHeight = 20.sp
                )
                HorizontalDivider(color = Color.LightGray.copy(alpha = 0.5f))
                DetailRow("Payable Now", String.format(java.util.Locale.getDefault(), "%s %.2f", symbol, bookingFee), highlight = true)
                
                if (referralBalance > 0) {
                    DetailRow("Referral Balance", String.format(java.util.Locale.getDefault(), "%s %.2f", symbol, referralBalance))
                }
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        if (canUseReferral) {
            PieceJobButton(
                text = "PAY WITH REFERRAL BALANCE",
                onClick = { 
                    android.util.Log.d("REFERRAL_AUDIT", "USER_SELECTED_REFERRAL_PAYMENT")
                    viewModel.createJob(useReferral = true)
                },
                modifier = Modifier.fillMaxWidth(),
                containerColor = Color(0xFF2E7D32),
                icon = Icons.Default.CardGiftcard,
                height = 64.dp,
                isLoading = isLoading
            )
            Spacer(modifier = Modifier.height(12.dp))
        }

        PieceJobButton(
            text = if (canUseReferral) "USE OTHER PAYMENT METHOD" else "PAY BOOKING FEE",
            onClick = { showPaymentWarning = true },
            modifier = Modifier.fillMaxWidth(),
            containerColor = if (canUseReferral) Color(0xFF121212) else Color(0xFFD32F2F),
            height = 64.dp,
            isLoading = isLoading
        )
    }
}

@Composable
fun DetailRow(label: String, value: String, highlight: Boolean = false) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, fontSize = 14.sp, color = if (highlight) Color.Black else Color.Gray, fontWeight = if (highlight) FontWeight.Black else FontWeight.Medium)
        Text(value, fontSize = 14.sp, fontWeight = if (highlight) FontWeight.Black else FontWeight.Bold)
    }
}

@Composable
fun PaymentGatewayStep(viewModel: BookingViewModel) {
    val methods by viewModel.availablePaymentMethods.collectAsState()
    val selectedMethod by viewModel.selectedPaymentMethod.collectAsState()

    Column(modifier = Modifier.fillMaxSize().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Text("Select Payment Method", fontWeight = FontWeight.Black, fontSize = 20.sp)
        Spacer(modifier = Modifier.height(32.dp))

        methods.forEach { method ->
            Card(
                onClick = { viewModel.selectedPaymentMethod.value = method },
                modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (selectedMethod?.code == method.code) Color(0xFFFDECEA) else Color.White
                ),
                border = androidx.compose.foundation.BorderStroke(
                    width = if (selectedMethod?.code == method.code) 2.dp else 1.dp,
                    color = if (selectedMethod?.code == method.code) Color(0xFFD32F2F) else Color(0xFFEEEEEE)
                )
            ) {
                Row(modifier = Modifier.padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.size(40.dp).background(Color(0xFFF5F5F5), RoundedCornerShape(8.dp)), contentAlignment = Alignment.Center) {
                        Text(method.name.take(1), fontWeight = FontWeight.Black)
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Text(method.name, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.weight(1f))
                    if (selectedMethod?.code == method.code) {
                        Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFFD32F2F))
                    }
                }
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        Button(
            onClick = { viewModel.payBookingFee() },
            modifier = Modifier.fillMaxWidth().height(64.dp),
            shape = RoundedCornerShape(20.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF121212)),
            enabled = selectedMethod != null
        ) {
            Text("PROCEED TO SECURE PAYMENT", fontWeight = FontWeight.Black)
        }
    }
}

@Composable
fun PaymentWebViewStep(viewModel: BookingViewModel) {
    val url by viewModel.paymentUrl.collectAsState()
    val isVerifying by viewModel.isLoading.collectAsState()

    if (url == null || isVerifying) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                CircularProgressIndicator(color = Color(0xFFD32F2F))
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = if (isVerifying) "Verifying payment..." else "Initializing secure payment...",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
            }
        }
        return
    }

    AndroidView(
        factory = { context ->
            WebView(context).apply {
                settings.javaScriptEnabled = true
                settings.domStorageEnabled = true
                webViewClient = object : WebViewClient() {
                    override fun onPageStarted(view: WebView?, url: String?, favicon: android.graphics.Bitmap?) {
                        super.onPageStarted(view, url, favicon)
                        android.util.Log.d("TowMechSecurity", "WEBVIEW_LOADING_URL: $url")
                        
                        // Broad check for success indicators in URL to trigger silent verification
                        if (url != null && (
                            url.startsWith("piecejob://payment-callback") || 
                            url.contains("checkout.paystack.com/success") ||
                            url.contains("/payments/verify/")
                        )) {
                            android.util.Log.d("TowMechSecurity", "SILENT_SUCCESS_DETECTED: Intercepting $url")
                            viewModel.verifyPayment()
                        }
                    }

                    override fun shouldOverrideUrlLoading(view: WebView?, url: String?): Boolean {
                        android.util.Log.d("PAYMENT_WEBVIEW", "shouldOverrideUrlLoading: $url")
                        if (url != null && url.startsWith("piecejob://payment-callback")) {
                            android.util.Log.d("TowMechSecurity", "PAYMENT_CALLBACK_DETECTED: Starting verification")
                            viewModel.verifyPayment()
                            return true
                        }
                        return false
                    }
                }
                loadUrl(url!!)
            }
        },
        modifier = Modifier.fillMaxSize()
    )
}

fun getStepTitle(step: BookingStep): String {
    return when (step) {
        BookingStep.ADDRESS_SELECTION -> "Select Location"
        BookingStep.RECIPIENT_SELECTION -> "Service Recipient"
        BookingStep.CATEGORY_SELECTION -> "Select Category"
        BookingStep.SERVICE_SELECTION -> "Select Service"
        BookingStep.BOOKING_FEE -> "Review & Pay"
        BookingStep.PAYMENT_GATEWAY -> "Secure Payment"
        BookingStep.PAYMENT_WEBVIEW -> "Secure Payment"
        BookingStep.TRACKING -> "Tracking"
    }
}
