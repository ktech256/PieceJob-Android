package com.piecejob.customer.ui.account

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SupportScreen(
    onBack: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Support", fontWeight = FontWeight.Bold) },
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
                SupportMenuItem("Submit a Ticket", Icons.Default.ConfirmationNumber) { /* Ticket Logic */ }
                SupportMenuItem("Ticket History", Icons.Default.History) { /* History Logic */ }
                SupportMenuItem("Frequently Asked Questions", Icons.Default.Quiz) { /* FAQ Logic */ }
                SupportMenuItem("Chat with Support", Icons.Default.Chat) { /* Chat Support Logic */ }
                
                Spacer(modifier = Modifier.height(32.dp))
                
                Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Emergency?", color = Color.Gray)
                    TextButton(onClick = { /* SOS Logic */ }) {
                        Text("TRIGGER SOS", color = Color.Red, fontWeight = FontWeight.Black)
                    }
                }
            }
        }
    }
}

@Composable
fun SupportMenuItem(title: String, icon: androidx.compose.ui.graphics.vector.ImageVector, onClick: () -> Unit) {
    ListItem(
        headlineContent = { Text(title, fontWeight = FontWeight.SemiBold) },
        leadingContent = { Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
        trailingContent = { Icon(Icons.Default.ChevronRight, contentDescription = null, tint = Color.LightGray) },
        modifier = Modifier.clickable { onClick() }
    )
}
