package com.piecejob.provider.ui.wallet

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun ProviderWalletTabScreen() {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text(text = "Wallet Dashboard", fontSize = 24.sp, fontWeight = FontWeight.Black)
        }

        item {
            // Balance Cards Placeholder
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                BalanceCard("Available", "$0.00", Color(0xFF4CAF50), Modifier.weight(1f))
                BalanceCard("Escrow", "$0.00", Color(0xFFFFA000), Modifier.weight(1f))
            }
        }

        item {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                BalanceCard("Pending", "$0.00", Color(0xFF1976D2), Modifier.weight(1f))
                BalanceCard("Lifetime", "$0.00", Color(0xFF673AB7), Modifier.weight(1f))
            }
        }

        item {
            Text(text = "Wallet Menu", fontSize = 18.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 16.dp))
        }

        val menuItems = listOf(
            "Transactions", "Statements", "Payout History", 
            "Tax Documents", "Invoices", "Referral Earnings", "Bonuses"
        )

        items(menuItems.size) { index ->
            OutlinedCard(
                modifier = Modifier.fillMaxWidth(),
                onClick = { /* Placeholder */ }
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(text = menuItems[index], fontWeight = FontWeight.Medium)
                    Icon(Icons.Default.ChevronRight, contentDescription = null)
                }
            }
        }
    }
}

@Composable
fun BalanceCard(title: String, amount: String, color: Color, modifier: Modifier) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = color.copy(alpha = 0.1f)),
        border = androidx.compose.foundation.BorderStroke(1.dp, color.copy(alpha = 0.3f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(text = title, fontSize = 12.sp, color = color, fontWeight = FontWeight.Bold)
            Text(text = amount, fontSize = 20.sp, fontWeight = FontWeight.Black, color = color)
        }
    }
}
