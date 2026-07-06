package com.piecejob.provider.ui.wallet

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.BorderStroke
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
import androidx.hilt.navigation.compose.hiltViewModel
import com.piecejob.core.data.remote.dto.*
import java.util.Locale

@Composable
fun ProviderWalletScreen(
    viewModel: ProviderWalletViewModel = hiltViewModel(),
    onWithdrawClick: () -> Unit
) {
    val wallet by viewModel.wallet.collectAsState()
    val history by viewModel.history.collectAsState()
    val statements by viewModel.statements.collectAsState()
    val invoices by viewModel.invoices.collectAsState()
    val currencySymbol by viewModel.currencySymbol.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val context = LocalContext.current
    
    var showPayServiceFeeDialog by remember { mutableStateOf(false) }
    var voucherNumber by remember { mutableStateOf("") }
    var selectedVendor by remember { mutableStateOf("OTT") }

    if (showPayServiceFeeDialog) {
        AlertDialog(
            onDismissRequest = { showPayServiceFeeDialog = false },
            title = { Text("Pay Service Fee") },
            text = {
                Column {
                    Text("Select Voucher Vendor", fontSize = 12.sp, color = Color.Gray)
                    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("OTT", "BLUE", "1VOUCHER").forEach { vendor ->
                            FilterChip(
                                selected = selectedVendor == vendor,
                                onClick = { selectedVendor = vendor },
                                label = { Text(vendor, fontSize = 10.sp) }
                            )
                        }
                    }
                    OutlinedTextField(
                        value = voucherNumber,
                        onValueChange = { voucherNumber = it },
                        label = { Text("Voucher Number") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Text("Enter 'TEST' for simulation (R100) or 'PRE' + amount.", fontSize = 10.sp, color = Color.Gray, modifier = Modifier.padding(top = 4.dp))
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.payServiceFee(selectedVendor, voucherNumber)
                        showPayServiceFeeDialog = false
                        voucherNumber = ""
                    },
                    enabled = voucherNumber.isNotBlank()
                ) { Text("Redeem & Pay") }
            },
            dismissButton = {
                TextButton(onClick = { showPayServiceFeeDialog = false }) { Text("Cancel") }
            }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
            .padding(16.dp)
    ) {
        Text(
            text = "Earnings & Wallet",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF212121)
        )

        Spacer(modifier = Modifier.height(24.dp))

        if (isLoading) {
            LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            Spacer(modifier = Modifier.height(16.dp))
        }

        // Main Balance Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF212121))
        ) {
            Column(modifier = Modifier.padding(24.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Column {
                        Text(text = "Available for Withdrawal", color = Color.White.copy(alpha = 0.7f), fontSize = 12.sp)
                        Text(
                            text = String.format(Locale.getDefault(), "%s %.2f", currencySymbol, wallet?.balanceMain ?: 0.0),
                            color = Color.White,
                            fontSize = 32.sp,
                            fontWeight = FontWeight.Black
                        )
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text(text = "In Escrow", color = Color.White.copy(alpha = 0.7f), fontSize = 10.sp)
                        Text(
                            text = String.format(Locale.getDefault(), "%s %.2f", currencySymbol, wallet?.balanceEscrow ?: 0.0),
                            color = Color(0xFFFFA000),
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(24.dp))
                
                // SERVICE FEE Section
                val serviceFeeBalance = wallet?.serviceFeeBalance ?: 0.0
                val (statusText, statusColor, displayAmount) = when {
                    serviceFeeBalance > 0 -> Triple("Outstanding", Color.Red, String.format(Locale.getDefault(), "%s %.2f", currencySymbol, serviceFeeBalance))
                    serviceFeeBalance < 0 -> Triple("Credit", Color(0xFF2E7D32), String.format(Locale.getDefault(), "%s +%.2f", currencySymbol, -serviceFeeBalance))
                    else -> Triple("Settled", Color(0xFF1976D2), String.format(Locale.getDefault(), "%s 0.00", currencySymbol))
                }

                Card(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF333333)),
                    border = BorderStroke(1.dp, if (wallet?.isSuspended == true) Color.Red else Color.Gray)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("SERVICE FEE", color = Color.White, fontWeight = FontWeight.Black, fontSize = 12.sp)
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(statusText, color = statusColor, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                Text(
                                    text = displayAmount,
                                    color = if (wallet?.isSuspended == true && serviceFeeBalance > 0) Color.Red else Color.White,
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Button(
                                onClick = { showPayServiceFeeDialog = true },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32)),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                            ) {
                                Text("PAY", fontSize = 12.sp)
                            }
                        }
                    }
                }

                Button(
                    onClick = onWithdrawClick,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD32F2F)),
                    shape = RoundedCornerShape(12.dp),
                    enabled = (wallet?.balanceMain ?: 0.0) >= 50.0
                ) {
                    Text("WITHDRAW FUNDS", fontWeight = FontWeight.Bold)
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        var activeTab by remember { mutableStateOf(0) }
        TabRow(selectedTabIndex = activeTab) {
            Tab(selected = activeTab == 0, onClick = { activeTab = 0 }, text = { Text("Recent") })
            Tab(selected = activeTab == 1, onClick = { activeTab = 1 }, text = { Text("Statements") })
            Tab(selected = activeTab == 2, onClick = { activeTab = 2 }, text = { Text("Invoices") })
        }

        Spacer(modifier = Modifier.height(16.dp))

        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            when (activeTab) {
                0 -> {
                    items(history) { tx -> TransactionItem(tx, currencySymbol) }
                    if (history.isEmpty() && !isLoading) {
                        item { EmptyState("No transaction history.") }
                    }
                }
                1 -> {
                    items(statements) { statement ->
                        StatementRow(statement, currencySymbol, onDownload = { url ->
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                            context.startActivity(intent)
                        })
                    }
                    if (statements.isEmpty() && !isLoading) {
                        item { EmptyState("No statements generated yet.") }
                    }
                }
                2 -> {
                    items(invoices) { invoice ->
                        InvoiceItem(invoice, onDownload = { url ->
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                            context.startActivity(intent)
                        })
                    }
                    if (invoices.isEmpty() && !isLoading) {
                        item { EmptyState("No invoices available.") }
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
fun TransactionItem(tx: WalletTransactionDto, currency: String) {
    val displayType = when (tx.type) {
        "COMMISSION" -> "SERVICE FEE"
        else -> tx.type.replace("_", " ")
    }
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFF9F9F9))
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(text = displayType, fontWeight = FontWeight.Black, fontSize = 14.sp)
                Text(text = tx.description ?: "", fontSize = 11.sp, color = Color.Gray, maxLines = 1)
                Text(text = tx.createdAt.take(10), fontSize = 9.sp, color = Color.LightGray)
            }
            Text(
                text = String.format(Locale.getDefault(), "%s%s %.2f", if (tx.amount > 0) "+" else "-", currency, tx.amount),
                fontWeight = FontWeight.Black,
                fontSize = 16.sp,
                color = if (tx.amount > 0) Color(0xFF2E7D32) else Color(0xFFD32F2F)
            )
        }
    }
}

@Composable
fun StatementRow(statement: StatementDto, currency: String, onDownload: (String) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(text = "Statement ${statement.periodStart}", fontSize = 14.sp, fontWeight = FontWeight.Bold)
            Text(text = "${statement.summary.jobCount} Jobs Completed", fontSize = 10.sp, color = Color.Gray)
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = String.format(Locale.getDefault(), "%s %.2f", currency, statement.summary.netEarnings),
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(end = 8.dp)
            )
            TextButton(onClick = { onDownload(statement.pdfUrl) }) {
                Text("PDF", fontSize = 12.sp)
            }
        }
    }
}

@Composable
fun InvoiceItem(invoice: InvoiceDto, onDownload: (String) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(text = invoice.invoiceNumber, fontSize = 14.sp, fontWeight = FontWeight.Bold)
            Text(text = invoice.createdAt.take(10), fontSize = 10.sp, color = Color.Gray)
        }
        TextButton(onClick = { invoice.pdfUrl?.let { onDownload(it) } }) {
            Text("Download", fontSize = 12.sp)
        }
    }
}
