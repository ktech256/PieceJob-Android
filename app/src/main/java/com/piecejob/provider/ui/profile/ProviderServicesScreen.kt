package com.piecejob.provider.ui.profile

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
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
fun ProviderServicesScreen(
    viewModel: ProviderServicesViewModel = hiltViewModel(),
    onBack: () -> Unit
) {
    val tempCodes by viewModel.tempServiceCodes.collectAsState()
    val allServices by viewModel.allServices.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val saveSuccess by viewModel.saveSuccess.collectAsState()
    val requirements by viewModel.pendingRequirements.collectAsState()

    val snackbarHostState = remember { SnackbarHostState() }
    var showUnsavedDialog by remember { mutableStateOf(false) }

    LaunchedEffect(saveSuccess) {
        if (saveSuccess == true && requirements.isNotEmpty()) {
            snackbarHostState.showSnackbar("Some services require additional documents for activation.")
        }
    }

    val handleBack = {
        if (viewModel.hasUnsavedChanges()) {
            showUnsavedDialog = true
        } else {
            onBack()
        }
    }

    if (showUnsavedDialog) {
        AlertDialog(
            onDismissRequest = { showUnsavedDialog = false },
            title = { Text("Unsaved Changes") },
            text = { Text("You have unsaved service changes. Would you like to save before leaving?") },
            confirmButton = {
                Button(onClick = {
                    viewModel.saveChanges()
                    showUnsavedDialog = false
                    onBack()
                }) { Text("Save") }
            },
            dismissButton = {
                Row {
                    TextButton(onClick = {
                        viewModel.discardChanges()
                        showUnsavedDialog = false
                        onBack()
                    }) { Text("Discard") }
                    TextButton(onClick = {
                        showUnsavedDialog = false
                    }) { Text("Cancel") }
                }
            }
        )
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("My Services", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = handleBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (viewModel.hasUnsavedChanges()) {
                        TextButton(onClick = { viewModel.saveChanges() }) {
                            Text("SAVE", fontWeight = FontWeight.Black)
                        }
                    }
                }
            )
        }
    ) { padding ->
        if (isLoading && allServices.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            val categories = listOf("HDS", "CSS", "HMS", "OPS", "LLS", "TSS")
            val groupedServices = allServices.groupBy { it.category }

            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                categories.forEach { cat ->
                    val services = groupedServices[cat] ?: emptyList()
                    if (services.isNotEmpty()) {
                        item {
                            Text(
                                text = cat,
                                fontWeight = FontWeight.Black,
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(top = 16.dp, bottom = 8.dp)
                            )
                        }
                        items(services) { service ->
                            val isActive = tempCodes.contains(service.code)
                            ServiceItemRow(service, isActive) {
                                viewModel.toggleService(service.code)
                            }
                        }
                    }
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
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(text = service.name, fontWeight = FontWeight.Bold)
                    if (isActive && service.verificationLevel != "STANDARD") {
                         Spacer(modifier = Modifier.width(8.dp))
                         Surface(color = Color(0xFFFFF3E0), shape = RoundedCornerShape(4.dp)) {
                             Text("Vetting Required", modifier = Modifier.padding(horizontal = 4.dp), fontSize = 8.sp, color = Color(0xFFE65100), fontWeight = FontWeight.Bold)
                         }
                    }
                }
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
