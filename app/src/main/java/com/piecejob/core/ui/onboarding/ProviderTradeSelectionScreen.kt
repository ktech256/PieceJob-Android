package com.piecejob.core.ui.onboarding

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.piecejob.customer.ui.dashboard.CustomerDashboardViewModel
import com.piecejob.core.data.remote.ServiceDto
import com.piecejob.core.ui.auth.AuthViewModel
import com.piecejob.core.ui.auth.AuthState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProviderTradeSelectionScreen(
    authViewModel: AuthViewModel,
    serviceViewModel: CustomerDashboardViewModel = hiltViewModel(),
    onSuccess: () -> Unit
) {
    val groupedServices by serviceViewModel.groupedServices.collectAsState()
    val isLoadingServices by serviceViewModel.isLoading.collectAsState()
    val authState by authViewModel.authState.collectAsState()
    val gender by authViewModel.gender.collectAsState()
    
    val selectedServices = remember { mutableStateListOf<String>() }

    LaunchedEffect(Unit) {
        serviceViewModel.loadServices(gender)
    }

    LaunchedEffect(authState) {
        if (authState is AuthState.Authenticated) {
            onSuccess()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Select Your Trades", fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 24.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Which services would you like to offer to customers?",
                    fontSize = 16.sp,
                    color = Color.Gray,
                    lineHeight = 24.sp,
                    modifier = Modifier.padding(top = 16.dp)
                )
                
                Spacer(modifier = Modifier.height(24.dp))

                if (isLoadingServices) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(24.dp),
                        contentPadding = PaddingValues(bottom = 24.dp)
                    ) {
                        items(groupedServices) { group ->
                            Column {
                                Text(
                                    text = group.label,
                                    fontWeight = FontWeight.Black,
                                    fontSize = 14.sp,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = group.requirements,
                                    fontSize = 10.sp,
                                    color = Color.Gray,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                
                                val rows = group.services.chunked(2)
                                rows.forEach { rowItems ->
                                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                        rowItems.forEach { service ->
                                            Box(modifier = Modifier.weight(1f)) {
                                                TradeCard(
                                                    service = service,
                                                    isSelected = selectedServices.contains(service.code),
                                                    onToggle = {
                                                        if (selectedServices.contains(service.code)) {
                                                            selectedServices.remove(service.code)
                                                        } else {
                                                            if (selectedServices.size < 3) {
                                                                selectedServices.add(service.code)
                                                            }
                                                        }
                                                    }
                                                )
                                            }
                                        }
                                        if (rowItems.size == 1) {
                                            Spacer(modifier = Modifier.weight(1f))
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(12.dp))
                                }
                            }
                        }
                    }
                }
            }

            Column {
                if (authState is AuthState.Error) {
                    Text(
                        text = (authState as AuthState.Error).message,
                        color = Color.Red,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                }

                // Dynamic Requirement Counter (Strictly Additive)
                val activeRequirements = remember(selectedServices, groupedServices) {
                    val levels = mutableSetOf("STANDARD")
                    val allS = groupedServices.flatMap { it.services }
                    selectedServices.forEach { code ->
                        val service = allS.find { it.code == code }
                        service?.let {
                            var level = it.verificationLevel
                            if (it.category == "CSS") level = "HIGH_VETTING"
                            if (listOf("HMS", "OPS", "TSS").contains(it.category)) {
                                if (level != "HIGH_VETTING") level = "TRADE"
                            }
                            
                            val levelOrder = listOf("STANDARD", "PROFESSIONAL", "TRADE", "HIGH_VETTING")
                            val currentIdx = levelOrder.indexOf(level)
                            for (i in 0..currentIdx) {
                                levels.add(levelOrder[i])
                            }
                        }
                    }
                    val docs = mutableSetOf("ID", "Selfie")
                    if (levels.contains("PROFESSIONAL")) { docs.add("Certification"); docs.add("Experience") }
                    if (levels.contains("TRADE")) { docs.add("Trade Licence"); docs.add("Tools") }
                    if (levels.contains("HIGH_VETTING")) { docs.add("Interview"); docs.add("References") }
                    docs.toList().sorted()
                }

                if (selectedServices.isNotEmpty()) {
                    Surface(
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text("REQUIREMENTS", fontWeight = FontWeight.Black, fontSize = 9.sp, letterSpacing = 1.sp)
                            Text(
                                text = activeRequirements.joinToString(", "),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }

                Text(
                    text = "${selectedServices.size}/3 Selected",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (selectedServices.size == 3) MaterialTheme.colorScheme.primary else Color.Gray,
                    modifier = Modifier.padding(bottom = 16.dp, start = 4.dp)
                )
                
                Button(
                    onClick = { 
                        authViewModel.selectedServices.value = selectedServices.toList()
                        authViewModel.register() 
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(58.dp),
                    shape = RoundedCornerShape(16.dp),
                    enabled = selectedServices.isNotEmpty() && authState !is AuthState.Loading
                ) {
                    if (authState is AuthState.Loading) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp), color = Color.White)
                    } else {
                        Text("COMPLETE REGISTRATION", fontSize = 15.sp, fontWeight = FontWeight.Black)
                    }
                }
            }
        }
    }
}

@Composable
fun TradeCard(
    service: ServiceDto,
    isSelected: Boolean,
    onToggle: () -> Unit
) {
    OutlinedCard(
        onClick = onToggle,
        modifier = Modifier
            .fillMaxWidth()
            .height(100.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.05f) else Color.White
        ),
        border = if (isSelected) androidx.compose.foundation.BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else androidx.compose.foundation.BorderStroke(1.dp, Color.LightGray)
    ) {
        Box(modifier = Modifier.fillMaxSize().padding(12.dp)) {
            Column(modifier = Modifier.align(Alignment.CenterStart)) {
                Text(
                    text = service.name,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 13.sp,
                    lineHeight = 18.sp
                )
                Text(
                    text = service.category,
                    fontSize = 10.sp,
                    color = Color.Gray,
                    fontWeight = FontWeight.Bold
                )
            }
            
            if (isSelected) {
                Icon(
                    Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.align(Alignment.TopEnd).size(18.dp)
                )
            }
        }
    }
}
