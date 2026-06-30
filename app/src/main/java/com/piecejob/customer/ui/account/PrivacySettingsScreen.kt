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
import androidx.hilt.navigation.compose.hiltViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PrivacySettingsScreen(
    onBack: () -> Unit,
    viewModel: CustomerAccountViewModel = hiltViewModel()
) {
    val user by viewModel.user.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()

    var profileVisibility by remember { mutableStateOf("PUBLIC") }
    var shareLocation by remember { mutableStateOf(true) }
    var dataSharing by remember { mutableStateOf(true) }
    var marketingPreferences by remember { mutableStateOf(true) }

    LaunchedEffect(user) {
        user?.privacySettings?.let {
            profileVisibility = it.profileVisibility
            shareLocation = it.shareLocation
            dataSharing = it.dataSharing
            marketingPreferences = it.marketingPreferences
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Privacy", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    TextButton(onClick = {
                        viewModel.updatePrivacy(profileVisibility, shareLocation, dataSharing, marketingPreferences)
                    }, enabled = !isLoading) {
                        Text("SAVE", fontWeight = FontWeight.Black)
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
                Text(text = "Visibility", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            }
            item {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(text = "Profile Visibility", modifier = Modifier.weight(1f))
                    TextButton(onClick = { profileVisibility = if (profileVisibility == "PUBLIC") "PRIVATE" else "PUBLIC" }) {
                        Text(profileVisibility)
                    }
                }
            }
            item { HorizontalDivider(thickness = 0.5.dp, color = Color.LightGray) }
            item {
                Text(text = "Location & Data", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            }
            item {
                PrivacyToggle("Share Location with Providers", shareLocation) { shareLocation = it }
            }
            item {
                PrivacyToggle("Anonymized Data Sharing", dataSharing) { dataSharing = it }
            }
            item { HorizontalDivider(thickness = 0.5.dp, color = Color.LightGray) }
            item {
                Text(text = "Marketing", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            }
            item {
                PrivacyToggle("Personalized Offers & Ads", marketingPreferences) { marketingPreferences = it }
            }
        }
    }
}

@Composable
fun PrivacyToggle(label: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
        Text(text = label, modifier = Modifier.weight(1f))
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}
