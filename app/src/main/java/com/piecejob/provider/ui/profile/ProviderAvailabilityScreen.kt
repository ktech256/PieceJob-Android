package com.piecejob.provider.ui.profile

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.BeachAccess
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.piecejob.core.data.remote.dto.TimeSlotDto
import com.piecejob.core.data.remote.dto.WorkingDayDto

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProviderAvailabilityScreen(
    viewModel: ProviderAvailabilityViewModel = hiltViewModel(),
    onBack: () -> Unit
) {
    val availability by viewModel.availability.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val isSuccess by viewModel.isSuccess.collectAsState()

    var vacationMode by remember { mutableStateOf(false) }
    val workingHours = remember { mutableStateListOf<WorkingDayDto>() }

    LaunchedEffect(availability) {
        availability?.let {
            vacationMode = it.vacationMode
            workingHours.clear()
            workingHours.addAll(it.workingHours)
        }
    }

    if (isSuccess) {
        LaunchedEffect(Unit) {
            viewModel.resetSuccess()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Availability", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        bottomBar = {
            Surface(modifier = Modifier.fillMaxWidth(), shadowElevation = 8.dp, color = Color.White) {
                Button(
                    onClick = { viewModel.updateAvailability(vacationMode, workingHours.toList()) },
                    modifier = Modifier.fillMaxWidth().padding(24.dp).height(56.dp),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    if (isLoading) CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                    else Text("SAVE SETTINGS", fontWeight = FontWeight.Black)
                }
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 24.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
            contentPadding = PaddingValues(top = 16.dp, bottom = 100.dp)
        ) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = if(vacationMode) Color(0xFFFFF3E0) else Color.White),
                    border = if(vacationMode) androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE65100)) else null
                ) {
                    Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.BeachAccess, null, tint = if(vacationMode) Color(0xFFE65100) else Color.Gray)
                        Spacer(modifier = Modifier.width(16.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Vacation Mode", fontWeight = FontWeight.Bold)
                            Text("Temporarily stop receiving new job broadcasts", fontSize = 11.sp, color = Color.Gray)
                        }
                        Switch(checked = vacationMode, onCheckedChange = { vacationMode = it })
                    }
                }
            }

            item {
                Text("Working Hours", fontWeight = FontWeight.Black, fontSize = 14.sp, color = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.height(8.dp))
            }

            items(workingHours) { day ->
                WorkingDayRow(day) { updated ->
                    val index = workingHours.indexOfFirst { it.day == updated.day }
                    if (index != -1) workingHours[index] = updated
                }
            }
        }
    }
}

@Composable
fun WorkingDayRow(day: WorkingDayDto, onUpdate: (WorkingDayDto) -> Unit) {
    val dayName = when(day.day) {
        0 -> "Sunday"
        1 -> "Monday"
        2 -> "Tuesday"
        3 -> "Wednesday"
        4 -> "Thursday"
        5 -> "Friday"
        6 -> "Saturday"
        else -> ""
    }

    Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = Color.White)) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text(dayName, fontWeight = FontWeight.Bold)
                Switch(checked = day.enabled, onCheckedChange = { onUpdate(day.copy(enabled = it)) })
            }
            if (day.enabled) {
                Spacer(modifier = Modifier.height(12.dp))
                day.slots.forEach { slot ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Schedule, null, modifier = Modifier.size(14.dp), tint = Color.Gray)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("${slot.start} - ${slot.end}", fontSize = 13.sp, fontWeight = FontWeight.Medium)
                    }
                }
            }
        }
    }
}
