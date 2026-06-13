package com.piecejob.provider.ui.profile

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Work
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.piecejob.core.data.remote.dto.ExperienceDto

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProviderExperienceScreen(
    viewModel: ProviderExperienceViewModel = hiltViewModel(),
    onBack: () -> Unit
) {
    val experience by viewModel.experience.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    
    var showAddDialog by remember { mutableStateOf(false) }
    var company by remember { mutableStateOf("") }
    var role by remember { mutableStateOf("") }
    var startDate by remember { mutableStateOf("") }
    var endDate by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }

    if (showAddDialog) {
        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = { Text("Add Experience") },
            text = {
                Column {
                    OutlinedTextField(value = company, onValueChange = { company = it }, label = { Text("Company Name") })
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(value = role, onValueChange = { role = it }, label = { Text("Role") })
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(value = startDate, onValueChange = { startDate = it }, label = { Text("Start Date (YYYY-MM-DD)") })
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(value = endDate, onValueChange = { endDate = it }, label = { Text("End Date (or Present)") })
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(value = description, onValueChange = { description = it }, label = { Text("Description") })
                }
            },
            confirmButton = {
                Button(onClick = {
                    viewModel.addExperience(company, role, startDate, endDate.takeIf { it.isNotBlank() }, description.takeIf { it.isNotBlank() })
                    showAddDialog = false
                    company = ""
                    role = ""
                    startDate = ""
                    endDate = ""
                    description = ""
                }) { Text("Add") }
            },
            dismissButton = {
                TextButton(onClick = { showAddDialog = false }) { Text("Cancel") }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Work Experience", fontWeight = FontWeight.Bold) },
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
        if (isLoading && experience.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else if (experience.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("No work experience added.", color = Color.Gray)
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(experience) { exp ->
                    ExperienceRow(exp)
                }
            }
        }
    }
}

@Composable
fun ExperienceRow(exp: ExperienceDto) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.Top) {
            Surface(modifier = Modifier.size(40.dp), shape = RoundedCornerShape(10.dp), color = MaterialTheme.colorScheme.tertiaryContainer) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.Work, contentDescription = null, modifier = Modifier.size(20.dp))
                }
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(text = exp.role, fontWeight = FontWeight.Black, fontSize = 16.sp)
                Text(text = exp.companyName, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary, fontSize = 14.sp)
                Text(text = "${exp.startDate} - ${exp.endDate ?: "Present"}", fontSize = 12.sp, color = Color.Gray)
                if (exp.description != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(text = exp.description, fontSize = 13.sp, lineHeight = 18.sp)
                }
            }
        }
    }
}
