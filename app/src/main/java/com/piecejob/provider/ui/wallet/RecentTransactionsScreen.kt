package com.piecejob.provider.ui.wallet

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
fun RecentTransactionsScreen(
    viewModel: ProviderWalletViewModel = hiltViewModel(),
    onBack: () -> Unit
) {
    val transactions by viewModel.transactions.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val currencySymbol by viewModel.currencySymbol.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Recent Transactions", fontSize = 18.sp, fontWeight = FontWeight.Black) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
            )
        }
    ) { padding ->
        Box(modifier = Modifier.padding(padding).fillMaxSize().background(Color(0xFFF8F9FA))) {
            if (isLoading && transactions.isEmpty()) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            } else if (transactions.isEmpty()) {
                EmptyState("No transactions found.")
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Display latest 25 transactions only
                    items(transactions.take(25)) { tx ->
                        ProviderTransactionRow(tx, currencySymbol)
                    }
                    
                    item {
                        if (transactions.size > 25) {
                            Text(
                                text = "Showing latest 25 transactions",
                                modifier = Modifier.fillMaxWidth().padding(16.dp),
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                fontSize = 12.sp,
                                color = Color.Gray
                            )
                        }
                        Spacer(modifier = Modifier.height(32.dp))
                    }
                }
            }
        }
    }
}
