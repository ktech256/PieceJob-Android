package com.piecejob.provider.ui.wallet

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.piecejob.core.data.remote.dto.*
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
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
    var selectedVendor by remember { mutableStateOf("OTT Voucher") }
    var expandedVendorDropdown by remember { mutableStateOf(false) }
    val vendors = listOf("OTT Voucher", "Blue Voucher", "1Voucher")

    if (showPayServiceFeeDialog) {
        AlertDialog(
            onDismissRequest = { showPayServiceFeeDialog = false },
            title = { Text("Pay Service Fee", fontWeight = FontWeight.Black) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Select Voucher Type", fontSize = 12.sp, color = Color.Gray, fontWeight = FontWeight.Bold)
                    
                    Box {
                        OutlinedTextField(
                            value = selectedVendor,
                            onValueChange = { },
                            readOnly = true,
                            modifier = Modifier.fillMaxWidth(),
                            trailingIcon = {
                                IconButton(onClick = { expandedVendorDropdown = true }) {
                                    Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                                }
                            }
                        )
                        DropdownMenu(
                            expanded = expandedVendorDropdown,
                            onDismissRequest = { expandedVendorDropdown = false },
                            modifier = Modifier.fillMaxWidth(0.7f)
                        ) {
                            vendors.forEach { vendor ->
                                DropdownMenuItem(
                                    text = { Text(vendor) },
                                    onClick = {
                                        selectedVendor = vendor
                                        expandedVendorDropdown = false
                                    }
                                )
                            }
                        }
                    }

                    OutlinedTextField(
                        value = voucherNumber,
                        onValueChange = { voucherNumber = it },
                        label = { Text("Voucher Number") },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("Enter 12-16 digit code") }
                    )
                    
                    Surface(
                        color = Color(0xFFF5F5F5),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Row(modifier = Modifier.padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Info, contentDescription = null, modifier = Modifier.size(14.dp), tint = Color.Gray)
                            Spacer(Modifier.width(8.dp))
                            Text("Voucher will be applied to your outstanding balance first.", fontSize = 10.sp, color = Color.Gray)
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val vendorCode = when(selectedVendor) {
                            "OTT Voucher" -> "OTT"
                            "Blue Voucher" -> "BLUE"
                            "1Voucher" -> "1VOUCHER"
                            else -> "OTT"
                        }
                        viewModel.payServiceFee(vendorCode, voucherNumber)
                        showPayServiceFeeDialog = false
                        voucherNumber = ""
                    },
                    enabled = voucherNumber.isNotBlank(),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32))
                ) { Text("SUBMIT PAYMENT") }
            },
            dismissButton = {
                TextButton(onClick = { showPayServiceFeeDialog = false }) { Text("CANCEL") }
            }
        )
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF8F9FA))
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(vertical = 24.dp)
    ) {
        item {
            Text(
                text = "Wallet & Earnings",
                fontSize = 28.sp,
                fontWeight = FontWeight.Black,
                color = Color(0xFF121212)
            )
            Spacer(modifier = Modifier.height(24.dp))
        }

        if (isLoading) {
            item {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(2.dp)))
                Spacer(modifier = Modifier.height(16.dp))
            }
        }

        // --- SECTION: PRIMARY BALANCES ---
        item {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                BalanceSmallCard(
                    label = "Available",
                    value = wallet?.balanceMain ?: 0.0,
                    currency = currencySymbol,
                    modifier = Modifier.weight(1f),
                    color = Color(0xFF121212)
                )
                BalanceSmallCard(
                    label = "Escrow",
                    value = wallet?.balanceEscrow ?: 0.0,
                    currency = currencySymbol,
                    modifier = Modifier.weight(1f),
                    color = Color(0xFFFFA000)
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                BalanceSmallCard(
                    label = "Credit",
                    value = wallet?.balanceCredit ?: 0.0,
                    currency = currencySymbol,
                    modifier = Modifier.weight(1f),
                    color = Color(0xFF2E7D32)
                )
                BalanceSmallCard(
                    label = "Referral",
                    value = wallet?.balanceReferral ?: 0.0,
                    currency = currencySymbol,
                    modifier = Modifier.weight(1f),
                    color = Color(0xFF1976D2)
                )
            }
            Spacer(modifier = Modifier.height(24.dp))
        }

        // --- SECTION: DEDICATED SERVICE FEE ---
        item {
            val balance = wallet?.serviceFeeBalance ?: 0.0
            val (statusText, statusColor, displayBalance) = when {
                balance > 0 -> Triple("Outstanding", Color(0xFFD32F2F), String.format(Locale.getDefault(), "-%s %.2f", currencySymbol, balance))
                balance < 0 -> Triple("Credit", Color(0xFF2E7D32), String.format(Locale.getDefault(), "+%s %.2f", currencySymbol, -balance))
                else -> Triple("Settled", Color(0xFF1976D2), String.format(Locale.getDefault(), "%s 0.00", currencySymbol))
            }

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = BorderStroke(1.dp, Color(0xFFEEEEEE)),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(24.dp)) {
                    Text("SERVICE FEE", fontWeight = FontWeight.Black, fontSize = 12.sp, letterSpacing = 1.sp, color = Color.Gray)
                    
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(text = statusText, color = statusColor, fontSize = 14.sp, fontWeight = FontWeight.Black)
                            Text(text = displayBalance, fontSize = 32.sp, fontWeight = FontWeight.Black, color = Color(0xFF121212))
                        }
                        Button(
                            onClick = { showPayServiceFeeDialog = true },
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF121212)),
                            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp)
                        ) {
                            Text("PAY SERVICE FEE", fontSize = 11.sp, fontWeight = FontWeight.Black)
                        }
                    }

                    // Financial Breakdown (from last job)
                    wallet?.lastServiceFeeDetails?.let { details ->
                        Divider(modifier = Modifier.padding(vertical = 12.dp), thickness = 0.5.dp, color = Color(0xFFEEEEEE))
                        
                        BreakdownRow("Service Fee %", "${details.serviceFeePercentage}%")
                        BreakdownRow("Booking Fee Contribution", String.format(Locale.getDefault(), "%s %.2f", currencySymbol, details.bookingFeePaid))
                        BreakdownRow("Negotiated Price", String.format(Locale.getDefault(), "%s %.2f", currencySymbol, details.acceptedPrice))
                        BreakdownRow("Platform Share", String.format(Locale.getDefault(), "%s %.2f", currencySymbol, details.serviceFeeAmount))
                        BreakdownRow("Provider Keeps", String.format(Locale.getDefault(), "%s %.2f", currencySymbol, details.providerKeeps), isHighlight = true)
                        
                        Spacer(Modifier.height(12.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth().background(statusColor.copy(alpha = 0.05f), RoundedCornerShape(8.dp)).padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Outstanding Service Fee", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = statusColor)
                            Text(String.format(Locale.getDefault(), "%s %.2f", currencySymbol, details.outstandingBalance), fontSize = 11.sp, fontWeight = FontWeight.Black, color = statusColor)
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(24.dp))
        }

        item {
            Button(
                onClick = onWithdrawClick,
                modifier = Modifier.fillMaxWidth().height(56.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD32F2F)),
                shape = RoundedCornerShape(16.dp),
                enabled = (wallet?.balanceMain ?: 0.0) >= 50.0
            ) {
                Text("WITHDRAW FUNDS", fontWeight = FontWeight.Black, letterSpacing = 1.sp)
            }
            Spacer(modifier = Modifier.height(32.dp))
        }

        item {
            var activeTab by remember { mutableIntStateOf(0) }
            TabRow(
                selectedTabIndex = activeTab,
                containerColor = Color.Transparent,
                contentColor = Color(0xFF121212),
                indicator = { tabPositions ->
                    TabRowDefaults.Indicator(
                        modifier = Modifier.tabIndicatorOffset(tabPositions[activeTab]),
                        color = Color(0xFF121212)
                    )
                }
            ) {
                Tab(selected = activeTab == 0, onClick = { activeTab = 0 }, text = { Text("Recent", fontWeight = FontWeight.Bold) })
                Tab(selected = activeTab == 1, onClick = { activeTab = 1 }, text = { Text("Statements", fontWeight = FontWeight.Bold) })
                Tab(selected = activeTab == 2, onClick = { activeTab = 2 }, text = { Text("Invoices", fontWeight = FontWeight.Bold) })
            }
            Spacer(modifier = Modifier.height(16.dp))

            when (activeTab) {
                0 -> {
                    if (history.isEmpty() && !isLoading) {
                        EmptyState("No transaction history.")
                    } else {
                        history.forEach { tx -> TransactionItem(tx, currencySymbol) }
                    }
                }
                1 -> {
                    if (statements.isEmpty() && !isLoading) {
                        EmptyState("No statements generated yet.")
                    } else {
                        statements.forEach { statement ->
                            StatementRow(statement, currencySymbol, onDownload = { url ->
                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                                context.startActivity(intent)
                            })
                        }
                    }
                }
                2 -> {
                    if (invoices.isEmpty() && !isLoading) {
                        EmptyState("No invoices available.")
                    } else {
                        invoices.forEach { invoice ->
                            InvoiceItem(invoice, onDownload = { url ->
                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                                context.startActivity(intent)
                            })
                        }
                    }
                }
            }
        }
        
        item {
            Spacer(modifier = Modifier.height(100.dp))
        }
    }
}

