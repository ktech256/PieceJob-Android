package com.piecejob.customer.ui.wallet

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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.piecejob.core.data.remote.dto.WalletDto
import com.piecejob.core.data.remote.dto.WalletTransactionDto

@Composable
fun CustomerWalletScreen(
    wallet: WalletDto?,
    history: List<WalletTransactionDto>
) {
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
            color = Color(0xFF410200) // Brand Red
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Balance Cards
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            WalletMiniCard(
                label = "Credit",
                amount = wallet?.balanceCredit ?: 0.0,
                modifier = Modifier.weight(1f)
            )
            WalletMiniCard(
                label = "Referral",
                amount = wallet?.balanceReferral ?: 0.0,
                modifier = Modifier.weight(1f)
            )
            WalletMiniCard(
                label = "Bonus",
                amount = wallet?.balanceBonus ?: 0.0,
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(32.dp))

        Text(
            text = "Transaction History",
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(16.dp))

        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.weight(1f)
        ) {
            items(history) { tx ->
                TransactionRow(tx)
            }
            if (history.isEmpty()) {
                item {
                    Box(modifier = Modifier.fillMaxWidth().padding(40.dp), contentAlignment = Alignment.Center) {
                        Text("No transactions yet.", color = Color.Gray)
                    }
                }
            }
        }
    }
}

@Composable
fun WalletMiniCard(label: String, amount: Double, modifier: Modifier) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = Color(0xFFF5F5F5)),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(text = label, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.Gray)
            Text(text = "$${String.format("%.2f", amount)}", fontSize = 16.sp, fontWeight = FontWeight.Black)
        }
    }
}

@Composable
fun TransactionRow(tx: WalletTransactionDto) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(text = tx.type.replace("_", " "), fontSize = 14.sp, fontWeight = FontWeight.Bold)
            Text(text = tx.createdAt, fontSize = 10.sp, color = Color.Gray)
        }
        Text(
            text = "$${String.format("%.2f", tx.amount)}",
            fontSize = 16.sp,
            fontWeight = FontWeight.Black,
            color = if (tx.amount > 0) Color(0xFF2E7D32) else Color.Red
        )
    }
}
