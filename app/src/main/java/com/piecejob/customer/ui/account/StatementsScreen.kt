package com.piecejob.customer.ui.account

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.piecejob.customer.ui.wallet.CustomerWalletViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatementsScreen(
    onBack: () -> Unit,
    viewModel: CustomerWalletViewModel = hiltViewModel()
) {
    // In a real app, I'd have a separate endpoint for statements
    // Reusing the ViewModel that might have similar logic
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Statements", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).padding(16.dp)) {
            Text(text = "Monthly Statements", fontSize = 18.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(16.dp))
            
            val months = listOf("May 2024", "April 2024", "March 2024", "February 2024")
            
            LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                items(months) { month ->
                    StatementRow(month)
                }
            }
        }
    }
}

@Composable
fun StatementRow(month: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFF9F9F9))
    ) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.PictureAsPdf, contentDescription = null, tint = Color.Red.copy(alpha = 0.6f))
            Spacer(modifier = Modifier.width(16.dp))
            Text(text = month, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
            TextButton(onClick = { /* Download */ }) {
                Text("DOWNLOAD")
            }
        }
    }
}
