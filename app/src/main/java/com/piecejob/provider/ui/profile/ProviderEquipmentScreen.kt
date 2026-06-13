package com.piecejob.provider.ui.profile

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Build
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.piecejob.core.data.remote.dto.EquipmentDto

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProviderEquipmentScreen(
    viewModel: ProviderEquipmentViewModel = hiltViewModel(),
    onBack: () -> Unit
) {
    val equipment by viewModel.equipment.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    
    var showAddDialog by remember { mutableStateOf(false) }
    var toolName by remember { mutableStateOf("") }
    var toolCategory by remember { mutableStateOf("") }

    if (showAddDialog) {
        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = { Text("Register New Tool") },
            text = {
                Column {
                    OutlinedTextField(value = toolName, onValueChange = { toolName = it }, label = { Text("Tool Name") })
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(value = toolCategory, onValueChange = { toolCategory = it }, label = { Text("Category (e.g. Gardening)") })
                }
            },
            confirmButton = {
                Button(onClick = {
                    viewModel.addTool(toolName, toolCategory)
                    showAddDialog = false
                    toolName = ""
                    toolCategory = ""
                }) { Text("Register") }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Equipment & Tools", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showAddDialog = true }) {
                Icon(Icons.Default.Add, contentDescription = "Add")
            }
        }
    ) { padding ->
        if (isLoading && equipment.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else if (equipment.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("No tools registered.", color = Color.Gray)
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(equipment) { tool ->
                    ToolRow(tool)
                }
            }
        }
    }
}

@Composable
fun ToolRow(tool: EquipmentDto) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Surface(modifier = Modifier.size(48.dp), shape = RoundedCornerShape(12.dp), color = MaterialTheme.colorScheme.primaryContainer) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.Build, contentDescription = null)
                }
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(text = tool.name, fontWeight = FontWeight.Bold)
                Text(text = tool.category, fontSize = 12.sp, color = Color.Gray)
            }
            Spacer(modifier = Modifier.weight(1f))
            if (tool.isVerified) {
                Text("VERIFIED", color = Color(0xFF2E7D32), fontWeight = FontWeight.Black, fontSize = 10.sp)
            } else {
                Text("PENDING", color = Color.Gray, fontWeight = FontWeight.Black, fontSize = 10.sp)
            }
        }
    }
}