@Composable
fun BalanceSmallCard(label: String, value: Double, currency: String, modifier: Modifier, color: Color) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, Color(0xFFEEEEEE))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(text = label.uppercase(), fontSize = 9.sp, fontWeight = FontWeight.Black, color = Color.Gray, letterSpacing = 1.sp)
            Spacer(Modifier.height(4.dp))
            Text(
                text = String.format(Locale.getDefault(), "%s %.2f", currency, value),
                fontSize = 18.sp,
                fontWeight = FontWeight.Black,
                color = color
            )
        }
    }
}

@Composable
fun BreakdownRow(label: String, value: String, isHighlight: Boolean = false) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, fontSize = 11.sp, color = Color.Gray, fontWeight = FontWeight.Medium)
        Text(text = value, fontSize = 11.sp, fontWeight = if (isHighlight) FontWeight.Black else FontWeight.Bold, color = if (isHighlight) Color(0xFF2E7D32) else Color(0xFF121212))
    }
}

@Composable
fun EmptyState(message: String) {
    Box(modifier = Modifier.fillMaxWidth().padding(40.dp), contentAlignment = Alignment.Center) {
        Text(message, color = Color.Gray, fontSize = 12.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun TransactionItem(tx: WalletTransactionDto, currency: String) {
    val displayType = when (tx.type) {
        "COMMISSION", "SERVICE_FEE" -> "SERVICE FEE"
        "VOUCHER_PAYMENT", "CREDIT_TOPUP" -> "VOUCHER PAYMENT"
        else -> tx.type.replace("_", " ")
    }
    
    Card(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, Color(0xFFF1F1F1))
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(text = displayType, fontWeight = FontWeight.Black, fontSize = 13.sp, letterSpacing = 0.5.sp)
                Text(text = tx.description ?: "", fontSize = 10.sp, color = Color.Gray, maxLines = 1)
                Text(text = tx.createdAt.take(10), fontSize = 9.sp, color = Color.LightGray)
            }
            Text(
                text = String.format(Locale.getDefault(), "%s%s %.2f", if (tx.amount > 0) "+" else "-", currency, tx.amount),
                fontWeight = FontWeight.Black,
                fontSize = 15.sp,
                color = if (tx.amount > 0) Color(0xFF2E7D32) else Color(0xFFD32F2F)
            )
        }
    }
}

