package com.piecejob.provider.ui.wallet

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.piecejob.core.data.remote.dto.WalletTransactionDto
import java.util.Locale

@Composable
fun ProviderTransactionRow(tx: WalletTransactionDto, currency: String) {
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
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(text = displayType, fontWeight = FontWeight.Black, fontSize = 13.sp, letterSpacing = 0.5.sp)
                Text(text = tx.description ?: "", fontSize = 10.sp, color = Color.Gray, maxLines = 1)
                Text(text = tx.createdAt.take(10), color = Color.LightGray, fontSize = 9.sp)
            }
            Text(
                text = String.format(Locale.getDefault(), "%s%s %.2f", if (tx.amount >= 0) "+" else "-", currency, Math.abs(tx.amount)),
                fontWeight = FontWeight.Black,
                fontSize = 15.sp,
                color = if (tx.amount >= 0) Color(0xFF2E7D32) else Color(0xFFD32F2F)
            )
        }
    }
}

@Composable
fun ProviderBalanceCard(title: String, amount: String, color: Color, modifier: Modifier) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, Color(0xFFEEEEEE))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(text = title.uppercase(), fontSize = 9.sp, color = Color.Gray, fontWeight = FontWeight.Black, letterSpacing = 1.sp)
            Spacer(Modifier.height(4.dp))
            Text(text = amount, fontSize = 18.sp, fontWeight = FontWeight.Black, color = color)
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
