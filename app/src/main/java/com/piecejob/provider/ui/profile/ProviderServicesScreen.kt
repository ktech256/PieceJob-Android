package com.piecejob.provider.ui.profile

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.piecejob.core.data.remote.ServiceDto

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProviderServicesScreen(
    viewModel: ProviderServicesViewModel = hiltViewModel(),
    onBack: () -> Unit
) {
    val myServices by viewModel.myServices.collectAsState()
    val allServices by viewModel.allServices.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("My Services", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        if (isLoading && myServices.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    Text("ACTIVE SERVICES", fontWeight = FontWeight.Black, fontSize = 12.sp, color = MaterialTheme.colorScheme.primary)
                }

                if (myServices.isEmpty()) {
                    item {
                        Text("No services active. Add some below.", color = androidx.compose.ui.graphics.Color.Gray, fontSize = 14.sp)
                    }
                } else {
                    items(myServices) { service ->
                        ServiceItemRow(service, true) { viewModel.toggleService(service.code) }
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(24.dp))
                    Text("AVAILABLE TO ADD", fontWeight = FontWeight.Black, fontSize = 12.sp, color = MaterialTheme.colorScheme.primary)
                }

                val availableToAdd = allServices.filter { all -> myServices.none { my -> my.code == all.code } }

                items(availableToAdd) { service ->
                    ServiceItemRow(service, false) { viewModel.toggleService(service.code) }
                }
            }
        }
    }
}

@Composable
fun ServiceItemRow(service: ServiceDto, isActive: Boolean, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = if (isActive) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(text = service.name, fontWeight = FontWeight.Bold)
                Text(text = service.category, fontSize = 12.sp, color = androidx.compose.ui.graphics.Color.Gray)
            }
            IconButton(onClick = onClick) {
                Icon(
                    imageVector = if (isActive) Icons.Default.Remove else Icons.Default.Add,
                    contentDescription = null,
                    tint = if (isActive) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}