@Composable
fun StatementRow(statement: StatementDto, currency: String, onDownload: (String) -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp).clickable { onDownload(statement.pdfUrl) },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, Color(0xFFF1F1F1))
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(text = "Statement ${statement.periodStart}", fontSize = 13.sp, fontWeight = FontWeight.Black)
                Text(text = "${statement.summary.jobCount} Jobs Completed", fontSize = 10.sp, color = Color.Gray)
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = String.format(Locale.getDefault(), "%s %.2f", currency, statement.summary.netEarnings),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Black,
                    modifier = Modifier.padding(end = 8.dp)
                )
                Icon(Icons.Default.ArrowDropDown, contentDescription = null, tint = Color.Gray)
            }
        }
    }
}

@Composable
fun InvoiceItem(invoice: InvoiceDto, onDownload: (String) -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp).clickable { invoice.pdfUrl?.let { onDownload(it) } },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, Color(0xFFF1F1F1))
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(text = invoice.invoiceNumber, fontSize = 13.sp, fontWeight = FontWeight.Black)
                Text(text = invoice.createdAt.take(10), fontSize = 10.sp, color = Color.Gray)
            }
            Text(
                text = "DOWNLOAD",
                fontSize = 10.sp,
                fontWeight = FontWeight.Black,
                color = Color(0xFF1976D2)
            )
        }
    }
}
