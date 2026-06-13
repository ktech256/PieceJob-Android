package com.piecejob.provider.ui.profile

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.piecejob.core.data.remote.dto.NotificationSettingsDto

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProviderNotificationsScreen(
    viewModel: ProviderNotificationsViewModel = hiltViewModel(),
    onBack: () -> Unit
) {
    val settings by viewModel.settings.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()

    var jobBroadcasts by remember { mutableStateOf(true) }
    var chatMessages by remember { mutableStateOf(true) }
    var walletAlerts by remember { mutableStateOf(true) }
    var payoutAlerts by remember { mutableStateOf(true) }
    var verificationUpdates by remember { mutableStateOf(true) }
    var marketing by remember { mutableStateOf(false) }
    var sosAlerts by remember { mutableStateOf(true) }

    LaunchedEffect(settings) {
        settings?.let {
            jobBroadcasts = it.jobBroadcasts
            chatMessages = it.chatMessages
            walletAlerts = it.walletAlerts
            payoutAlerts = it.payoutAlerts
            verificationUpdates = it.verificationUpdates
            marketing = it.marketing
            sosAlerts = it.sosAlerts
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Notification Settings", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(modifier = Modifier.fillMaxSize().padding(padding).padding(24.dp)) {
            item {
                NotificationToggle("Job Broadcast Notifications", jobBroadcasts) { jobBroadcasts = it }
                NotificationToggle("Chat Messages", chatMessages) { chatMessages = it }
                NotificationToggle("Wallet Alerts", walletAlerts) { walletAlerts = it }
                NotificationToggle("Payout Notifications", payoutAlerts) { payoutAlerts = it }
                NotificationToggle("Verification Updates", verificationUpdates) { verificationUpdates = it }
                NotificationToggle("Marketing & Promotions", marketing) { marketing = it }
                NotificationToggle("SOS Alerts", sosAlerts) { sosAlerts = it }
                
                Spacer(modifier = Modifier.height(32.dp))
                
                Button(
                    onClick = {
                        viewModel.updateSettings(
                            NotificationSettingsDto(
                                jobBroadcasts, chatMessages, walletAlerts, payoutAlerts, verificationUpdates, marketing, sosAlerts
                            )
                        )
                    },
                    modifier = Modifier.fillMaxWidth().height(56.dp)
                ) {
                    Text("SAVE PREFERENCES", fontWeight = FontWeight.Black)
                }
            }
        }
    }
}

@Composable
fun NotificationToggle(label: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = label, fontSize = 14.sp, fontWeight = FontWeight.Medium)
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}
