package com.piecejob.core.ui.onboarding

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
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
    val services by serviceViewModel.services.collectAsState()
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
                .padding(24.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Which services would you like to offer to customers?",
                    fontSize = 16.sp,
                    color = Color.Gray,
                    lineHeight = 24.sp
                )
                
                Spacer(modifier = Modifier.height(32.dp))

                if (isLoadingServices) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                } else {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(2),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(services) { service ->
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
