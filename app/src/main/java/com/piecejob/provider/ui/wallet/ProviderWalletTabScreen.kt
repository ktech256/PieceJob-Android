package com.piecejob.provider.ui.wallet

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.piecejob.core.ui.navigation.Screen
import com.piecejob.core.data.remote.dto.*
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProviderWalletTabScreen(
    viewModel: ProviderWalletViewModel = hiltViewModel(),
    onNavigate: (com.piecejob.core.ui.navigation.Screen) -> Unit = {}
) {
    val wallet by viewModel.wallet.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val withdrawSuccess by viewModel.withdrawSuccess.collectAsState()
    val error by viewModel.error.collectAsState()
    val navEvent by viewModel.navigationEvent.collectAsState()
    val currencySymbol by viewModel.currencySymbol.collectAsState()

    LaunchedEffect(navEvent) {
        navEvent?.let {
            onNavigate(it)
            viewModel.resetNavigationEvent()
        }
    }

    var showWithdrawDialog by remember { mutableStateOf(false) }
    var withdrawAmount by remember { mutableStateOf("") }
    
    var showPayServiceFeeDialog by remember { mutableStateOf(false) }
    var voucherNumber by remember { mutableStateOf("") }
    var voucherAmount by remember { mutableStateOf("") }
    var selectedVendor by remember { mutableStateOf("OTT Voucher") }
    var expandedVendorDropdown by remember { mutableStateOf(false) }
    val vendors = listOf("OTT Voucher", "Blue Voucher", "1Voucher")

    if (showWithdrawDialog) {
        AlertDialog(
            onDismissRequest = { showWithdrawDialog = false },
            title = { Text("Cash Out", fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    Text("Available: $currencySymbol${wallet?.balanceMain ?: 0.0}", fontSize = 12.sp, color = Color.Gray)
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = withdrawAmount,
                        onValueChange = { if(it.all { c -> c.isDigit() || c == '.' }) withdrawAmount = it },
                        label = { Text("Amount to Withdraw") },
                        prefix = { Text("$currencySymbol ") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )
                    if (error != null) {
                        Text(text = error!!, color = Color.Red, fontSize = 11.sp, modifier = Modifier.padding(top = 4.dp))
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val amt = withdrawAmount.toDoubleOrNull()
                        if (amt != null) viewModel.requestWithdrawal(amt)
                    },
                    enabled = !isLoading && withdrawAmount.isNotEmpty()
                ) {
                    if (isLoading) CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Color.White)
                    else Text("SUBMIT")
                }
            },
            dismissButton = {
                TextButton(onClick = { showWithdrawDialog = false; viewModel.resetWithdrawState() }) { Text("CANCEL") }
            }
        )
    }

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
                    
                    OutlinedTextField(
                        value = voucherAmount,
                        onValueChange = { if(it.all { c -> c.isDigit() || c == '.' }) voucherAmount = it },
                        label = { Text("Amount") },
                        prefix = { Text("$currencySymbol ") },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("0.00") }
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
                        val amt = voucherAmount.toDoubleOrNull() ?: 0.0
                        viewModel.payServiceFee(vendorCode, voucherNumber, amt)
                        showPayServiceFeeDialog = false
                        voucherNumber = ""
                        voucherAmount = ""
                    },
                    enabled = voucherNumber.isNotBlank() && (voucherAmount.toDoubleOrNull() ?: 0.0) > 0.0,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32))
                ) { Text("SUBMIT PAYMENT") }
            },
            dismissButton = {
                TextButton(onClick = { showPayServiceFeeDialog = false }) { Text("CANCEL") }
            }
        )
    }

    if (withdrawSuccess) {
        LaunchedEffect(Unit) {
            showWithdrawDialog = false
            viewModel.resetWithdrawState()
            withdrawAmount = ""
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize().background(Color(0xFFF8F9FA)),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = "Wallet Dashboard", fontSize = 24.sp, fontWeight = FontWeight.Black)
                Button(
                    onClick = { showWithdrawDialog = true },
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD32F2F)),
                    enabled = (wallet?.balanceMain ?: 0.0) >= 50.0
                ) {
                    Icon(Icons.Default.AccountBalance, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("CASH OUT", fontSize = 12.sp, fontWeight = FontWeight.Black)
                }
            }
        }

        if (isLoading && wallet == null) {
            item { LinearProgressIndicator(modifier = Modifier.fillMaxWidth()) }
        }

        // --- PRIMARY BALANCES ---
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ProviderBalanceCard("Available", "$currencySymbol${wallet?.balanceMain ?: 0.0}", Color(0xFF121212), Modifier.weight(1f))
                    ProviderBalanceCard("Escrow", "$currencySymbol${wallet?.balanceEscrow ?: 0.0}", Color(0xFFFFA000), Modifier.weight(1f))
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ProviderBalanceCard("Credit", "$currencySymbol${wallet?.balanceCredit ?: 0.0}", Color(0xFF2E7D32), Modifier.weight(1f))
                    ProviderBalanceCard("Referral", "$currencySymbol${wallet?.balanceReferral ?: 0.0}", Color(0xFF1976D2), Modifier.weight(1f))
                }
            }
        }

        // --- SERVICE FEE SECTION ---
        item {
            val balance = wallet?.serviceFeeBalance ?: 0.0
            val (statusText, statusColor, displayBalance) = when {
                balance < 0 -> Triple("PLEASE PAY A SERVICE FEE OF", Color(0xFFD32F2F), String.format(Locale.getDefault(), "- %s %.2f", currencySymbol, Math.abs(balance)))
                balance > 0 -> Triple("SERVICE FEE CREDIT", Color(0xFF2E7D32), String.format(Locale.getDefault(), "+ %s %.2f", currencySymbol, balance))
                else -> Triple("SERVICE FEE SETTLED", Color(0xFF1976D2), String.format(Locale.getDefault(), "%s 0.00", currencySymbol))
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
                        Column(modifier = Modifier.weight(1f)) {
                            Text(text = statusText, color = statusColor, fontSize = 13.sp, fontWeight = FontWeight.Black)
                            Text(text = displayBalance, fontSize = 28.sp, fontWeight = FontWeight.Black, color = statusColor)
                        }
                        Button(
                            onClick = { showPayServiceFeeDialog = true },
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF121212)),
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
                        ) {
                            Text("PAY SERVICE FEE", fontSize = 10.sp, fontWeight = FontWeight.Black)
                        }
                    }

                    // Financial Breakdown (from last job)
                    wallet?.lastServiceFeeDetails?.let { details ->
                        Divider(modifier = Modifier.padding(vertical = 12.dp), thickness = 0.5.dp, color = Color(0xFFEEEEEE))
                        
                        BreakdownRow("Service Fee %", "${details.serviceFeePercentage}%")
                        BreakdownRow("Customer Booking Fee Contribution", String.format(Locale.getDefault(), "%s %.2f", currencySymbol, details.bookingFeePaid))
                        BreakdownRow("Negotiated Price", String.format(Locale.getDefault(), "%s %.2f", currencySymbol, details.acceptedPrice))
                        BreakdownRow("Platform Share", String.format(Locale.getDefault(), "%s %.2f", currencySymbol, details.serviceFeeAmount))
                        BreakdownRow("Provider Keeps", String.format(Locale.getDefault(), "%s %.2f", currencySymbol, details.providerKeeps), isHighlight = true)
                        
                        if (balance < 0) {
                            Spacer(Modifier.height(16.dp))
                            Text("Payment methods supported", fontSize = 11.sp, fontWeight = FontWeight.Black, color = Color.Gray)
                            Text("• OTT Voucher\n• Blue Voucher\n• 1Voucher\n• Bank Card", fontSize = 10.sp, color = Color.Gray, lineHeight = 14.sp)
                        }

                    } ?: run {
                        // Placeholder if no last job details
                        Text(
                            text = "No recent job details available.",
                            fontSize = 11.sp,
                            color = Color.LightGray,
                            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                }
            }
        }

        item {
            Text(text = "Wallet Menu", fontSize = 18.sp, fontWeight = FontWeight.Black, modifier = Modifier.padding(top = 16.dp))
        }

        val menuItems = listOf(
            "Recent Transactions" to Screen.RecentTransactions,
            "Payout History" to Screen.ProviderStatements, 
            "Tax Documents" to Screen.ProviderStatements,
            "Invoices" to Screen.ProviderStatements,
            "Statements" to Screen.ProviderStatements
        )

        items(menuItems) { (label, screen) ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = BorderStroke(1.dp, Color(0xFFF1F1F1)),
                onClick = { viewModel.onMenuClick(screen) }
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(text = label, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    Icon(Icons.Default.ChevronRight, contentDescription = null, tint = Color.Gray)
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(80.dp))
        }
    }
}
