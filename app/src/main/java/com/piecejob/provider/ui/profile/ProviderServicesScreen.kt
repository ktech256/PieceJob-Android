package com.piecejob.provider.ui.profile

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
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
    val error by viewModel.error.collectAsState()
    val canSave by viewModel.canSave.collectAsState()

    val snackbarHostState = remember { SnackbarHostState() }
    var showUnsavedDialog by remember { mutableStateOf(false) }
    var showRequirementsDialog by remember { mutableStateOf(false) }

    LaunchedEffect(saveSuccess, error) {
        if (saveSuccess == false && error != null) {
            snackbarHostState.showSnackbar(error!!)
            viewModel.resetSaveState()
        }
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
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shadowElevation = 8.dp,
                color = Color.White
            ) {
                Button(
                    onClick = { viewModel.saveChanges() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp)
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
    ) { padding ->
        if (isLoading && allServices.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            val categories = listOf("HDS", "CSS", "HMS", "OPS", "LLS", "TSS")
            val groupedServices = allServices.groupBy { it.category }

            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                categories.forEach { cat ->
                    val services = groupedServices[cat] ?: emptyList()
                    if (services.isNotEmpty()) {
                        item(span = { GridItemSpan(2) }) {
                            Text(
                                text = cat,
                                fontWeight = FontWeight.Black,
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(top = 16.dp, bottom = 4.dp)
                            )
                        }
                        
                        items(services) { service ->
                            TradeCard(
                                service = service,
                                isSelected = tempCodes.contains(service.code),
                                onToggle = { viewModel.toggleService(service.code) }
                            )
                        }
                    }
                }
                
                item(span = { GridItemSpan(2) }) {
                    Spacer(modifier = Modifier.height(100.dp))
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
