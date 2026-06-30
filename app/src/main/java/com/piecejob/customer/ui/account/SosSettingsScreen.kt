package com.piecejob.customer.ui.account

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SosSettingsScreen(
    onBack: () -> Unit
) {
    var audioRecording by remember { mutableStateOf(true) }
    var locationSharing by remember { mutableStateOf(true) }
    var notifyContacts by remember { mutableStateOf(true) }
    var silentMode by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("SOS Settings", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Text(text = "Security Features", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            }
            item {
                SosToggle("Emergency Audio Recording", "Automatically record 30s of audio when SOS is triggered.", audioRecording) { audioRecording = it }
            }
            item {
                SosToggle("Continuous Location Sharing", "Share live location with emergency response team until resolved.", locationSharing) { locationSharing = it }
            }
            item {
                SosToggle("Notify Emergency Contacts", "Send SMS alerts to your saved emergency contacts.", notifyContacts) { notifyContacts = it }
            }
            item { HorizontalDivider(thickness = 0.5.dp, color = Color.LightGray) }
            item {
                Text(text = "Trigger Behaviour", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            }
            item {
                SosToggle("Silent SOS", "Do not play alarm sound on your device when triggered.", silentMode) { silentMode = it }
            }
        }
    }
}

@Composable
fun SosToggle(label: String, desc: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = label, fontWeight = FontWeight.Bold)
            Text(text = desc, fontSize = 12.sp, color = Color.Gray)
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}
