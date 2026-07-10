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
fun ProviderBalanceCard(title: String, amount: String, color: Color, modifier: Modifier, subTitle: String? = null) {
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
            if (subTitle != null) {
                Text(text = subTitle, fontSize = 8.sp, color = color.copy(alpha = 0.7f), fontWeight = FontWeight.Bold)
            }
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
fun ServiceFeeTable(records: List<com.piecejob.core.data.remote.dto.RecentServiceFeeDto>, currency: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
    ) {
        // Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("JOB ID", modifier = Modifier.weight(1.2f), fontSize = 10.sp, fontWeight = FontWeight.Black, color = Color.Gray)
            Text("DATE", modifier = Modifier.weight(0.8f), fontSize = 10.sp, fontWeight = FontWeight.Black, color = Color.Gray)
            Text("TOTAL", modifier = Modifier.weight(1f), fontSize = 10.sp, fontWeight = FontWeight.Black, color = Color.Gray, textAlign = androidx.compose.ui.text.style.TextAlign.End)
            Text("FEE", modifier = Modifier.weight(1f), fontSize = 10.sp, fontWeight = FontWeight.Black, color = Color.Gray, textAlign = androidx.compose.ui.text.style.TextAlign.End)
            Text("STATUS", modifier = Modifier.weight(1.3f), fontSize = 10.sp, fontWeight = FontWeight.Black, color = Color.Gray, textAlign = androidx.compose.ui.text.style.TextAlign.End)
        }
        
        HorizontalDivider(thickness = 0.5.dp, color = Color(0xFFEEEEEE))

        records.forEach { record ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "PJ-${record.jobId.takeLast(4).uppercase()}",
                    modifier = Modifier.weight(1.2f),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = try {
                        val parts = record.date.split("T")[0].split("-")
                        val day = parts[2]
                        val month = when(parts[1]) {
                            "01" -> "Jan"
                            "02" -> "Feb"
                            "03" -> "Mar"
                            "04" -> "Apr"
                            "05" -> "May"
                            "06" -> "Jun"
                            "07" -> "Jul"
                            "08" -> "Aug"
                            "09" -> "Sep"
                            "10" -> "Oct"
                            "11" -> "Nov"
                            "12" -> "Dec"
                            else -> parts[1]
                        }
                        "$day $month"
                    } catch (e: Exception) {
                        record.date.take(10)
                    },
                    modifier = Modifier.weight(0.8f),
                    fontSize = 11.sp,
                    color = Color.Gray
                )
                Text(
                    text = if (record.acceptedPrice > 0) String.format(Locale.getDefault(), "%s%.0f", currency, record.acceptedPrice) else "N/A",
                    modifier = Modifier.weight(1f),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = androidx.compose.ui.text.style.TextAlign.End
                )
                Text(
                    text = if (record.acceptedPrice > 0) String.format(Locale.getDefault(), "%s%.0f", currency, record.originalFee) else "N/A",
                    modifier = Modifier.weight(1f),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Black,
                    color = Color(0xFF121212),
                    textAlign = androidx.compose.ui.text.style.TextAlign.End
                )
                
                val (displayText, statusColor) = when(record.status) {
                    "PAID" -> "PAID" to Color(0xFF2E7D32)
                    "PARTIAL" -> "PARTIAL" to Color(0xFFFFA000)
                    "WAIVED" -> "WAIVED" to Color(0xFF1976D2)
                    "OUTSTANDING" -> "UNPAID" to Color(0xFFD32F2F)
                    else -> record.status to Color.Gray
                }
                
                Surface(
                    color = statusColor.copy(alpha = 0.1f),
                    shape = RoundedCornerShape(4.dp),
                    modifier = Modifier.weight(1.3f).wrapContentWidth(Alignment.End)
                ) {
                    Text(
                        text = displayText,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Black,
                        color = statusColor,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }
            }
            HorizontalDivider(thickness = 0.5.dp, color = Color(0xFFF5F5F5))
        }
    }
}

@Composable
fun EmptyState(message: String) {
    Box(modifier = Modifier.fillMaxWidth().padding(40.dp), contentAlignment = Alignment.Center) {
        Text(message, color = Color.Gray, fontSize = 12.sp, fontWeight = FontWeight.Bold)
    }
}
