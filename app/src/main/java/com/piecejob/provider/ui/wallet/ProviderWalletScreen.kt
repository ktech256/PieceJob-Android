package com.piecejob.provider.ui.wallet

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

// Provider Theme Colors (Aligned with Section 2.1)
val ForestGreen = Color(0xFF006400)
val CadetGray = Color(0xFF91A3B0)

@Composable
fun ProviderWalletScreen(
    wallet: WalletDto,
    transactions: List<TransactionItem>,
    onWithdrawClick: () -> Unit
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
            color = ForestGreen
        )
        
        Spacer(modifier = Modifier.height(24.dp))

        // Balance Cards
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            BalanceCard(
                title = "Available",
                amount = wallet.balanceMain,
                containerColor = ForestGreen,
                contentColor = Color.White,
                modifier = Modifier.weight(1f)
            )
            BalanceCard(
                title = "Escrow",
                amount = wallet.balanceEscrow,
                containerColor = CadetGray.copy(alpha = 0.2f),
                contentColor = ForestGreen,
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = onWithdrawClick,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = ForestGreen),
            shape = RoundedCornerShape(8.dp)
        ) {
            Text("Request Withdrawal", fontWeight = FontWeight.Bold)
        }

        Spacer(modifier = Modifier.height(32.dp))

        Text(
            text = "Recent Transactions",
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(16.dp))

        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.weight(1f)
        ) {
            items(transactions) { tx ->
                TransactionRow(tx)
            }
        }
    }
}

@Composable
fun BalanceCard(
    title: String,
    amount: Double,
    containerColor: Color,
    contentColor: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = containerColor),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(text = title, fontSize = 12.sp, color = contentColor.copy(alpha = 0.7f))
            Text(
                text = "$${String.format("%.2f", amount)}",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = contentColor
            )
        }
    }
}

@Composable
fun TransactionRow(tx: TransactionItem) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(text = tx.type, fontWeight = FontWeight.Medium)
            Text(text = tx.date, fontSize = 12.sp, color = Color.Gray)
        }
        Text(
            text = "${if (tx.isCredit) "+" else "-"}$${String.format("%.2f", tx.amount)}",
            fontWeight = FontWeight.Bold,
            color = if (tx.isCredit) Color(0xFF2E7D32) else Color.Red
        )
    }
}

data class TransactionItem(
    val type: String,
    val amount: Double,
    val date: String,
    val isCredit: Boolean
)
