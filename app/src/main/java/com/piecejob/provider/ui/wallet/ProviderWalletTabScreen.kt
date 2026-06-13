package com.piecejob.provider.ui.wallet

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.hilt.navigation.compose.hiltViewModel
import com.piecejob.core.data.remote.dto.*

@Composable
fun ProviderWalletTabScreen(
    viewModel: ProviderWalletViewModel = hiltViewModel()
) {
    val wallet by viewModel.wallet.collectAsState()
    val transactions by viewModel.transactions.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()

    LazyColumn(
        modifier = Modifier.fillMaxSize().background(Color(0xFFF4F5F7)),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text(text = "Wallet Dashboard", fontSize = 24.sp, fontWeight = FontWeight.Black)
        }

        if (isLoading && wallet == null) {
            item { LinearProgressIndicator(modifier = Modifier.fillMaxWidth()) }
        }

        item {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ProviderBalanceCard("Available", "R${wallet?.balanceMain ?: 0.0}", Color(0xFF4CAF50), Modifier.weight(1f))
                ProviderBalanceCard("Escrow", "R${wallet?.balanceEscrow ?: 0.0}", Color(0xFFFFA000), Modifier.weight(1f))
            }
        }

        item {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ProviderBalanceCard("Credit", "R${wallet?.balanceCredit ?: 0.0}", Color(0xFF1976D2), Modifier.weight(1f))
                ProviderBalanceCard("Referral", "R${wallet?.balanceReferral ?: 0.0}", Color(0xFF673AB7), Modifier.weight(1f))
            }
        }

        item {
            Text(text = "Recent Transactions", fontSize = 18.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 16.dp))
        }

        if (transactions.isEmpty()) {
            item { Text("No transactions found", color = Color.Gray, fontSize = 12.sp, modifier = Modifier.padding(16.dp)) }
        } else {
            items(transactions) { tx ->
                ProviderTransactionRow(tx)
            }
        }

        item {
            Text(text = "Wallet Menu", fontSize = 18.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 16.dp))
        }

        val menuItems = listOf(
            "Payout History", "Tax Documents", "Invoices", "Statements"
        )

        items(menuItems) { item ->
            OutlinedCard(
                modifier = Modifier.fillMaxWidth(),
                onClick = { /* Placeholder */ }
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(text = item, fontWeight = FontWeight.Medium)
                    Icon(Icons.Default.ChevronRight, contentDescription = null)
                }
            }
        }
    }
}

@Composable
fun ProviderTransactionRow(tx: WalletTransactionDto) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(text = tx.type.replace("_", " "), fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Text(text = tx.createdAt.take(10), color = Color.Gray, fontSize = 10.sp)
            }
            Text(
                text = "${if(tx.amount >= 0) "+" else ""}R${tx.amount}",
                fontWeight = FontWeight.Black,
                color = if(tx.amount >= 0) Color(0xFF2E7D32) else Color(0xFFD32F2F)
            )
        }
    }
}

@Composable
fun ProviderBalanceCard(title: String, amount: String, color: Color, modifier: Modifier) {
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
