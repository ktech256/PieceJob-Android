package com.piecejob.provider.ui.profile

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProviderWalletSettingsScreen(
    viewModel: ProviderWalletSettingsViewModel = hiltViewModel(),
    onBack: () -> Unit
) {
    val payoutPrefs by viewModel.payoutPrefs.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()

    var selectedFrequency by remember { mutableStateOf("WEEKLY") }
    var selectedMethod by remember { mutableStateOf("BANK_TRANSFER") }

    LaunchedEffect(payoutPrefs) {
        payoutPrefs?.let {
            selectedFrequency = it.frequency
            selectedMethod = it.method
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Wallet Settings", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        if (isLoading && payoutPrefs == null) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            Column(modifier = Modifier.fillMaxSize().padding(padding).padding(24.dp)) {
                Text(text = "Payout Frequency", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Spacer(modifier = Modifier.height(8.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    FilterChip(
                        selected = selectedFrequency == "WEEKLY",
                        onClick = { selectedFrequency = "WEEKLY" },
                        label = { Text("Weekly") },
                        modifier = Modifier.weight(1f)
                    )
                    FilterChip(
                        selected = selectedFrequency == "MONTHLY",
                        onClick = { selectedFrequency = "MONTHLY" },
                        label = { Text("Monthly") },
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(32.dp))

                Text(text = "Payout Method", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Spacer(modifier = Modifier.height(8.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    FilterChip(
                        selected = selectedMethod == "BANK_TRANSFER",
                        onClick = { selectedMethod = "BANK_TRANSFER" },
                        label = { Text("Bank Transfer") },
                        modifier = Modifier.weight(1f)
                    )
                    FilterChip(
                        selected = selectedMethod == "WALLET_TRANSFER",
                        onClick = { selectedMethod = "WALLET_TRANSFER" },
                        label = { Text("Wallet Balance") },
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(48.dp))

                Button(
                    onClick = { viewModel.updateSettings(selectedFrequency, selectedMethod) },
                    modifier = Modifier.fillMaxWidth().height(56.dp)
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                    } else {
                        Text("SAVE PREFERENCES", fontWeight = FontWeight.Black)
                    }
                }
                
                Spacer(modifier = Modifier.height(24.dp))
                
                Text(
                    text = "Threshold Info: Payouts are triggered once you reach the minimum threshold of R200.00.",
                    fontSize = 12.sp,
                    color = Color.Gray
                )
            }
        }
    }
}
