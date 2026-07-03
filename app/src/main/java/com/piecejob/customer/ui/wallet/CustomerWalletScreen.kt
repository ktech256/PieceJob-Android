package com.piecejob.customer.ui.wallet

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.style.TextAlign
import androidx.hilt.navigation.compose.hiltViewModel
import com.piecejob.core.data.remote.dto.InvoiceDto
import com.piecejob.core.data.remote.dto.WalletTransactionDto

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

    var selectedFilter by remember { mutableStateOf("ALL") }
    val filteredHistory = remember(history, selectedFilter) {
        if (selectedFilter == "ALL") history
        else history.filter { it.type == selectedFilter }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF8F9FA))
    ) {
        // Top Header
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = MaterialTheme.colorScheme.primary,
            shape = RoundedCornerShape(bottomStart = 32.dp, bottomEnd = 32.dp),
            shadowElevation = 8.dp
        ) {
            Column(modifier = Modifier.padding(24.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "My Wallet",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Black,
                        color = Color.White
                    )
                    Surface(
                        color = Color.White.copy(alpha = 0.2f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            text = "ACTIVE",
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            color = Color.White,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                Text(
                    text = "Total Available Balance",
                    color = Color.White.copy(alpha = 0.7f),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "$currencySymbol ${String.format("%.2f", (wallet?.balanceMain ?: 0.0) + (wallet?.balanceCredit ?: 0.0))}",
                    color = Color.White,
                    fontSize = 36.sp,
                    fontWeight = FontWeight.Black
                )

                Spacer(modifier = Modifier.height(24.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    WalletActionButton(
                        label = "Top Up",
                        icon = Icons.Default.Add,
                        onClick = { /* Implement Payment Gateway */ },
                        modifier = Modifier.weight(1f)
                    )
                    WalletActionButton(
                        label = "Transfer",
                        icon = Icons.Default.Send,
                        onClick = { /* Future Ready */ },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item { Spacer(modifier = Modifier.height(16.dp)) }

            // Sub-balances
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    BalanceCard(
                        label = "Referral",
                        amount = wallet?.balanceReferral ?: 0.0,
                        currency = currencySymbol,
                        color = Color(0xFF673AB7),
                        modifier = Modifier.weight(1f)
                    )
                    BalanceCard(
                        label = "Bonus",
                        amount = wallet?.balanceBonus ?: 0.0,
                        currency = currencySymbol,
                        color = Color(0xFFFF9800),
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // Pending Stats
            item {
                PendingStatsRow(
                    pendingRefunds = history.filter { it.type == "REFUND" && it.status == "PENDING" }.sumOf { it.amount },
                    pendingCredits = history.filter { it.type == "CREDIT_TOPUP" && it.status == "PENDING" }.sumOf { it.amount },
                    currency = currencySymbol
                )
            }

            // Transactions Header & Filters
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Recent Transactions",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Black,
                        color = Color.Black
                    )
                    IconButton(onClick = { /* Show Filter BottomSheet */ }) {
                        Icon(Icons.Default.FilterList, contentDescription = "Filter")
                    }
                }
            }

            if (isLoading) {
                item {
                    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }
            } else if (filteredHistory.isEmpty()) {
                item {
                    EmptyState("No transactions match your filter.")
                }
            } else {
                items(filteredHistory) { tx ->
                    TransactionRow(tx, currencySymbol)
                }
            }

            item { Spacer(modifier = Modifier.height(80.dp)) }
        }
    }
}

@Composable
fun WalletActionButton(label: String, icon: ImageVector, onClick: () -> Unit, modifier: Modifier) {
    Button(
        onClick = onClick,
        modifier = modifier.height(48.dp),
        colors = ButtonDefaults.buttonColors(containerColor = Color.White.copy(alpha = 0.2f)),
        shape = RoundedCornerShape(12.dp),
        contentPadding = PaddingValues(0.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text(label, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun BalanceCard(label: String, amount: Double, currency: String, color: Color, modifier: Modifier) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .background(color.copy(alpha = 0.1f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Box(modifier = Modifier.size(8.dp).background(color, CircleShape))
            }
            Spacer(modifier = Modifier.height(12.dp))
            Text(text = label, fontSize = 11.sp, color = Color.Gray, fontWeight = FontWeight.Bold)
            Text(text = "$currency ${String.format("%.2f", amount)}", fontSize = 16.sp, fontWeight = FontWeight.Black)
        }
    }
}

@Composable
fun PendingStatsRow(pendingRefunds: Double, pendingCredits: Double, currency: String) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = Color.White,
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, Color(0xFFEEEEEE))
    ) {
        Row(modifier = Modifier.padding(16.dp), horizontalArrangement = Arrangement.SpaceBetween) {
            Column {
                Text("Pending Refunds", fontSize = 10.sp, color = Color.Gray, fontWeight = FontWeight.Bold)
                Text("$currency ${String.format("%.2f", pendingRefunds)}", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color(0xFFD32F2F))
            }
            // Vertical Divider
            Box(modifier = Modifier
                .width(1.dp)
                .height(32.dp)
                .background(Color(0xFFEEEEEE)))
            
            Column(horizontalAlignment = Alignment.End) {
                Text("Pending Credits", fontSize = 10.sp, color = Color.Gray, fontWeight = FontWeight.Bold)
                Text("$currency ${String.format("%.2f", pendingCredits)}", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1976D2))
            }
        }
    }
}

@Composable
fun TransactionRow(tx: WalletTransactionDto, currency: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                val icon = when(tx.type) {
                    "SERVICE_FEE" -> Icons.Default.Work
                    "BOOKING_FEE" -> Icons.Default.Receipt
                    "REFUND" -> Icons.Default.Undo
                    "PROMO_CREDIT" -> Icons.Default.CardGiftcard
                    "REFERRAL_REWARD" -> Icons.Default.Group
                    "CREDIT_TOPUP" -> Icons.Default.AccountBalanceWallet
                    else -> Icons.Default.Payments
                }
                val iconColor = if (tx.amount > 0) Color(0xFF4CAF50) else Color(0xFFF44336)
                
                Surface(
                    modifier = Modifier.size(40.dp),
                    shape = CircleShape,
                    color = iconColor.copy(alpha = 0.1f)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(icon, contentDescription = null, tint = iconColor, modifier = Modifier.size(20.dp))
                    }
                }
                
                Spacer(modifier = Modifier.width(16.dp))
                
                Column {
                    Text(text = tx.type.replace("_", " "), fontSize = 14.sp, fontWeight = FontWeight.Black)
                    Text(text = tx.description ?: "No description", fontSize = 11.sp, color = Color.Gray, maxLines = 1)
                    Text(text = tx.createdAt.take(16).replace("T", " "), fontSize = 9.sp, color = Color.LightGray)
                }
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "${if (tx.amount > 0) "+" else ""}${String.format("%.2f", tx.amount)} $currency",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Black,
                    color = if (tx.amount > 0) Color(0xFF2E7D32) else Color.Red
                )
                if (tx.status != "COMPLETED") {
                    Surface(
                        color = Color(0xFFFFF3E0),
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Text(
                            text = tx.status,
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp),
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFE65100)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun EmptyState(message: String) {
    Box(modifier = Modifier.fillMaxWidth().padding(40.dp), contentAlignment = Alignment.Center) {
        Text(message, color = Color.Gray, textAlign = TextAlign.Center)
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
