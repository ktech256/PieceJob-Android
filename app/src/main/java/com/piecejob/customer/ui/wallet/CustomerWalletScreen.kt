package com.piecejob.customer.ui.wallet

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.piecejob.core.data.remote.dto.WalletTransactionDto
import com.piecejob.core.data.remote.dto.InvoiceDto
import androidx.hilt.navigation.compose.hiltViewModel

@Composable
fun CustomerWalletScreen(
    viewModel: CustomerWalletViewModel = hiltViewModel()
) {
    val wallet by viewModel.wallet.collectAsState()
    val history by viewModel.history.collectAsState()
    val invoices by viewModel.invoices.collectAsState()
    val currencySymbol by viewModel.currencySymbol.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
            .padding(16.dp)
    ) {
        Text(
            text = "My Wallet",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )

        Spacer(modifier = Modifier.height(24.dp))

        if (isLoading) {
            LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            Spacer(modifier = Modifier.height(16.dp))
        }

        // Balance Cards
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            WalletMiniCard(
                label = "Credit",
                amount = wallet?.balanceCredit ?: 0.0,
                currency = currencySymbol,
                modifier = Modifier.weight(1f)
            )
            WalletMiniCard(
                label = "Referral",
                amount = wallet?.balanceReferral ?: 0.0,
                currency = currencySymbol,
                modifier = Modifier.weight(1f)
            )
            WalletMiniCard(
                label = "Bonus",
                amount = wallet?.balanceBonus ?: 0.0,
                currency = currencySymbol,
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(32.dp))

        var activeTab by remember { mutableStateOf(0) }
        TabRow(selectedTabIndex = activeTab) {
            Tab(selected = activeTab == 0, onClick = { activeTab = 0 }, text = { Text("Transactions") })
            Tab(selected = activeTab == 1, onClick = { activeTab = 1 }, text = { Text("Invoices") })
        }

        Spacer(modifier = Modifier.height(16.dp))

        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.weight(1f)
        ) {
            if (activeTab == 0) {
                items(history) { tx ->
                    TransactionRow(tx, currencySymbol)
                }
                if (history.isEmpty() && !isLoading) {
                    item {
                        EmptyState("No transactions yet.")
                    }
                }
            } else {
                items(invoices) { invoice ->
                    InvoiceRow(invoice, currencySymbol, onDownload = { url ->
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                        context.startActivity(intent)
                    })
                }
                if (invoices.isEmpty() && !isLoading) {
                    item {
                        EmptyState("No invoices available.")
                    }
                }
            }
        }
    }
}

@Composable
fun EmptyState(message: String) {
    Box(modifier = Modifier.fillMaxWidth().padding(40.dp), contentAlignment = Alignment.Center) {
        Text(message, color = Color.Gray)
    }
}

@Composable
fun WalletMiniCard(label: String, amount: Double, currency: String, modifier: Modifier) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(text = label, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(text = "$currency ${String.format("%.2f", amount)}", fontSize = 16.sp, fontWeight = FontWeight.Black)
        }
    }
}

@Composable
fun TransactionRow(tx: WalletTransactionDto, currency: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(text = tx.type.replace("_", " "), fontSize = 14.sp, fontWeight = FontWeight.Bold)
            Text(text = tx.createdAt.take(10), fontSize = 10.sp, color = Color.Gray)
        }
        Text(
            text = "$currency ${String.format("%.2f", tx.amount)}",
            fontSize = 16.sp,
            fontWeight = FontWeight.Black,
            color = if (tx.amount > 0) Color(0xFF2E7D32) else Color.Red
        )
    }
}

@Composable
fun InvoiceRow(invoice: InvoiceDto, currency: String, onDownload: (String) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(text = invoice.invoiceNumber, fontSize = 14.sp, fontWeight = FontWeight.Bold)
            Text(text = "Job #${invoice.jobId.takeLast(6)}", fontSize = 10.sp, color = Color.Gray)
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "$currency ${String.format("%.2f", invoice.amount)}",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(end = 8.dp)
            )
            TextButton(onClick = { invoice.pdfUrl?.let { onDownload(it) } }) {
                Text("View PDF", fontSize = 12.sp)
            }
        }
    }
}
