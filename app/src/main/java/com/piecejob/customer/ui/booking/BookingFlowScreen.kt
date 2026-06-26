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
import com.piecejob.customer.ui.dashboard.ServiceDetailsDialog

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BookingFlowScreen(
    viewModel: BookingViewModel = hiltViewModel(),
    onTrackingStart: (String) -> Unit,
    onBack: () -> Unit
) {
    val currentStep by viewModel.currentStep.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()

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
        Box(modifier = Modifier.padding(padding).fillMaxSize()) {
            when (currentStep) {
                BookingStep.ADDRESS_SELECTION -> AddressSelectionStep(viewModel)
                BookingStep.RECIPIENT_SELECTION -> RecipientSelectionStep(viewModel)
                BookingStep.CATEGORY_SELECTION -> CategorySelectionStep(viewModel)
                BookingStep.SERVICE_SELECTION -> ServiceSelectionStep(viewModel)
                BookingStep.BOOKING_FEE -> BookingFeeStep(viewModel)
                BookingStep.PAYMENT_GATEWAY -> { /* Skipped - handled automatically by backend routing */ }
                BookingStep.PAYMENT_WEBVIEW -> PaymentWebViewStep(viewModel)
                BookingStep.MATCHING -> MatchingStep(viewModel, onTrackingStart)
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
fun AddressSelectionStep(viewModel: BookingViewModel) {
    val nearbyProviders by viewModel.nearbyProviders.collectAsState()
    val error by viewModel.error.collectAsState()
    
    val johannesburg = LatLng(-26.2041, 28.0473)
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(johannesburg, 12f)
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // MAP (75%)
        Box(modifier = Modifier.weight(0.75f)) {
            val selectedCoords by viewModel.selectedCoordinates.collectAsState()
            var isMapLoaded by remember { mutableStateOf(false) }
            
            LaunchedEffect(Unit) {
                android.util.Log.d("MAP_INIT", "Starting GoogleMap initialization. API Key present: ${com.piecejob.BuildConfig.GOOGLE_MAPS_API_KEY.isNotBlank()}")
                // Fallback: If map doesn't load in 10 seconds, stop the spinner so we can see what's wrong
                kotlinx.coroutines.delay(10000)
                if (!isMapLoaded) {
                    android.util.Log.e("MAP_TIMEOUT", "Map load timed out after 10s. Forcing loaded state for diagnostics.")
                    isMapLoaded = true
                }
            }

            GoogleMap(
                modifier = Modifier.fillMaxSize(),
                cameraPositionState = cameraPositionState,
                onMapLoaded = { 
                    isMapLoaded = true 
                    android.util.Log.d("MAPS_DEBUG", "Map SDK initialized")
                    android.util.Log.d("MAP_LOADED", "onMapLoaded callback triggered.")
                },
                onMapClick = { latLng ->
                    android.util.Log.d("MAP_CLICK", "User clicked coordinates: ${latLng.latitude}, ${latLng.longitude}")
                    val formatted = "Selected: ${String.format(java.util.Locale.US, "%.4f, %.4f", latLng.latitude, latLng.longitude)}"
                    viewModel.setAddress(formatted, listOf(latLng.longitude, latLng.latitude))
                },
                onPOIClick = { poi ->
                    android.util.Log.d("MAP_POI", "User clicked POI: ${poi.name}")
                    viewModel.setAddress(poi.name, listOf(poi.latLng.longitude, poi.latLng.latitude))
                },
                uiSettings = MapUiSettings(
                    zoomControlsEnabled = false,
                    myLocationButtonEnabled = true
                ),
                properties = MapProperties(
                    isMyLocationEnabled = true
                )
            ) {
                // Nearby Providers
                nearbyProviders.forEach { provider ->
                    provider.location.coordinates.let { coords ->
                        Marker(
                            state = MarkerState(position = LatLng(coords[1], coords[0])),
                            title = "${provider.firstName} ${provider.lastName}",
                            icon = com.google.android.gms.maps.model.BitmapDescriptorFactory.defaultMarker(com.google.android.gms.maps.model.BitmapDescriptorFactory.HUE_GREEN)
                        )
                    }
                }

                // Selected Location Pin
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
            modifier = Modifier.weight(if (viewModel.addressPredictions.collectAsState().value.isNotEmpty()) 0.5f else 0.25f).fillMaxWidth(),
            shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 16.dp)
        ) {
            Column(modifier = Modifier.padding(24.dp)) {
                val focusManager = LocalFocusManager.current
                val addressText by viewModel.selectedAddress.collectAsState()
                val predictions by viewModel.addressPredictions.collectAsState()
                
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

                if (predictions.isNotEmpty()) {
                    LazyColumn(
                        modifier = Modifier.fillMaxWidth().heightIn(max = 200.dp).padding(top = 8.dp)
                    ) {
                        items(predictions) { prediction ->
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
                
                Button(
                    onClick = { 
                        viewModel.confirmRecipient() // Proceeding
                    },
                    enabled = (viewModel.selectedAddress.collectAsState().value.isNotBlank() && viewModel.selectedCoordinates.collectAsState().value != null),
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD32F2F))
                ) {
                    Text("CONFIRM LOCATION", fontWeight = FontWeight.Black)
                }
            }
        }
    }
}

@Composable
fun RecipientSelectionStep(viewModel: BookingViewModel) {
    var recipientName by remember { mutableStateOf("") }
    var recipientPhone by remember { mutableStateOf("") }

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
        
        Button(
            onClick = { 
                viewModel.recipientName.value = recipientName
                viewModel.recipientPhone.value = recipientPhone
                viewModel.confirmRecipient()
            },
            enabled = recipientName.isNotBlank(),
            modifier = Modifier.fillMaxWidth().height(56.dp),
            shape = RoundedCornerShape(16.dp)
        ) {
            Text("CONTINUE", fontWeight = FontWeight.Black)
        }
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
    val error by viewModel.error.collectAsState()
    var showPaymentWarning by remember { mutableStateOf(false) }

    if (showPaymentWarning) {
        AlertDialog(
            onDismissRequest = { showPaymentWarning = false },
            title = { Text("Booking Fee Payment", fontWeight = FontWeight.Black) },
            text = { Text("The booking fee you pay today will be credited toward the final amount agreed between you and the service provider. It is not an additional charge.") },
            confirmButton = {
                Button(onClick = { 
                    android.util.Log.d("TowMechSecurity", "PAYMENT_CONFIRM_CLICKED: Review & Pay Confirm Button")
                    showPaymentWarning = false
                    viewModel.createJob()
                }) { Text("CONFIRM") }
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

        Spacer(modifier = Modifier.height(48.dp))
        Surface(modifier = Modifier.size(80.dp), shape = CircleShape, color = Color(0xFFFDECEA)) {
            Box(contentAlignment = Alignment.Center) {
                Icon(Icons.Default.AccountBalanceWallet, contentDescription = null, tint = Color(0xFFD32F2F), modifier = Modifier.size(40.dp))
            }
        }
        Spacer(modifier = Modifier.height(24.dp))
        Text("Booking Fee", fontSize = 14.sp, color = Color.Gray, fontWeight = FontWeight.Bold)
        Text("${estimate?.currency ?: "$"} ${estimate?.bookingFee ?: "0.00"}", fontSize = 42.sp, fontWeight = FontWeight.Black)
        
        Spacer(modifier = Modifier.height(48.dp))
        
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFFF8F9FA))
        ) {
            Column(modifier = Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                DetailRow("Base Service Price", "${estimate?.currency} ${estimate?.basePrice}")
                DetailRow("Estimated Tax", "${estimate?.currency} ${estimate?.taxAmount}")
                HorizontalDivider(color = Color.LightGray.copy(alpha = 0.5f))
                DetailRow("Payable Now", "${estimate?.currency} ${estimate?.bookingFee}", highlight = true)
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        Button(
            onClick = { showPaymentWarning = true },
            modifier = Modifier.fillMaxWidth().height(64.dp),
            shape = RoundedCornerShape(20.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD32F2F))
        ) {
            Text("PAY BOOKING FEE", fontWeight = FontWeight.Black, fontSize = 16.sp)
        }
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

    if (url == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Initializing payment...")
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
                    }
                    override fun shouldOverrideUrlLoading(view: WebView?, url: String?): Boolean {
                        android.util.Log.d("PAYMENT_WEBVIEW", "Loading URL: $url")
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

@Composable
fun MatchingStep(viewModel: BookingViewModel, onTrackingStart: (String) -> Unit) {
    val job by viewModel.createdJob.collectAsState()
    
    LaunchedEffect(Unit) {
        // Here we would start polling or waiting for socket event
        // Simulate acceptance after 5 seconds
        kotlinx.coroutines.delay(5000)
        job?.id?.let { onTrackingStart(it) }
    }

    Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
        Surface(modifier = Modifier.size(120.dp), shape = CircleShape, color = Color(0xFFFDECEA)) {
             Box(contentAlignment = Alignment.Center) {
                 CircularProgressIndicator(modifier = Modifier.size(100.dp), strokeWidth = 8.dp, color = Color(0xFFD32F2F))
                 Icon(Icons.Default.Search, contentDescription = null, tint = Color(0xFFD32F2F), modifier = Modifier.size(40.dp))
             }
        }
        Spacer(modifier = Modifier.height(32.dp))
        Text("Connecting you to a provider...", fontWeight = FontWeight.Black, fontSize = 18.sp)
        Text("Finding the best professional nearby", color = Color.Gray, fontSize = 14.sp)
    }
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
        BookingStep.MATCHING -> "Matching"
        BookingStep.TRACKING -> "Tracking"
    }
}
