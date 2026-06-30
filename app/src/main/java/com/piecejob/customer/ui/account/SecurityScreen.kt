package com.piecejob.customer.ui.account

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Devices
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.Phone
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
fun SecurityScreen(
    onBack: () -> Unit,
    viewModel: CustomerAccountViewModel = hiltViewModel()
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Security", fontWeight = FontWeight.Bold) },
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
        ) {
            item {
                SecurityMenuItem("Change Password", Icons.Default.Lock) { /* Change Password Logic */ }
                SecurityMenuItem("Change Phone Number", Icons.Default.Phone) { /* Change Phone Logic */ }
                SecurityMenuItem("Change Email", Icons.Default.Email) { /* Change Email Logic */ }
                HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))
                SecurityMenuItem("Active Sessions", Icons.Default.Devices) { /* Active Sessions Logic */ }
                SecurityMenuItem("Logout Other Devices", Icons.Default.Logout, tint = Color.Red) { /* Logout Others Logic */ }
            }
        }
    }
}

@Composable
fun SecurityMenuItem(title: String, icon: androidx.compose.ui.graphics.vector.ImageVector, tint: Color = MaterialTheme.colorScheme.primary, onClick: () -> Unit) {
    ListItem(
        headlineContent = { Text(title, fontWeight = FontWeight.SemiBold) },
        leadingContent = { Icon(icon, contentDescription = null, tint = tint) },
        trailingContent = { Icon(Icons.Default.ChevronRight, contentDescription = null, tint = Color.LightGray) },
        modifier = Modifier.clickable { onClick() }
    )
}
