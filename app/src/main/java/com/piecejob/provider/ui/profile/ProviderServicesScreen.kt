package com.piecejob.provider.ui.profile

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Warning
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
import com.piecejob.core.ui.onboarding.TradeCard
import com.piecejob.customer.ui.dashboard.CustomerDashboardViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProviderServicesScreen(
    viewModel: ProviderServicesViewModel = hiltViewModel(),
    serviceViewModel: CustomerDashboardViewModel = hiltViewModel(),
    onBack: () -> Unit
) {
    val tempCodes by viewModel.tempServiceCodes.collectAsState()
    val groupedServices by serviceViewModel.groupedServices.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val isLoadingServices by serviceViewModel.isLoading.collectAsState()
    val saveSuccess by viewModel.saveSuccess.collectAsState()
    val requirements by viewModel.pendingRequirements.collectAsState()
    val error by viewModel.error.collectAsState()
    val canSave by viewModel.canSave.collectAsState()

    var showUnsavedDialog by remember { mutableStateOf(false) }
    var showRequirementsDialog by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        serviceViewModel.loadServices()
    }

    LaunchedEffect(requirements) {
        if (requirements.isNotEmpty()) {
            showRequirementsDialog = true
        }
    }

    val handleBack = {
        if (viewModel.hasUnsavedChanges()) {
            showUnsavedDialog = true
        } else {
            onBack()
        }
    }

    BackHandler(onBack = handleBack)

    if (showUnsavedDialog) {
        AlertDialog(
            onDismissRequest = { showUnsavedDialog = false },
            title = { Text("Unsaved Changes", fontWeight = FontWeight.Bold) },
            text = { Text("You have unsaved changes. Would you like to save them before leaving?") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.saveChanges()
                        showUnsavedDialog = false
                        onBack()
                    },
                    shape = RoundedCornerShape(12.dp)
                ) { Text("SAVE") }
            },
            dismissButton = {
                Row {
                    TextButton(onClick = {
                        viewModel.discardChanges()
                        showUnsavedDialog = false
                        onBack()
                    }) { Text("DISCARD", color = MaterialTheme.colorScheme.error) }
                    
                    TextButton(onClick = { showUnsavedDialog = false }) {
                        Text("CANCEL")
                    }
                }
            }
        )
    }

    if (showRequirementsDialog) {
        AlertDialog(
            onDismissRequest = { 
                showRequirementsDialog = false
                viewModel.resetSaveState()
            },
            icon = { Icon(Icons.Default.Warning, contentDescription = null, tint = Color(0xFFE65100)) },
            title = { Text("Verification Required", fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    Text("Some selected services require higher verification levels before they can be activated:")
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    val allDocs = requirements.values.flatMap { it.docs }.distinct()
                    allDocs.forEach { doc ->
                        Text("• ${doc.replace("_", " ")}", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    Text("Please upload these documents in the Verification module to activate these services.")
                }
            },
            confirmButton = {
                Button(
                    onClick = { 
                        showRequirementsDialog = false
                        viewModel.resetSaveState()
                    },
                    shape = RoundedCornerShape(12.dp)
                ) { Text("UNDERSTOOD") }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("My Services", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = handleBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
            )
        },
        bottomBar = {
            val activeRequirements = remember(tempCodes, groupedServices) {
                val levels = mutableSetOf("STANDARD")
                val allS = groupedServices.flatMap { it.services }
                tempCodes.forEach { code ->
                    val service = allS.find { it.code == code }
                    service?.let {
                        val level = it.verificationLevel
                        
                        // Fill additive levels for requirement summary
                        val levelOrder = listOf("STANDARD", "PROFESSIONAL", "TRADE", "HIGH_VETTING")
                        val currentIdx = levelOrder.indexOf(level)
                        if (currentIdx != -1) {
                            for (i in 0..currentIdx) {
                                levels.add(levelOrder[i])
                            }
                        }
                    }
                }
                val docs = mutableSetOf("ID", "Selfie")
                if (levels.contains("PROFESSIONAL")) { docs.add("Certification"); docs.add("Experience") }
                if (levels.contains("TRADE")) { docs.add("Trade Licence"); docs.add("Tools") }
                if (levels.contains("HIGH_VETTING")) { docs.add("Interview"); docs.add("References") }
                docs.toList().sorted()
            }

            Surface(
                modifier = Modifier.fillMaxWidth(),
                shadowElevation = 8.dp,
                color = Color.White
            ) {
                Column(modifier = Modifier.padding(horizontal = 24.dp).padding(top = 16.dp, bottom = 24.dp)) {
                    if (tempCodes.isNotEmpty()) {
                         Text("ACTIVE REQUIREMENTS", fontWeight = FontWeight.Black, fontSize = 9.sp, letterSpacing = 1.sp, color = Color.Gray)
                         Text(
                             text = activeRequirements.joinToString(", "),
                             fontSize = 11.sp,
                             fontWeight = FontWeight.Bold,
                             color = MaterialTheme.colorScheme.primary,
                             modifier = Modifier.padding(bottom = 12.dp)
                         )
                    }

                    if (error != null) {
                        Text(
                            text = error!!,
                            color = Color.Red,
                            fontSize = 12.sp,
                            modifier = Modifier.padding(bottom = 12.dp)
                        )
                    }

                    Button(
                        onClick = { viewModel.saveChanges() },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        shape = RoundedCornerShape(16.dp),
                        enabled = canSave
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(modifier = Modifier.size(24.dp), color = Color.White)
                        } else {
                            Text("SAVE CHANGES", fontWeight = FontWeight.Black)
                        }
                    }
                }
            }
        }
    )
{ padding ->
        if (isLoadingServices) {
            Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = 24.dp),
                verticalArrangement = Arrangement.spacedBy(24.dp),
                contentPadding = PaddingValues(bottom = 100.dp, top = 16.dp)
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
                                            isSelected = tempCodes.contains(service.code),
                                            onToggle = { viewModel.toggleService(service.code) }
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
}
